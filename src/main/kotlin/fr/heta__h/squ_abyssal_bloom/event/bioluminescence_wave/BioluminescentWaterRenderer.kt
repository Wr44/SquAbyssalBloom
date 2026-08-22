package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.compat.iris.IrisRenderState
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.bloom.BioluminescentBloomRenderer
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.FULL_BRIGHT_LIGHTMAP
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.WHITE_RGB
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentCompensation
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentCompensation.VISIBILITY_EPSILON
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentRenderOpacity.ACTIVE_WAVE_MAXIMUM_OPACITY
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentRenderOpacity.INACTIVE_WAVE_MAXIMUM_OPACITY
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.material.FogType
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import kotlin.math.sqrt

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentWaterRenderer {

    private const val SURFACE_OFFSET = 0.004
    private const val MAX_TEMPORAL_ALPHA = 179
    private const val WAVE_HIGHLIGHT_COLOR = 0x8A2BE2
    private const val HIGHLIGHT_COLOR_STRENGTH = 0.7
    private const val ACTIVE_MOVEMENT_WAVE_OPACITY = 0.20f
    private const val BLOOM_PULSE_ACTIVE_OPACITY = 0.85f
    private const val BLOOM_PULSE_REVEAL_OPACITY = 0.55f
    private const val BLOOM_MOAT_INNER_RADIUS = 1.5
    private const val BLOOM_MOAT_OUTER_RADIUS = 5.0
    private const val BLOOM_MOAT_MIN_FACTOR = 0.35

    private var framePlan: FramePlan? = null
    private var irisPassPrepared = false

    private fun currentRenderGameTime(): Double {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return 0.0
        return level.gameTime + minecraft.deltaTracker.gameTimeDeltaTicks.toDouble()
    }

    private class VertexShade(val color: Int, val alpha: Int) {
        fun scaledAlpha(multiplier: Double): VertexShade {
            if (multiplier == 1.0) return this
            return VertexShade(color, (alpha * multiplier).toInt().coerceIn(0, 255))
        }

    }

    private class ResolvedQuad(
        val worldMinX: Double,
        val worldMaxX: Double,
        val worldMinZ: Double,
        val worldMaxZ: Double,
        val surfaceY: Double,
        val renderTop: Boolean,
        val minU: Float,
        val maxU: Float,
        val minV: Float,
        val maxV: Float,
        val northWest: VertexShade,
        val southWest: VertexShade,
        val southEast: VertexShade,
        val northEast: VertexShade,
        val northWestWaterDepth: Double,
        val southWestWaterDepth: Double,
        val southEastWaterDepth: Double,
        val northEastWaterDepth: Double
    )

    private class ResolvedTile(
        val tile: BioluminescentZoneTile,
        val quads: List<ResolvedQuad>
    )

    private class FramePlan(val tiles: List<ResolvedTile>)

    @SubscribeEvent
    fun onRenderLevelOpaque(event: RenderLevelStageEvent.AfterOpaqueBlocks) {
        framePlan = null
        irisPassPrepared = false
        if (!ModConfig.enableBioluminescenceRendering) return
        if (BioluminescentZoneManager.activeZones.isEmpty()) return
        if (!ModCompat.hasIris) return

        IrisRenderState.refresh()
        if (IrisRenderState.renderingShadowPass) return
        if (!IrisRenderState.shaderPackInUse) return

        val plan = prepareFrameState()
        framePlan = plan
        if (plan == null && !BioluminescentBloomRenderer.hasVisibleBlooms(currentRenderGameTime())) return
        irisPassPrepared = true
        renderShaderPrimary(event, plan)
    }

    @SubscribeEvent
    fun onRenderLevelTranslucent(event: RenderLevelStageEvent.AfterTranslucentBlocks) {
        if (!ModConfig.enableBioluminescenceRendering) return
        if (BioluminescentZoneManager.activeZones.isEmpty()) return
        if (ModCompat.hasIris && IrisRenderState.shaderPackInUse) {
            val plan = framePlan
            val prepared = irisPassPrepared
            framePlan = null
            irisPassPrepared = false
            val compensationRequested = ModConfig.shaderBioluminescenceVisibilityCompensation > 0.0
            if (prepared && compensationRequested) {
                renderShaderCompensation(event, plan)
            }
            return
        }

        renderVanillaPass(event)
    }

    private fun prepareFrameState(): FramePlan? {
        val minecraft = Minecraft.getInstance()
        val camera = minecraft.gameRenderer.mainCamera
        val cameraPosition = camera.position()
        val frustum = camera.cullFrustum
        val renderDistance = ModConfig.bioluminescenceRenderDistance.coerceIn(32.0, 256.0)
        val maxDistanceSqr = renderDistance * renderDistance
        val level = minecraft.level ?: return null
        val renderGameTime = level.gameTime + minecraft.deltaTracker.gameTimeDeltaTicks.toDouble()

        val resolvedTiles = ArrayList<ResolvedTile>()
        for (zone in BioluminescentZoneManager.activeZones) {
            if (!zone.hasRenderableTiles) continue
            val lifecycleIntensity = zone.lifecycleIntensityAt(renderGameTime)
            if (lifecycleIntensity <= 0.0f) continue
            val temporalIntensity = zone.temporalIntensityAt(renderGameTime)
            if (zone.activity == BioluminescentZoneActivity.INACTIVE &&
                zone.movementWaveVisibilityStrength() <= VISIBILITY_EPSILON &&
                zone.bloomPulseVisibilityStrength() <= VISIBILITY_EPSILON
            ) continue
            for (tile in zone.tiles) {
                if (!tile.uploaded) continue
                if (tile.horizontalDistanceSqr(cameraPosition.x, cameraPosition.z) > maxDistanceSqr) continue
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
                    movementFadeStrength <= VISIBILITY_EPSILON &&
                    !zone.bloomPulsesAffect(
                        tile.originX.toDouble(),
                        tile.originZ.toDouble(),
                        (tile.originX + BioluminescentZoneTile.TILE_SIZE).toDouble(),
                        (tile.originZ + BioluminescentZoneTile.TILE_SIZE).toDouble(),
                        renderGameTime
                    )
                ) continue
                val localLoadingIntensity = tile.localLoadingIntensityAt(renderGameTime)
                if (localLoadingIntensity <= VISIBILITY_EPSILON) continue
                val visibleLifecycleIntensity = lifecycleIntensity * localLoadingIntensity
                val visibleTemporalIntensity = temporalIntensity * localLoadingIntensity

                val resolvedQuads = ArrayList<ResolvedQuad>(tile.renderQuads.size)
                for (quad in tile.renderQuads) {
                    if (quad.invalidated) continue
                    val renderTop = resolveRenderTop(quad, cameraPosition.y)
                    resolvedQuads.add(
                        ResolvedQuad(
                            worldMinX = tile.originX + quad.minLocalX.toDouble(),
                            worldMaxX = tile.originX + quad.maxLocalX.toDouble(),
                            worldMinZ = tile.originZ + quad.minLocalZ.toDouble(),
                            worldMaxZ = tile.originZ + quad.maxLocalZ.toDouble(),
                            surfaceY = quad.surfaceY,
                            renderTop = renderTop,
                            minU = tile.uAt(quad.minLocalX),
                            maxU = tile.uAt(quad.maxLocalX),
                            minV = tile.vAt(quad.minLocalZ),
                            maxV = tile.vAt(quad.maxLocalZ),
                            northWest = vertexShadeAt(
                                zone, renderGameTime, visibleLifecycleIntensity, visibleTemporalIntensity,
                                quad.northWestPulsePhase,
                                tile.originX + quad.minLocalX.toDouble(),
                                tile.originZ + quad.minLocalZ.toDouble(),
                                movementFadeStrength
                            ),
                            southWest = vertexShadeAt(
                                zone, renderGameTime, visibleLifecycleIntensity, visibleTemporalIntensity,
                                quad.southWestPulsePhase,
                                tile.originX + quad.minLocalX.toDouble(),
                                tile.originZ + quad.maxLocalZ.toDouble(),
                                movementFadeStrength
                            ),
                            southEast = vertexShadeAt(
                                zone, renderGameTime, visibleLifecycleIntensity, visibleTemporalIntensity,
                                quad.southEastPulsePhase,
                                tile.originX + quad.maxLocalX.toDouble(),
                                tile.originZ + quad.maxLocalZ.toDouble(),
                                movementFadeStrength
                            ),
                            northEast = vertexShadeAt(
                                zone, renderGameTime, visibleLifecycleIntensity, visibleTemporalIntensity,
                                quad.northEastPulsePhase,
                                tile.originX + quad.maxLocalX.toDouble(),
                                tile.originZ + quad.minLocalZ.toDouble(),
                                movementFadeStrength
                            ),
                            northWestWaterDepth = quad.northWestWaterDepth,
                            southWestWaterDepth = quad.southWestWaterDepth,
                            southEastWaterDepth = quad.southEastWaterDepth,
                            northEastWaterDepth = quad.northEastWaterDepth
                        )
                    )
                }
                if (resolvedQuads.isNotEmpty()) {
                    resolvedTiles.add(ResolvedTile(tile, resolvedQuads))
                }
            }
        }
        if (resolvedTiles.isEmpty()) return null
        return FramePlan(resolvedTiles)
    }

    private fun renderShaderPrimary(event: RenderLevelStageEvent, plan: FramePlan?) {
        val minecraft = Minecraft.getInstance()
        val cameraPosition = minecraft.gameRenderer.mainCamera.position()
        val bufferSource = minecraft.renderBuffers().bufferSource()
        val poseStack = event.poseStack
        val alphaMultiplier = ModConfig.shaderBioluminescencePrimaryAlphaMultiplier

        poseStack.pushPose()
        val pose = poseStack.last()
        val subsurfaceOffset = ModConfig.shaderBioluminescenceSubsurfaceOffset
        for (resolvedTile in plan?.tiles.orEmpty()) {
            val renderType = resolvedTile.tile.renderTypeShaderUnderwater
            val consumer = bufferSource.getBuffer(renderType)
            for (quad in resolvedTile.quads) {
                emitQuad(
                    consumer, pose, quad, cameraPosition, subsurfaceOffset,
                    quad.northWest.scaledAlpha(alphaMultiplier),
                    quad.southWest.scaledAlpha(alphaMultiplier),
                    quad.southEast.scaledAlpha(alphaMultiplier),
                    quad.northEast.scaledAlpha(alphaMultiplier)
                )
            }
            bufferSource.endBatch(renderType)
        }
        val renderGameTime = currentRenderGameTime()
        BioluminescentBloomRenderer.renderShaderPrimary(
            poseStack, cameraPosition, bufferSource, renderGameTime, subsurfaceOffset, alphaMultiplier
        )
        poseStack.popPose()
    }

    private fun renderShaderCompensation(event: RenderLevelStageEvent, plan: FramePlan?) {
        val minecraft = Minecraft.getInstance()
        val camera = minecraft.gameRenderer.mainCamera
        val cameraPosition = camera.position()
        val bufferSource = minecraft.renderBuffers().bufferSource()
        val poseStack = event.poseStack
        val baseStrength = ModConfig.shaderBioluminescenceVisibilityCompensation
        val cameraUnderwater = camera.fluidInCamera == FogType.WATER
        val cameraEnvironmentMultiplier = if (cameraUnderwater) {
            ModConfig.shaderBioluminescenceUnderwaterCompensation
        } else {
            1.0
        }
        val offset = ModConfig.shaderBioluminescenceSubsurfaceOffset

        poseStack.pushPose()
        val pose = poseStack.last()
        for (resolvedTile in plan?.tiles.orEmpty()) {
            val renderType = resolvedTile.tile.renderTypeShaderCompensation
            val consumer = bufferSource.getBuffer(renderType)
            for (quad in resolvedTile.quads) {
                val angleFactor = angleFactorOf(quad, cameraPosition)
                emitQuad(
                    consumer, pose, quad, cameraPosition, offset,
                    compensationShade(
                        quad.northWest, quad.northWestWaterDepth,
                        baseStrength, angleFactor, cameraEnvironmentMultiplier
                    ),
                    compensationShade(
                        quad.southWest, quad.southWestWaterDepth,
                        baseStrength, angleFactor, cameraEnvironmentMultiplier
                    ),
                    compensationShade(
                        quad.southEast, quad.southEastWaterDepth,
                        baseStrength, angleFactor, cameraEnvironmentMultiplier
                    ),
                    compensationShade(
                        quad.northEast, quad.northEastWaterDepth,
                        baseStrength, angleFactor, cameraEnvironmentMultiplier
                    )
                )
            }
            bufferSource.endBatch(renderType)
        }
        val renderGameTime = currentRenderGameTime()
        BioluminescentBloomRenderer.renderShaderCompensation(
            poseStack, cameraPosition, bufferSource, renderGameTime, offset, baseStrength, cameraEnvironmentMultiplier
        )
        poseStack.popPose()
    }

    private fun depthFactorOf(waterDepth: Double): Double {
        return BioluminescentCompensation.depthFactor(waterDepth)
    }

    private fun angleFactorOf(quad: ResolvedQuad, cameraPosition: Vec3): Double {
        return BioluminescentCompensation.angleFactor(
            (quad.worldMinX + quad.worldMaxX) * 0.5,
            quad.surfaceY,
            (quad.worldMinZ + quad.worldMaxZ) * 0.5,
            cameraPosition
        )
    }

    private fun compensationShade(
        base: VertexShade,
        waterDepth: Double,
        baseStrength: Double,
        angleFactor: Double,
        cameraEnvironmentMultiplier: Double
    ): VertexShade {
        val depthMultiplier = BioluminescentCompensation.depthMultiplier(depthFactorOf(waterDepth))
        return base.scaledAlpha(baseStrength * depthMultiplier * angleFactor * cameraEnvironmentMultiplier)
    }

    private fun emitQuad(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        quad: ResolvedQuad,
        cameraPosition: Vec3,
        subsurfaceOffset: Double,
        northWest: VertexShade,
        southWest: VertexShade,
        southEast: VertexShade,
        northEast: VertexShade
    ) {
        val minX = (quad.worldMinX - cameraPosition.x).toFloat()
        val maxX = (quad.worldMaxX - cameraPosition.x).toFloat()
        val minZ = (quad.worldMinZ - cameraPosition.z).toFloat()
        val maxZ = (quad.worldMaxZ - cameraPosition.z).toFloat()
        val y = (quad.surfaceY - subsurfaceOffset - cameraPosition.y).toFloat()

        if (quad.renderTop) {
            vertex(consumer, pose, minX, y, minZ, quad.minU, quad.minV, 1.0f, northWest)
            vertex(consumer, pose, minX, y, maxZ, quad.minU, quad.maxV, 1.0f, southWest)
            vertex(consumer, pose, maxX, y, maxZ, quad.maxU, quad.maxV, 1.0f, southEast)
            vertex(consumer, pose, maxX, y, minZ, quad.maxU, quad.minV, 1.0f, northEast)
        } else {
            vertex(consumer, pose, minX, y, minZ, quad.minU, quad.minV, -1.0f, northWest)
            vertex(consumer, pose, maxX, y, minZ, quad.maxU, quad.minV, -1.0f, northEast)
            vertex(consumer, pose, maxX, y, maxZ, quad.maxU, quad.maxV, -1.0f, southEast)
            vertex(consumer, pose, minX, y, maxZ, quad.minU, quad.maxV, -1.0f, southWest)
        }
    }

    private fun renderVanillaPass(event: RenderLevelStageEvent.AfterTranslucentBlocks) {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return

        val camera = minecraft.gameRenderer.mainCamera
        val cameraPosition = camera.position()
        val frustum = camera.cullFrustum
        val poseStack = event.poseStack
        val bufferSource = minecraft.renderBuffers().bufferSource()
        val renderDistance = ModConfig.bioluminescenceRenderDistance.coerceIn(32.0, 256.0)
        val maxDistanceSqr = renderDistance * renderDistance
        val renderGameTime = level.gameTime + minecraft.deltaTracker.gameTimeDeltaTicks.toDouble()

        poseStack.pushPose()
        val pose = poseStack.last()
        for (zone in BioluminescentZoneManager.activeZones) {
            if (!zone.hasRenderableTiles) continue
            val lifecycleIntensity = zone.lifecycleIntensityAt(renderGameTime)
            if (lifecycleIntensity <= 0.0f) continue
            val temporalIntensity = zone.temporalIntensityAt(renderGameTime)
            if (zone.activity == BioluminescentZoneActivity.INACTIVE &&
                zone.movementWaveVisibilityStrength() <= VISIBILITY_EPSILON &&
                zone.bloomPulseVisibilityStrength() <= VISIBILITY_EPSILON
            ) continue
            for (tile in zone.tiles) {
                if (!tile.uploaded) continue
                if (tile.horizontalDistanceSqr(cameraPosition.x, cameraPosition.z) >
                    maxDistanceSqr
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
                    movementFadeStrength <= VISIBILITY_EPSILON &&
                    !zone.bloomPulsesAffect(
                        tile.originX.toDouble(),
                        tile.originZ.toDouble(),
                        (tile.originX + BioluminescentZoneTile.TILE_SIZE).toDouble(),
                        (tile.originZ + BioluminescentZoneTile.TILE_SIZE).toDouble(),
                        renderGameTime
                    )
                ) continue
                val localLoadingIntensity = tile.localLoadingIntensityAt(renderGameTime)
                if (localLoadingIntensity <= VISIBILITY_EPSILON) continue
                val consumer = bufferSource.getBuffer(tile.renderTypeVanilla)
                renderVanillaTile(
                    zone,
                    tile,
                    consumer,
                    pose,
                    cameraPosition,
                    renderGameTime,
                    lifecycleIntensity * localLoadingIntensity,
                    temporalIntensity * localLoadingIntensity,
                    movementFadeStrength
                )
                bufferSource.endBatch(tile.renderTypeVanilla)
            }
        }
        BioluminescentBloomRenderer.renderVanilla(poseStack, cameraPosition, bufferSource, renderGameTime)
        poseStack.popPose()
    }

    private fun renderVanillaTile(
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
            if (quad.invalidated) continue
            val minX = (tile.originX + quad.minLocalX - cameraPosition.x).toFloat()
            val maxX = (tile.originX + quad.maxLocalX - cameraPosition.x).toFloat()
            val minZ = (tile.originZ + quad.minLocalZ - cameraPosition.z).toFloat()
            val maxZ = (tile.originZ + quad.maxLocalZ - cameraPosition.z).toFloat()
            val renderTop = resolveRenderTop(quad, cameraPosition.y)
            val surfaceOffset = if (renderTop) SURFACE_OFFSET else -SURFACE_OFFSET
            val y = (quad.surfaceY + surfaceOffset - cameraPosition.y).toFloat()
            val minU = tile.uAt(quad.minLocalX)
            val maxU = tile.uAt(quad.maxLocalX)
            val minV = tile.vAt(quad.minLocalZ)
            val maxV = tile.vAt(quad.maxLocalZ)
            val northWest = vertexShadeAt(
                zone,
                renderGameTime,
                lifecycleIntensity,
                temporalIntensity,
                quad.northWestPulsePhase,
                tile.originX + quad.minLocalX.toDouble(),
                tile.originZ + quad.minLocalZ.toDouble(),
                movementFadeStrength
            )
            val southWest = vertexShadeAt(
                zone,
                renderGameTime,
                lifecycleIntensity,
                temporalIntensity,
                quad.southWestPulsePhase,
                tile.originX + quad.minLocalX.toDouble(),
                tile.originZ + quad.maxLocalZ.toDouble(),
                movementFadeStrength
            )
            val southEast = vertexShadeAt(
                zone,
                renderGameTime,
                lifecycleIntensity,
                temporalIntensity,
                quad.southEastPulsePhase,
                tile.originX + quad.maxLocalX.toDouble(),
                tile.originZ + quad.maxLocalZ.toDouble(),
                movementFadeStrength
            )
            val northEast = vertexShadeAt(
                zone,
                renderGameTime,
                lifecycleIntensity,
                temporalIntensity,
                quad.northEastPulsePhase,
                tile.originX + quad.maxLocalX.toDouble(),
                tile.originZ + quad.minLocalZ.toDouble(),
                movementFadeStrength
            )

            if (renderTop) {
                vertex(consumer, pose, minX, y, minZ, minU, minV, 1.0f, northWest)
                vertex(consumer, pose, minX, y, maxZ, minU, maxV, 1.0f, southWest)
                vertex(consumer, pose, maxX, y, maxZ, maxU, maxV, 1.0f, southEast)
                vertex(consumer, pose, maxX, y, minZ, maxU, minV, 1.0f, northEast)
            } else {
                vertex(consumer, pose, minX, y, minZ, minU, minV, -1.0f, northWest)
                vertex(consumer, pose, maxX, y, minZ, maxU, minV, -1.0f, northEast)
                vertex(consumer, pose, maxX, y, maxZ, maxU, maxV, -1.0f, southEast)
                vertex(consumer, pose, minX, y, maxZ, minU, maxV, -1.0f, southWest)
            }
        }
    }

    private fun resolveRenderTop(quad: BioluminescentZoneTile.RenderQuad, cameraY: Double): Boolean {
        val resolved = BioluminescentCompensation.resolveRenderTop(quad.lastRenderTop, cameraY, quad.surfaceY)
        quad.lastRenderTop = resolved
        return resolved
    }

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
            rawBaseOpacity.coerceAtMost(lifecycleIntensity * ACTIVE_WAVE_MAXIMUM_OPACITY)
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
            INACTIVE_WAVE_MAXIMUM_OPACITY
        }
        val waveOpacity = lifecycleIntensity * waveStrength * waveIntensity
        val baseAlpha = (baseOpacity * 255.0f).toInt().coerceIn(0, MAX_TEMPORAL_ALPHA)
        val waveAlpha = (waveOpacity * 255.0f).toInt()
        var alpha = (baseAlpha + waveAlpha).coerceIn(0, 255)
        if (zone.activity == BioluminescentZoneActivity.ACTIVE && waveIntensity > 0.0f) {
            val floorAlpha =
                (lifecycleIntensity * ACTIVE_WAVE_MAXIMUM_OPACITY * waveIntensity * 255.0f).toInt()
            alpha = maxOf(alpha, floorAlpha).coerceIn(0, 255)
        }

        var color = if (zone.activity == BioluminescentZoneActivity.ACTIVE && waveIntensity > 0.0f) {
            ModUtilities.lerpColor(
                WHITE_RGB,
                WAVE_HIGHLIGHT_COLOR,
                waveIntensity * HIGHLIGHT_COLOR_STRENGTH
            )
        } else {
            WHITE_RGB
        }

        if (!ModConfig.enableBioluminescenceBloomRendering) return VertexShade(color, alpha)

        val pulseStrength = ModConfig.bioluminescenceBloomPulseIntensity
        val bloomPulse = if (pulseStrength > 0.0) {
            (zone.bloomPulseIntensityAt(worldX, worldZ, renderGameTime) * pulseStrength).toFloat()
        } else {
            0.0f
        }
        if (zone.activity == BioluminescentZoneActivity.ACTIVE) {
            val proximityDim = bloomProximityDimFactor(zone, worldX, worldZ)
            if (proximityDim < 1.0) {
                alpha = (alpha * proximityDim).toInt().coerceIn(0, 255)
            }
            if (bloomPulse > 0.0f) {
                val pulseAlpha = (lifecycleIntensity * bloomPulse * BLOOM_PULSE_ACTIVE_OPACITY * 255.0f).toInt()
                alpha = maxOf(alpha, pulseAlpha).coerceIn(0, 255)
                color = ModUtilities.lerpColor(color, WHITE_RGB, bloomPulse.coerceAtMost(1.0f).toDouble())
            }
        } else if (bloomPulse > 0.0f) {
            val revealAlpha = (lifecycleIntensity * bloomPulse * BLOOM_PULSE_REVEAL_OPACITY * 255.0f).toInt()
            alpha = maxOf(alpha, revealAlpha).coerceIn(0, 255)
        }

        return VertexShade(color, alpha)
    }

    private fun bloomProximityDimFactor(zone: BioluminescentZone, worldX: Double, worldZ: Double): Double {
        var factor = 1.0
        for (bloom in zone.blooms.values) {
            if (bloom.lifecycle != PlanktonBloomLifecycle.ACTIVE) continue
            val geometry = bloom.geometry ?: continue
            val deltaX = worldX - geometry.centerX
            val deltaZ = worldZ - geometry.centerZ
            val distance = sqrt(deltaX * deltaX + deltaZ * deltaZ)
            if (distance >= BLOOM_MOAT_OUTER_RADIUS) continue
            val local = BLOOM_MOAT_MIN_FACTOR +
                (1.0 - BLOOM_MOAT_MIN_FACTOR) *
                ModUtilities.smooth(BLOOM_MOAT_INNER_RADIUS, BLOOM_MOAT_OUTER_RADIUS, distance)
            factor = minOf(factor, local)
        }
        return factor
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

}
