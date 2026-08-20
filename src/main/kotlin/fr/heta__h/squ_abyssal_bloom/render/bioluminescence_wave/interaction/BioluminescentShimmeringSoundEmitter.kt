package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationResult
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class BioluminescentShimmeringSoundEmitter {
    companion object {
        private const val MIN_INTERVAL_TICKS = 80
        private const val INTERVAL_SPREAD_TICKS = 160
        private const val RETRY_DELAY_TICKS = 20L
        private const val PLACEMENT_ATTEMPTS = 12
        private const val VOLUME = 1.2f
        private const val PITCH_SPREAD = 0.2f
        private const val MIN_LISTENER_DISTANCE = 5.0
        // The sound event has a variable range, so it carries only volume * 16 blocks: placing
        // a shimmer past that is placing one nobody hears.
        private const val MAX_LISTENER_DISTANCE = 18.0
    }

    private var nextShimmeringTick = Long.MIN_VALUE

    fun tick(level: ClientLevel, data: BioluminescentZoneGenerationResult, gameTime: Long) {
        val maxSchedule = gameTime + MIN_INTERVAL_TICKS + INTERVAL_SPREAD_TICKS
        if (nextShimmeringTick == Long.MIN_VALUE || nextShimmeringTick > maxSchedule) {
            schedule(level, gameTime)
            return
        }
        if (gameTime < nextShimmeringTick) return

        val listener = Minecraft.getInstance().player
        if (listener == null || !emitNear(level, data, listener.x, listener.z)) {
            nextShimmeringTick = gameTime + RETRY_DELAY_TICKS
            return
        }
        schedule(level, gameTime)
    }

    fun clear() {
        nextShimmeringTick = Long.MIN_VALUE
    }

    private fun schedule(level: ClientLevel, gameTime: Long) {
        nextShimmeringTick = gameTime + MIN_INTERVAL_TICKS + level.random.nextInt(INTERVAL_SPREAD_TICKS)
    }

    private fun emitNear(
        level: ClientLevel,
        data: BioluminescentZoneGenerationResult,
        listenerX: Double,
        listenerZ: Double
    ): Boolean {
        val domain = data.domain
        val emission = data.emissionField
        val random = level.random

        repeat(PLACEMENT_ATTEMPTS) {
            val angle = random.nextDouble() * PI * 2.0
            val distance = MIN_LISTENER_DISTANCE +
                sqrt(random.nextDouble()) * (MAX_LISTENER_DISTANCE - MIN_LISTENER_DISTANCE)
            val worldX = listenerX + cos(angle) * distance
            val worldZ = listenerZ + sin(angle) * distance
            val cellIndex = domain.cellIndexAt(floor(worldX).toInt(), floor(worldZ).toInt()) ?: return@repeat
            if (!domain.isLocalCell(cellIndex)) return@repeat
            if (!emission.hasLuminousCell(cellIndex)) return@repeat
            val cell = domain.cells[cellIndex]
            ModUtilities.playPositionedSound(
                level,
                ModSounds.BIOLUMINESCENT_WAVE_SHIMMERING.get(),
                worldX,
                cell.surfaceY,
                worldZ,
                VOLUME,
                PITCH_SPREAD
            )
            return true
        }
        return false
    }
}
