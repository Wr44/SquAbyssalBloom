package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.render

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.cache.SurfaceHeightCache
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
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
import net.neoforged.neoforge.event.level.LevelEvent
import org.joml.Matrix4f
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.tags.FluidTags
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object SurfaceAbyssOccluder {

    private val WHITE_TEXTURE = Identifier.withDefaultNamespace("textures/misc/white.png")

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

    private const val LAMP_REDUCTION_MULTIPLIER = 0.15f
    private const val ALPHA_RENDER_THRESHOLD = 0.01f

    private const val FULL_BRIGHT_LIGHTMAP = 15728880
    private const val DEFAULT_OVERLAY = 655360
    private const val UV_CENTER = 0.5f

    private var cachedBands: List<LodBand> = emptyList()
    private var cachedRenderDist = -1
    private var cachedBandWidth = -1

    private val chunkFadeProgress = Long2FloatOpenHashMap(4096).apply {
        defaultReturnValue(0f)
    }
    private val activeKeysThisFrame = it.unimi.dsi.fastutil.longs.LongOpenHashSet(4096)
    private var lastFrameNanos = 0L

    private var lastCeilingCheckTick = -1L
    private var cachedUnderCeiling = false
    private const val CEILING_CHECK_INTERVAL = 5L

    private fun reset() {
        cachedBands = emptyList()
        cachedRenderDist = -1
        cachedBandWidth = -1
        chunkFadeProgress.clear()
        activeKeysThisFrame.clear()
        lastFrameNanos = 0L
        lastCeilingCheckTick = -1L
        cachedUnderCeiling = false
        SurfaceHeightCache.clear()
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level !is ClientLevel) return
        reset()
    }

    private fun buildBands(maxRadius: Int, bandWidth: Int): List<LodBand> {
        val bands = mutableListOf<LodBand>()
        var offset = 0
        var step = BASE_STEP

        while (offset < maxRadius) {
            val width = maxOf(bandWidth, step)
            val isLast = (offset + width) >= maxRadius
            val bandEnd = if (isLast) maxRadius + width else offset + width

            bands.add(LodBand(
                minDist = offset,
                maxDist = bandEnd,
                step = step,
                scanRadius = bandEnd
            ))

            offset = bandEnd
            step *= 2
        }

        return bands
    }

    private fun getBands(maxRadius: Int, bandWidth: Int): List<LodBand> {
        if (maxRadius != cachedRenderDist || bandWidth != cachedBandWidth) {
            cachedBands = buildBands(maxRadius, bandWidth)
            cachedRenderDist = maxRadius
            cachedBandWidth = bandWidth
        }
        return cachedBands
    }

    private fun findMinWaterSurface(level: Level, camPos: Vec3): Int {
        val cx = camPos.x.toInt()
        val cz = camPos.z.toInt()
        val mutPos = BlockPos.MutableBlockPos()

        val beams = mutableListOf<Pair<Int, Int>>()
        val spread = intArrayOf(-16, -8, 0, 8, 16)

        for (s in spread) {
            beams.add(Pair(cx + s, cz - 32))
            beams.add(Pair(cx + s, cz + 32))
            beams.add(Pair(cx - 32, cz + s))
            beams.add(Pair(cx + 32, cz + s))
        }

        var minSurface = Int.MAX_VALUE

        for ((bx, bz) in beams) {
            if (!level.hasChunk(bx shr 4, bz shr 4)) continue

            val topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz)
            var solidStreak = 0

            for (y in topY downTo level.minY) {
                mutPos.set(bx, y, bz)
                val fluidState = level.getFluidState(mutPos)

                if (fluidState.`is`(FluidTags.WATER)) {
                    minSurface = minOf(minSurface, y)
                    break
                }

                val blockState = level.getBlockState(mutPos)
                if (blockState.blocksMotion()) {
                    solidStreak++
                    if (solidStreak > 10) break
                } else {
                    solidStreak = 0
                }
            }
        }

        return minSurface
    }

    @SubscribeEvent
    fun onRenderStage(event: RenderLevelStageEvent.AfterOpaqueFeatures) {
        if (!ModConfig.enableSurfaceOccluder) return

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val gameTick = level.gameTime
        val camera = mc.gameRenderer.mainCamera
        val camPos = camera.position()

        if (gameTick < lastCeilingCheckTick || gameTick - lastCeilingCheckTick >= CEILING_CHECK_INTERVAL) {
            val minSurface = findMinWaterSurface(level, camPos)
            cachedUnderCeiling = camPos.y > minSurface
            lastCeilingCheckTick = gameTick
        }
        if (!cachedUnderCeiling) return
        if (camera.fluidInCamera == FogType.WATER) return

        val now = System.nanoTime()
        val dt = if (lastFrameNanos == 0L) 0f else ((now - lastFrameNanos) / 1_000_000_000.0f).coerceAtMost(0.1f)
        lastFrameNanos = now

        val entity = camera.entity() as? LivingEntity
        if (entity != null && entity.hasEffect(MobEffects.NIGHT_VISION)) return

        val lampInfluence = ModUtilities.getCombinedLampInfluence(entity, level, BlockPos.containing(camPos), 16.0, 1.0).toFloat()
        val lampReduction = lampInfluence * ModConfig.nautilusLampInfluence.toFloat() * LAMP_REDUCTION_MULTIPLIER

        val alphaMin = ModConfig.surfaceOccluderAlphaMin.toFloat()
        val alphaRange = (ModConfig.surfaceOccluderAlphaMax - ModConfig.surfaceOccluderAlphaMin).toFloat()
        val numLayers = ModConfig.surfaceOccluderNumLayers
        val fadeSpeed = ModConfig.surfaceOccluderFadeSpeed.toFloat()
        val bandWidth = ModConfig.surfaceOccluderLodBandWidth

        val targetDepthRatio = ModConfig.surfaceOccluderTargetDepth
        val targetDarknessDepth = maxOf(1.0f,
            (ModConfig.abyssDepthStart + (ModConfig.abyssMaxDepth - ModConfig.abyssDepthStart) * targetDepthRatio).toFloat()
        )

        val maxRadius = mc.options.renderDistance().get() * 16
        val bands = getBands(maxRadius, bandWidth)
        val maxStep = bands.lastOrNull()?.step ?: BASE_STEP

        val px = snapToGrid(camPos.x.toInt(), maxStep)
        val pz = snapToGrid(camPos.z.toInt(), maxStep)

        SurfaceHeightCache.tick(gameTick, px, pz)

        val poseStack = event.poseStack
        poseStack.pushPose()
        val matrix4f = poseStack.last().pose()
        val bufferSource = mc.renderBuffers().bufferSource()

        val renderType = RenderTypes.entityTranslucent(WHITE_TEXTURE)
        val buffer = bufferSource.getBuffer(renderType)

        val depthStep = targetDarknessDepth / numLayers.toFloat()

        val frustum = mc.gameRenderer.mainCamera.cullFrustum

        activeKeysThisFrame.clear()

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

                    val cellData = SurfaceHeightCache.getOrCompute(level, cx, cz, step, gameTick, px, pz)
                    if (!SurfaceHeightCache.unpackIsValidWater(cellData)) continue

                    val waterSurfaceY = SurfaceHeightCache.unpackSurfaceY(cellData)
                    val floorY = SurfaceHeightCache.unpackFloorY(cellData).toFloat()

                    if (!(frustum?.isVisible(
                            AABB(
                                x.toDouble(), floorY.toDouble(), z.toDouble(),
                                (x + step).toDouble(), waterSurfaceY.toDouble(), (z + step).toDouble()
                            )
                        ) ?: true)
                    ) continue

                    val key = chunkKey(cx, cz)
                    val rawFade = chunkFadeProgress.get(key)
                    val newFade = (rawFade + dt * fadeSpeed).coerceAtMost(1.0f)
                    chunkFadeProgress.put(key, newFade)
                    activeKeysThisFrame.add(key)

                    val fadeFactor = newFade * newFade * (3f - 2f * newFade)

                    val size = step.toFloat()
                    val rx = x.toFloat() - camPos.x.toFloat()
                    val rz = z.toFloat() - camPos.z.toFloat()

                    for (i in 0 until numLayers) {
                        val layerCenterY = waterSurfaceY - (i * depthStep) - (depthStep * 0.5f)
                        if (layerCenterY <= floorY) continue

                        val physicalDepth = waterSurfaceY - layerCenterY
                        val xRatio = (physicalDepth / targetDarknessDepth).coerceIn(0f, 1f)
                        val curvedRatio = (3f * xRatio * xRatio) - (2f * xRatio * xRatio * xRatio)

                        val baseAlpha = alphaMin + (alphaRange * curvedRatio)
                        val finalAlpha = (baseAlpha - lampReduction).coerceIn(0f, 1f) * fadeFactor

                        if (finalAlpha <= ALPHA_RENDER_THRESHOLD) continue

                        val ry = layerCenterY - camPos.y.toFloat()

                        drawDoubleSidedQuad(buffer, matrix4f, rx, ry, rz, size, COLOR_R, COLOR_G, COLOR_B, finalAlpha)
                    }
                }
            }
        }

        bufferSource.endBatch(renderType)
        poseStack.popPose()

        val iter = chunkFadeProgress.long2FloatEntrySet().iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (!activeKeysThisFrame.contains(entry.longKey)) iter.remove()
        }
    }

    private fun chunkKey(cx: Int, cz: Int): Long {
        val chunkX = cx shr 4
        val chunkZ = cz shr 4
        return (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFFFFFFL)
    }

    private fun snapToGrid(value: Int, snap: Int): Int {
        return if (value >= 0) (value / snap) * snap
        else ((value - snap + 1) / snap) * snap
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
