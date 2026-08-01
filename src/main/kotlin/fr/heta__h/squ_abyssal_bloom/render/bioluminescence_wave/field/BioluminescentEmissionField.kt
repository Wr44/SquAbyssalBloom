package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
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

    private fun pixelIndex(cellIndex: Int, localPixelX: Int, localPixelZ: Int): Int {
        return cellIndex * PIXELS_PER_CELL +
            localPixelZ * PIXELS_PER_BLOCK + localPixelX
    }
}
