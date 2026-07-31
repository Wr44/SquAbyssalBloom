package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.PI
import kotlin.math.sin

data class BioluminescentBloomState(
    val seed: Long,
    val createdAt: Long,
    val lifetime: Long,
    val palette: BioluminescentPalette
) {
    data class BioluminescentPalette(
        val firstColor: Int,
        val secondColor: Int,
        val highlightColor: Int
    )

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
            progress < APPEARANCE_END -> ModUtilities.smoothstep(
                0.0,
                APPEARANCE_END,
                progress
            ).toFloat()
            progress < DISAPPEARANCE_START -> 1.0f
            else -> (1.0 - ModUtilities.smoothstep(
                DISAPPEARANCE_START,
                1.0,
                progress
            )).toFloat()
        }
    }

    fun pulseAt(gameTime: Long): Float {
        val elapsed = elapsedTicks(gameTime).toDouble()
        val phase = ((seed ushr 16) and 0xFFFFL).toDouble() / 65535.0 * PI * 2.0
        return (0.92 + 0.08 * sin(phase + elapsed * PI * 2.0 / PULSE_PERIOD_TICKS)).toFloat()
    }

    private fun elapsedTicks(gameTime: Long): Long {
        if (gameTime <= createdAt) return 0L
        return (gameTime - createdAt).coerceAtMost(lifetime)
    }

    companion object {
        private const val APPEARANCE_END = 0.2
        private const val DISAPPEARANCE_START = 0.8
        private const val PULSE_PERIOD_TICKS = 120.0

        private val PALETTES = listOf(
            BioluminescentPalette(0x18DDE3, 0x78F5AE, 0xEFFFF8),
            BioluminescentPalette(0x20D9EC, 0x966CFF, 0xF5EDFF),
            BioluminescentPalette(0x895FF4, 0xFF62B8, 0xFFF0FB),
            BioluminescentPalette(0x20DFC1, 0xE957D6, 0xFFF0FC)
        )

        fun create(seed: Long, createdAt: Long, lifetime: Long): BioluminescentBloomState {
            val paletteIndex = Math.floorMod(
                RandomSupport.mixStafford13(seed),
                PALETTES.size.toLong()
            ).toInt()

            return BioluminescentBloomState(
                seed = seed,
                createdAt = createdAt,
                lifetime = lifetime,
                palette = PALETTES[paletteIndex]
            )
        }
    }
}
