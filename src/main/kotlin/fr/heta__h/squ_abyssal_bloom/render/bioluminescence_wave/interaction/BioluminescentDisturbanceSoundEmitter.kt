package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction

import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

class BioluminescentDisturbanceSoundEmitter {
    companion object {
        private const val TRAVEL_PER_STEP = 3.0
        private const val STEP_COOLDOWN_TICKS = 20L
        private const val TRAIL_RESET_INTERVAL_TICKS = 6000L
        private const val TELEPORT_DISTANCE = 8.0
        private const val MIN_VOLUME = 0.35f
        private const val VOLUME_SPREAD = 0.4f
        private const val PITCH_SPREAD = 0.4f
    }

    private class SourceTrail(
        var lastX: Double,
        var lastZ: Double,
        var travelled: Double,
        var lastStepTick: Long
    )

    private val trails = ConcurrentHashMap<Int, SourceTrail>()
    private var nextTrailResetTick = Long.MIN_VALUE

    fun trackSource(level: ClientLevel, entity: Entity, surfaceY: Double, gameTime: Long) {
        val trail = trails.computeIfAbsent(entity.id) {
            SourceTrail(entity.x, entity.z, 0.0, Long.MIN_VALUE)
        }

        val tickDistance = sqrt(
            ModUtilities.horizontalDistanceSqr(trail.lastX, trail.lastZ, entity.x, entity.z)
        )
        trail.lastX = entity.x
        trail.lastZ = entity.z
        if (tickDistance > TELEPORT_DISTANCE) {
            trail.travelled = 0.0
            return
        }
        trail.travelled += tickDistance
        if (trail.travelled < TRAVEL_PER_STEP) return
        trail.travelled = 0.0
        if (trail.lastStepTick != Long.MIN_VALUE && gameTime - trail.lastStepTick < STEP_COOLDOWN_TICKS) return
        trail.lastStepTick = gameTime

        val speedFactor = ModUtilities.smooth(
            BioluminescentMovementWaveField.MIN_MOVEMENT_SPEED,
            BioluminescentMovementWaveField.FULL_MOVEMENT_SPEED,
            ModUtilities.horizontalMovementSpeed(entity)
        )
        ModUtilities.playPositionedSound(
            level,
            ModSounds.BIOLUMINESCENT_WAVE_AMBIENT.get(),
            entity.x,
            surfaceY,
            entity.z,
            MIN_VOLUME + VOLUME_SPREAD * speedFactor.toFloat(),
            PITCH_SPREAD
        )
    }

    fun tick(gameTime: Long) {
        if (nextTrailResetTick == Long.MIN_VALUE ||
            gameTime < nextTrailResetTick - TRAIL_RESET_INTERVAL_TICKS
        ) {
            nextTrailResetTick = gameTime + TRAIL_RESET_INTERVAL_TICKS
            return
        }
        if (gameTime < nextTrailResetTick) return
        trails.clear()
        nextTrailResetTick = gameTime + TRAIL_RESET_INTERVAL_TICKS
    }

    fun clear() {
        trails.clear()
        nextTrailResetTick = Long.MIN_VALUE
    }
}
