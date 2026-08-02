package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.phys.AABB
import kotlin.math.PI
import kotlin.math.sin

class BioluminescentZoneTile private constructor(
    private val textureManager: TextureManager,
    private val identifier: Identifier,
    val originX: Int,
    val originZ: Int,
    val renderQuads: List<RenderQuad>,
    val bounds: AABB,
    private val texture: BioluminescentDynamicTexture,
    private var preparedPixels: IntArray?
) : AutoCloseable {
    val renderType: RenderType = createRenderType(identifier)
    val pixelCount: Int = texture.pixelCount

    var uploaded: Boolean = false
        private set

    var uploadCount: Int = 0
        private set

    private var closed = false
    private var movementFadeStrength: Double = 0.0
    private var lastMovementFadeNanos: Long = 0L

    fun updateMovementFade(targetVisible: Boolean): Float {
        val now = System.nanoTime()
        val dt = if (lastMovementFadeNanos == 0L) {
            0.0
        } else {
            ((now - lastMovementFadeNanos) / 1_000_000_000.0).coerceAtMost(0.1)
        }
        lastMovementFadeNanos = now
        val target = if (targetVisible) 1.0 else 0.0
        movementFadeStrength = ModUtilities.smoothTowards(
            movementFadeStrength,
            target,
            dt,
            MOVEMENT_FADE_RATE
        ).coerceIn(0.0, 1.0)
        return movementFadeStrength.toFloat()
    }

    fun uAt(localBlockX: Int): Float {
        return (GUTTER_PIXELS + localBlockX * PIXELS_PER_BLOCK).toFloat() / TEXTURE_SIZE
    }

    fun vAt(localBlockZ: Int): Float {
        return (GUTTER_PIXELS + localBlockZ * PIXELS_PER_BLOCK).toFloat() / TEXTURE_SIZE
    }

    fun horizontalDistanceSquared(worldX: Double, worldZ: Double): Double {
        val deltaX = originX + TILE_SIZE * 0.5 - worldX
        val deltaZ = originZ + TILE_SIZE * 0.5 - worldZ
        return deltaX * deltaX + deltaZ * deltaZ
    }

    fun upload() {
        check(!closed)
        if (uploaded) return
        val source = checkNotNull(preparedPixels)
        texture.upload(source)
        preparedPixels = null
        uploaded = true
        uploadCount++
    }

    override fun close() {
        if (closed) return
        preparedPixels = null
        textureManager.release(identifier)
        closed = true
    }

    class RenderQuad internal constructor(
        val minimumLocalX: Int,
        val minimumLocalZ: Int,
        val maximumLocalX: Int,
        val maximumLocalZ: Int,
        val surfaceY: Double,
        val northWestPulsePhase: Double,
        val southWestPulsePhase: Double,
        val southEastPulsePhase: Double,
        val northEastPulsePhase: Double
    ) {
        var lastRenderTop: Boolean? = null
    }

    companion object {
        const val TILE_SIZE = 32
        const val PIXELS_PER_BLOCK = 8
        const val MIP_LEVELS = 4
        const val GUTTER_PIXELS = 1 shl (MIP_LEVELS - 1)
        const val INNER_TEXTURE_SIZE = TILE_SIZE * PIXELS_PER_BLOCK
        const val TEXTURE_SIZE = INNER_TEXTURE_SIZE + GUTTER_PIXELS * 2

        internal fun prepare(
            textureManager: TextureManager,
            identifier: Identifier,
            emission: BioluminescentEmissionField,
            originX: Int,
            originZ: Int,
            zoneSeed: Long
        ): BioluminescentZoneTile? {
            val domain = emission.domain
            val surfaceHeights = DoubleArray(TILE_SIZE * TILE_SIZE) { Double.NaN }
            var minimumY = Double.POSITIVE_INFINITY
            var maximumY = Double.NEGATIVE_INFINITY

            for (localZ in 0 until TILE_SIZE) {
                for (localX in 0 until TILE_SIZE) {
                    val domainIndex = domain.cellIndexAt(originX + localX, originZ + localZ) ?: continue
                    if (!emission.hasLuminousCell(domainIndex)) continue
                    val surfaceY = domain.cells[domainIndex].surfaceY
                    val localIndex = localZ * TILE_SIZE + localX
                    surfaceHeights[localIndex] = surfaceY
                    minimumY = minOf(minimumY, surfaceY)
                    maximumY = maxOf(maximumY, surfaceY)
                }
            }
            val renderQuads = createRenderQuads(
                surfaceHeights,
                originX,
                originZ,
                zoneSeed
            )
            if (renderQuads.isEmpty()) return null

            val preparedPixels = IntArray(TEXTURE_SIZE * TEXTURE_SIZE)
            BioluminescentPixelationFilter.apply(
                emission,
                originX,
                originZ,
                TEXTURE_SIZE,
                GUTTER_PIXELS,
                PIXELS_PER_BLOCK,
                preparedPixels
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
                    renderQuads,
                    AABB(
                        originX.toDouble(),
                        minimumY - 0.1,
                        originZ.toDouble(),
                        (originX + TILE_SIZE).toDouble(),
                        maximumY + 0.1,
                        (originZ + TILE_SIZE).toDouble()
                    ),
                    texture,
                    preparedPixels
                )
            } catch (exception: RuntimeException) {
                textureManager.release(identifier)
                throw exception
            }
        }

        private fun createRenderQuads(
            surfaceHeights: DoubleArray,
            originX: Int,
            originZ: Int,
            zoneSeed: Long
        ): List<RenderQuad> {
            val consumed = BooleanArray(surfaceHeights.size)
            val result = ArrayList<RenderQuad>()
            val zonePhase = phaseFromSeed(zoneSeed xor PULSE_PHASE_SEED_SALT)
            val warpPhase = phaseFromSeed(zoneSeed xor PULSE_WARP_SEED_SALT)
            for (meshOriginZ in 0 until TILE_SIZE step PULSE_MESH_SIZE) {
                val meshMaximumZ = minOf(meshOriginZ + PULSE_MESH_SIZE, TILE_SIZE)
                for (meshOriginX in 0 until TILE_SIZE step PULSE_MESH_SIZE) {
                    val meshMaximumX = minOf(meshOriginX + PULSE_MESH_SIZE, TILE_SIZE)
                    for (minimumZ in meshOriginZ until meshMaximumZ) {
                        for (minimumX in meshOriginX until meshMaximumX) {
                            val firstIndex = minimumZ * TILE_SIZE + minimumX
                            val surfaceY = surfaceHeights[firstIndex]
                            if (consumed[firstIndex] || surfaceY.isNaN()) continue

                            var width = 1
                            while (minimumX + width < meshMaximumX) {
                                val index = minimumZ * TILE_SIZE + minimumX + width
                                if (consumed[index] ||
                                    surfaceHeights[index].toBits() != surfaceY.toBits()
                                ) break
                                width++
                            }
                            var height = 1
                            rows@ while (minimumZ + height < meshMaximumZ) {
                                val rowOffset = (minimumZ + height) * TILE_SIZE + minimumX
                                for (offsetX in 0 until width) {
                                    val index = rowOffset + offsetX
                                    if (consumed[index] ||
                                        surfaceHeights[index].toBits() != surfaceY.toBits()
                                    ) break@rows
                                }
                                height++
                            }
                            for (offsetZ in 0 until height) {
                                val rowOffset = (minimumZ + offsetZ) * TILE_SIZE + minimumX
                                for (offsetX in 0 until width) consumed[rowOffset + offsetX] = true
                            }
                            result.add(
                                createRenderQuad(
                                    minimumX,
                                    minimumZ,
                                    width,
                                    height,
                                    surfaceY,
                                    originX,
                                    originZ,
                                    zonePhase,
                                    warpPhase
                                )
                            )
                        }
                    }
                }
            }
            return result
        }

        private fun createRenderQuad(
            minimumX: Int,
            minimumZ: Int,
            width: Int,
            height: Int,
            surfaceY: Double,
            originX: Int,
            originZ: Int,
            zonePhase: Double,
            warpPhase: Double
        ): RenderQuad {
            val maximumX = minimumX + width
            val maximumZ = minimumZ + height
            return RenderQuad(
                minimumX,
                minimumZ,
                maximumX,
                maximumZ,
                surfaceY,
                pulsePhaseAt(originX + minimumX, originZ + minimumZ, zonePhase, warpPhase),
                pulsePhaseAt(originX + minimumX, originZ + maximumZ, zonePhase, warpPhase),
                pulsePhaseAt(originX + maximumX, originZ + maximumZ, zonePhase, warpPhase),
                pulsePhaseAt(originX + maximumX, originZ + minimumZ, zonePhase, warpPhase)
            )
        }

        private fun pulsePhaseAt(
            worldX: Int,
            worldZ: Int,
            zonePhase: Double,
            warpPhase: Double
        ): Double {
            val x = worldX.toDouble()
            val z = worldZ.toDouble()
            val broadWarp = sin(x * 0.024 - z * 0.019 + warpPhase) * 0.95
            val crossWarp = sin(x * 0.013 + z * 0.027 - warpPhase * 0.63) * 0.55
            return zonePhase + x * 0.087 + z * 0.063 + broadWarp + crossWarp
        }

        private fun phaseFromSeed(seed: Long): Double {
            val mixed = RandomSupport.mixStafford13(seed)
            val unit = ((mixed ushr 40) and 0xFFFFFFL).toDouble() / 0xFFFFFFL.toDouble()
            return unit * PI * 2.0
        }

        private fun createRenderType(identifier: Identifier): RenderType {
            return RenderType.create(
                "squ_bioluminescent_wave",
                RenderSetup.builder(RenderPipelines.EYES)
                    .withTexture("Sampler0", identifier)
                    .sortOnUpload()
                    .createRenderSetup()
            )
        }

        private const val PULSE_PHASE_SEED_SALT = 0x510E527FADE682D1L
        private const val PULSE_WARP_SEED_SALT = 0x1F83D9ABFB41BD6BL
        private const val PULSE_MESH_SIZE = 4
        private const val MOVEMENT_FADE_RATE = 3.0

        fun lodDebugText(): String {
            return (0 until MIP_LEVELS).joinToString(">") { level ->
                maxOf(1, PIXELS_PER_BLOCK shr level).toString()
            }
        }
    }
}
