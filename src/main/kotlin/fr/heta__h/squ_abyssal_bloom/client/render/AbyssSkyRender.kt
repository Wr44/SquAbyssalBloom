package fr.heta__h.squ_abyssal_bloom.client.render

import com.mojang.blaze3d.vertex.VertexConsumer
import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.findWaterSurface
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getDepthFactor
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.material.FogType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f
import kotlin.math.pow

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object AbyssSkyRender {

    
    private val WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png")

    @SubscribeEvent
    fun onRenderAfterSky(event: RenderLevelStageEvent.AfterSky) {
        if (!ModConfig.enableAbyssFog) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera

        if (camera.fluidInCamera != FogType.WATER) return

        val entity = camera.entity ?: return
        val level = mc.level ?: return
        val camPos = BlockPos.containing(camera.position)

        val surfaceY = findWaterSurface(level, camPos)
        val currentDepth = surfaceY + 1 - camera.position.y
        val depthFactor = getDepthFactor(currentDepth)

        if (depthFactor <= 0.0) return

        val alpha = (depthFactor.pow(1.5)).toFloat().coerceIn(0.0f, 1.0f)
        if (alpha < 0.05f) return

        val poseStack = event.poseStack
        poseStack.pushPose()

        
        val matrix4f = poseStack.last().pose()

        
        
        
        
        
        
        val renderType = RenderType.textSeeThrough(WHITE_TEXTURE)

        val bufferSource = mc.renderBuffers().bufferSource()
        val buffer = bufferSource.getBuffer(renderType)

        val size = 100.0f
        
        val light = 0xF000F0

        drawBox(buffer, matrix4f, size, alpha, light)

        
        bufferSource.endBatch(renderType)

        poseStack.popPose()
    }

    private fun drawBox(buffer: VertexConsumer, matrix: Matrix4f, size: Float, alpha: Float, light: Int) {
        
        
        fun v(x: Float, y: Float, z: Float) {
            buffer.addVertex(matrix, x, y, z)
                .setColor(0f, 0f, 0f, alpha) 
                .setUv(0f, 0f)         
                .setLight(light)       
        }

        
        v(-size, size, -size); v(-size, size, size); v(size, size, size); v(size, size, -size)
        
        v(-size, -size, -size); v(size, -size, -size); v(size, -size, size); v(-size, -size, size)
        
        v(-size, size, -size); v(size, size, -size); v(size, -size, -size); v(-size, -size, -size)
        
        v(-size, -size, size); v(size, -size, size); v(size, size, size); v(-size, size, size)
        
        v(-size, size, size); v(-size, size, -size); v(-size, -size, -size); v(-size, -size, size)
        
        v(size, -size, size); v(size, -size, -size); v(size, size, -size); v(size, size, size)
    }
}