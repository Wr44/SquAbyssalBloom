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
        destination: IntArray
    ) {
        require(destination.size == textureSize * textureSize)
        val minimumWorldPixelX = originX * pixelsPerBlock - gutterPixels
        val minimumWorldPixelZ = originZ * pixelsPerBlock - gutterPixels
        for (pixelZ in 0 until textureSize) {
            val worldPixelZ = minimumWorldPixelZ + pixelZ
            val cellZ = Math.floorDiv(worldPixelZ, pixelsPerBlock)
            val localPixelZ = Math.floorMod(worldPixelZ, pixelsPerBlock)
            for (pixelX in 0 until textureSize) {
                val worldPixelX = minimumWorldPixelX + pixelX
                val cellX = Math.floorDiv(worldPixelX, pixelsPerBlock)
                val cellIndex = emission.domain.cellIndexAt(cellX, cellZ) ?: continue
                val localPixelX = Math.floorMod(worldPixelX, pixelsPerBlock)
                val alphaLevel = (
                    emission.alphaAt(cellIndex, localPixelX, localPixelZ) * MAXIMUM_ALPHA_LEVEL
                    ).roundToInt().coerceIn(0, MAXIMUM_ALPHA_LEVEL)
                if (alphaLevel == 0) continue
                val alpha = alphaLevel * 255 / MAXIMUM_ALPHA_LEVEL
                destination[pixelZ * textureSize + pixelX] =
                    (alpha shl 24) or emission.colorAt(cellIndex, localPixelX, localPixelZ)
            }
        }
    }

    private const val MAXIMUM_ALPHA_LEVEL = 15
}
