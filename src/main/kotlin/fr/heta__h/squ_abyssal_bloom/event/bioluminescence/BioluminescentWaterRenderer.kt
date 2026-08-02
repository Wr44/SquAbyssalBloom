package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
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
    private const val MAXIMUM_TEMPORAL_ALPHA = 179
    private const val VISIBILITY_EPSILON = 0.01

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
            val lifecycleIntensity = zone.lifecycleIntensityAt(renderGameTime)
            if (lifecycleIntensity <= 0.0f) continue
            val temporalIntensity = zone.temporalIntensityAt(renderGameTime)
            if (zone.activity == BioluminescentZoneActivity.INACTIVE &&
                zone.movementWaveVisibilityStrength() <= VISIBILITY_EPSILON
            ) continue
            for (tile in zone.tiles) {
                if (!tile.uploaded) continue
                if (tile.horizontalDistanceSquared(cameraPosition.x, cameraPosition.z) >
                    maximumDistanceSquared
                ) continue
                if (!frustum.isVisible(tile.bounds)) continue
                val movementWavesAffectTile = zone.movementWavesAffect(
                    tile.originX.toDouble(),
                    tile.originZ.toDouble(),
                    (tile.originX + BioluminescentZoneTile.TILE_SIZE).toDouble(),
                    (tile.originZ + BioluminescentZoneTile.TILE_SIZE).toDouble(),
                    renderGameTime
                )
                val movementFadeStrength = tile.updateMovementFade(movementWavesAffectTile)
                if (zone.activity == BioluminescentZoneActivity.INACTIVE &&
                    movementFadeStrength <= VISIBILITY_EPSILON
                ) continue
                val consumer = bufferSource.getBuffer(tile.renderType)
                renderTile(
                    zone,
                    tile,
                    consumer,
                    pose,
                    cameraPosition,
                    renderGameTime,
                    lifecycleIntensity,
                    temporalIntensity,
                    movementFadeStrength
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
        lifecycleIntensity: Float,
        temporalIntensity: Float,
        movementFadeStrength: Float
    ) {
        for (quad in tile.renderQuads) {
            val minimumX = (tile.originX + quad.minimumLocalX - cameraPosition.x).toFloat()
            val maximumX = (tile.originX + quad.maximumLocalX - cameraPosition.x).toFloat()
            val minimumZ = (tile.originZ + quad.minimumLocalZ - cameraPosition.z).toFloat()
            val maximumZ = (tile.originZ + quad.maximumLocalZ - cameraPosition.z).toFloat()
            val renderTop = resolveRenderTop(quad, cameraPosition.y)
            val surfaceOffset = if (renderTop) SURFACE_OFFSET else -SURFACE_OFFSET
            val y = (quad.surfaceY + surfaceOffset - cameraPosition.y).toFloat()
            val minimumU = tile.uAt(quad.minimumLocalX)
            val maximumU = tile.uAt(quad.maximumLocalX)
            val minimumV = tile.vAt(quad.minimumLocalZ)
            val maximumV = tile.vAt(quad.maximumLocalZ)
            val northWest = vertexShadeAt(
                zone,
                renderGameTime,
                lifecycleIntensity,
                temporalIntensity,
                quad.northWestPulsePhase,
                tile.originX + quad.minimumLocalX.toDouble(),
                tile.originZ + quad.minimumLocalZ.toDouble(),
                movementFadeStrength
            )
            val southWest = vertexShadeAt(
                zone,
                renderGameTime,
                lifecycleIntensity,
                temporalIntensity,
                quad.southWestPulsePhase,
                tile.originX + quad.minimumLocalX.toDouble(),
                tile.originZ + quad.maximumLocalZ.toDouble(),
                movementFadeStrength
            )
            val southEast = vertexShadeAt(
                zone,
                renderGameTime,
                lifecycleIntensity,
                temporalIntensity,
                quad.southEastPulsePhase,
                tile.originX + quad.maximumLocalX.toDouble(),
                tile.originZ + quad.maximumLocalZ.toDouble(),
                movementFadeStrength
            )
            val northEast = vertexShadeAt(
                zone,
                renderGameTime,
                lifecycleIntensity,
                temporalIntensity,
                quad.northEastPulsePhase,
                tile.originX + quad.maximumLocalX.toDouble(),
                tile.originZ + quad.minimumLocalZ.toDouble(),
                movementFadeStrength
            )

            if (renderTop) {
                vertex(consumer, pose, minimumX, y, minimumZ, minimumU, minimumV, 1.0f, northWest)
                vertex(consumer, pose, minimumX, y, maximumZ, minimumU, maximumV, 1.0f, southWest)
                vertex(consumer, pose, maximumX, y, maximumZ, maximumU, maximumV, 1.0f, southEast)
                vertex(consumer, pose, maximumX, y, minimumZ, maximumU, minimumV, 1.0f, northEast)
            } else {
                vertex(consumer, pose, minimumX, y, minimumZ, minimumU, minimumV, -1.0f, northWest)
                vertex(consumer, pose, maximumX, y, minimumZ, maximumU, minimumV, -1.0f, northEast)
                vertex(consumer, pose, maximumX, y, maximumZ, maximumU, maximumV, -1.0f, southEast)
                vertex(consumer, pose, minimumX, y, maximumZ, minimumU, maximumV, -1.0f, southWest)
            }
        }
    }

    private fun resolveRenderTop(quad: BioluminescentZoneTile.RenderQuad, cameraY: Double): Boolean {
        val previous = quad.lastRenderTop
        val resolved = when {
            previous == null -> cameraY >= quad.surfaceY
            previous && cameraY < quad.surfaceY - RENDER_SIDE_HYSTERESIS -> false
            !previous && cameraY > quad.surfaceY + RENDER_SIDE_HYSTERESIS -> true
            else -> previous
        }
        quad.lastRenderTop = resolved
        return resolved
    }

    private class VertexShade(val color: Int, val alpha: Int)

    private fun vertexShadeAt(
        zone: BioluminescentZone,
        renderGameTime: Double,
        lifecycleIntensity: Float,
        temporalIntensity: Float,
        spatialPhase: Double,
        worldX: Double,
        worldZ: Double,
        movementFadeStrength: Float
    ): VertexShade {
        val baseOpacity = if (zone.activity == BioluminescentZoneActivity.ACTIVE) {
            val rawBaseOpacity = temporalIntensity * zone.localPulseIntensityAt(renderGameTime, spatialPhase)
            rawBaseOpacity.coerceAtMost(lifecycleIntensity * ACTIVE_BASE_MAXIMUM_OPACITY)
        } else {
            0.0f
        }
        val waveIntensity = if (movementFadeStrength > 0.0f) {
            zone.movementWaveIntensityAt(worldX, worldZ, renderGameTime) * movementFadeStrength
        } else {
            0.0f
        }
        val waveStrength = if (zone.activity == BioluminescentZoneActivity.ACTIVE) {
            ACTIVE_MOVEMENT_WAVE_OPACITY
        } else {
            INACTIVE_MOVEMENT_WAVE_OPACITY
        }
        val waveOpacity = lifecycleIntensity * waveStrength * waveIntensity
        val baseAlpha = (baseOpacity * 255.0f).toInt().coerceIn(0, MAXIMUM_TEMPORAL_ALPHA)
        val waveAlpha = (waveOpacity * 255.0f).toInt()
        var alpha = (baseAlpha + waveAlpha).coerceIn(0, 255)
        if (zone.activity == BioluminescentZoneActivity.ACTIVE && waveIntensity > 0.0f) {
            val floorAlpha = (lifecycleIntensity * ACTIVE_WAVE_MINIMUM_OPACITY * waveIntensity * 255.0f).toInt()
            alpha = maxOf(alpha, floorAlpha).coerceIn(0, 255)
        }

        val color = if (zone.activity == BioluminescentZoneActivity.ACTIVE && waveIntensity > 0.0f) {
            ModUtilities.lerpColor(
                WHITE_COLOR,
                WAVE_HIGHLIGHT_COLOR,
                waveIntensity * HIGHLIGHT_COLOR_STRENGTH
            )
        } else {
            WHITE_COLOR
        }
        return VertexShade(color, alpha)
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
        shade: VertexShade
    ) {
        val red = shade.color shr 16 and 0xFF
        val green = shade.color shr 8 and 0xFF
        val blue = shade.color and 0xFF
        consumer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, shade.alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT_LIGHTMAP)
            .setNormal(pose, 0.0f, normalY, 0.0f)
    }

    private const val WHITE_COLOR = 0xFFFFFF
    private const val WAVE_HIGHLIGHT_COLOR = 0x8A2BE2
    private const val HIGHLIGHT_COLOR_STRENGTH = 0.7
    private const val RENDER_SIDE_HYSTERESIS = 0.05
    private const val ACTIVE_BASE_MAXIMUM_OPACITY = 0.60f
    private const val ACTIVE_MOVEMENT_WAVE_OPACITY = 0.20f
    private const val ACTIVE_WAVE_MINIMUM_OPACITY = 0.60f
    private const val INACTIVE_MOVEMENT_WAVE_OPACITY = 0.45f
}
