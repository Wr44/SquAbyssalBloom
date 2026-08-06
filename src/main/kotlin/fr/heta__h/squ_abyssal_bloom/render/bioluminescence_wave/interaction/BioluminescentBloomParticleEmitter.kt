package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water.BioluminescentWaterParticleOptions
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom.BioluminescentBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPalette
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.multiplayer.ClientLevel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BioluminescentBloomParticleEmitter {
    companion object {
        private const val AMBIENT_COUNT_PER_TICK = 5
        private const val PULSE_BURST_COUNT = 20
        private const val MIN_ALPHA = 0.5f
        private const val ALPHA_SPREAD = 0.4f
    }

    fun tickAmbient(
        level: ClientLevel,
        bloom: BioluminescentBloom,
        palette: BioluminescentPalette,
        intensity: Float
    ) {
        emit(level, bloom, palette, AMBIENT_COUNT_PER_TICK, intensity)
    }

    fun emitPulseBurst(
        level: ClientLevel,
        bloom: BioluminescentBloom,
        palette: BioluminescentPalette,
        intensity: Float
    ) {
        emit(level, bloom, palette, PULSE_BURST_COUNT, intensity)
    }

    private fun emit(
        level: ClientLevel,
        bloom: BioluminescentBloom,
        palette: BioluminescentPalette,
        baseCount: Int,
        intensity: Float
    ) {
        if (intensity <= 0.0f) return
        val geometry = bloom.geometry ?: return
        val count = (baseCount * intensity * ModConfig.bioluminescenceBloomParticleDensity).toInt()
        if (count <= 0) return

        val random = level.random
        repeat(count) {
            val angle = random.nextDouble() * PI * 2.0
            val distance = sqrt(random.nextDouble()) * geometry.haloRadius
            val color = ModUtilities.lerpColor(palette.accentColor, palette.highlightColor, random.nextDouble())
            level.addParticle(
                BioluminescentWaterParticleOptions(color, MIN_ALPHA + random.nextFloat() * ALPHA_SPREAD),
                geometry.centerX + cos(angle) * distance,
                geometry.surfaceY,
                geometry.centerZ + sin(angle) * distance,
                0.0, 0.0, 0.0
            )
        }
    }
}
