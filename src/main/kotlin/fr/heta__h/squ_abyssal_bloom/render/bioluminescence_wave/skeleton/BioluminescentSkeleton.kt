package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton

import kotlin.math.exp

class BioluminescentSkeleton internal constructor(
    val paths: List<BioluminescentSkeletonPath>,
    val cellIndices: IntArray,
    val totalLength: Int,
    private val nearestWorldX: IntArray,
    private val nearestWorldZ: IntArray,
    private val nearestWidth: FloatArray
) {
    val connectionCount: Int
        get() = paths.size

    fun influenceAt(worldX: Double, worldZ: Double, cellIndex: Int): Double {
        if (paths.isEmpty()) return 0.0
        val width = nearestWidth[cellIndex].toDouble().coerceAtLeast(0.25)
        val deltaX = worldX - (nearestWorldX[cellIndex] + 0.5)
        val deltaZ = worldZ - (nearestWorldZ[cellIndex] + 0.5)
        val distanceSquared = deltaX * deltaX + deltaZ * deltaZ
        return exp(-CONNECTION_FALLOFF * distanceSquared / (width * width))
    }

    private companion object {
        const val CONNECTION_FALLOFF = 1.35
    }
}
