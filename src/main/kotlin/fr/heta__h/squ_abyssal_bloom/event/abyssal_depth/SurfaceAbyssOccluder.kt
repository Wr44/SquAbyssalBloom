package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.material.FogType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object SurfaceAbyssOccluder {

    private val WHITE_TEXTURE = Identifier.withDefaultNamespace("textures/misc/white.png")

    private const val NUM_LAYERS = 5

    private const val BAND_WIDTH = 32
    private const val BASE_STEP = 4

    private data class LodBand(
        val minDist: Int,   
        val maxDist: Int,   
        val step: Int,
        val scanRadius: Int
    )

    private const val COLOR_R = 0.0f
    private const val COLOR_G = 0.01f
    private const val COLOR_B = 0.03f

    private const val BASE_ALPHA_MIN = 0.05f
    private const val BASE_ALPHA_RANGE = 0.65f

    private const val LAMP_REDUCTION_MULTIPLIER = 0.15f
    private const val ALPHA_RENDER_THRESHOLD = 0.01f

    private const val FULL_BRIGHT_LIGHTMAP = 15728880
    private const val DEFAULT_OVERLAY = 655360
    private const val UV_CENTER = 0.5f

    private var lastLogTime = 0L

    private var cachedBands: List<LodBand> = emptyList()
    private var cachedRenderDist = -1

    private fun buildBands(maxRadius: Int): List<LodBand> {
        val bands = mutableListOf<LodBand>()
        var offset = 0
        var step = BASE_STEP

        while (offset < maxRadius) {
            val isLast = (offset + BAND_WIDTH) >= maxRadius
            val bandEnd = if (isLast) maxRadius + BAND_WIDTH else offset + BAND_WIDTH

            bands.add(
                LodBand(
                    minDist = offset,
                    maxDist = bandEnd,
                    step = step,
                    scanRadius = bandEnd
                )
            )

            offset = bandEnd
            step *= 2
        }

        return bands
    }

    private fun getBands(maxRadius: Int): List<LodBand> {
        if (maxRadius != cachedRenderDist) {
            cachedBands = buildBands(maxRadius)
            cachedRenderDist = maxRadius
        }
        return cachedBands
    }

    private fun isUnderMassiveCeiling(level: Level, camPos: Vec3): Boolean {
        val cx = camPos.x.toInt()
        val cy = camPos.y.toInt()
        val cz = camPos.z.toInt()

        val offsetsX = intArrayOf(0, 3, -3, 0, 0)
        val offsetsZ = intArrayOf(0, 0, 0, 3, -3)

        var solidCeilingCount = 0

        for (i in 0 until 5) {
            val x = cx + offsetsX[i]
            val z = cz + offsetsZ[i]
            val floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z)

            if (floorY > cy + 2) {
                solidCeilingCount++
            }
        }

        return solidCeilingCount == 5
    }

    @SubscribeEvent
    fun onRenderStage(event: RenderLevelStageEvent.AfterEntities) {
        if (!ModConfig.enableAbyssFog) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera

        if (camera.fluidInCamera == FogType.WATER) return

        val level = mc.level ?: return
        val camPos = camera.position()

        val isCave = isUnderMassiveCeiling(level, camPos)

        val currentTime = System.currentTimeMillis()
        val shouldLog = currentTime - lastLogTime > 1000L

        if (isCave) {
            return
        }

        val entity = camera.entity() as? LivingEntity
        AbyssDepthCache.refreshSurfaceIfNeeded(level, BlockPos.containing(camPos), entity)

        val gameTick = level.gameTime
        val maxRadius = mc.options.renderDistance().get() * 16

        
        val px = snapToGrid(camPos.x.toInt(), BASE_STEP)
        val pz = snapToGrid(camPos.z.toInt(), BASE_STEP)

        SurfaceHeightCache.tick(gameTick, px, pz)

        val poseStack = event.poseStack
        poseStack.pushPose()
        val matrix4f = poseStack.last().pose()
        val bufferSource = mc.renderBuffers().bufferSource()
        val buffer = bufferSource.getBuffer(RenderTypes.entityTranslucent(WHITE_TEXTURE))

        val lookVec = camera.forwardVector()
        val lookX = lookVec.x()
        val lookY = lookVec.y()
        val lookZ = lookVec.z()

        val fovV = mc.options.fov().get().toFloat()
        val aspect = mc.window.screenWidth.toFloat() / mc.window.screenHeight.coerceAtLeast(1).toFloat()
        val halfFovV = Math.toRadians(fovV.toDouble() * 0.5)
        val halfFovH = kotlin.math.atan(kotlin.math.tan(halfFovV) * aspect)
        val thresholdAngle = (halfFovH + Math.toRadians(45.0)).coerceAtMost(Math.PI)
        val cullDotThreshold = kotlin.math.cos(thresholdAngle).toFloat()

        val look2DLen = sqrt(lookX * lookX + lookZ * lookZ)
        val normLookX = if (look2DLen > 0.001f) lookX / look2DLen else 0f
        val normLookZ = if (look2DLen > 0.001f) lookZ / look2DLen else 0f

        val isLookingSteep = abs(lookY) > 0.8f

        val targetDarknessDepth = maxOf(1.0f, (ModConfig.abyssDepthStart + ModConfig.abyssMaxDepth).toFloat() / 2)
        val depthStep = targetDarknessDepth / NUM_LAYERS.toFloat()

        val lampReduction = AbyssDepthCache.displayedLampInfluence.toFloat() *
                ModConfig.nautilusLampInfluence.toFloat() *
                LAMP_REDUCTION_MULTIPLIER

        val bands = getBands(maxRadius)

        for (band in bands) {
            val step = band.step
            val scanR = band.scanRadius

            
            val gridMinX = px - scanR
            val gridMinZ = pz - scanR
            val gridMaxX = px + scanR - step
            val gridMaxZ = pz + scanR - step

            for (x in gridMinX..gridMaxX step step) {
                for (z in gridMinZ..gridMaxZ step step) {

                    val cx = x + (step / 2)
                    val cz = z + (step / 2)

                    val dx = cx - px
                    val dz = cz - pz

                    
                    val boxDist = max(abs(dx), abs(dz))

                    if (boxDist < band.minDist || boxDist >= band.maxDist) {
                        continue
                    }

                    val dxFloat = dx.toFloat()
                    val dzFloat = dz.toFloat()
                    val distSq2D = (dxFloat * dxFloat) + (dzFloat * dzFloat)

                    if (!isLookingSteep && distSq2D > 1024f) {
                        val dist2D = sqrt(distSq2D)
                        val dot2D = (dxFloat * normLookX + dzFloat * normLookZ) / dist2D
                        if (dot2D < cullDotThreshold) {
                            continue
                        }
                    }

                    val cell = SurfaceHeightCache.getOrCompute(
                        level, cx, cz, step, gameTick, px, pz
                    )

                    if (!cell.isValidWater) {
                        continue
                    }

                    val waterSurfaceY = cell.waterSurfaceY
                    val floorY = cell.floorY.toFloat()
                    val size = step.toFloat()

                    
                    val rx = x.toFloat() - camPos.x.toFloat()
                    val rz = z.toFloat() - camPos.z.toFloat()

                    for (i in 0 until NUM_LAYERS) {
                        val layerCenterY = waterSurfaceY - (i * depthStep) - (depthStep * 0.5f)

                        if (layerCenterY <= floorY) continue

                        val physicalDepth = waterSurfaceY - layerCenterY
                        val xRatio = (physicalDepth / targetDarknessDepth).coerceIn(0f, 1f)
                        val curvedRatio = (3f * xRatio * xRatio) - (2f * xRatio * xRatio * xRatio)

                        val baseAlpha = BASE_ALPHA_MIN + (BASE_ALPHA_RANGE * curvedRatio)
                        val finalAlpha = (baseAlpha - lampReduction).coerceIn(0f, 1f)

                        if (finalAlpha <= ALPHA_RENDER_THRESHOLD) continue

                        val ry = layerCenterY - camPos.y.toFloat()

                        drawDoubleSidedQuad(
                            buffer, matrix4f,
                            rx, ry, rz, size,
                            COLOR_R, COLOR_G, COLOR_B,
                            finalAlpha 
                        )
                    }
                }
            }
        }

        bufferSource.endBatch(RenderTypes.entityTranslucent(WHITE_TEXTURE))
        poseStack.popPose()
    }

    private fun snapToGrid(value: Int, snap: Int): Int {
        return if (value >= 0) {
            (value / snap) * snap
        } else {
            ((value - snap + 1) / snap) * snap
        }
    }

    private fun drawDoubleSidedQuad(
        buffer: VertexConsumer, matrix: Matrix4f,
        x: Float, y: Float, z: Float, size: Float,
        r: Float, g: Float, b: Float, alpha: Float
    ) {
        fun v(vx: Float, vy: Float, vz: Float, nx: Float, ny: Float, nz: Float) {
            buffer.addVertex(matrix, vx, vy, vz)
                .setColor(r, g, b, alpha)
                .setUv(UV_CENTER, UV_CENTER)
                .setOverlay(DEFAULT_OVERLAY)
                .setLight(FULL_BRIGHT_LIGHTMAP)
                .setNormal(nx, ny, nz)
        }

        v(x, y, z, 0f, 1f, 0f)
        v(x, y, z + size, 0f, 1f, 0f)
        v(x + size, y, z + size, 0f, 1f, 0f)
        v(x + size, y, z, 0f, 1f, 0f)

        v(x, y, z, 0f, -1f, 0f)
        v(x + size, y, z, 0f, -1f, 0f)
        v(x + size, y, z + size, 0f, -1f, 0f)
        v(x, y, z + size, 0f, -1f, 0f)
    }
}