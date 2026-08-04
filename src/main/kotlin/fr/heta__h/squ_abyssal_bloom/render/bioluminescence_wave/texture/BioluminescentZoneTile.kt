package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.pipeline.BioluminescentRenderPipelines
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
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
    private var preparedPixels: IntArray?,
    private var pixelationContext: BioluminescentPixelationContext?
) : AutoCloseable {
    companion object {
        const val TILE_SIZE = 32
        const val MIP_LEVELS = 4
        private const val PIXELS_PER_BLOCK = 8
        private const val GUTTER_PIXELS = 1 shl (MIP_LEVELS - 1)
        private const val INNER_TEXTURE_SIZE = TILE_SIZE * PIXELS_PER_BLOCK
        const val TEXTURE_SIZE = INNER_TEXTURE_SIZE + GUTTER_PIXELS * 2
        private const val PULSE_PHASE_SEED_SALT = 0x510E527FADE682D1L
        private const val PULSE_WARP_SEED_SALT = 0x1F83D9ABFB41BD6BL
        private const val PULSE_MESH_SIZE = 4
        private const val MOVEMENT_FADE_RATE = 3.0

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
            val waterDepths = DoubleArray(TILE_SIZE * TILE_SIZE)
            var minY = Double.POSITIVE_INFINITY
            var maxY = Double.NEGATIVE_INFINITY

            for (localZ in 0 until TILE_SIZE) {
                for (localX in 0 until TILE_SIZE) {
                    val domainIndex = domain.cellIndexAt(originX + localX, originZ + localZ) ?: continue
                    if (!emission.hasLuminousCell(domainIndex)) continue
                    val cell = domain.cells[domainIndex]
                    val localIndex = localZ * TILE_SIZE + localX
                    surfaceHeights[localIndex] = cell.surfaceY
                    waterDepths[localIndex] = cell.waterDepth
                    minY = minOf(minY, cell.surfaceY)
                    maxY = maxOf(maxY, cell.surfaceY)
                }
            }
            val renderQuads = createRenderQuads(
                surfaceHeights,
                waterDepths,
                originX,
                originZ,
                zoneSeed
            )
            if (renderQuads.isEmpty()) return null

            val preparedPixels = IntArray(TEXTURE_SIZE * TEXTURE_SIZE)
            val pixelationContext = BioluminescentPixelationFilter.begin(
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
                        minY - 0.1,
                        originZ.toDouble(),
                        (originX + TILE_SIZE).toDouble(),
                        maxY + 0.1,
                        (originZ + TILE_SIZE).toDouble()
                    ),
                    texture,
                    preparedPixels,
                    pixelationContext
                )
            } catch (exception: RuntimeException) {
                textureManager.release(identifier)
                throw exception
            }
        }

        private fun createRenderQuads(
            surfaceHeights: DoubleArray,
            waterDepths: DoubleArray,
            originX: Int,
            originZ: Int,
            zoneSeed: Long
        ): List<RenderQuad> {
            val consumed = BooleanArray(surfaceHeights.size)
            val result = ArrayList<RenderQuad>()
            val zonePhase = phaseFromSeed(zoneSeed xor PULSE_PHASE_SEED_SALT)
            val warpPhase = phaseFromSeed(zoneSeed xor PULSE_WARP_SEED_SALT)
            for (meshOriginZ in 0 until TILE_SIZE step PULSE_MESH_SIZE) {
                val meshMaxZ = minOf(meshOriginZ + PULSE_MESH_SIZE, TILE_SIZE)
                for (meshOriginX in 0 until TILE_SIZE step PULSE_MESH_SIZE) {
                    val meshMaxX = minOf(meshOriginX + PULSE_MESH_SIZE, TILE_SIZE)
                    for (minZ in meshOriginZ until meshMaxZ) {
                        for (minX in meshOriginX until meshMaxX) {
                            val firstIndex = minZ * TILE_SIZE + minX
                            val surfaceY = surfaceHeights[firstIndex]
                            if (consumed[firstIndex] || surfaceY.isNaN()) continue

                            var width = 1
                            while (minX + width < meshMaxX) {
                                val index = minZ * TILE_SIZE + minX + width
                                if (consumed[index] ||
                                    surfaceHeights[index].toBits() != surfaceY.toBits()
                                ) break
                                width++
                            }
                            var height = 1
                            rows@ while (minZ + height < meshMaxZ) {
                                val rowOffset = (minZ + height) * TILE_SIZE + minX
                                for (offsetX in 0 until width) {
                                    val index = rowOffset + offsetX
                                    if (consumed[index] ||
                                        surfaceHeights[index].toBits() != surfaceY.toBits()
                                    ) break@rows
                                }
                                height++
                            }
                            for (offsetZ in 0 until height) {
                                val rowOffset = (minZ + offsetZ) * TILE_SIZE + minX
                                for (offsetX in 0 until width) consumed[rowOffset + offsetX] = true
                            }
                            result.add(
                                createRenderQuad(
                                    minX,
                                    minZ,
                                    width,
                                    height,
                                    surfaceY,
                                    originX,
                                    originZ,
                                    zonePhase,
                                    warpPhase,
                                    waterDepths
                                )
                            )
                        }
                    }
                }
            }
            return result
        }

        private fun createRenderQuad(
            minX: Int,
            minZ: Int,
            width: Int,
            height: Int,
            surfaceY: Double,
            originX: Int,
            originZ: Int,
            zonePhase: Double,
            warpPhase: Double,
            waterDepths: DoubleArray
        ): RenderQuad {
            val maxX = minX + width
            val maxZ = minZ + height
            return RenderQuad(
                minX,
                minZ,
                maxX,
                maxZ,
                surfaceY,
                pulsePhaseAt(originX + minX, originZ + minZ, zonePhase, warpPhase),
                pulsePhaseAt(originX + minX, originZ + maxZ, zonePhase, warpPhase),
                pulsePhaseAt(originX + maxX, originZ + maxZ, zonePhase, warpPhase),
                pulsePhaseAt(originX + maxX, originZ + minZ, zonePhase, warpPhase),
                waterDepthAt(waterDepths, minX, minZ),
                waterDepthAt(waterDepths, minX, maxZ - 1),
                waterDepthAt(waterDepths, maxX - 1, maxZ - 1),
                waterDepthAt(waterDepths, maxX - 1, minZ)
            )
        }

        private fun waterDepthAt(waterDepths: DoubleArray, localX: Int, localZ: Int): Double {
            val clampedX = localX.coerceIn(0, TILE_SIZE - 1)
            val clampedZ = localZ.coerceIn(0, TILE_SIZE - 1)
            return waterDepths[clampedZ * TILE_SIZE + clampedX]
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
            return ModUtilities.stableUnitValue(mixed) * PI * 2.0
        }

        private fun createRenderType(name: String, pipeline: RenderPipeline, identifier: Identifier): RenderType {
            return RenderType.create(
                name,
                RenderSetup.builder(pipeline)
                    .withTexture("Sampler0", identifier)
                    .sortOnUpload()
                    .createRenderSetup()
            )
        }

        fun lodDebugText(): String {
            return (0 until MIP_LEVELS).joinToString(">") { level ->
                maxOf(1, PIXELS_PER_BLOCK shr level).toString()
            }
        }
    }

    val renderTypeVanilla: RenderType = createRenderType(
        "squ_bioluminescent_wave",
        BioluminescentRenderPipelines.VANILLA_SURFACE,
        identifier
    )
    val renderTypeShaderUnderwater: RenderType = createRenderType(
        "squ_bioluminescent_wave_shader_underwater",
        BioluminescentRenderPipelines.SHADER_UNDERWATER_SURFACE,
        identifier
    )
    val renderTypeShaderCompensation: RenderType = createRenderType(
        "squ_bioluminescent_wave_shader_compensation",
        BioluminescentRenderPipelines.SHADER_VISIBILITY_COMPENSATION,
        identifier
    )
    val pixelCount: Int = texture.pixelCount

    var uploaded: Boolean = false
        private set

    var state: BioluminescentTileState = BioluminescentTileState.BUILDING_CPU
        private set

    var uploadCount: Int = 0
        private set

    private var closed = false
    private var uploadStarted = false
    private var movementFadeStrength: Double = 0.0
    private var lastMovementFadeNanos: Long = 0L
    private var readyAtGameTime: Long? = null

    fun fillPixelsStep(rowBudget: Int): Boolean {
        val context = pixelationContext ?: return true
        val done = context.advance(rowBudget)
        if (done) {
            pixelationContext = null
            state = BioluminescentTileState.WAITING_GPU_UPLOAD
        }
        return done
    }

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

    fun horizontalDistanceSqr(worldX: Double, worldZ: Double): Double {
        return ModUtilities.horizontalDistanceSqr(
            originX + TILE_SIZE * 0.5,
            originZ + TILE_SIZE * 0.5,
            worldX,
            worldZ
        )
    }

    fun localLoadingIntensityAt(renderGameTime: Double): Float {
        val readyAt = readyAtGameTime ?: return 0.0f
        val fadeTicks = ModConfig.bioluminescenceTileFadeInTicks.coerceAtLeast(0)
        if (fadeTicks == 0) return 1.0f
        return ((renderGameTime - readyAt) / fadeTicks.toDouble()).toFloat().coerceIn(0.0f, 1.0f)
    }

    fun uploadStep(gameTime: Long): Boolean {
        check(!closed)
        if (uploaded) return true
        RenderSystem.assertOnRenderThread()
        val finished = if (!uploadStarted) {
            val source = checkNotNull(preparedPixels)
            uploadStarted = true
            val done = texture.beginUpload(source)
            preparedPixels = null
            done
        } else {
            texture.uploadNextMipLevel()
        }
        if (!finished) return false
        uploaded = true
        uploadCount++
        readyAtGameTime = gameTime
        state = BioluminescentTileState.READY
        return true
    }

    override fun close() {
        if (closed) return
        preparedPixels = null
        pixelationContext = null
        textureManager.release(identifier)
        closed = true
        state = BioluminescentTileState.CLOSED
    }

    class RenderQuad internal constructor(
        val minLocalX: Int,
        val minLocalZ: Int,
        val maxLocalX: Int,
        val maxLocalZ: Int,
        val surfaceY: Double,
        val northWestPulsePhase: Double,
        val southWestPulsePhase: Double,
        val southEastPulsePhase: Double,
        val northEastPulsePhase: Double,
        val northWestWaterDepth: Double,
        val southWestWaterDepth: Double,
        val southEastWaterDepth: Double,
        val northEastWaterDepth: Double
    ) {
        var lastRenderTop: Boolean? = null
        var invalidated: Boolean = false
    }

}
