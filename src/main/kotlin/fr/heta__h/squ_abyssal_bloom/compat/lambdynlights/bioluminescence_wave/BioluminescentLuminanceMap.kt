package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import java.util.Arrays
import kotlin.math.floor

object BioluminescentLuminanceMap {
    const val MAX_LUMINANCE = 15.0
    const val SAMPLE_COUNT =
        BioluminescentEmissionField.PIXELS_PER_BLOCK * BioluminescentEmissionField.PIXELS_PER_BLOCK
    private const val VISIBLE_PERCENTILE = 0.9
    private const val MIN_VISIBLE_ALPHA = 0.12

    fun columnLuminance(
        emissionField: BioluminescentEmissionField,
        cellIndex: Int,
        samples: FloatArray
    ): Double {
        if (!emissionField.hasLuminousCell(cellIndex)) return 0.0
        require(samples.size >= SAMPLE_COUNT)

        var sampleCount = 0
        for (localZ in 0 until BioluminescentEmissionField.PIXELS_PER_BLOCK) {
            for (localX in 0 until BioluminescentEmissionField.PIXELS_PER_BLOCK) {
                samples[sampleCount++] = emissionField.alphaAt(cellIndex, localX, localZ).coerceIn(0.0f, 1.0f)
            }
        }
        Arrays.sort(samples, 0, sampleCount)

        val percentileIndex = floor((sampleCount - 1) * VISIBLE_PERCENTILE).toInt()
            .coerceIn(0, sampleCount - 1)
        val alpha = samples[percentileIndex].toDouble()
        if (alpha < MIN_VISIBLE_ALPHA) return 0.0

        return alpha.coerceIn(0.0, 1.0) * MAX_LUMINANCE
    }
}
