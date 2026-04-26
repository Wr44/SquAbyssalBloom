package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.render

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.cache.SurfaceHeightCache
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.material.FogType
import net.minecraft.world.phys.AABB
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.world.effect.MobEffects
import kotlin.math.abs
import kotlin.math.max

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object SurfaceAbyssOccluder {

    private val WHITE_TEXTURE = Identifier.withDefaultNamespace("textures/misc/white.png")

    private const val NUM_LAYERS = 5

    
    private const val BAND_WIDTH = 64
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

    private var cachedBands: List<LodBand> = emptyList()
    private var cachedRenderDist = -1

    
    private val chunkFadeProgress = HashMap<Long, Float>()
    private var lastFrameNanos = 0L
    private const val FADE_SPEED = 2.5f

    private fun buildBands(maxRadius: Int): List<LodBand> {
        val bands = mutableListOf<LodBand>()
        var offset = 0
        var step = BASE_STEP

        while (offset < maxRadius) {
            val width = maxOf(BAND_WIDTH, step)
            val isLast = (offset + width) >= maxRadius
            val bandEnd = if (isLast) maxRadius + width else offset + width

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

    private fun isUnderMassiveCeiling(level: Level, camPos: net.minecraft.world.phys.Vec3): Boolean {
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

        val now = System.nanoTime()
        val dt = if (lastFrameNanos == 0L) 0f else ((now - lastFrameNanos) / 1_000_000_000.0f).coerceAtMost(0.1f)
        lastFrameNanos = now

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera

        if (camera.fluidInCamera == FogType.WATER) return

        val level = mc.level ?: return
        val camPos = camera.position()

        if (isUnderMassiveCeiling(level, camPos)) return

        val entity = camera.entity() as? LivingEntity
        if (entity != null && entity.hasEffect(MobEffects.NIGHT_VISION)) return

        val lampInfluence = if (entity != null) {
            maxOf(
                ModUtilities.getRiderLampInfluence(entity),
                ModUtilities.getNautilusLampInfluence(level, BlockPos.containing(camPos), 16.0, 1.0)
            ).toFloat()
        } else 0f
        val lampReduction = lampInfluence * ModConfig.nautilusLampInfluence.toFloat() * LAMP_REDUCTION_MULTIPLIER

        val gameTick = level.gameTime
        val maxRadius = mc.options.renderDistance().get() * 16

        
        val bands = getBands(maxRadius)
        val maxStep = bands.lastOrNull()?.step ?: BASE_STEP

        
        val px = snapToGrid(camPos.x.toInt(), maxStep)
        val pz = snapToGrid(camPos.z.toInt(), maxStep)

        SurfaceHeightCache.tick(gameTick, px, pz)

        val poseStack = event.poseStack
        poseStack.pushPose()
        val matrix4f = poseStack.last().pose()
        val bufferSource = mc.renderBuffers().bufferSource()
        val buffer = bufferSource.getBuffer(RenderTypes.entityTranslucent(WHITE_TEXTURE))

        val targetDarknessDepth = maxOf(1.0f, (ModConfig.abyssDepthStart + ModConfig.abyssMaxDepth).toFloat() / 2)
        val depthStep = targetDarknessDepth / NUM_LAYERS.toFloat()

        val frustum = Minecraft.getInstance().levelRenderer.capturedFrustum
        val camXi = camPos.x.toInt()
        val camZi = camPos.z.toInt()

        for (band in bands) {
            val step = band.step
            val scanR = band.scanRadius

            val gridMinX = snapToGrid(px - scanR, step)
            val gridMinZ = snapToGrid(pz - scanR, step)
            val gridMaxX = snapToGrid(px + scanR, step)
            val gridMaxZ = snapToGrid(pz + scanR, step)

            for (x in gridMinX..gridMaxX step step) {
                for (z in gridMinZ..gridMaxZ step step) {

                    val cx = x + (step / 2)
                    val cz = z + (step / 2)

                    val dx = cx - px
                    val dz = cz - pz

                    val boxDist = max(abs(dx), abs(dz))
                    if (boxDist < band.minDist || boxDist >= band.maxDist) continue

                    val cell = SurfaceHeightCache.getOrCompute(level, cx, cz, step, gameTick, px, pz)
                    if (!cell.isValidWater) continue

                    if (!(frustum?.isVisible(
                            AABB(
                                x.toDouble(), cell.floorY.toDouble(), z.toDouble(),
                                (x + step).toDouble(), cell.waterSurfaceY.toDouble(), (z + step).toDouble()
                            )
                        ) ?: true)
                    ) continue

                    
                    val key = chunkKey(cx, cz)
                    val rawFade = chunkFadeProgress.getOrDefault(key, 0.0f)
                    val newFade = (rawFade + dt * FADE_SPEED).coerceAtMost(1.0f)
                    chunkFadeProgress[key] = newFade
                    val fadeFactor = newFade * newFade * (3f - 2f * newFade)

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
                        val finalAlpha = (baseAlpha - lampReduction).coerceIn(0f, 1f) * fadeFactor

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

    
    private fun chunkKey(cx: Int, cz: Int): Long {
        val chunkX = cx shr 4
        val chunkZ = cz shr 4
        return (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFFFFFFL)
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