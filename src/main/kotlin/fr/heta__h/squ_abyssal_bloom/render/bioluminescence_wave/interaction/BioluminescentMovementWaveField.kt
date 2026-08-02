package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterCell
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationResult
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.entity.animal.squid.Squid
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.phys.AABB
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

class BioluminescentMovementWaveField {
    companion object {
        const val SCAN_INTERVAL_TICKS = 2L
        const val DUPLICATE_WINDOW_TICKS = 2L
        const val MAX_MOVING_SOURCES = 16
        const val MAX_ACTIVE_WAVES = 96
        const val WATER_CELL_SEARCH_RADIUS = 0
        const val HORIZONTAL_SEARCH_MARGIN = 0.5
        const val VERTICAL_SEARCH_RANGE = 4.0
        const val BOAT_SURFACE_TOLERANCE = 1.75
        const val DUPLICATE_DISTANCE = 0.6
        const val MIN_MOVEMENT_SPEED = 0.012
        const val FULL_MOVEMENT_SPEED = 0.22
        const val MIN_RADIUS = 6.0
        const val MAX_RADIUS = 11.0
        const val VISIBILITY_TICK_DT = 1.0 / 20.0
        const val VISIBILITY_FADE_RATE = 4.0
    }

    private val waves = ArrayDeque<BioluminescentMovementWave>()
    private var lastScanTick = Long.MIN_VALUE

    var movingSourceCount: Int = 0
        private set

    var visibilityStrength: Double = 0.0
        private set

    val activeWaveCount: Int
        get() = waves.size

    fun tick(level: ClientLevel, data: BioluminescentZoneGenerationResult, gameTime: Long) {
        if (lastScanTick != Long.MIN_VALUE && gameTime < lastScanTick) clear()
        removeExpiredWaves(gameTime)
        val visibilityTarget = if (hasVisibleWaves(gameTime.toDouble())) 1.0 else 0.0
        visibilityStrength = ModUtilities.smoothTowards(
            visibilityStrength,
            visibilityTarget,
            VISIBILITY_TICK_DT,
            VISIBILITY_FADE_RATE
        ).coerceIn(0.0, 1.0)
        if (lastScanTick != Long.MIN_VALUE && gameTime - lastScanTick < SCAN_INTERVAL_TICKS) return
        lastScanTick = gameTime

        val bounds = data.domain.bounds
        val searchBounds = AABB(
            bounds.minX - HORIZONTAL_SEARCH_MARGIN,
            bounds.minY - VERTICAL_SEARCH_RANGE,
            bounds.minZ - HORIZONTAL_SEARCH_MARGIN,
            bounds.maxX + 1.0 + HORIZONTAL_SEARCH_MARGIN,
            bounds.maxY + 1.0 + VERTICAL_SEARCH_RANGE,
            bounds.maxZ + 1.0 + HORIZONTAL_SEARCH_MARGIN
        )
        val candidates = level.getEntitiesOfClass(Entity::class.java, searchBounds) { entity ->
            entity.isAlive && !entity.isSpectator && !entity.isPassenger &&
                entity !is AbstractFish && entity !is Squid &&
                (entity.isInWater || entity is AbstractBoat)
        }

        var acceptedSources = 0
        for (entity in candidates) {
            if (acceptedSources >= MAX_MOVING_SOURCES) break
            val speed = horizontalSpeed(entity)
            if (speed < MIN_MOVEMENT_SPEED) continue
            val waterCell = nearestWaterCell(data.domain, entity.x, entity.z) ?: continue
            if (!isValidWaterSource(entity, waterCell)) continue

            acceptedSources++
            emitWave(entity.x, entity.z, speed, gameTime)
        }
        movingSourceCount = acceptedSources
    }

    fun intensityAt(worldX: Double, worldZ: Double, renderGameTime: Double): Float {
        var intensity = 0.0
        for (wave in waves) {
            intensity = max(intensity, wave.intensityAt(worldX, worldZ, renderGameTime))
            if (intensity >= 0.999) break
        }
        return intensity.toFloat().coerceIn(0.0f, 1.0f)
    }

    fun affects(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Boolean {
        return waves.any { wave ->
            wave.affects(minX, minZ, maxX, maxZ, renderGameTime)
        }
    }

    fun hasVisibleWaves(renderGameTime: Double): Boolean {
        return waves.any { !it.isExpiredAt(renderGameTime.toLong()) }
    }

    fun clear() {
        waves.clear()
        movingSourceCount = 0
        visibilityStrength = 0.0
        lastScanTick = Long.MIN_VALUE
    }

    private fun removeExpiredWaves(gameTime: Long) {
        waves.removeIf { wave -> wave.isExpiredAt(gameTime) }
    }

    private fun emitWave(worldX: Double, worldZ: Double, speed: Double, gameTime: Long) {
        val recentDuplicate = waves.any { wave ->
            gameTime - wave.startedAt <= DUPLICATE_WINDOW_TICKS &&
                ModUtilities.horizontalDistanceSqr(wave.originX, wave.originZ, worldX, worldZ) <=
                DUPLICATE_DISTANCE * DUPLICATE_DISTANCE
        }
        if (recentDuplicate) return
        while (waves.size >= MAX_ACTIVE_WAVES) waves.removeFirst()

        val movement = ModUtilities.smooth(MIN_MOVEMENT_SPEED, FULL_MOVEMENT_SPEED, speed)
        waves.addLast(
            BioluminescentMovementWave(
                worldX,
                worldZ,
                gameTime,
                1.0,
                MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * movement
            )
        )
    }

    private fun nearestWaterCell(
        domain: BioluminescentWaterDomain,
        worldX: Double,
        worldZ: Double
    ): BioluminescentWaterCell? {
        val blockX = floor(worldX).toInt()
        val blockZ = floor(worldZ).toInt()
        var nearest: BioluminescentWaterCell? = null
        var nearestDistanceSqr = Double.POSITIVE_INFINITY
        for (offsetX in -WATER_CELL_SEARCH_RADIUS..WATER_CELL_SEARCH_RADIUS) {
            for (offsetZ in -WATER_CELL_SEARCH_RADIUS..WATER_CELL_SEARCH_RADIUS) {
                val index = domain.cellIndexAt(blockX + offsetX, blockZ + offsetZ) ?: continue
                if (!domain.isLocalCell(index)) continue
                val cell = domain.cells[index]
                val cellDistanceSqr = ModUtilities.horizontalDistanceSqr(
                    cell.waterPos.x + 0.5,
                    cell.waterPos.z + 0.5,
                    worldX,
                    worldZ
                )
                if (cellDistanceSqr >= nearestDistanceSqr) continue
                nearestDistanceSqr = cellDistanceSqr
                nearest = cell
            }
        }
        return nearest
    }

    private fun isValidWaterSource(entity: Entity, waterCell: BioluminescentWaterCell): Boolean {
        return if (entity is AbstractBoat) {
            abs(entity.boundingBox.minY - waterCell.surfaceY) <= BOAT_SURFACE_TOLERANCE
        } else {
            entity.isInWater && abs(entity.y - waterCell.surfaceY) <= VERTICAL_SEARCH_RANGE
        }
    }

    private fun horizontalSpeed(entity: Entity): Double {
        val positionDeltaX = entity.x - entity.xo
        val positionDeltaZ = entity.z - entity.zo
        val movement = entity.deltaMovement
        return max(
            sqrt(positionDeltaX * positionDeltaX + positionDeltaZ * positionDeltaZ),
            sqrt(movement.x * movement.x + movement.z * movement.z)
        )
    }

}
