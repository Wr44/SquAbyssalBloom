package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
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
    private const val SURFACE_OFFSET = 0.004
    private const val FULL_BRIGHT_LIGHTMAP = 15728880
    private const val RENDER_DISTANCE = 96.0
    private const val MAXIMUM_TEMPORAL_ALPHA = 204

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
        val renderGameTime = level.gameTime + minecraft.deltaTracker.gameTimeDeltaTicks.toDouble()

        poseStack.pushPose()
        val pose = poseStack.last()
        for (zone in BioluminescentZoneManager.activeZones) {
            if (!zone.isReady) continue
            val temporalIntensity = zone.temporalIntensityAt(renderGameTime)
            if (temporalIntensity <= 0.0f) continue
            for (tile in zone.tiles) {
                if (!tile.uploaded) continue
                if (tile.horizontalDistanceSquared(cameraPosition.x, cameraPosition.z) >
                    maximumDistanceSquared
                ) continue
                if (!frustum.isVisible(tile.bounds)) continue
                val consumer = bufferSource.getBuffer(tile.renderType)
                renderTile(
                    zone,
                    tile,
                    consumer,
                    pose,
                    cameraPosition,
                    renderGameTime,
                    temporalIntensity
                )
                bufferSource.endBatch(tile.renderType)
            }
        }
        poseStack.popPose()
    }

    private fun renderTile(
        zone: BioluminescentZone,
        tile: BioluminescentZoneTile,
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        cameraPosition: Vec3,
        renderGameTime: Double,
        temporalIntensity: Float
    ) {
        for (quad in tile.renderQuads) {
            val minimumX = (tile.originX + quad.minimumLocalX - cameraPosition.x).toFloat()
            val maximumX = (tile.originX + quad.maximumLocalX - cameraPosition.x).toFloat()
            val minimumZ = (tile.originZ + quad.minimumLocalZ - cameraPosition.z).toFloat()
            val maximumZ = (tile.originZ + quad.maximumLocalZ - cameraPosition.z).toFloat()
            val renderTop = cameraPosition.y >= quad.surfaceY
            val surfaceOffset = if (renderTop) SURFACE_OFFSET else -SURFACE_OFFSET
            val y = (quad.surfaceY + surfaceOffset - cameraPosition.y).toFloat()
            val minimumU = tile.uAt(quad.minimumLocalX)
            val maximumU = tile.uAt(quad.maximumLocalX)
            val minimumV = tile.vAt(quad.minimumLocalZ)
            val maximumV = tile.vAt(quad.maximumLocalZ)
            val northWestAlpha = temporalAlphaAt(
                zone,
                renderGameTime,
                temporalIntensity,
                quad.northWestPulsePhase
            )
            val southWestAlpha = temporalAlphaAt(
                zone,
                renderGameTime,
                temporalIntensity,
                quad.southWestPulsePhase
            )
            val southEastAlpha = temporalAlphaAt(
                zone,
                renderGameTime,
                temporalIntensity,
                quad.southEastPulsePhase
            )
            val northEastAlpha = temporalAlphaAt(
                zone,
                renderGameTime,
                temporalIntensity,
                quad.northEastPulsePhase
            )

            if (renderTop) {
                vertex(consumer, pose, minimumX, y, minimumZ, minimumU, minimumV, 1.0f, northWestAlpha)
                vertex(consumer, pose, minimumX, y, maximumZ, minimumU, maximumV, 1.0f, southWestAlpha)
                vertex(consumer, pose, maximumX, y, maximumZ, maximumU, maximumV, 1.0f, southEastAlpha)
                vertex(consumer, pose, maximumX, y, minimumZ, maximumU, minimumV, 1.0f, northEastAlpha)
            } else {
                vertex(consumer, pose, minimumX, y, minimumZ, minimumU, minimumV, -1.0f, northWestAlpha)
                vertex(consumer, pose, maximumX, y, minimumZ, maximumU, minimumV, -1.0f, northEastAlpha)
                vertex(consumer, pose, maximumX, y, maximumZ, maximumU, maximumV, -1.0f, southEastAlpha)
                vertex(consumer, pose, minimumX, y, maximumZ, minimumU, maximumV, -1.0f, southWestAlpha)
            }
        }
    }

    private fun temporalAlphaAt(
        zone: BioluminescentZone,
        renderGameTime: Double,
        temporalIntensity: Float,
        spatialPhase: Double
    ): Int {
        return (temporalIntensity * zone.localPulseIntensityAt(renderGameTime, spatialPhase) * 255.0f)
            .toInt().coerceIn(0, MAXIMUM_TEMPORAL_ALPHA)
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
