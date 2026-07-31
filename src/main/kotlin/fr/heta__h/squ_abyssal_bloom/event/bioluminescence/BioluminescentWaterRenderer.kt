package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentBloom
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentWaterRenderer {
    private const val SURFACE_OFFSET = 0.002
    private const val FULL_BRIGHT_LIGHTMAP = 15728880
    private const val RENDER_DISTANCE = 96.0

    @SubscribeEvent
    fun onRenderLevel(event: RenderLevelStageEvent.AfterTranslucentBlocks) {
        val minecraft = Minecraft.getInstance()
        if (minecraft.level == null || BioluminescentBloomManager.activeBlooms.isEmpty()) return

        val camera = minecraft.gameRenderer.mainCamera
        val cameraPos = camera.position()
        val frustum = camera.cullFrustum
        val poseStack = event.poseStack
        val bufferSource = minecraft.renderBuffers().bufferSource()
        val maximumDistanceSquared = RENDER_DISTANCE * RENDER_DISTANCE

        poseStack.pushPose()
        val pose = poseStack.last()

        for (bloom in BioluminescentBloomManager.activeBlooms) {
            if (!bloom.texture.isVisible || !bloom.hasRenderableMask()) continue
            if (bloom.horizontalDistanceSquared(cameraPos.x, cameraPos.z) > maximumDistanceSquared) continue
            if (!frustum.isVisible(bloom.bounds)) continue

            val consumer = bufferSource.getBuffer(bloom.renderType)
            renderBloom(bloom, consumer, pose, cameraPos)
            bufferSource.endBatch(bloom.renderType)
        }

        poseStack.popPose()
    }

    private fun renderBloom(
        bloom: BioluminescentBloom,
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        cameraPos: Vec3
    ) {
        for (index in 0 until bloom.cellCount) {
            if (!bloom.ownsCell(index)) continue
            val surfaceY = bloom.surfaceYAt(index)
            if (surfaceY.isNaN()) continue

            val cellX = index % bloom.widthInBlocks
            val cellZ = index / bloom.widthInBlocks
            val minX = (bloom.originX + cellX - cameraPos.x).toFloat()
            val maxX = (bloom.originX + cellX + 1.0 - cameraPos.x).toFloat()
            val minZ = (bloom.originZ + cellZ - cameraPos.z).toFloat()
            val maxZ = (bloom.originZ + cellZ + 1.0 - cameraPos.z).toFloat()
            val y = (surfaceY + SURFACE_OFFSET - cameraPos.y).toFloat()

            val minU = cellX.toFloat() / bloom.widthInBlocks
            val maxU = (cellX + 1).toFloat() / bloom.widthInBlocks
            val minV = cellZ.toFloat() / bloom.lengthInBlocks
            val maxV = (cellZ + 1).toFloat() / bloom.lengthInBlocks

            vertex(consumer, pose, minX, y, minZ, minU, minV, 1.0f)
            vertex(consumer, pose, minX, y, maxZ, minU, maxV, 1.0f)
            vertex(consumer, pose, maxX, y, maxZ, maxU, maxV, 1.0f)
            vertex(consumer, pose, maxX, y, minZ, maxU, minV, 1.0f)

            vertex(consumer, pose, minX, y, minZ, minU, minV, -1.0f)
            vertex(consumer, pose, maxX, y, minZ, maxU, minV, -1.0f)
            vertex(consumer, pose, maxX, y, maxZ, maxU, maxV, -1.0f)
            vertex(consumer, pose, minX, y, maxZ, minU, maxV, -1.0f)
        }
    }

    private fun vertex(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        normalY: Float
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT_LIGHTMAP)
            .setNormal(pose, 0.0f, normalY, 0.0f)
    }
}
