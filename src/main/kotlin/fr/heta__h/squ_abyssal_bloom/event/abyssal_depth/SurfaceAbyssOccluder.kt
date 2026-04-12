package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.material.FogType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object SurfaceAbyssOccluder {

    private val WHITE_TEXTURE = Identifier.withDefaultNamespace("textures/misc/white.png")

    
    private const val GRID_STEP = 8
    private const val NUM_LAYERS = 8

    
    private const val COLOR_R = 0.0f
    private const val COLOR_G = 0.01f
    private const val COLOR_B = 0.03f

    
    private const val LAMP_REDUCTION_MULTIPLIER = 0.15f
    private const val FADE_THRESHOLD = 0.01f
    private const val ALPHA_RENDER_THRESHOLD = 0.01f

    
    private const val WATER_SEARCH_DEPTH = 10
    private const val INVALID_HEIGHT = -999
    private const val LAYER_Y_OFFSET = 0.05f
    private const val FLOOR_CLEARANCE = 0.5f

    
    private const val BASE_ALPHA_MIN = 0.10f
    private const val BASE_ALPHA_RANGE = 0.25f

    
    private const val FULL_BRIGHT_LIGHTMAP = 15728880
    private const val DEFAULT_OVERLAY = 655360
    private const val UV_CENTER = 0.5f

    @SubscribeEvent
    fun onRenderStage(event: RenderLevelStageEvent.AfterTranslucentBlocks) {
        if (!ModConfig.enableAbyssFog) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera

        if (camera.fluidInCamera == FogType.WATER) return

        val level = mc.level ?: return
        val camPos = camera.position()
        val entity = camera.entity() as? LivingEntity

        val blockCamPos = BlockPos.containing(camPos)

        val isAirPocket = ModUtilities.isDeepUnderwaterAirPocket(level, blockCamPos)

        if (isAirPocket) {
            AbyssDepthCache.refreshIfNeeded(level, blockCamPos)
        } else {
            AbyssDepthCache.refreshSurfaceIfNeeded(level, blockCamPos, entity)
        }

        val poseStack = event.poseStack
        poseStack.pushPose()
        val matrix4f = poseStack.last().pose()

        val bufferSource = mc.renderBuffers().bufferSource()
        val renderType = RenderTypes.entityTranslucent(WHITE_TEXTURE)
        val buffer = bufferSource.getBuffer(renderType)

        val px = camPos.x.toInt()
        val pz = camPos.z.toInt()

        val gridStartX = (px shr 3) shl 3
        val gridStartZ = (pz shr 3) shl 3

        val globalLampInfluence = AbyssDepthCache.displayedLampInfluence.toFloat()
        val lampReduction = globalLampInfluence * ModConfig.nautilusLampInfluence.toFloat() * LAMP_REDUCTION_MULTIPLIER

        val abyssStart = ModConfig.abyssDepthStart.toFloat()
        val abyssMax = ModConfig.abyssMaxDepth.toFloat()
        val totalOcclusionDepth = (abyssStart + abyssMax) / 2.0f

        val depthStep = totalOcclusionDepth / NUM_LAYERS.toFloat()

        val renderDistanceChunks = mc.options.renderDistance().get()
        val dynamicRadius = (renderDistanceChunks * 16).coerceAtMost(256)

        for (x in (gridStartX - dynamicRadius)..(gridStartX + dynamicRadius) step GRID_STEP) {
            for (z in (gridStartZ - dynamicRadius)..(gridStartZ + dynamicRadius) step GRID_STEP) {

                val cx = x + (GRID_STEP / 2)
                val cz = z + (GRID_STEP / 2)

                val distSq = ((cx - px) * (cx - px) + (cz - pz) * (cz - pz)).toFloat()
                val maxDistSq = (dynamicRadius * dynamicRadius).toFloat()

                val distanceFade = if (isAirPocket) {
                    1.0f
                } else {
                    (1.0f - (distSq / maxDistSq)).coerceIn(0f, 1f)
                }

                if (distanceFade <= FADE_THRESHOLD) continue

                var waterSurfaceY = INVALID_HEIGHT

                
                if (isAirPocket) {
                    waterSurfaceY = ModUtilities.findRegionalWaterSurface(level, BlockPos(cx, camPos.y.toInt(), cz))
                    if (waterSurfaceY == INVALID_HEIGHT) {
                        waterSurfaceY = level.seaLevel 
                    }
                } else {
                    val topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz)
                    for (dy in 0..WATER_SEARCH_DEPTH) {
                        val checkY = topY - dy
                        if (level.getFluidState(BlockPos(cx, checkY, cz)).`is`(FluidTags.WATER)) {
                            waterSurfaceY = checkY
                            break
                        }
                    }
                }

                if (waterSurfaceY == INVALID_HEIGHT) continue

                val floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, cx, cz).toFloat()

                val size = GRID_STEP.toFloat()
                val rx = x.toFloat() - camPos.x.toFloat()
                val rz = z.toFloat() - camPos.z.toFloat()

                val layerOrder = if (isAirPocket) {
                    1..NUM_LAYERS
                } else {
                    NUM_LAYERS downTo 1
                }

                for (i in layerOrder) {

                    val currentDepth = i * depthStep
                    val yLayer = (waterSurfaceY - currentDepth) + LAYER_Y_OFFSET

                    val cubeCenterY = yLayer - (depthStep / 2)
                    val checkPos = BlockPos(cx, cubeCenterY.toInt(), cz)
                    val isCubeInWater = !isAirPocket || level.getFluidState(checkPos).`is`(FluidTags.WATER)

                    if (floorY < yLayer - FLOOR_CLEARANCE && isCubeInWater) {
                        val ry = yLayer - camPos.y.toFloat()

                        
                        
                        
                        val depthFraction = i.toFloat() / NUM_LAYERS.toFloat()

                        val baseAlpha = BASE_ALPHA_MIN + (BASE_ALPHA_RANGE * depthFraction)

                        val finalAlpha = (baseAlpha - lampReduction).coerceIn(0f, 1f)

                        if (finalAlpha > ALPHA_RENDER_THRESHOLD) {
                            if (isAirPocket) {
                                drawVolumetricBox(buffer, matrix4f, rx, ry, rz, size, depthStep, COLOR_R, COLOR_G, COLOR_B, finalAlpha * distanceFade)
                            } else {
                                drawDoubleSidedQuad(buffer, matrix4f, rx, ry, rz, size, COLOR_R, COLOR_G, COLOR_B, finalAlpha * distanceFade)
                            }
                        }
                    }
                }
            }
        }

        bufferSource.endBatch(renderType)
        poseStack.popPose()
    }

    private fun drawDoubleSidedQuad(buffer: VertexConsumer, matrix: Matrix4f, x: Float, y: Float, z: Float, size: Float, r: Float, g: Float, b: Float, alpha: Float) {
        fun v(vx: Float, vy: Float, vz: Float, nx: Float, ny: Float, nz: Float) {
            buffer.addVertex(matrix, vx, vy, vz)
                .setColor(r, g, b, alpha)
                .setUv(UV_CENTER, UV_CENTER)
                .setOverlay(DEFAULT_OVERLAY)
                .setLight(FULL_BRIGHT_LIGHTMAP)
                .setNormal(nx, ny, nz)
        }

        v(x, y, z, 0f, 1f, 0f); v(x, y, z + size, 0f, 1f, 0f); v(x + size, y, z + size, 0f, 1f, 0f); v(x + size, y, z, 0f, 1f, 0f)
        v(x, y, z, 0f, -1f, 0f); v(x + size, y, z, 0f, -1f, 0f); v(x + size, y, z + size, 0f, -1f, 0f); v(x, y, z + size, 0f, -1f, 0f)
    }

    private fun drawVolumetricBox(buffer: VertexConsumer, matrix: Matrix4f, x: Float, y: Float, z: Float, w: Float, h: Float, r: Float, g: Float, b: Float, alpha: Float) {
        fun v(vx: Float, vy: Float, vz: Float, nx: Float, ny: Float, nz: Float) {
            buffer.addVertex(matrix, vx, vy, vz)
                .setColor(r, g, b, alpha)
                .setUv(UV_CENTER, UV_CENTER)
                .setOverlay(DEFAULT_OVERLAY)
                .setLight(FULL_BRIGHT_LIGHTMAP)
                .setNormal(nx, ny, nz)
        }

        v(x, y, z, 0f, -1f, 0f); v(x + w, y, z, 0f, -1f, 0f); v(x + w, y, z + w, 0f, -1f, 0f); v(x, y, z + w, 0f, -1f, 0f)
        v(x, y + h, z, 0f, 1f, 0f); v(x, y + h, z + w, 0f, 1f, 0f); v(x + w, y + h, z + w, 0f, 1f, 0f); v(x + w, y + h, z, 0f, 1f, 0f)
        v(x, y, z, 0f, 0f, -1f); v(x, y + h, z, 0f, 0f, -1f); v(x + w, y + h, z, 0f, 0f, -1f); v(x + w, y, z, 0f, 0f, -1f)
        v(x, y, z + w, 0f, 0f, 1f); v(x + w, y, z + w, 0f, 0f, 1f); v(x + w, y + h, z + w, 0f, 0f, 1f); v(x, y + h, z + w, 0f, 0f, 1f)
        v(x, y, z, -1f, 0f, 0f); v(x, y, z + w, -1f, 0f, 0f); v(x, y + h, z + w, -1f, 0f, 0f); v(x, y + h, z, -1f, 0f, 0f)
        v(x + w, y, z, 1f, 0f, 0f); v(x + w, y + h, z, 1f, 0f, 0f); v(x + w, y + h, z + w, 1f, 0f, 0f); v(x + w, y, z + w, 1f, 0f, 0f)
    }
}