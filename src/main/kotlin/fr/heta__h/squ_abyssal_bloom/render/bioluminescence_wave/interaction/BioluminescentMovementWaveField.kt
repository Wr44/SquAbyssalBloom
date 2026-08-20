package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction

import fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water.BioluminescentWaterParticleOptions
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterCell
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationResult
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.phys.AABB
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

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
        const val MOVEMENT_PARTICLE_COUNT = 8
        const val MOVEMENT_PARTICLE_SPREAD = 0.9
        const val MOVEMENT_PARTICLE_MIN_ALPHA = 0.05f
        const val PLAYER_DISTURBANCE_WINDOW_TICKS = 100L
    }

    private val waves = ArrayDeque<BioluminescentMovementWave>()
    private val disturbanceSounds = BioluminescentDisturbanceSoundEmitter()
    private var lastScanTick = Long.MIN_VALUE
    private var lastPlayerDisturbanceTick = Long.MIN_VALUE

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
        disturbanceSounds.tick(gameTime)
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
        val candidates = level.getEntitiesOfClass(Entity::class.java, searchBounds, ModUtilities::isQualifyingWaterMover)

        var acceptedSources = 0
        for (entity in candidates) {
            if (acceptedSources >= MAX_MOVING_SOURCES) break
            val speed = ModUtilities.horizontalMovementSpeed(entity)
            if (speed < MIN_MOVEMENT_SPEED) continue
            val waterCell = nearestWaterCell(data.domain, entity.x, entity.z) ?: continue
            if (!isValidWaterSource(entity, waterCell)) continue

            acceptedSources++
            if (isPlayerDriven(entity)) lastPlayerDisturbanceTick = gameTime
            disturbanceSounds.trackSource(level, entity, waterCell.surfaceY, gameTime)
            if (emitWave(entity.x, entity.z, speed, gameTime)) {
                spawnMovementParticles(level, data.emissionField, waterCell, entity.x, entity.z)
            }
        }
        movingSourceCount = acceptedSources
    }

    fun playerDisturbedRecently(gameTime: Long): Boolean {
        if (lastPlayerDisturbanceTick == Long.MIN_VALUE) return false
        return gameTime - lastPlayerDisturbanceTick in 0..PLAYER_DISTURBANCE_WINDOW_TICKS
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
        disturbanceSounds.clear()
        movingSourceCount = 0
        visibilityStrength = 0.0
        lastScanTick = Long.MIN_VALUE
        lastPlayerDisturbanceTick = Long.MIN_VALUE
    }

    private fun removeExpiredWaves(gameTime: Long) {
        waves.removeIf { wave -> wave.isExpiredAt(gameTime) }
    }

    private fun emitWave(worldX: Double, worldZ: Double, speed: Double, gameTime: Long): Boolean {
        val recentDuplicate = waves.any { wave ->
            gameTime - wave.startedAt <= DUPLICATE_WINDOW_TICKS &&
                ModUtilities.horizontalDistanceSqr(wave.originX, wave.originZ, worldX, worldZ) <=
                DUPLICATE_DISTANCE * DUPLICATE_DISTANCE
        }
        if (recentDuplicate) return false
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
        return true
    }

    private fun spawnMovementParticles(
        level: ClientLevel,
        emission: BioluminescentEmissionField,
        waterCell: BioluminescentWaterCell,
        worldX: Double,
        worldZ: Double
    ) {
        val cellIndex = emission.domain.cellIndexAt(floor(worldX).toInt(), floor(worldZ).toInt())
            ?: return
        if (!emission.hasLuminousCell(cellIndex)) return
        val random = level.random

        repeat(MOVEMENT_PARTICLE_COUNT) {
            val localPixelX = random.nextInt(BioluminescentEmissionField.PIXELS_PER_BLOCK)
            val localPixelZ = random.nextInt(BioluminescentEmissionField.PIXELS_PER_BLOCK)
            val alpha = emission.alphaAt(cellIndex, localPixelX, localPixelZ)
            if (alpha < MOVEMENT_PARTICLE_MIN_ALPHA) return@repeat
            val color = emission.colorAt(cellIndex, localPixelX, localPixelZ)

            val offsetX = (random.nextDouble() - 0.5) * MOVEMENT_PARTICLE_SPREAD
            val offsetZ = (random.nextDouble() - 0.5) * MOVEMENT_PARTICLE_SPREAD

            level.addParticle(
                BioluminescentWaterParticleOptions(color, alpha),
                worldX + offsetX,
                waterCell.surfaceY,
                worldZ + offsetZ,
                0.0, 0.0, 0.0
            )
        }
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

    private fun isPlayerDriven(entity: Entity): Boolean {
        return entity is Player || entity.passengers.any { passenger -> passenger is Player }
    }

    private fun isValidWaterSource(entity: Entity, waterCell: BioluminescentWaterCell): Boolean {
        return if (entity is AbstractBoat) {
            abs(entity.boundingBox.minY - waterCell.surfaceY) <= BOAT_SURFACE_TOLERANCE
        } else {
            entity.isInWater && abs(entity.y - waterCell.surfaceY) <= VERTICAL_SEARCH_RANGE
        }
    }

}
