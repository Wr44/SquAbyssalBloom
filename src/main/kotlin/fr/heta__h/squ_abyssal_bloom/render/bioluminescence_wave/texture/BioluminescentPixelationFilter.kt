package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import kotlin.math.roundToInt

internal object BioluminescentPixelationFilter {
    fun apply(
        emission: BioluminescentEmissionField,
        originX: Int,
        originZ: Int,
        textureSize: Int,
        gutterPixels: Int,
        pixelsPerBlock: Int,
        alpha: FloatArray,
        colors: IntArray
    ) {
        val minimumWorldPixelX = originX * pixelsPerBlock - gutterPixels
        val minimumWorldPixelZ = originZ * pixelsPerBlock - gutterPixels
        val maximumWorldPixelX = minimumWorldPixelX + textureSize - 1
        val maximumWorldPixelZ = minimumWorldPixelZ + textureSize - 1
        var groupWorldZ = Math.floorDiv(minimumWorldPixelZ, PIXEL_SIZE) * PIXEL_SIZE
        while (groupWorldZ <= maximumWorldPixelZ) {
            var groupWorldX = Math.floorDiv(minimumWorldPixelX, PIXEL_SIZE) * PIXEL_SIZE
            while (groupWorldX <= maximumWorldPixelX) {
                sampleGroup(
                    emission,
                    groupWorldX,
                    groupWorldZ,
                    pixelsPerBlock,
                    minimumWorldPixelX,
                    minimumWorldPixelZ,
                    textureSize,
                    alpha,
                    colors
                )
                groupWorldX += PIXEL_SIZE
            }
            groupWorldZ += PIXEL_SIZE
        }
    }

    private fun sampleGroup(
        emission: BioluminescentEmissionField,
        groupWorldX: Int,
        groupWorldZ: Int,
        pixelsPerBlock: Int,
        minimumWorldPixelX: Int,
        minimumWorldPixelZ: Int,
        textureSize: Int,
        alpha: FloatArray,
        colors: IntArray
    ) {
        var alphaSum = 0.0
        var redSum = 0.0
        var greenSum = 0.0
        var blueSum = 0.0
        for (offsetZ in 0 until PIXEL_SIZE) {
            for (offsetX in 0 until PIXEL_SIZE) {
                val worldX = (groupWorldX + offsetX + 0.5) / pixelsPerBlock
                val worldZ = (groupWorldZ + offsetZ + 0.5) / pixelsPerBlock
                val sampleAlpha = emission.sampleAlpha(worldX, worldZ).toDouble()
                val sampleColor = emission.sampleColor(worldX, worldZ)
                alphaSum += sampleAlpha
                redSum += ((sampleColor ushr 16) and 0xFF) * sampleAlpha
                greenSum += ((sampleColor ushr 8) and 0xFF) * sampleAlpha
                blueSum += (sampleColor and 0xFF) * sampleAlpha
            }
        }

        val filteredAlpha = (alphaSum / SAMPLE_COUNT).toFloat()
        val filteredColor = if (alphaSum <= 0.0) 0 else {
            val red = (redSum / alphaSum).roundToInt().coerceIn(0, 255)
            val green = (greenSum / alphaSum).roundToInt().coerceIn(0, 255)
            val blue = (blueSum / alphaSum).roundToInt().coerceIn(0, 255)
            (red shl 16) or (green shl 8) or blue
        }
        val minimumLocalX = maxOf(0, groupWorldX - minimumWorldPixelX)
        val minimumLocalZ = maxOf(0, groupWorldZ - minimumWorldPixelZ)
        val maximumLocalX = minOf(
            textureSize,
            groupWorldX + PIXEL_SIZE - minimumWorldPixelX
        )
        val maximumLocalZ = minOf(
            textureSize,
            groupWorldZ + PIXEL_SIZE - minimumWorldPixelZ
        )
        for (localZ in minimumLocalZ until maximumLocalZ) {
            for (localX in minimumLocalX until maximumLocalX) {
                val index = localZ * textureSize + localX
                alpha[index] = filteredAlpha
                colors[index] = filteredColor
            }
        }
    }

    private const val PIXEL_SIZE = 2
    private const val SAMPLE_COUNT = 4.0
}
