package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class BioluminescentCore internal constructor(
    val cellIndex: Int,
    val worldX: Double,
    val worldZ: Double,
    val radiusX: Double,
    val radiusZ: Double,
    val rotationRadians: Double,
    val superellipseExponent: Double,
    val weight: Double,
    val falloff: Double,
    val shape: BioluminescentCoreShape,
    private val lobes: List<BioluminescentCoreLobe>,
    private val crescentOffset: Double
) {
    companion object {
        const val CRESCENT_CUT_STRENGTH = 0.92
    }

    private val cosine = cos(rotationRadians)
    private val sine = sin(rotationRadians)

    fun influenceAt(sampleX: Double, sampleZ: Double): Double {
        val deltaX = sampleX - worldX
        val deltaZ = sampleZ - worldZ
        val localX = deltaX * cosine + deltaZ * sine
        val localZ = -deltaX * sine + deltaZ * cosine
        val influence = when (shape) {
            BioluminescentCoreShape.ELLIPTICAL -> gaussianEllipse(localX, localZ, radiusX, radiusZ)
            BioluminescentCoreShape.SUPERELLIPTICAL -> superellipse(localX, localZ)
            BioluminescentCoreShape.ELONGATED -> elongated(localX, localZ)
            BioluminescentCoreShape.CLUSTERED -> clustered(localX, localZ)
            BioluminescentCoreShape.CRESCENT -> crescent(localX, localZ)
        } * weight
        return influence.coerceIn(0.0, 1.0)
    }

    private fun gaussianEllipse(x: Double, z: Double, xRadius: Double, zRadius: Double): Double {
        val distanceSqr = x * x / (xRadius * xRadius) + z * z / (zRadius * zRadius)
        return exp(-falloff * distanceSqr)
    }

    private fun superellipse(x: Double, z: Double): Double {
        val distance = (abs(x) / radiusX).pow(superellipseExponent) +
            (abs(z) / radiusZ).pow(superellipseExponent)
        return exp(-falloff * distance)
    }

    private fun elongated(x: Double, z: Double): Double {
        val straightLength = radiusX * 0.58
        val axialDistance = max(abs(x) - straightLength, 0.0)
        val distanceSqr = axialDistance * axialDistance / (radiusX * radiusX * 0.20) +
            z * z / (radiusZ * radiusZ)
        return exp(-falloff * distanceSqr)
    }

    private fun clustered(x: Double, z: Double): Double {
        var product = 1.0
        for (lobe in lobes) {
            val influence = gaussianEllipse(
                x - lobe.offsetX,
                z - lobe.offsetZ,
                lobe.radiusX,
                lobe.radiusZ
            )
            product *= 1.0 - influence
        }
        return 1.0 - product
    }

    private fun crescent(x: Double, z: Double): Double {
        val outer = gaussianEllipse(x, z, radiusX, radiusZ)
        val inner = gaussianEllipse(
            x - crescentOffset,
            z,
            radiusX * 0.78,
            radiusZ * 0.78
        )
        return outer * (1.0 - inner * CRESCENT_CUT_STRENGTH)
    }

}
