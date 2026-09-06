package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.footprint

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveSize
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.abs
import kotlin.math.floor

object BioluminescentFootprintPlacement {
    const val MAXIMUM_CONFIGURED_RADIUS = 64

    private const val MAX_SURFACE_DELTA = 0.55
    private const val SURFACE_SAMPLE_EPSILON = 1.0e-4
    private const val SHAPE_EDGE_EPSILON = 0.015
    private const val SUPPORT_HEIGHT_EPSILON = 0.025
    private const val COLOR_VARIATION_SALT = 0x243F6A8885A308D3L
    private const val COLOR_CYCLE_STEPS = 36.0
    private const val COLOR_HIGHLIGHT_MIX = 0.10

    data class HorizontalOffset(
        val x: Int,
        val z: Int,
        val distanceSquared: Int
    )

    data class ZoneProximity(
        val zone: BioluminescentZone,
        val distanceSquared: Int,
        val radius: Int
    )

    private val proximityOffsets = buildList {
        for (offsetX in -MAXIMUM_CONFIGURED_RADIUS..MAXIMUM_CONFIGURED_RADIUS) {
            for (offsetZ in -MAXIMUM_CONFIGURED_RADIUS..MAXIMUM_CONFIGURED_RADIUS) {
                val distanceSquared = offsetX * offsetX + offsetZ * offsetZ
                if (distanceSquared <= MAXIMUM_CONFIGURED_RADIUS * MAXIMUM_CONFIGURED_RADIUS) {
                    add(HorizontalOffset(offsetX, offsetZ, distanceSquared))
                }
            }
        }
    }.sortedWith(
        compareBy<HorizontalOffset> { offset -> offset.distanceSquared }
            .thenBy { offset -> offset.x }
            .thenBy { offset -> offset.z }
    )

    fun continuousFootprintColor(
        zone: BioluminescentZone,
        player: Player,
        stepIndex: Long
    ): Int {
        val phaseSeed = RandomSupport.mixStafford13(
            zone.zoneSeed xor player.uuid.mostSignificantBits xor
                player.uuid.leastSignificantBits xor COLOR_VARIATION_SALT
        )
        val rawPhase = ModUtilities.stableUnitValue(phaseSeed) + stepIndex.toDouble() / COLOR_CYCLE_STEPS
        val phase = rawPhase - floor(rawPhase)
        val paletteProgress = phase * 3.0
        val segment = floor(paletteProgress).toInt().coerceIn(0, 2)
        val blend = ModUtilities.smooth(0.0, 1.0, paletteProgress - segment)
        val palette = zone.palette
        val fromColor: Int
        val toColor: Int
        when (segment) {
            0 -> {
                fromColor = palette.firstColor
                toColor = palette.secondColor
            }
            1 -> {
                fromColor = palette.secondColor
                toColor = palette.accentColor
            }
            else -> {
                fromColor = palette.accentColor
                toColor = palette.firstColor
            }
        }
        val interpolated = ModUtilities.lerpColor(fromColor, toColor, blend)
        return ModUtilities.lerpColor(interpolated, palette.highlightColor, COLOR_HIGHLIGHT_MIX)
    }

    fun nearestEligibleZone(
        zones: List<BioluminescentZone>,
        worldX: Int,
        worldZ: Int
    ): ZoneProximity? {
        var bestProximity: ZoneProximity? = null
        var bestNormalizedDistanceSquared = Double.POSITIVE_INFINITY
        for (zone in zones) {
            val domain = zone.spatialData?.domain ?: continue
            val radius = configuredRadius(zone.preset.size)
            val distanceSquared = nearestLocalWaterDistanceSquared(
                domain,
                worldX,
                worldZ,
                radius
            ) ?: continue
            val normalizedDistanceSquared = distanceSquared.toDouble() / (radius * radius)
            if (normalizedDistanceSquared < bestNormalizedDistanceSquared) {
                bestNormalizedDistanceSquared = normalizedDistanceSquared
                bestProximity = ZoneProximity(zone, distanceSquared, radius)
                if (distanceSquared == 0) break
            }
        }
        return bestProximity
    }

