package fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceBounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceServerSettings
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BiomeTags
import net.minecraft.world.level.levelgen.Heightmap
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.UUID


class BioluminescenceBeachResolver(
    private val level: ServerLevel
) {
    data class BeachZone(
        val id: UUID,
        val waterSurface: BlockPos,
        val bounds: BioluminescenceBounds
    )

    data class Diagnostic(
        val position: BlockPos,
        val dimensionSupported: Boolean,
        val biomeId: Identifier?,
        val isBeachBiome: Boolean,
        val waterSurface: BlockPos?,
        val hasEnoughNearbyWater: Boolean?,
        val zone: BeachZone?,
        val playerDetectionCacheHit: Boolean,
        val cellZoneCacheHit: Boolean,
        val failureReason: String?
    )

    private data class CachedZone(
        val zone: BeachZone,
        val expiresAtGameTime: Long
    )

    private data class CachedDetection(
        val position: Long,
        val zone: BeachZone?,
        val expiresAtGameTime: Long
    )

    companion object {
        private const val GRID_SIZE = 4
        private const val MAX_COMPONENT_CELLS = 4096
        private const val MAX_EXAMINED_CELLS = 16384
        private const val WATER_LEVEL_TOLERANCE = 3
        private const val CACHE_LIFETIME_TICKS = 200L
        private const val DETECTION_CACHE_LIFETIME_TICKS = 200L
        private const val CACHE_CLEANUP_INTERVAL_TICKS = 200L

        private val CARDINAL_OFFSETS = arrayOf(
            1 to 0,
            -1 to 0,
            0 to 1,
            0 to -1
        )
    }

    private val cachedZonesByCell: MutableMap<Long, CachedZone> = hashMapOf()
    private val cachedDetectionsByPlayer: MutableMap<UUID, CachedDetection> = hashMapOf()
    private var nextCleanupGameTime = 0L
    private val connectedCellKeys: MutableSet<Long> = linkedSetOf()

    fun resolve(player: ServerPlayer, settings: BioluminescenceServerSettings): BeachZone? {
        val gameTime = level.gameTime
        val position = player.blockPosition()
        cachedDetectionsByPlayer[player.uuid]
            ?.takeIf { cached ->
                cached.position == position.asLong() && cached.expiresAtGameTime >= gameTime
            }
            ?.let { cached -> return cached.zone }

        val zone = resolveUncached(position, settings)
        cachedDetectionsByPlayer[player.uuid] = CachedDetection(
            position.asLong(),
            zone,
            gameTime + DETECTION_CACHE_LIFETIME_TICKS
        )
        cleanExpired(gameTime)
        return zone
    }

    private fun resolveUncached(
        playerPosition: BlockPos,
        settings: BioluminescenceServerSettings
    ): BeachZone? {
        if (!ModUtilities.isOverworldLikeDimension(level)) return null
        if (!level.getBiome(playerPosition).`is`(BiomeTags.IS_BEACH)) return null

        val waterSurface = ModUtilities.findNearbyRenderableWaterSurface(
            level,
            playerPosition,
            settings.waterSearchRadius,
            settings.waterSearchRadius
        ) { surface -> level.getBiome(surface).`is`(BiomeTags.IS_BEACH) } ?: return null

        if (!hasEnoughNearbyWater(waterSurface.x, waterSurface.z, settings.minimumNearbyWaterCells)) return null

        val startCellX = Math.floorDiv(waterSurface.x, GRID_SIZE)
        val startCellZ = Math.floorDiv(waterSurface.z, GRID_SIZE)
        val startKey = ModUtilities.horizontalPositionKey(startCellX, startCellZ)
        val gameTime = level.gameTime
        cachedZonesByCell[startKey]
            ?.takeIf { cached -> cached.expiresAtGameTime >= gameTime }
            ?.let { cached -> return cached.zone.copy(waterSurface = waterSurface) }

        val zone = resolveConnectedZone(startCellX, startCellZ)
        val cached = CachedZone(zone, gameTime + CACHE_LIFETIME_TICKS)
        for (cellKey in connectedCellKeys) cachedZonesByCell[cellKey] = cached
        return zone.copy(waterSurface = waterSurface)
    }

    val cachedZoneCount: Int
        get() = cachedZonesByCell.size

    val cachedDetectionCount: Int
        get() = cachedDetectionsByPlayer.size

    fun diagnose(
        playerId: UUID,
        position: BlockPos,
        settings: BioluminescenceServerSettings
    ): Diagnostic {
        val gameTime = level.gameTime
        val detectionCacheHit = cachedDetectionsByPlayer[playerId]
            ?.let { cached -> cached.position == position.asLong() && cached.expiresAtGameTime >= gameTime }
            ?: false

        if (!ModUtilities.isOverworldLikeDimension(level)) {
            return Diagnostic(
                position, false, null, false, null, null, null,
                detectionCacheHit, false, "dimension_non_compatible"
            )
        }

        val biomeHolder = level.getBiome(position)
        val biomeId = biomeHolder.unwrapKey().map { key -> key.identifier() }.orElse(null)
        val isBeachBiome = biomeHolder.`is`(BiomeTags.IS_BEACH)
        if (!isBeachBiome) {
            return Diagnostic(
                position, true, biomeId, false, null, null, null,
                detectionCacheHit, false, "biome_pas_une_plage"
            )
        }

        val waterSurface = ModUtilities.findNearbyRenderableWaterSurface(
            level,
            position,
            settings.waterSearchRadius,
            settings.waterSearchRadius
        ) { surface -> level.getBiome(surface).`is`(BiomeTags.IS_BEACH) }
        if (waterSurface == null) {
            return Diagnostic(
                position, true, biomeId, true, null, null, null,
                detectionCacheHit, false, "aucune_surface_eau_a_proximite"
            )
        }

        val enoughWater = hasEnoughNearbyWater(waterSurface.x, waterSurface.z, settings.minimumNearbyWaterCells)
        if (!enoughWater) {
            return Diagnostic(
                position, true, biomeId, true, waterSurface, false, null,
                detectionCacheHit, false, "pas_assez_de_cellules_eau_proches"
            )
        }

        val startCellX = Math.floorDiv(waterSurface.x, GRID_SIZE)
        val startCellZ = Math.floorDiv(waterSurface.z, GRID_SIZE)
        val startKey = ModUtilities.horizontalPositionKey(startCellX, startCellZ)
        val cachedZone = cachedZonesByCell[startKey]?.takeIf { cached -> cached.expiresAtGameTime >= gameTime }
        val zone = (cachedZone?.zone ?: resolveConnectedZone(startCellX, startCellZ))
            .copy(waterSurface = waterSurface)

        return Diagnostic(
            position, true, biomeId, true, waterSurface, true, zone,
            detectionCacheHit, cachedZone != null, null
        )
    }

    fun clear() {
        cachedZonesByCell.clear()
        cachedDetectionsByPlayer.clear()
        connectedCellKeys.clear()
    }


    private fun resolveConnectedZone(startCellX: Int, startCellZ: Int): BeachZone {
        connectedCellKeys.clear()
        val visitedCellKeys = hashSetOf<Long>()
        val pending = ArrayDeque<Pair<Int, Int>>()
        pending.add(startCellX to startCellZ)
        visitedCellKeys.add(ModUtilities.horizontalPositionKey(startCellX, startCellZ))

        var minimumCellX = startCellX
        var minimumCellZ = startCellZ
        var maximumCellX = startCellX
        var maximumCellZ = startCellZ
        var canonicalCellX = Int.MAX_VALUE
        var canonicalCellZ = Int.MAX_VALUE
        var canonicalCellY = 0
        var examinedCells = 0

        while (pending.isNotEmpty() &&
            connectedCellKeys.size < MAX_COMPONENT_CELLS &&
            examinedCells < MAX_EXAMINED_CELLS
        ) {
            val (cellX, cellZ) = pending.removeFirst()
            examinedCells++
            val cellWaterY = compatibleCoastalCellHeight(cellX, cellZ)
            if (cellWaterY == null) continue

            connectedCellKeys.add(ModUtilities.horizontalPositionKey(cellX, cellZ))
            minimumCellX = minOf(minimumCellX, cellX)
            minimumCellZ = minOf(minimumCellZ, cellZ)
            maximumCellX = maxOf(maximumCellX, cellX)
            maximumCellZ = maxOf(maximumCellZ, cellZ)
            if (cellX < canonicalCellX || (cellX == canonicalCellX && cellZ < canonicalCellZ)) {
                canonicalCellX = cellX
                canonicalCellZ = cellZ
                canonicalCellY = cellWaterY
            }

            for ((offsetX, offsetZ) in CARDINAL_OFFSETS) {
                val neighborX = cellX + offsetX
                val neighborZ = cellZ + offsetZ
                val neighborKey = ModUtilities.horizontalPositionKey(neighborX, neighborZ)
                if (visitedCellKeys.add(neighborKey)) pending.add(neighborX to neighborZ)
            }
        }

        val dimension = level.dimension().identifier()
        val serializedId = "$dimension:$canonicalCellX:$canonicalCellZ:${Math.floorDiv(canonicalCellY, GRID_SIZE)}"
        val beachId = UUID.nameUUIDFromBytes(serializedId.toByteArray(StandardCharsets.UTF_8))
        val bounds = BioluminescenceBounds(
            minimumCellX * GRID_SIZE,
            minimumCellZ * GRID_SIZE,
            maximumCellX * GRID_SIZE + GRID_SIZE - 1,
            maximumCellZ * GRID_SIZE + GRID_SIZE - 1
        )
        return BeachZone(
            beachId,
            BlockPos(canonicalCellX * GRID_SIZE, canonicalCellY, canonicalCellZ * GRID_SIZE),
            bounds
        )
    }

    /**
     * Purely intrinsic to this cell's own column: uses the world-surface heightmap as a local
     * search hint, never an external reference height. Two calls for the same cell always agree.
     */
    private fun compatibleCoastalCellHeight(cellX: Int, cellZ: Int): Int? {
        val worldX = cellX * GRID_SIZE + GRID_SIZE / 2
        val worldZ = cellZ * GRID_SIZE + GRID_SIZE / 2
        if (!ModUtilities.hasLoadedChunk(level, worldX shr 4, worldZ shr 4)) return null
        val surfaceHint = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1
        val water = ModUtilities.findWaterBlockBelow(
            level,
            worldX,
            worldZ,
            minOf(level.maxY - 1, surfaceHint + WATER_LEVEL_TOLERANCE),
            maxOf(level.minY, surfaceHint - WATER_LEVEL_TOLERANCE)
        ) ?: return null
        val surface = ModUtilities.findTopWaterBlock(level, water) ?: return null
        if (!ModUtilities.isRenderableWaterSurface(level, surface)) return null
        if (!level.getBiome(surface).`is`(BiomeTags.IS_BEACH)) return null
        return surface.y
    }

    private fun hasEnoughNearbyWater(anchorX: Int, anchorZ: Int, requiredCells: Int): Boolean {
        var compatibleCells = 0
        for (offsetX in -4..4) {
            for (offsetZ in -4..4) {
                val worldX = anchorX + offsetX
                val worldZ = anchorZ + offsetZ
                if (!ModUtilities.hasLoadedChunk(level, worldX shr 4, worldZ shr 4)) continue
                val surfaceHint = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1
                val water = ModUtilities.findWaterBlockBelow(
                    level,
                    worldX,
                    worldZ,
                    minOf(level.maxY - 1, surfaceHint + WATER_LEVEL_TOLERANCE),
                    maxOf(level.minY, surfaceHint - WATER_LEVEL_TOLERANCE)
                ) ?: continue
                val surface = ModUtilities.findTopWaterBlock(level, water) ?: continue
                if (!ModUtilities.isRenderableWaterSurface(level, surface)) continue
                if (!level.getBiome(surface).`is`(BiomeTags.IS_BEACH)) continue
                compatibleCells++
                if (compatibleCells >= requiredCells) return true
            }
        }
        return false
    }

    fun cleanExpired(gameTime: Long = level.gameTime) {
        if (gameTime < nextCleanupGameTime) return
        nextCleanupGameTime = gameTime + CACHE_CLEANUP_INTERVAL_TICKS
        cachedZonesByCell.entries.removeIf { (_, cached) -> cached.expiresAtGameTime < gameTime }
        cachedDetectionsByPlayer.entries.removeIf { (_, cached) -> cached.expiresAtGameTime < gameTime }
    }
}
