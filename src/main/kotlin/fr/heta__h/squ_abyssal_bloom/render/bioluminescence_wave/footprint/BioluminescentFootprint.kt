package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.footprint

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities

data class BioluminescentFootprint(
    val centerX: Double,
    val surfaceY: Double,
    val centerZ: Double,
    val forwardX: Double,
    val forwardZ: Double,
    val color: Int,
    val visibilityFactor: Float,
    val waveMaximumOpacity: Float,
    val createdAtGameTime: Long
) {
    companion object {
        const val HALF_WIDTH = 0.15
        const val HALF_LENGTH = 0.15
        const val BOUNDING_RADIUS = 0.213
    }

    fun opacityAt(renderGameTime: Double): Float {
        val lifetime = ModConfig.bioluminescenceFootprintLifetimeTicks.coerceIn(20, 300).toDouble()
        val fadeStart = minOf(20.0, lifetime * 0.2)
        val age = renderGameTime - createdAtGameTime
        if (age < 0.0 || age >= lifetime) return 0.0f
        val temporalFade = if (age <= fadeStart) {
            1.0
        } else {
            1.0 - ModUtilities.smooth(fadeStart, lifetime, age)
        }
        val configuredMaximum = ModConfig.bioluminescenceFootprintOpacity.coerceIn(
            0.0,
            BioluminescentZoneActivity.ACTIVE.maximumOpacity.toDouble()
        ).toFloat()
        val effectiveMaximum = minOf(waveMaximumOpacity, configuredMaximum)
        return (effectiveMaximum * visibilityFactor * temporalFade).toFloat()
    }
}
