package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
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
        val level = minecraft.level ?: return
        if (BioluminescentZoneManager.activeZones.isEmpty()) return

        val camera = minecraft.gameRenderer.mainCamera
        val cameraPosition = camera.position()
        val frustum = camera.cullFrustum
        val poseStack = event.poseStack
        val bufferSource = minecraft.renderBuffers().bufferSource()
        val maximumDistanceSquared = RENDER_DISTANCE * RENDER_DISTANCE

        poseStack.pushPose()
        val pose = poseStack.last()
        for (zone in BioluminescentZoneManager.activeZones) {
            if (!zone.isReady) continue
            val temporalAlpha = (zone.temporalIntensityAt(level.gameTime) * 255.0f)
                .toInt().coerceIn(0, 255)
            if (temporalAlpha == 0) continue
            for (tile in zone.tiles) {
                if (!tile.uploaded) continue
                if (tile.horizontalDistanceSquared(cameraPosition.x, cameraPosition.z) >
                    maximumDistanceSquared
                ) continue
                if (!frustum.isVisible(tile.bounds)) continue
                val consumer = bufferSource.getBuffer(tile.renderType)
                renderTile(tile, consumer, pose, cameraPosition, temporalAlpha)
                bufferSource.endBatch(tile.renderType)
            }
        }
        poseStack.popPose()
    }

    private fun renderTile(
        tile: BioluminescentZoneTile,
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        cameraPosition: Vec3,
        temporalAlpha: Int
    ) {
        for (localIndex in tile.renderableCellIndices) {
            val localX = localIndex % BioluminescentZoneTile.TILE_SIZE
            val localZ = localIndex / BioluminescentZoneTile.TILE_SIZE
            val minimumX = (tile.originX + localX - cameraPosition.x).toFloat()
            val maximumX = (tile.originX + localX + 1.0 - cameraPosition.x).toFloat()
            val minimumZ = (tile.originZ + localZ - cameraPosition.z).toFloat()
            val maximumZ = (tile.originZ + localZ + 1.0 - cameraPosition.z).toFloat()
            val y = (tile.surfaceYAt(localIndex) + SURFACE_OFFSET - cameraPosition.y).toFloat()
            val minimumU = tile.minimumU(localX)
            val maximumU = tile.maximumU(localX)
            val minimumV = tile.minimumV(localZ)
            val maximumV = tile.maximumV(localZ)

            vertex(consumer, pose, minimumX, y, minimumZ, minimumU, minimumV, 1.0f, temporalAlpha)
            vertex(consumer, pose, minimumX, y, maximumZ, minimumU, maximumV, 1.0f, temporalAlpha)
            vertex(consumer, pose, maximumX, y, maximumZ, maximumU, maximumV, 1.0f, temporalAlpha)
            vertex(consumer, pose, maximumX, y, minimumZ, maximumU, minimumV, 1.0f, temporalAlpha)

            vertex(consumer, pose, minimumX, y, minimumZ, minimumU, minimumV, -1.0f, temporalAlpha)
            vertex(consumer, pose, maximumX, y, minimumZ, maximumU, minimumV, -1.0f, temporalAlpha)
            vertex(consumer, pose, maximumX, y, maximumZ, maximumU, maximumV, -1.0f, temporalAlpha)
            vertex(consumer, pose, minimumX, y, maximumZ, minimumU, maximumV, -1.0f, temporalAlpha)
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
        normalY: Float,
        alpha: Int
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(255, 255, 255, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT_LIGHTMAP)
            .setNormal(pose, 0.0f, normalY, 0.0f)
    }
}
