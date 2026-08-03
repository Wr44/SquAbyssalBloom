package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.compat.iris.IrisRenderState
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.material.FogType
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import kotlin.math.abs
import kotlin.math.sqrt

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentWaterRenderer {

    private const val SURFACE_OFFSET = 0.004
    private const val SHADER_UNDERWATER_OFFSET = 0.02
    private const val SHADER_COMPENSATION_OFFSET = 0.006
    private const val SHALLOW_DEPTH = 4.0
    private const val DEEP_DEPTH = 24.0
    private const val SHALLOW_COMPENSATION_MULTIPLIER = 0.65
    private const val GRAZING_VISIBILITY_START = 0.15
    private const val TOP_VIEW_VISIBILITY_FULL = 0.65
    private const val DEBUG_FIXED_ALPHA = 200
    private const val DEPTH_VISUALIZATION_COLOR = 0xFF3030
    private const val ANGLE_VISUALIZATION_COLOR = 0x30FF60
    private const val UNDERWATER_VISUALIZATION_COLOR = 0x3060FF
    private const val FULL_BRIGHT_LIGHTMAP = 15728880
    private const val MAX_TEMPORAL_ALPHA = 179
    private const val VISIBILITY_EPSILON = 0.01
    private const val WHITE_COLOR = 0xFFFFFF
    private const val WAVE_HIGHLIGHT_COLOR = 0x8A2BE2
    private const val HIGHLIGHT_COLOR_STRENGTH = 0.7
    private const val RENDER_SIDE_HYSTERESIS = 0.05
    private const val ACTIVE_BASE_MAX_OPACITY = 0.60f
    private const val ACTIVE_MOVEMENT_WAVE_OPACITY = 0.20f
    private const val ACTIVE_WAVE_MIN_OPACITY = 0.60f
    private const val INACTIVE_MOVEMENT_WAVE_OPACITY = 0.45f

    private var framePlan: FramePlan? = null

    private class VertexShade(val color: Int, val alpha: Int) {
        fun scaledAlpha(multiplier: Double): VertexShade {
            if (multiplier == 1.0) return this
            return VertexShade(color, (alpha * multiplier).toInt().coerceIn(0, 255))
        }

        fun withAlpha(newAlpha: Int): VertexShade {
            return VertexShade(color, newAlpha.coerceIn(0, 255))
        }

        fun withColor(newColor: Int): VertexShade {
            return VertexShade(newColor, alpha)
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
        IrisRenderState.beginFrame()
        framePlan = null
        if (!ModConfig.enableBioluminescenceRendering) return
        if (BioluminescentZoneManager.activeZones.isEmpty()) return
        if (!ModCompat.hasIris) return

        IrisRenderState.refresh()
        if (IrisRenderState.renderingShadowPass) {
            IrisRenderState.recordShadowPassSkip()
            return
        }
        if (!IrisRenderState.shaderPackInUse) return

        val plan = prepareFrameState() ?: return
        framePlan = plan
        if (BioluminescentIrisDebugState.mode != BioluminescentIrisDebugMode.COMPENSATION_ONLY) {
            renderShaderPrimary(event, plan)
        }
    }

    @SubscribeEvent
    fun onRenderLevelTranslucent(event: RenderLevelStageEvent.AfterTranslucentBlocks) {
        if (!ModConfig.enableBioluminescenceRendering) return
        if (BioluminescentZoneManager.activeZones.isEmpty()) return
        if (ModCompat.hasIris && IrisRenderState.shaderPackInUse) {
            val plan = framePlan
            framePlan = null
            val debugMode = BioluminescentIrisDebugState.mode
            val compensationRequested = ModConfig.shaderBioluminescenceVisibilityCompensation > 0.0
            if (plan != null && compensationRequested && debugMode != BioluminescentIrisDebugMode.PRIMARY_ONLY) {
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
                zone.movementWaveVisibilityStrength() <= VISIBILITY_EPSILON
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
                    movementFadeStrength <= VISIBILITY_EPSILON
                ) continue
                val localLoadingIntensity = tile.localLoadingIntensityAt(renderGameTime)
                if (localLoadingIntensity <= VISIBILITY_EPSILON) continue
                val visibleLifecycleIntensity = lifecycleIntensity * localLoadingIntensity
                val visibleTemporalIntensity = temporalIntensity * localLoadingIntensity

                val resolvedQuads = ArrayList<ResolvedQuad>(tile.renderQuads.size)
                for (quad in tile.renderQuads) {
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

    private fun renderShaderPrimary(event: RenderLevelStageEvent, plan: FramePlan) {
        val minecraft = Minecraft.getInstance()
        val cameraPosition = minecraft.gameRenderer.mainCamera.position()
        val bufferSource = minecraft.renderBuffers().bufferSource()
        val poseStack = event.poseStack
        val alphaMultiplier = ModConfig.shaderBioluminescencePrimaryAlphaMultiplier
        var quadsSubmitted = 0

        poseStack.pushPose()
        val pose = poseStack.last()
        for (resolvedTile in plan.tiles) {
            val renderType = resolvedTile.tile.renderTypeShaderUnderwater
            val consumer = bufferSource.getBuffer(renderType)
            for (quad in resolvedTile.quads) {
                emitQuad(
                    consumer, pose, quad, cameraPosition, SHADER_UNDERWATER_OFFSET,
                    quad.northWest.scaledAlpha(alphaMultiplier),
                    quad.southWest.scaledAlpha(alphaMultiplier),
                    quad.southEast.scaledAlpha(alphaMultiplier),
                    quad.northEast.scaledAlpha(alphaMultiplier)
                )
                quadsSubmitted++
            }
            bufferSource.endBatch(renderType)
        }
        poseStack.popPose()

        IrisRenderState.recordShaderPrimaryPass(quadsSubmitted)
    }

    private fun renderShaderCompensation(event: RenderLevelStageEvent, plan: FramePlan) {
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
        val offset = BioluminescentIrisDebugState.compensationOffsetOverride ?: SHADER_COMPENSATION_OFFSET
        val debugMode = BioluminescentIrisDebugState.mode
        var quadsSubmitted = 0
        var depthFactorSum = 0.0
        var depthFactorMin = Double.POSITIVE_INFINITY
        var depthFactorMax = Double.NEGATIVE_INFINITY
        var cornerCount = 0
        var angleFactorSum = 0.0
        var multiplierSum = 0.0
        var quadCount = 0

        poseStack.pushPose()
        val pose = poseStack.last()
        for (resolvedTile in plan.tiles) {
            val renderType = resolvedTile.tile.renderTypeShaderCompensation
            val consumer = bufferSource.getBuffer(renderType)
            for (quad in resolvedTile.quads) {
                val depthFactorNW = depthFactorOf(quad.northWestWaterDepth)
                val depthFactorSW = depthFactorOf(quad.southWestWaterDepth)
                val depthFactorSE = depthFactorOf(quad.southEastWaterDepth)
                val depthFactorNE = depthFactorOf(quad.northEastWaterDepth)
                depthFactorSum += depthFactorNW + depthFactorSW + depthFactorSE + depthFactorNE
                depthFactorMin = minOf(depthFactorMin, depthFactorNW, depthFactorSW, depthFactorSE, depthFactorNE)
                depthFactorMax = maxOf(depthFactorMax, depthFactorNW, depthFactorSW, depthFactorSE, depthFactorNE)
                cornerCount += 4

                val finalAngleFactor = angleFactorOf(quad, cameraPosition)
                angleFactorSum += finalAngleFactor
                quadCount++

                val multiplierNW = compensationMultiplier(baseStrength, depthFactorNW, finalAngleFactor, cameraEnvironmentMultiplier, debugMode)
                val multiplierSW = compensationMultiplier(baseStrength, depthFactorSW, finalAngleFactor, cameraEnvironmentMultiplier, debugMode)
                val multiplierSE = compensationMultiplier(baseStrength, depthFactorSE, finalAngleFactor, cameraEnvironmentMultiplier, debugMode)
                val multiplierNE = compensationMultiplier(baseStrength, depthFactorNE, finalAngleFactor, cameraEnvironmentMultiplier, debugMode)
                multiplierSum += multiplierNW + multiplierSW + multiplierSE + multiplierNE

                var northWest = compensationShade(quad.northWest, multiplierNW, debugMode)
                var southWest = compensationShade(quad.southWest, multiplierSW, debugMode)
                var southEast = compensationShade(quad.southEast, multiplierSE, debugMode)
                var northEast = compensationShade(quad.northEast, multiplierNE, debugMode)

                if (debugMode == BioluminescentIrisDebugMode.COMPENSATION_FACTOR_VISUALIZATION) {
                    northWest = visualizeFactors(northWest, depthFactorNW, finalAngleFactor, cameraUnderwater)
                    southWest = visualizeFactors(southWest, depthFactorSW, finalAngleFactor, cameraUnderwater)
                    southEast = visualizeFactors(southEast, depthFactorSE, finalAngleFactor, cameraUnderwater)
                    northEast = visualizeFactors(northEast, depthFactorNE, finalAngleFactor, cameraUnderwater)
                }

                emitQuad(consumer, pose, quad, cameraPosition, offset, northWest, southWest, southEast, northEast)
                quadsSubmitted++
            }
            bufferSource.endBatch(renderType)
        }
        poseStack.popPose()

        IrisRenderState.recordShaderCompensationPass(
            quadsSubmitted,
            if (cornerCount > 0) depthFactorSum / cornerCount else 0.0,
            if (cornerCount > 0) depthFactorMin else 0.0,
            if (cornerCount > 0) depthFactorMax else 0.0,
            if (quadCount > 0) angleFactorSum / quadCount else 0.0,
            if (cornerCount > 0) multiplierSum / cornerCount else 0.0,
            cameraUnderwater
        )
    }

    private fun depthFactorOf(waterDepth: Double): Double {
        return ModUtilities.smoothstep(SHALLOW_DEPTH, DEEP_DEPTH, waterDepth)
    }

    private fun angleFactorOf(quad: ResolvedQuad, cameraPosition: Vec3): Double {
        val centerX = (quad.worldMinX + quad.worldMaxX) * 0.5
        val centerZ = (quad.worldMinZ + quad.worldMaxZ) * 0.5
        val dx = centerX - cameraPosition.x
        val dy = quad.surfaceY - cameraPosition.y
        val dz = centerZ - cameraPosition.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        val viewAlignment = if (distance > 1.0e-6) (abs(dy) / distance) else 1.0
        val angleFactor = ModUtilities.smoothstep(GRAZING_VISIBILITY_START, TOP_VIEW_VISIBILITY_FULL, viewAlignment)
        return lerp(ModConfig.shaderBioluminescenceGrazingStrength, 1.0, angleFactor)
    }

    private fun compensationMultiplier(
        baseStrength: Double,
        depthFactor: Double,
        finalAngleFactor: Double,
        cameraEnvironmentMultiplier: Double,
        debugMode: BioluminescentIrisDebugMode
    ): Double {
        if (debugMode == BioluminescentIrisDebugMode.COMPENSATION_IGNORE_MODULATION) return baseStrength
        val depthMultiplier = lerp(SHALLOW_COMPENSATION_MULTIPLIER, ModConfig.shaderBioluminescenceDeepWaterBoost, depthFactor)
        return baseStrength * depthMultiplier * finalAngleFactor * cameraEnvironmentMultiplier
    }

    private fun compensationShade(
        base: VertexShade,
        multiplier: Double,
        debugMode: BioluminescentIrisDebugMode
    ): VertexShade {
        if (debugMode == BioluminescentIrisDebugMode.COMPENSATION_FIXED_ALPHA) {
            return base.withAlpha(DEBUG_FIXED_ALPHA)
        }
        return base.scaledAlpha(multiplier)
    }

    private fun visualizeFactors(
        shade: VertexShade,
        depthFactor: Double,
        finalAngleFactor: Double,
        cameraUnderwater: Boolean
    ): VertexShade {
        val color = when {
            cameraUnderwater -> UNDERWATER_VISUALIZATION_COLOR
            depthFactor >= (1.0 - finalAngleFactor) -> DEPTH_VISUALIZATION_COLOR
            else -> ANGLE_VISUALIZATION_COLOR
        }
        return shade.withColor(color)
    }

    private fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t
    }

    private fun emitQuad(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        quad: ResolvedQuad,
        cameraPosition: Vec3,
        extraYOffset: Double,
        northWest: VertexShade,
        southWest: VertexShade,
        southEast: VertexShade,
        northEast: VertexShade
    ) {
        val minX = (quad.worldMinX - cameraPosition.x).toFloat()
        val maxX = (quad.worldMaxX - cameraPosition.x).toFloat()
        val minZ = (quad.worldMinZ - cameraPosition.z).toFloat()
        val maxZ = (quad.worldMaxZ - cameraPosition.z).toFloat()
        val surfaceOffset = if (quad.renderTop) SURFACE_OFFSET else -SURFACE_OFFSET
        val y = (quad.surfaceY + surfaceOffset + extraYOffset - cameraPosition.y).toFloat()

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
        var quadsSubmitted = 0

        poseStack.pushPose()
        val pose = poseStack.last()
        for (zone in BioluminescentZoneManager.activeZones) {
            if (!zone.hasRenderableTiles) continue
            val lifecycleIntensity = zone.lifecycleIntensityAt(renderGameTime)
            if (lifecycleIntensity <= 0.0f) continue
            val temporalIntensity = zone.temporalIntensityAt(renderGameTime)
            if (zone.activity == BioluminescentZoneActivity.INACTIVE &&
                zone.movementWaveVisibilityStrength() <= VISIBILITY_EPSILON
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
                    movementFadeStrength <= VISIBILITY_EPSILON
                ) continue
                val localLoadingIntensity = tile.localLoadingIntensityAt(renderGameTime)
                if (localLoadingIntensity <= VISIBILITY_EPSILON) continue
                val consumer = bufferSource.getBuffer(tile.renderTypeVanilla)
                quadsSubmitted += renderVanillaTile(
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
        poseStack.popPose()

        IrisRenderState.recordVanillaPass(quadsSubmitted)
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
    ): Int {
        for (quad in tile.renderQuads) {
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
        return tile.renderQuads.size
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
            rawBaseOpacity.coerceAtMost(lifecycleIntensity * ACTIVE_BASE_MAX_OPACITY)
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
        val baseAlpha = (baseOpacity * 255.0f).toInt().coerceIn(0, MAX_TEMPORAL_ALPHA)
        val waveAlpha = (waveOpacity * 255.0f).toInt()
        var alpha = (baseAlpha + waveAlpha).coerceIn(0, 255)
        if (zone.activity == BioluminescentZoneActivity.ACTIVE && waveIntensity > 0.0f) {
            val floorAlpha = (lifecycleIntensity * ACTIVE_WAVE_MIN_OPACITY * waveIntensity * 255.0f).toInt()
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

}
