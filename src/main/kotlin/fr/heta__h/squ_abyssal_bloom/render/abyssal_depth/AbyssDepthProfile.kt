package fr.heta__h.squ_abyssal_bloom.render.abyssal_depth

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import kotlin.math.pow

object AbyssDepthProfile {
    val entry: Double
        get() = entry(AbyssDepthCache.displayedDepthFactor)

    val presence: Double
        get() = presence(AbyssDepthCache.displayedDepthFactor)

    val oppression: Double
        get() = oppression(AbyssDepthCache.displayedDepthFactor)

    fun entry(depthFactor: Double): Double =
        depthFactor.coerceIn(0.0, 1.0).pow(0.75)

    fun presence(depthFactor: Double): Double =
        ModUtilities.smooth(0.08, 0.92, depthFactor)

    fun oppression(depthFactor: Double): Double =
        ModUtilities.smooth(0.38, 1.0, depthFactor)
}
