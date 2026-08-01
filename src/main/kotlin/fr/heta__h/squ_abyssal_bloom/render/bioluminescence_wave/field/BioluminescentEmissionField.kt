package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import kotlin.math.floor

class BioluminescentEmissionField internal constructor(
    val domain: BioluminescentWaterDomain,
    val macroField: BioluminescentMacroField,
    val targetVisibleCoverage: Double,
    val achievedVisibleCoverage: Double,
    val porousThreshold: Double,
    val supportPixelCount: Int,
    val luminousPixelCount: Int,
    val averageVisibleAlpha: Double,
    val highlightPixelCount: Int,
    private val spatialAlpha: FloatArray,
    private val spatialColors: IntArray,
    private val luminousCells: BooleanArray
) {
    val pixelCount: Int
        get() = spatialAlpha.size

    val highlightCoverage: Double
        get() = if (luminousPixelCount == 0) 0.0 else {
            highlightPixelCount.toDouble() / luminousPixelCount
        }

    companion object {
        const val PIXELS_PER_BLOCK = 4
        private const val PIXELS_PER_CELL = PIXELS_PER_BLOCK * PIXELS_PER_BLOCK
    }

    fun hasLuminousCell(cellIndex: Int): Boolean = luminousCells[cellIndex]

    fun alphaAt(cellIndex: Int, localPixelX: Int, localPixelZ: Int): Float {
        return spatialAlpha[pixelIndex(cellIndex, localPixelX, localPixelZ)]
    }

    fun colorAt(cellIndex: Int, localPixelX: Int, localPixelZ: Int): Int {
        return spatialColors[pixelIndex(cellIndex, localPixelX, localPixelZ)]
    }

    fun sampleAlpha(worldX: Double, worldZ: Double): Float {
        val sample = sampleIndex(worldX, worldZ) ?: return 0.0f
        return alphaAt(sample.first, sample.second, sample.third)
    }

    fun sampleColor(worldX: Double, worldZ: Double): Int {
        val sample = sampleIndex(worldX, worldZ) ?: return 0
        return colorAt(sample.first, sample.second, sample.third)
    }

    private fun sampleIndex(worldX: Double, worldZ: Double): Triple<Int, Int, Int>? {
        val cellX = floor(worldX).toInt()
        val cellZ = floor(worldZ).toInt()
        val cellIndex = domain.cellIndexAt(cellX, cellZ) ?: return null
        val localX = floor((worldX - cellX) * PIXELS_PER_BLOCK)
            .toInt().coerceIn(0, PIXELS_PER_BLOCK - 1)
        val localZ = floor((worldZ - cellZ) * PIXELS_PER_BLOCK)
            .toInt().coerceIn(0, PIXELS_PER_BLOCK - 1)
        return Triple(cellIndex, localX, localZ)
    }

    private fun pixelIndex(cellIndex: Int, localPixelX: Int, localPixelZ: Int): Int {
        return cellIndex * PIXELS_PER_CELL +
            localPixelZ * PIXELS_PER_BLOCK + localPixelX
    }
}
