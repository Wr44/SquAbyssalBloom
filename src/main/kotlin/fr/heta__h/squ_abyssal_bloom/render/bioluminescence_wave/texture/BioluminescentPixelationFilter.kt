package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import kotlin.math.floor
import kotlin.math.roundToInt

internal object BioluminescentPixelationFilter {
    fun apply(
        emission: BioluminescentEmissionField,
        originX: Int,
        originZ: Int,
        textureSize: Int,
        gutterPixels: Int,
        pixelsPerBlock: Int,
        destination: IntArray
    ) {
        require(destination.size == textureSize * textureSize)
        val minimumWorldPixelX = originX * pixelsPerBlock - gutterPixels
        val minimumWorldPixelZ = originZ * pixelsPerBlock - gutterPixels
        val sourcePixelsPerBlock = BioluminescentEmissionField.PIXELS_PER_BLOCK
        val sourceScale = sourcePixelsPerBlock.toDouble() / pixelsPerBlock
        val lowerSampleX = IntArray(textureSize)
        val lowerSampleZ = IntArray(textureSize)
        val fractionX = DoubleArray(textureSize)
        val fractionZ = DoubleArray(textureSize)
        prepareCoordinates(minimumWorldPixelX, sourceScale, lowerSampleX, fractionX)
        prepareCoordinates(minimumWorldPixelZ, sourceScale, lowerSampleZ, fractionZ)

        val minimumSampleX = lowerSampleX.min()
        val minimumSampleZ = lowerSampleZ.min()
        val maximumSampleX = lowerSampleX.max() + 1
        val maximumSampleZ = lowerSampleZ.max() + 1
        val sampleWidth = maximumSampleX - minimumSampleX + 1
        val sampleHeight = maximumSampleZ - minimumSampleZ + 1
        val samples = LongArray(sampleWidth * sampleHeight)
        for (sampleZ in minimumSampleZ..maximumSampleZ) {
            val rowOffset = (sampleZ - minimumSampleZ) * sampleWidth
            for (sampleX in minimumSampleX..maximumSampleX) {
                samples[rowOffset + sampleX - minimumSampleX] = sampleAt(emission, sampleX, sampleZ)
            }
        }

        for (pixelZ in 0 until textureSize) {
            val sampleZ = lowerSampleZ[pixelZ] - minimumSampleZ
            val sampleRow = sampleZ * sampleWidth
            val nextSampleRow = sampleRow + sampleWidth
            for (pixelX in 0 until textureSize) {
                val sampleX = lowerSampleX[pixelX] - minimumSampleX
                destination[pixelZ * textureSize + pixelX] = interpolate(
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

    private fun prepareCoordinates(
        minimumWorldPixel: Int,
        sourceScale: Double,
        lowerSamples: IntArray,
        fractions: DoubleArray
    ) {
        for (pixel in lowerSamples.indices) {
            val sourceCoordinate = (minimumWorldPixel + pixel + 0.5) * sourceScale - 0.5
            val lower = floor(sourceCoordinate).toInt()
            lowerSamples[pixel] = lower
            fractions[pixel] = sourceCoordinate - lower
        }
    }

    private fun sampleAt(
        emission: BioluminescentEmissionField,
        globalSampleX: Int,
        globalSampleZ: Int
    ): Long {
        val samplesPerBlock = BioluminescentEmissionField.PIXELS_PER_BLOCK
        val cellX = Math.floorDiv(globalSampleX, samplesPerBlock)
        val cellZ = Math.floorDiv(globalSampleZ, samplesPerBlock)
        val cellIndex = emission.domain.cellIndexAt(cellX, cellZ) ?: return 0L
        val localX = Math.floorMod(globalSampleX, samplesPerBlock)
        val localZ = Math.floorMod(globalSampleZ, samplesPerBlock)
        val alphaBits = emission.alphaAt(cellIndex, localX, localZ).toRawBits().toLong() and 0xFFFFFFFFL
        val color = emission.colorAt(cellIndex, localX, localZ).toLong() and 0xFFFFFFFFL
        return (alphaBits shl 32) or color
    }

    private fun interpolate(
        northWest: Long,
        northEast: Long,
        southWest: Long,
        southEast: Long,
        fractionX: Double,
        fractionZ: Double
    ): Int {
        val inverseX = 1.0 - fractionX
        val inverseZ = 1.0 - fractionZ
        val northWestWeight = inverseX * inverseZ
        val northEastWeight = fractionX * inverseZ
        val southWestWeight = inverseX * fractionZ
        val southEastWeight = fractionX * fractionZ
        val northWestAlpha = alpha(northWest)
        val northEastAlpha = alpha(northEast)
        val southWestAlpha = alpha(southWest)
        val southEastAlpha = alpha(southEast)
        val weightedAlpha = northWestAlpha * northWestWeight +
            northEastAlpha * northEastWeight +
            southWestAlpha * southWestWeight +
            southEastAlpha * southEastWeight
        val alphaLevel = (weightedAlpha * MAXIMUM_ALPHA_LEVEL)
            .roundToInt().coerceIn(0, MAXIMUM_ALPHA_LEVEL)
        if (alphaLevel == 0) return 0
        val colorWeight = weightedAlpha.coerceAtLeast(MINIMUM_COLOR_WEIGHT)
        val red = weightedChannel(
            northWest,
            northEast,
            southWest,
            southEast,
            northWestAlpha * northWestWeight,
            northEastAlpha * northEastWeight,
            southWestAlpha * southWestWeight,
            southEastAlpha * southEastWeight,
            16,
            colorWeight
        )
        val green = weightedChannel(
            northWest,
            northEast,
            southWest,
            southEast,
            northWestAlpha * northWestWeight,
            northEastAlpha * northEastWeight,
            southWestAlpha * southWestWeight,
            southEastAlpha * southEastWeight,
            8,
            colorWeight
        )
        val blue = weightedChannel(
            northWest,
            northEast,
            southWest,
            southEast,
            northWestAlpha * northWestWeight,
            northEastAlpha * northEastWeight,
            southWestAlpha * southWestWeight,
            southEastAlpha * southEastWeight,
            0,
            colorWeight
        )
        val outputAlpha = alphaLevel * 255 / MAXIMUM_ALPHA_LEVEL
        return (outputAlpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun weightedChannel(
        northWest: Long,
        northEast: Long,
        southWest: Long,
        southEast: Long,
        northWestWeight: Double,
        northEastWeight: Double,
        southWestWeight: Double,
        southEastWeight: Double,
        shift: Int,
        totalWeight: Double
    ): Int {
        val value = channel(northWest, shift) * northWestWeight +
            channel(northEast, shift) * northEastWeight +
            channel(southWest, shift) * southWestWeight +
            channel(southEast, shift) * southEastWeight
        return (value / totalWeight).roundToInt().coerceIn(0, 255)
    }

    private fun alpha(sample: Long): Double {
        return Float.fromBits((sample ushr 32).toInt()).toDouble()
    }

    private fun channel(sample: Long, shift: Int): Int {
        return (sample ushr shift).toInt() and 0xFF
    }

    private const val MAXIMUM_ALPHA_LEVEL = 15
    private const val MINIMUM_COLOR_WEIGHT = 1.0e-6
}
