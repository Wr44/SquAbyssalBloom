package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.render

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.abyssal_depth.AbyssDepthCache
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.material.FogType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderGuiEvent
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object AbyssOverlayRender {

    private val WHITE_TEXTURE = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/misc/white.png")
    private const val VERT_HALF_FOV = 22.5f

    @SubscribeEvent
    fun onRenderStage(event: RenderLevelStageEvent.AfterWeather) {
        if (!ModConfig.enableAbyssFog) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        val level = mc.level ?: return
        val camPos = BlockPos.containing(camera.position())

        val inWater = camera.fluidInCamera == FogType.WATER
        if (!inWater) return

        val entity = camera.entity() as? LivingEntity ?: return
        if (entity.hasEffect(MobEffects.NIGHT_VISION)) return

        AbyssDepthCache.refreshIfNeeded(level, camPos)

        val physicalDepth = AbyssDepthCache.rawPhysicalDepth
        val alpha = AbyssDepthCache.displayedDepthFactor.toFloat()

        if (alpha < 0.005f) return

        val safeSize = minOf(50.0f, physicalDepth.toFloat() - 1.5f).coerceAtLeast(1.0f)
        val boxFade = ((safeSize - 3.0f) / 4.0f).coerceIn(0.0f, 1.0f)
        val finalBoxAlpha = alpha * boxFade

        if (finalBoxAlpha < 0.005f) return

        val poseStack = event.poseStack
        poseStack.pushPose()
        val matrix4f = poseStack.last().pose()
        val renderType = RenderTypes.entityTranslucent(WHITE_TEXTURE)
        val bufferSource = mc.renderBuffers().bufferSource()
        val buffer = bufferSource.getBuffer(renderType)

        drawInsideOutBox(buffer, matrix4f, safeSize, finalBoxAlpha, 0xF000F0)

        bufferSource.endBatch(renderType)
        poseStack.popPose()
    }

    private fun drawInsideOutBox(buffer: VertexConsumer, matrix: Matrix4f, size: Float, alpha: Float, light: Int) {
        fun v(x: Float, y: Float, z: Float) {
            buffer.addVertex(matrix, x, y, z)
                .setColor(0f, 0f, 0f, alpha)
                .setUv(0f, 0f)
                .setOverlay(655360)
                .setLight(light)
                .setNormal(0f, 1f, 0f)
        }

        v(-size, size, -size); v(-size, size, size); v(size, size, size); v(size, size, -size)
        v(-size, -size, -size); v(size, -size, -size); v(size, -size, size); v(-size, -size, size)
        v(-size, size, -size); v(size, size, -size); v(size, -size, -size); v(-size, -size, -size)
        v(-size, -size, size); v(size, -size, size); v(size, size, size); v(-size, size, size)
        v(-size, size, size); v(-size, size, -size); v(-size, -size, -size); v(-size, -size, size)
        v(size, -size, size); v(size, -size, -size); v(size, size, -size); v(size, size, size)
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderGui(event: RenderGuiEvent.Pre) {
        if (!ModConfig.enableAbyssFog) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        val level = mc.level ?: return
        val camPos = BlockPos.containing(camera.position())

        val inWater = camera.fluidInCamera == FogType.WATER

        if (!inWater) return

        val entity = camera.entity() as? LivingEntity ?: return
        if (entity.hasEffect(MobEffects.NIGHT_VISION)) return

        AbyssDepthCache.refreshIfNeeded(level, camPos)

        val alpha = AbyssDepthCache.displayedDepthFactor.toFloat()
        if (alpha < 0.005f) return

        val lampInfluence = AbyssDepthCache.displayedAmbientFogRepellerInfluence

        val waterColor = level.getBiome(camPos).value().waterColor
        val r = ((waterColor shr 16 and 0xFF) * 0.12f).toInt().coerceIn(0, 255)
        val g = ((waterColor shr 8 and 0xFF) * 0.12f).toInt().coerceIn(0, 255)
        val b = ((waterColor and 0xFF) * 0.12f).toInt().coerceIn(0, 255)

        val guiGraphics = event.guiGraphics
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight

        val ambientAlphaBase = alpha * 0.08f
        val lampReduction = (lampInfluence * ModConfig.fogRepellerInfluence * 0.06).toFloat()
        val finalAmbientAlpha = (ambientAlphaBase - lampReduction).coerceIn(0f, 0.08f)

        if (finalAmbientAlpha >= 0.005f) {
            val aInt = (finalAmbientAlpha * 255).toInt()
            guiGraphics.fill(0, 0, screenW, screenH, (aInt shl 24) or (r shl 16) or (g shl 8) or b)
        }

        val horizonFraction = (0.5f - camera.xRot() / (VERT_HALF_FOV * 2f)).coerceIn(0f, 1f)
        val horizonY = (horizonFraction * screenH).toInt()
        if (horizonY > 0) {
            val finalSkyAlpha = (alpha - lampReduction * 2f).coerceIn(0f, 0.95f)
            if (finalSkyAlpha >= 0.005f) {
                val sInt = (finalSkyAlpha * 255).toInt()
                val skyColorFull = (sInt shl 24) or (r shl 16) or (g shl 8) or b
                guiGraphics.fillGradient(0, 0, screenW, horizonY, skyColorFull, 0x00000000)
            }
        }
    }
}
