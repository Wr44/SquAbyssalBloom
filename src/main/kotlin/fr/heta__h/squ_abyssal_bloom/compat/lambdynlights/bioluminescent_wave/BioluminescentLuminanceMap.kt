package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.bioluminescent_wave

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import kotlin.math.floor

object BioluminescentLuminanceMap {
    const val MAX_LUMINANCE = 15.0
    private const val VISIBLE_PERCENTILE = 0.9
    private const val MIN_VISIBLE_ALPHA = 0.12

    fun columnLuminance(emissionField: BioluminescentEmissionField, cellIndex: Int): Double {
        if (!emissionField.hasLuminousCell(cellIndex)) return 0.0

        val subPixelCount = BioluminescentEmissionField.PIXELS_PER_BLOCK *
            BioluminescentEmissionField.PIXELS_PER_BLOCK
        val samples = DoubleArray(subPixelCount)
        var index = 0
        for (localZ in 0 until BioluminescentEmissionField.PIXELS_PER_BLOCK) {
            for (localX in 0 until BioluminescentEmissionField.PIXELS_PER_BLOCK) {
                samples[index++] = emissionField.alphaAt(cellIndex, localX, localZ).toDouble()
            }
        }
        samples.sort()
        val percentileIndex = floor((samples.size - 1) * VISIBLE_PERCENTILE).toInt()
            .coerceIn(0, samples.size - 1)
        val alpha = samples[percentileIndex]
        if (alpha < MIN_VISIBLE_ALPHA) return 0.0

        return alpha.coerceIn(0.0, 1.0) * MAX_LUMINANCE
    }
}
