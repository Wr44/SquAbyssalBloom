package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.bloom

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.BioluminescentZoneManager
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom.BioluminescentBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom.BioluminescentBloomGeometry
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPalette
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.pipeline.BioluminescentRenderPipelines
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import com.mojang.blaze3d.pipeline.RenderPipeline
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object BioluminescentBloomRenderer {
    private val WHITE_TEXTURE = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/misc/white.png")

    private const val FULL_BRIGHT_LIGHTMAP = 15728880
    private const val VANILLA_SURFACE_OFFSET = 0.006
    private const val RENDER_SIDE_HYSTERESIS = 0.05
    private const val VISIBILITY_EPSILON = 0.01f
    private const val UV_CENTER = 0.5

    private const val HALO_PEAK_ALPHA = 0.22f
    private const val HALO_COLOR_MIX = 0.3
    private const val HALO_WEDGES = 16
    private const val SATELLITE_WEDGES = 8
    private const val SATELLITE_SHIMMER_PERIOD_TICKS = 90.0
    private const val SATELLITE_SHIMMER_MIN = 0.55
    private const val LOBE_PEAK_ALPHA = 0.8f
    private const val LOBE_WEDGES = 12

    private const val SHALLOW_DEPTH = 4.0
    private const val DEEP_DEPTH = 24.0
    private const val SHALLOW_COMPENSATION_MULTIPLIER = 0.65
    private const val DEEP_COMPENSATION_MULTIPLIER = 1.35
    private const val GRAZING_VISIBILITY_START = 0.15
    private const val TOP_VIEW_VISIBILITY_FULL = 0.65
    private const val GRAZING_MINIMUM_FACTOR = 0.35

    private val renderTypeVanilla: RenderType by lazy {
        createRenderType("squ_plankton_bloom_vanilla", BioluminescentRenderPipelines.VANILLA_SURFACE)
    }
    private val renderTypeShaderUnderwater: RenderType by lazy {
        createRenderType("squ_plankton_bloom_shader_underwater", BioluminescentRenderPipelines.SHADER_UNDERWATER_SURFACE)
    }
    private val renderTypeShaderCompensation: RenderType by lazy {
        createRenderType("squ_plankton_bloom_shader_compensation", BioluminescentRenderPipelines.SHADER_VISIBILITY_COMPENSATION)
    }

    private fun createRenderType(name: String, pipeline: RenderPipeline): RenderType {
        return RenderType.create(
            name,
            RenderSetup.builder(pipeline).withTexture("Sampler0", WHITE_TEXTURE).sortOnUpload().createRenderSetup()
        )
    }

    fun renderVanilla(
        poseStack: PoseStack,
        cameraPosition: Vec3,
        bufferSource: MultiBufferSource.BufferSource,
        renderGameTime: Double
    ) {
        val consumer = bufferSource.getBuffer(renderTypeVanilla)
        val pose = poseStack.last()
        forEachVisibleBloom(renderGameTime) { palette, bloom, geometry, opacity, tint ->
            val renderTop = bloom.resolveRenderTop(cameraPosition.y, geometry.surfaceY, RENDER_SIDE_HYSTERESIS)
            val signedOffset = if (renderTop) VANILLA_SURFACE_OFFSET else -VANILLA_SURFACE_OFFSET
            emitBloom(consumer, pose, cameraPosition, geometry, palette, bloom, opacity, tint, signedOffset, renderGameTime)
        }
        bufferSource.endBatch(renderTypeVanilla)
    }

    fun renderShaderPrimary(
        poseStack: PoseStack,
        cameraPosition: Vec3,
        bufferSource: MultiBufferSource.BufferSource,
        renderGameTime: Double,
        subsurfaceOffset: Double,
        alphaMultiplier: Double
    ) {
        val consumer = bufferSource.getBuffer(renderTypeShaderUnderwater)
        val pose = poseStack.last()
        forEachVisibleBloom(renderGameTime) { palette, bloom, geometry, opacity, tint ->
            emitBloom(
                consumer, pose, cameraPosition, geometry, palette, bloom,
                (opacity * alphaMultiplier).toFloat(), tint, -subsurfaceOffset, renderGameTime
            )
        }
        bufferSource.endBatch(renderTypeShaderUnderwater)
    }

    fun renderShaderCompensation(
        poseStack: PoseStack,
        cameraPosition: Vec3,
        bufferSource: MultiBufferSource.BufferSource,
        renderGameTime: Double,
        subsurfaceOffset: Double,
        baseStrength: Double,
        cameraEnvironmentMultiplier: Double
    ) {
        val consumer = bufferSource.getBuffer(renderTypeShaderCompensation)
        val pose = poseStack.last()
        forEachVisibleBloom(renderGameTime) { palette, bloom, geometry, opacity, tint ->
            val depthFactor = ModUtilities.smoothstep(SHALLOW_DEPTH, DEEP_DEPTH, geometry.waterDepth)
            val depthMultiplier = SHALLOW_COMPENSATION_MULTIPLIER +
                (DEEP_COMPENSATION_MULTIPLIER - SHALLOW_COMPENSATION_MULTIPLIER) * depthFactor
            val angleFactor = angleFactorOf(geometry, cameraPosition)
            val multiplier = baseStrength * depthMultiplier * angleFactor * cameraEnvironmentMultiplier
            emitBloom(
                consumer, pose, cameraPosition, geometry, palette, bloom,
                (opacity * multiplier).toFloat(), tint, -subsurfaceOffset, renderGameTime
            )
        }
        bufferSource.endBatch(renderTypeShaderCompensation)
    }

    fun hasVisibleBlooms(renderGameTime: Double): Boolean {
        var found = false
        forEachVisibleBloom(renderGameTime) { _, _, _, _, _ -> found = true }
        return found
    }

    private inline fun forEachVisibleBloom(
        renderGameTime: Double,
        action: (BioluminescentPalette, BioluminescentBloom, BioluminescentBloomGeometry, Float, Float) -> Unit
    ) {
        if (!ModConfig.enableBioluminescenceBloomRendering) return
        val glowIntensity = ModConfig.bioluminescenceBloomGlowIntensity.toFloat()
        if (glowIntensity <= 0.0f) return

        for (zone in BioluminescentZoneManager.activeZones) {
            if (zone.blooms.isEmpty()) continue
            val waveLifecycle = zone.lifecycleIntensityAt(renderGameTime)
            if (waveLifecycle <= 0.0f) continue
            for (bloom in zone.blooms.values) {
                if (bloom.lifecycle == PlanktonBloomLifecycle.DORMANT) continue
                val geometry = bloom.geometry ?: continue
                val appearance = bloom.appearanceFadeAt(renderGameTime, zone.serverGameTimeOffset)
                val terminal = bloom.terminalFadeAt(renderGameTime)
                if (appearance <= 0.0f || terminal <= 0.0f) continue
                val pulse = bloom.pulseIntensityAt(renderGameTime, zone.serverGameTimeOffset)
                val opacity = appearance * terminal * pulse * waveLifecycle * glowIntensity
                if (opacity <= VISIBILITY_EPSILON) continue
                val invalidatedTint = if (bloom.lifecycle == PlanktonBloomLifecycle.INVALIDATED) 1.0f - terminal else 0.0f
                action(zone.palette, bloom, geometry, opacity, invalidatedTint)
            }
        }
    }

    private fun angleFactorOf(geometry: BioluminescentBloomGeometry, cameraPosition: Vec3): Double {
        val deltaX = geometry.centerX - cameraPosition.x
        val deltaY = geometry.surfaceY - cameraPosition.y
        val deltaZ = geometry.centerZ - cameraPosition.z
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
        val viewAlignment = if (distance > 1.0e-6) abs(deltaY) / distance else 1.0
        val factor = ModUtilities.smoothstep(GRAZING_VISIBILITY_START, TOP_VIEW_VISIBILITY_FULL, viewAlignment)
        return GRAZING_MINIMUM_FACTOR + (1.0 - GRAZING_MINIMUM_FACTOR) * factor
    }

    private fun emitBloom(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        cameraPosition: Vec3,
        geometry: BioluminescentBloomGeometry,
        palette: BioluminescentPalette,
        bloom: BioluminescentBloom,
        opacity: Float,
        invalidatedTint: Float,
        surfaceOffset: Double,
        renderGameTime: Double
    ) {
        if (opacity <= VISIBILITY_EPSILON) return
        val y = (geometry.surfaceY + surfaceOffset - cameraPosition.y).toFloat()
        val centerX = (geometry.centerX - cameraPosition.x).toFloat()
        val centerZ = (geometry.centerZ - cameraPosition.z).toFloat()

        emitHalo(consumer, pose, centerX, y, centerZ, geometry, palette, opacity, invalidatedTint)
        emitSatellites(consumer, pose, centerX, y, centerZ, geometry, palette, opacity, invalidatedTint, renderGameTime)
        emitLobes(consumer, pose, centerX, y, centerZ, geometry, palette, bloom, opacity, invalidatedTint)
    }

    private fun tintedColor(baseColor: Int, palette: BioluminescentPalette, invalidatedTint: Float): Int {
        if (invalidatedTint <= 0.0f) return baseColor
        return ModUtilities.lerpColor(baseColor, palette.shadowColor, invalidatedTint.toDouble())
    }

    private fun emitHalo(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        centerX: Float,
        y: Float,
        centerZ: Float,
        geometry: BioluminescentBloomGeometry,
        palette: BioluminescentPalette,
        opacity: Float,
        invalidatedTint: Float
    ) {
        val alpha = (HALO_PEAK_ALPHA * opacity).coerceIn(0.0f, 1.0f)
        if (alpha <= VISIBILITY_EPSILON) return
        val color = tintedColor(
            ModUtilities.lerpColor(palette.accentColor, palette.highlightColor, HALO_COLOR_MIX),
            palette, invalidatedTint
        )
        emitGradientDisc(consumer, pose, centerX, y, centerZ, geometry.haloRadius, color, alpha, color, HALO_WEDGES)
    }

    private fun emitSatellites(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        centerX: Float,
        y: Float,
        centerZ: Float,
        geometry: BioluminescentBloomGeometry,
        palette: BioluminescentPalette,
        opacity: Float,
        invalidatedTint: Float,
        renderGameTime: Double
    ) {
        for (satellite in geometry.satellites) {
            val shimmerWave = 0.5 + 0.5 * sin(renderGameTime * PI * 2.0 / SATELLITE_SHIMMER_PERIOD_TICKS + satellite.phaseOffset)
            val shimmer = SATELLITE_SHIMMER_MIN + (1.0 - SATELLITE_SHIMMER_MIN) * shimmerWave
            val alpha = (satellite.alphaScale * shimmer * opacity).toFloat().coerceIn(0.0f, 1.0f)
            if (alpha <= VISIBILITY_EPSILON) continue
            val color = tintedColor(
                ModUtilities.lerpColor(palette.accentColor, palette.highlightColor, satellite.colorMix),
                palette, invalidatedTint
            )
            emitGradientDisc(
                consumer, pose,
                centerX + satellite.offsetX.toFloat(), y, centerZ + satellite.offsetZ.toFloat(),
                satellite.radius, color, alpha, color, SATELLITE_WEDGES
            )
        }
    }

    private fun emitLobes(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        centerX: Float,
        y: Float,
        centerZ: Float,
        geometry: BioluminescentBloomGeometry,
        palette: BioluminescentPalette,
        bloom: BioluminescentBloom,
        opacity: Float,
        invalidatedTint: Float
    ) {
        val centerAlpha = (LOBE_PEAK_ALPHA * opacity).coerceIn(0.0f, 1.0f)
        if (centerAlpha <= VISIBILITY_EPSILON) return
        val centerColor = tintedColor(palette.highlightColor, palette, invalidatedTint)
        val rimColor = tintedColor(palette.accentColor, palette, invalidatedTint)

        for (index in geometry.lobeAngles.indices) {
            if (bloom.remainingHarvests <= (bloom.maxHarvests - 1 - index)) continue
            val angle = geometry.lobeAngles[index]
            val lx = centerX + (cos(angle) * geometry.lobeOrbitRadius).toFloat()
            val lz = centerZ + (sin(angle) * geometry.lobeOrbitRadius).toFloat()
            emitGradientDisc(consumer, pose, lx, y, lz, geometry.lobeRadius, centerColor, centerAlpha, rimColor, LOBE_WEDGES)
        }
    }

    private fun emitGradientDisc(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        centerX: Float,
        y: Float,
        centerZ: Float,
        radius: Double,
        centerColor: Int,
        centerAlpha: Float,
        rimColor: Int,
        wedges: Int
    ) {
        for (wedge in 0 until wedges) {
            val angleA = 2.0 * PI * wedge / wedges
            val angleB = 2.0 * PI * (wedge + 1) / wedges
            val cosA = cos(angleA)
            val sinA = sin(angleA)
            val cosB = cos(angleB)
            val sinB = sin(angleB)

            emitDoubleSidedQuad(
                consumer, pose,
                centerX, y, centerZ, UV_CENTER.toFloat(), UV_CENTER.toFloat(), centerColor, centerAlpha,
                centerX + (cosA * radius).toFloat(), y, centerZ + (sinA * radius).toFloat(),
                rimU(cosA), rimU(sinA), rimColor, 0.0f,
                centerX + (cosB * radius).toFloat(), y, centerZ + (sinB * radius).toFloat(),
                rimU(cosB), rimU(sinB), rimColor, 0.0f,
                centerX, y, centerZ, UV_CENTER.toFloat(), UV_CENTER.toFloat(), centerColor, centerAlpha
            )
        }
    }

    private fun rimU(unitOffset: Double): Float = (UV_CENTER + unitOffset * UV_CENTER).toFloat()

    private fun emitDoubleSidedQuad(
        consumer: VertexConsumer, pose: PoseStack.Pose,
        x0: Float, y0: Float, z0: Float, u0: Float, v0: Float, color0: Int, alpha0: Float,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float, color1: Int, alpha1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float, color2: Int, alpha2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float, color3: Int, alpha3: Float
    ) {
        vertex(consumer, pose, x0, y0, z0, u0, v0, color0, alpha0, 1.0f)
        vertex(consumer, pose, x1, y1, z1, u1, v1, color1, alpha1, 1.0f)
        vertex(consumer, pose, x2, y2, z2, u2, v2, color2, alpha2, 1.0f)
        vertex(consumer, pose, x3, y3, z3, u3, v3, color3, alpha3, 1.0f)

        vertex(consumer, pose, x0, y0, z0, u0, v0, color0, alpha0, -1.0f)
        vertex(consumer, pose, x3, y3, z3, u3, v3, color3, alpha3, -1.0f)
        vertex(consumer, pose, x2, y2, z2, u2, v2, color2, alpha2, -1.0f)
        vertex(consumer, pose, x1, y1, z1, u1, v1, color1, alpha1, -1.0f)
    }

    private fun vertex(
        consumer: VertexConsumer,
        pose: PoseStack.Pose,
        x: Float, y: Float, z: Float,
        u: Float, v: Float,
        color: Int, alpha: Float,
        normalY: Float
    ) {
        val red = color shr 16 and 0xFF
        val green = color shr 8 and 0xFF
        val blue = color and 0xFF
        val a = (alpha * 255.0f).toInt().coerceIn(0, 255)
        consumer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, a)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT_LIGHTMAP)
            .setNormal(pose, 0.0f, normalY, 0.0f)
    }
}
