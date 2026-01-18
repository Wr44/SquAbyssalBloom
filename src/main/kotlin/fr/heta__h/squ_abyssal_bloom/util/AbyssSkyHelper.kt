package fr.heta__h.squ_abyssal_bloom.util

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getDepthFactor

object AbyssSkyHelper {
    var smoothedSkyFade = 0.0

    fun computeFade(currentDepth: Double): Double {
        val targetFade = getDepthFactor(currentDepth)
        smoothedSkyFade += (targetFade - smoothedSkyFade) * 0.04
        return smoothedSkyFade.coerceIn(0.0, 1.0)
    }
}
