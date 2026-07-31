package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.PI
import kotlin.math.sin

data class BioluminescentBloomState(
    val seed: Long,
    val createdAt: Long,
    val lifetime: Long,
    val palette: BioluminescentPalette,
    val pulseOriginTick: Long,
    val colorPhase: Double,
    val localPulseOffset: Double,
    val brightnessScale: Float,
    val flickerDelayTicks: Int,
    val flickerStrengthScale: Float
) {
    init {
        require(lifetime > 0L)
    }

    fun isCompleteAt(gameTime: Long): Boolean {
        if (gameTime < createdAt) return false
        return gameTime - createdAt >= lifetime
    }

    fun lifecycleIntensityAt(gameTime: Long): Float {
        val elapsed = elapsedTicks(gameTime)
        if (elapsed >= lifetime) return 0.0f

        val progress = elapsed.toDouble() / lifetime.toDouble()
        return when {
            progress < APPEARANCE_END -> ModUtilities.smooth(
                0.0,
                APPEARANCE_END,
                progress
            ).toFloat()
            progress < DISAPPEARANCE_START -> 1.0f
            else -> (1.0 - ModUtilities.smooth(
                DISAPPEARANCE_START,
                1.0,
                progress
            )).toFloat()
        }
    }

    fun lifecyclePhaseAt(gameTime: Long): BioluminescentLifecyclePhase {
        val elapsed = elapsedTicks(gameTime)
        if (elapsed >= lifetime) return BioluminescentLifecyclePhase.COMPLETE
        val progress = elapsed.toDouble() / lifetime.toDouble()
        return when {
            progress < APPEARANCE_END -> BioluminescentLifecyclePhase.APPEARING
            progress < DISAPPEARANCE_START -> BioluminescentLifecyclePhase.STABLE
            else -> BioluminescentLifecyclePhase.DISAPPEARING
        }
    }

    fun localPulseAt(gameTime: Long): Float {
        val elapsed = if (gameTime <= pulseOriginTick) 0.0 else (gameTime - pulseOriginTick).toDouble()
        val phase = colorPhase + elapsed * PI * 2.0 / PULSE_PERIOD_TICKS
        return (0.995 + 0.005 * sin(phase + localPulseOffset)).toFloat()
    }

    private fun elapsedTicks(gameTime: Long): Long {
        if (gameTime <= createdAt) return 0L
        return (gameTime - createdAt).coerceAtMost(lifetime)
    }

    companion object {
        private const val APPEARANCE_END = 0.2
        private const val DISAPPEARANCE_START = 0.8
        private const val PULSE_PERIOD_TICKS = 200.0

        fun create(
            seed: Long,
            createdAt: Long,
            lifetime: Long,
            palette: BioluminescentPalette = BioluminescentPalettes.select(seed),
            pulseOriginTick: Long = createdAt,
            colorPhase: Double = phaseFromSeed(seed)
        ): BioluminescentBloomState {
            val mixedSeed = RandomSupport.mixStafford13(seed)
            val localPulseOffset = (((mixedSeed ushr 24) and 0xFFFFL).toDouble() / 65535.0 - 0.5) * 0.24
            val brightnessScale = (0.99 + ((mixedSeed ushr 40) and 0xFFFFL).toDouble() / 65535.0 * 0.02).toFloat()
            val flickerDelayTicks = ((mixedSeed ushr 12) and 0x1L).toInt()
            val flickerStrengthScale = (0.99 + ((mixedSeed ushr 4) and 0xFFL).toDouble() / 255.0 * 0.02).toFloat()

            return BioluminescentBloomState(
                seed = seed,
                createdAt = createdAt,
                lifetime = lifetime,
                palette = palette,
                pulseOriginTick = pulseOriginTick,
                colorPhase = colorPhase,
                localPulseOffset = localPulseOffset,
                brightnessScale = brightnessScale,
                flickerDelayTicks = flickerDelayTicks,
                flickerStrengthScale = flickerStrengthScale
            )
        }

        fun phaseFromSeed(seed: Long): Double {
            return ((seed ushr 16) and 0xFFFFL).toDouble() / 65535.0 * PI * 2.0
        }
    }
}
