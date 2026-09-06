package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.renderer.texture.AbstractTexture
import kotlin.math.max

internal class BioluminescentDynamicTexture(
    label: String,
    width: Int,
    height: Int,
    zero: Boolean
) : AbstractTexture() {
    private val mipImages = Array(BioluminescentZoneTile.MIP_LEVELS) { level ->
        NativeImage(max(1, width shr level), max(1, height shr level), zero || level > 0)
    }
    private var closed = false
    private var sourcePixels: IntArray? = null
    private var sourceWidth = 0
    private var sourceHeight = 0
    private var nextMipLevel = 0

    val pixelCount: Int = mipImages.sumOf { it.width * it.height }

    init {
        val device = RenderSystem.getDevice()
        texture = device.createTexture(
            label,
            GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.RGBA8,
            width,
            height,
            1,
            mipImages.size
        )
        textureView = device.createTextureView(checkNotNull(texture))
        sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true)
    }

    fun beginUpload(basePixels: IntArray): Boolean {
        require(basePixels.size == mipImages[0].width * mipImages[0].height)
        writePixels(mipImages[0], basePixels)
        writeLevelToGpu(0)
        sourcePixels = basePixels
        sourceWidth = mipImages[0].width
        sourceHeight = mipImages[0].height
        nextMipLevel = 1
        return nextMipLevel >= mipImages.size
    }

    fun uploadNextMipLevel(): Boolean {
        if (nextMipLevel >= mipImages.size) return true
        val level = nextMipLevel
        val destination = mipImages[level]
        val destinationPixels = downsample(
            checkNotNull(sourcePixels),
            sourceWidth,
            sourceHeight,
            destination.width,
            destination.height
        )
        writePixels(destination, destinationPixels)
        writeLevelToGpu(level)
        sourcePixels = destinationPixels
        sourceWidth = destination.width
        sourceHeight = destination.height
        nextMipLevel++
        return nextMipLevel >= mipImages.size
    }

    override fun close() {
        if (closed) return
        closed = true
        mipImages.forEach(NativeImage::close)
        super.close()
    }

    private fun writeLevelToGpu(level: Int) {
        val image = mipImages[level]
        val destination = checkNotNull(texture)
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        encoder.writeToTexture(
            destination,
            image,
            level,
            0,
            0,
            0,
            image.width,
            image.height,
            0,
            0
        )
    }

    private fun writePixels(image: NativeImage, pixels: IntArray) {
        for (y in 0 until image.height) {
            val rowOffset = y * image.width
            for (x in 0 until image.width) {
                image.setPixel(x, y, pixels[rowOffset + x])
            }
        }
    }

    private fun downsample(
        source: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        destinationWidth: Int,
        destinationHeight: Int
    ): IntArray {
        val destination = IntArray(destinationWidth * destinationHeight)
        for (targetY in 0 until destinationHeight) {
            for (targetX in 0 until destinationWidth) {
                destination[targetY * destinationWidth + targetX] = averagePixel(
                    source,
                    sourceWidth,
                    sourceHeight,
                    targetX * 2,
                    targetY * 2
                )
            }
        }
        return destination
    }

    private fun averagePixel(
        source: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        sourceX: Int,
        sourceY: Int
    ): Int {
        var alphaSum = 0
        var redSum = 0
        var greenSum = 0
        var blueSum = 0
        var samples = 0
        for (offsetY in 0..1) {
            val y = sourceY + offsetY
            if (y >= sourceHeight) continue
            for (offsetX in 0..1) {
                val x = sourceX + offsetX
                if (x >= sourceWidth) continue
                val color = source[y * sourceWidth + x]
                val alpha = color ushr 24 and 0xFF
                alphaSum += alpha
                redSum += (color ushr 16 and 0xFF) * alpha
                greenSum += (color ushr 8 and 0xFF) * alpha
                blueSum += (color and 0xFF) * alpha
                samples++
            }
        }
        if (alphaSum == 0 || samples == 0) return 0
        val alpha = (alphaSum + samples / 2) / samples
        val red = (redSum + alphaSum / 2) / alphaSum
        val green = (greenSum + alphaSum / 2) / alphaSum
        val blue = (blueSum + alphaSum / 2) / alphaSum
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
