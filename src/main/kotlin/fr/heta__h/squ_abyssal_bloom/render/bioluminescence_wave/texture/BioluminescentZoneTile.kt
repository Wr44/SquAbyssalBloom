package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.AABB
import kotlin.math.roundToInt

class BioluminescentZoneTile private constructor(
    private val textureManager: TextureManager,
    private val identifier: Identifier,
    val originX: Int,
    val originZ: Int,
    private val surfaceHeights: DoubleArray,
    val renderableCellIndices: IntArray,
    private val spatialAlpha: FloatArray,
    private val spatialColors: IntArray,
    private val waterMask: BooleanArray,
    val bounds: AABB,
    private val texture: DynamicTexture
) : AutoCloseable {
    val renderType: RenderType = RenderTypes.eyes(identifier)
    val pixelCount: Int = TEXTURE_SIZE * TEXTURE_SIZE

    var uploaded: Boolean = false
        private set

    var uploadCount: Int = 0
        private set

    private var closed = false

    fun surfaceYAt(localCellIndex: Int): Double = surfaceHeights[localCellIndex]

    fun minimumU(localX: Int): Float {
        return (GUTTER_PIXELS + localX * PIXELS_PER_BLOCK).toFloat() / TEXTURE_SIZE
    }

    fun maximumU(localX: Int): Float {
        return (GUTTER_PIXELS + (localX + 1) * PIXELS_PER_BLOCK).toFloat() / TEXTURE_SIZE
    }

    fun minimumV(localZ: Int): Float {
        return (GUTTER_PIXELS + localZ * PIXELS_PER_BLOCK).toFloat() / TEXTURE_SIZE
    }

    fun maximumV(localZ: Int): Float {
        return (GUTTER_PIXELS + (localZ + 1) * PIXELS_PER_BLOCK).toFloat() / TEXTURE_SIZE
    }

    fun horizontalDistanceSquared(worldX: Double, worldZ: Double): Double {
        val deltaX = originX + TILE_SIZE * 0.5 - worldX
        val deltaZ = originZ + TILE_SIZE * 0.5 - worldZ
        return deltaX * deltaX + deltaZ * deltaZ
    }

    fun upload() {
        check(!closed)
        if (uploaded) return
        val pixels = texture.pixels
        for (pixelZ in 0 until TEXTURE_SIZE) {
            for (pixelX in 0 until TEXTURE_SIZE) {
                val index = pixelZ * TEXTURE_SIZE + pixelX
                val alpha = (spatialAlpha[index] * MAXIMUM_TEXTURE_ALPHA)
                    .roundToInt().coerceIn(0, 255)
                val color = if (!waterMask[index] || alpha == 0) 0 else {
                    (alpha shl 24) or spatialColors[index]
                }
                pixels.setPixel(pixelX, pixelZ, color)
            }
        }
        texture.upload()
        uploaded = true
        uploadCount++
    }

    override fun close() {
        if (closed) return
        textureManager.release(identifier)
        closed = true
    }

    companion object {
        const val TILE_SIZE = 16
        const val PIXELS_PER_BLOCK = BioluminescentEmissionField.PIXELS_PER_BLOCK
        const val GUTTER_PIXELS = 1
        const val INNER_TEXTURE_SIZE = TILE_SIZE * PIXELS_PER_BLOCK
        const val TEXTURE_SIZE = INNER_TEXTURE_SIZE + GUTTER_PIXELS * 2
        private const val MAXIMUM_TEXTURE_ALPHA = 255.0

        internal fun prepare(
            textureManager: TextureManager,
            identifier: Identifier,
            emission: BioluminescentEmissionField,
            originX: Int,
            originZ: Int
        ): BioluminescentZoneTile? {
            val domain = emission.domain
            val surfaceHeights = DoubleArray(TILE_SIZE * TILE_SIZE) { Double.NaN }
            val renderableCells = ArrayList<Int>()
            var minimumY = Double.POSITIVE_INFINITY
            var maximumY = Double.NEGATIVE_INFINITY

            for (localZ in 0 until TILE_SIZE) {
                for (localX in 0 until TILE_SIZE) {
                    val domainIndex = domain.cellIndexAt(originX + localX, originZ + localZ) ?: continue
                    if (!emission.hasLuminousCell(domainIndex)) continue
                    val localIndex = localZ * TILE_SIZE + localX
                    val surfaceY = domain.cells[domainIndex].surfaceY
                    surfaceHeights[localIndex] = surfaceY
                    renderableCells.add(localIndex)
                    minimumY = minOf(minimumY, surfaceY)
                    maximumY = maxOf(maximumY, surfaceY)
                }
            }
            if (renderableCells.isEmpty()) return null

            val alpha = FloatArray(TEXTURE_SIZE * TEXTURE_SIZE)
            val colors = IntArray(TEXTURE_SIZE * TEXTURE_SIZE)
            val water = BooleanArray(TEXTURE_SIZE * TEXTURE_SIZE)
            for (pixelZ in 0 until TEXTURE_SIZE) {
                for (pixelX in 0 until TEXTURE_SIZE) {
                    val worldX = originX +
                        (pixelX - GUTTER_PIXELS + 0.5) / PIXELS_PER_BLOCK
                    val worldZ = originZ +
                        (pixelZ - GUTTER_PIXELS + 0.5) / PIXELS_PER_BLOCK
                    val index = pixelZ * TEXTURE_SIZE + pixelX
                    water[index] = domain.cellIndexAt(
                        kotlin.math.floor(worldX).toInt(),
                        kotlin.math.floor(worldZ).toInt()
                    ) != null
                }
            }
            BioluminescentPixelationFilter.apply(
                emission,
                originX,
                originZ,
                TEXTURE_SIZE,
                GUTTER_PIXELS,
                PIXELS_PER_BLOCK,
                alpha,
                colors
            )

            val texture = BioluminescentDynamicTexture(
                "Squ Abyssal Bloom zone tile ${identifier.path}",
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                true
            )
            textureManager.register(identifier, texture)
            try {
                return BioluminescentZoneTile(
                    textureManager,
                    identifier,
                    originX,
                    originZ,
                    surfaceHeights,
                    renderableCells.toIntArray(),
                    alpha,
                    colors,
                    water,
                    AABB(
                        originX.toDouble(),
                        minimumY - 0.1,
                        originZ.toDouble(),
                        (originX + TILE_SIZE).toDouble(),
                        maximumY + 0.1,
                        (originZ + TILE_SIZE).toDouble()
                    ),
                    texture
                )
            } catch (exception: RuntimeException) {
                textureManager.release(identifier)
                throw exception
            }
        }
    }
}
