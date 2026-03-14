package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth

import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.material.FogType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f
import kotlin.math.pow

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object AbyssOverlayRender {

    private val WHITE_TEXTURE = Identifier.withDefaultNamespace("textures/misc/white.png")

    @SubscribeEvent
    fun onRenderStage(event: RenderLevelStageEvent.AfterWeather) {
        if (!ModConfig.enableAbyssFog) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        if (camera.fluidInCamera != FogType.WATER) return

        val level = mc.level ?: return
        val camPos = BlockPos.containing(camera.position())
        val entity = camera.entity() as? LivingEntity ?: return

        AbyssDepthCache.refreshIfNeeded(level, camPos)
        if (!AbyssDepthCache.isLargeBody) return

        val rawFactor = AbyssDepthCache.displayedDepthFactor

        val alpha = (rawFactor.pow(0.5)).toFloat().coerceIn(0.0f, 1.0f)

        if (alpha < 0.005f) return

        val lampInfluence = maxOf(
            ModUtilities.getRiderLampInfluence(entity),
            ModUtilities.getNautilusLampInfluence(level, camPos, 16.0, 1.0)
        )

        val size = 50.0f + (lampInfluence.toFloat() * 40.0f)

        val poseStack = event.poseStack
        poseStack.pushPose()

        val matrix4f = poseStack.last().pose()
        val renderType = RenderTypes.entityTranslucent(WHITE_TEXTURE)
        val bufferSource = mc.renderBuffers().bufferSource()
        val buffer = bufferSource.getBuffer(renderType)

        val light = 0xF000F0

        drawInsideOutBox(buffer, matrix4f, size, alpha, light)

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
}