    private fun configuredRadius(size: BioluminescenceWaveSize): Int {
        val configured = when (size) {
            BioluminescenceWaveSize.SMALL -> ModConfig.bioluminescenceFootprintSmallRadius
            BioluminescenceWaveSize.LARGE -> ModConfig.bioluminescenceFootprintLargeRadius
        }
        return configured.coerceIn(1, MAXIMUM_CONFIGURED_RADIUS)
    }

    private fun nearestLocalWaterDistanceSquared(
        domain: BioluminescentWaterDomain,
        worldX: Int,
        worldZ: Int,
        radius: Int
    ): Int? {
        val bounds = domain.bounds
        val boundsDistanceX = when {
            worldX < bounds.minX -> bounds.minX - worldX
            worldX > bounds.maxX -> worldX - bounds.maxX
            else -> 0
        }
        val boundsDistanceZ = when {
            worldZ < bounds.minZ -> bounds.minZ - worldZ
            worldZ > bounds.maxZ -> worldZ - bounds.maxZ
            else -> 0
        }
        val radiusSquared = radius * radius
        if (boundsDistanceX * boundsDistanceX + boundsDistanceZ * boundsDistanceZ > radiusSquared) return null

        for (offset in proximityOffsets) {
            if (offset.distanceSquared > radiusSquared) break
            val cellIndex = domain.cellIndexAt(worldX + offset.x, worldZ + offset.z) ?: continue
            if (domain.isLocalCell(cellIndex)) return offset.distanceSquared
        }
        return null
    }

    fun resolveWalkedSurface(
        level: ClientLevel,
        worldX: Double,
        sampledSurfaceY: Double,
        worldZ: Double
    ): Double? {
        val blockX = floor(worldX).toInt()
        val blockZ = floor(worldZ).toInt()
        if (!ModUtilities.hasLoadedChunk(level, blockX shr 4, blockZ shr 4)) return null

        val localX = worldX - blockX
        val localZ = worldZ - blockZ
        val startY = floor(sampledSurfaceY - SURFACE_SAMPLE_EPSILON).toInt()
        val supportPos = BlockPos.MutableBlockPos()
        var bestSurface: Double? = null
        var bestDelta = Double.POSITIVE_INFINITY
        for (blockY in startY downTo startY - 2) {
            supportPos.set(blockX, blockY, blockZ)
            val collisionShape = level.getBlockState(supportPos).getCollisionShape(level, supportPos)
            if (collisionShape.isEmpty) continue
            for (box in collisionShape.toAabbs()) {
                if (
                    localX < box.minX - SHAPE_EDGE_EPSILON ||
                    localX > box.maxX + SHAPE_EDGE_EPSILON ||
                    localZ < box.minZ - SHAPE_EDGE_EPSILON ||
                    localZ > box.maxZ + SHAPE_EDGE_EPSILON
                ) continue
                val surface = blockY + box.maxY
                val delta = abs(surface - sampledSurfaceY)
                if (delta <= MAX_SURFACE_DELTA && delta < bestDelta) {
                    bestSurface = surface
                    bestDelta = delta
                }
            }
        }
        return bestSurface
    }

    fun hasCompleteFootprintSupport(
        level: ClientLevel,
        centerX: Double,
        surfaceY: Double,
        centerZ: Double,
        forwardX: Double,
        forwardZ: Double
    ): Boolean {
        val rightX = -forwardZ
        val rightZ = forwardX
        for (lengthFactor in -1..1) {
            for (widthFactor in -1..1) {
                if (lengthFactor == 0 && widthFactor == 0) continue
                val sampleX = centerX +
                    forwardX * BioluminescentFootprint.HALF_LENGTH * lengthFactor +
                    rightX * BioluminescentFootprint.HALF_WIDTH * widthFactor
                val sampleZ = centerZ +
                    forwardZ * BioluminescentFootprint.HALF_LENGTH * lengthFactor +
                    rightZ * BioluminescentFootprint.HALF_WIDTH * widthFactor
                val supportedSurface = resolveWalkedSurface(level, sampleX, surfaceY, sampleZ) ?: return false
                if (abs(supportedSurface - surfaceY) > SUPPORT_HEIGHT_EPSILON) return false
            }
        }
        return true
    }
}
