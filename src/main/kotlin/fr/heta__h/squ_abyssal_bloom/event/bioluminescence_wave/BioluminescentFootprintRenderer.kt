package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.compat.iris.IrisRenderState
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.footprint.BioluminescentFootprint
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.footprint.BioluminescentFootprintField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.pipeline.BioluminescentRenderPipelines
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.FULL_BRIGHT_LIGHTMAP
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import kotlin.math.abs

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentFootprintRenderer {
    private val WHITE_TEXTURE = Identifier.fromNamespaceAndPath(
        SquAbyssalBloom.ID,
        "textures/misc/white.png"
    )

    private const val SURFACE_OFFSET = 0.009
    private const val GRID_RESOLUTION = 12
    private const val FULL_OPACITY_HALF_EXTENT = 0.10
    private const val TRANSLUCENT_BASE_WEIGHT = 0.58
    private const val MINIMUM_CELL_OPACITY = 0.003f

    private val lateralCoordinates = footprintCoordinates(BioluminescentFootprint.HALF_WIDTH)
    private val lengthCoordinates = footprintCoordinates(BioluminescentFootprint.HALF_LENGTH)
    private val squareOpacityGrid = buildSquareOpacityGrid()

    private val vanillaRenderType: RenderType by lazy {
        createRenderType("squ_bioluminescent_footprint", BioluminescentRenderPipelines.VANILLA_FOOTPRINT)
    }

    private val shaderRenderType: RenderType by lazy {
        createRenderType(
            "squ_bioluminescent_shader_footprint",
            BioluminescentRenderPipelines.SHADER_FOOTPRINT
        )
    }

    @SubscribeEvent
    fun onRenderLevelOpaque(event: RenderLevelStageEvent.AfterOpaqueBlocks) {
        if (!canRender() || !ModCompat.hasIris) return
        IrisRenderState.refresh()
        if (IrisRenderState.renderingShadowPass || !IrisRenderState.shaderPackInUse) return
        render(event.poseStack, shaderRenderType)
    }

    @SubscribeEvent
    fun onRenderLevelTranslucent(event: RenderLevelStageEvent.AfterTranslucentBlocks) {
        if (!canRender()) return
        if (ModCompat.hasIris) {
            IrisRenderState.refresh()
            if (IrisRenderState.shaderPackInUse) return
        }
        render(event.poseStack, vanillaRenderType)
    }

    private fun canRender(): Boolean {
        return ModConfig.enableBioluminescenceRendering &&
            ModConfig.enableBioluminescenceFootprints &&
            ModConfig.bioluminescenceFootprintOpacity > 0.0 &&
            BioluminescentFootprintField.hasFootprints
    }

    private fun createRenderType(name: String, pipeline: RenderPipeline): RenderType {
        return RenderType.create(
            name,
            RenderSetup.builder(pipeline)
                .withTexture("Sampler0", WHITE_TEXTURE)
                .sortOnUpload()
                .createRenderSetup()
        )
    }

    private fun render(poseStack: PoseStack, renderType: RenderType) {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        val camera = minecraft.gameRenderer.mainCamera
        val cameraPosition = camera.position()
        val frustum = camera.cullFrustum
        val renderDistance = ModConfig.bioluminescenceRenderDistance.coerceIn(32.0, 256.0)
        val maximumDistanceSquared = renderDistance * renderDistance
        val renderGameTime = level.gameTime + minecraft.deltaTracker.gameTimeDeltaTicks.toDouble()
        val bufferSource = minecraft.renderBuffers().bufferSource()

        poseStack.pushPose()
        val pose = poseStack.last()
        val consumer = bufferSource.getBuffer(renderType)
        BioluminescentFootprintField.forEachVisible(renderGameTime) { footprint, opacity ->
            val deltaX = footprint.centerX - cameraPosition.x
            val deltaY = footprint.surfaceY - cameraPosition.y
            val deltaZ = footprint.centerZ - cameraPosition.z
            if (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > maximumDistanceSquared) {
                return@forEachVisible
            }
            if (!frustum.isVisible(footprintBounds(footprint))) return@forEachVisible
            emitFootprint(consumer, pose, cameraPosition, footprint, opacity)
        }
        bufferSource.endBatch(renderType)
        poseStack.popPose()
    }

    private fun footprintBounds(footprint: BioluminescentFootprint): AABB {
        return AABB(
            footprint.centerX - BioluminescentFootprint.BOUNDING_RADIUS,
            footprint.surfaceY - 0.02,
            footprint.centerZ - BioluminescentFootprint.BOUNDING_RADIUS,
            footprint.centerX + BioluminescentFootprint.BOUNDING_RADIUS,
            footprint.surfaceY + 0.04,
            footprint.centerZ + BioluminescentFootprint.BOUNDING_RADIUS
        )
    }

    private fun emitFootprint(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        cameraPosition: Vec3,
        footprint: BioluminescentFootprint,
        opacity: Float
    ) {
        val centerX = (footprint.centerX - cameraPosition.x).toFloat()
        val centerY = (footprint.surfaceY + SURFACE_OFFSET - cameraPosition.y).toFloat()
        val centerZ = (footprint.centerZ - cameraPosition.z).toFloat()
        val forwardX = footprint.forwardX
        val forwardZ = footprint.forwardZ
        val rightX = -forwardZ
        val rightZ = forwardX

        for (lengthIndex in 0 until GRID_RESOLUTION) {
            for (widthIndex in 0 until GRID_RESOLUTION) {
                val firstOpacity = shapeOpacity(widthIndex, lengthIndex)
                val secondOpacity = shapeOpacity(widthIndex + 1, lengthIndex)
                val thirdOpacity = shapeOpacity(widthIndex + 1, lengthIndex + 1)
                val fourthOpacity = shapeOpacity(widthIndex, lengthIndex + 1)
                if (
                    maxOf(firstOpacity, secondOpacity, thirdOpacity, fourthOpacity) <=
                    MINIMUM_CELL_OPACITY
                ) continue

                footprintVertex(
                    consumer, pose, centerX, centerY, centerZ,
                    forwardX, forwardZ, rightX, rightZ,
                    footprint.color, opacity, widthIndex, lengthIndex, firstOpacity
                )
                footprintVertex(
                    consumer, pose, centerX, centerY, centerZ,
                    forwardX, forwardZ, rightX, rightZ,
                    footprint.color, opacity, widthIndex + 1, lengthIndex, secondOpacity
                )
                footprintVertex(
                    consumer, pose, centerX, centerY, centerZ,
                    forwardX, forwardZ, rightX, rightZ,
                    footprint.color, opacity, widthIndex + 1, lengthIndex + 1, thirdOpacity
                )
                footprintVertex(
                    consumer, pose, centerX, centerY, centerZ,
                    forwardX, forwardZ, rightX, rightZ,
                    footprint.color, opacity, widthIndex, lengthIndex + 1, fourthOpacity
                )
            }
        }
    }

    private fun shapeOpacity(
        widthIndex: Int,
        lengthIndex: Int
    ): Float {
        return squareOpacityGrid[widthIndex][lengthIndex]
    }

    private fun footprintVertex(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        forwardX: Double,
        forwardZ: Double,
        rightX: Double,
        rightZ: Double,
        color: Int,
        opacity: Float,
        widthIndex: Int,
        lengthIndex: Int,
        shapeOpacity: Float
    ) {
        val lateralOffset = lateralCoordinates[widthIndex]
        val lengthOffset = lengthCoordinates[lengthIndex]
        val x = centerX + (rightX * lateralOffset + forwardX * lengthOffset).toFloat()
        val z = centerZ + (rightZ * lateralOffset + forwardZ * lengthOffset).toFloat()
        val u = widthIndex.toFloat() / GRID_RESOLUTION
        val v = lengthIndex.toFloat() / GRID_RESOLUTION
        vertex(consumer, pose, x, centerY, z, u, v, color, opacity * shapeOpacity)
    }

    private fun footprintCoordinates(halfExtent: Double): DoubleArray {
        return DoubleArray(GRID_RESOLUTION + 1) { index ->
            -halfExtent + halfExtent * 2.0 * index / GRID_RESOLUTION
        }
    }

    private fun buildSquareOpacityGrid(): Array<FloatArray> {
        return Array(GRID_RESOLUTION + 1) { widthIndex ->
            val normalizedLateral = lateralCoordinates[widthIndex] / BioluminescentFootprint.HALF_WIDTH
            FloatArray(GRID_RESOLUTION + 1) { lengthIndex ->
                val normalizedLength = lengthCoordinates[lengthIndex] / BioluminescentFootprint.HALF_LENGTH
                squareShapeOpacity(normalizedLateral, normalizedLength)
            }
        }
    }

    private fun squareShapeOpacity(lateral: Double, length: Double): Float {
        val squareDistance = maxOf(abs(lateral), abs(length))
        val fadeProgress = (
            (squareDistance - FULL_OPACITY_HALF_EXTENT) / (1.0 - FULL_OPACITY_HALF_EXTENT)
            ).coerceIn(0.0, 1.0)
        val smoothFade = fadeProgress * fadeProgress * (3.0 - 2.0 * fadeProgress)
        val edgeFade = 1.0 - smoothFade
        val translucentWeight = TRANSLUCENT_BASE_WEIGHT +
            (1.0 - TRANSLUCENT_BASE_WEIGHT) * edgeFade
        return (edgeFade * translucentWeight).toFloat()
    }

    private fun vertex(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        color: Int,
        opacity: Float
    ) {
        val red = color shr 16 and 0xFF
        val green = color shr 8 and 0xFF
        val blue = color and 0xFF
        val alpha = (opacity * 255.0f).toInt().coerceIn(0, 255)
        consumer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT_LIGHTMAP)
            .setNormal(pose, 0.0f, 1.0f, 0.0f)
    }
}
