package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture

class BioluminescentPixelationContext(
    private val samples: LongArray,
    private val sampleWidth: Int,
    private val minSampleX: Int,
    private val minSampleZ: Int,
    private val lowerSampleX: IntArray,
    private val lowerSampleZ: IntArray,
    private val fractionX: DoubleArray,
    private val fractionZ: DoubleArray,
    private val textureSize: Int,
    private val destination: IntArray
) {
    private var nextRow = 0

    val complete: Boolean
        get() = nextRow >= textureSize

    fun advance(rowBudget: Int): Boolean {
        val endRow = minOf(textureSize, nextRow + rowBudget)
        while (nextRow < endRow) {
            fillRow(nextRow)
            nextRow++
        }
        return complete
    }

    private fun fillRow(pixelZ: Int) {
        val sampleZ = lowerSampleZ[pixelZ] - minSampleZ
        val sampleRow = sampleZ * sampleWidth
        val nextSampleRow = sampleRow + sampleWidth
        for (pixelX in 0 until textureSize) {
            val sampleX = lowerSampleX[pixelX] - minSampleX
            destination[pixelZ * textureSize + pixelX] = BioluminescentPixelationFilter.interpolate(
                samples[sampleRow + sampleX],
                samples[sampleRow + sampleX + 1],
                samples[nextSampleRow + sampleX],
                samples[nextSampleRow + sampleX + 1],
                fractionX[pixelX],
                fractionZ[pixelZ]
            )
        }
    }
}
