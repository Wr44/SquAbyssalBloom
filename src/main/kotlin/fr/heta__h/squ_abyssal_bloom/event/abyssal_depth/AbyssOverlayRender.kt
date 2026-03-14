package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.compat.OculusCompatDetector
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.material.FogType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderGuiEvent
import kotlin.math.pow

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object AbyssOverlayRender {


    private const val VERT_HALF_FOV = 22.5f

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderGui(event: RenderGuiEvent.Post) {
        if (!ModConfig.enableAbyssFog) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        if (camera.fluidInCamera != FogType.WATER) return

        val entity = camera.entity() as? LivingEntity ?: return
        val level = entity.level()
        val camPos = BlockPos.containing(camera.position())

        AbyssDepthCache.refreshIfNeeded(level, camPos)
        if (!AbyssDepthCache.isLargeBody) return

        val rawFactor = AbyssDepthCache.displayedDepthFactor
        val lampInfluence = maxOf(
            ModUtilities.getRiderLampInfluence(entity),
            ModUtilities.getNautilusLampInfluence(level, camPos, 16.0, 1.0)
        )
        val effectiveFactor = rawFactor * (1.0 - lampInfluence * ModConfig.nautilusLampInfluence)
        if (effectiveFactor < 0.01) return

        val easingPow = if (OculusCompatDetector.isShadersActive()) 0.2 else 0.3

        
        
        val waterColor = level.getBiome(camPos).value().waterColor
        val r = ((waterColor shr 16 and 0xFF) * 0.12f).toInt().coerceIn(0, 255)
        val g = ((waterColor shr 8  and 0xFF) * 0.12f).toInt().coerceIn(0, 255)
        val b = ((waterColor        and 0xFF) * 0.12f).toInt().coerceIn(0, 255)

        val guiGraphics = event.guiGraphics
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight

        
        
        
        val ambientAlpha = (effectiveFactor.pow(easingPow) * 0.08).toFloat().coerceIn(0f, 0.08f)
        if (ambientAlpha >= 0.005f) {
            val aInt = (ambientAlpha * 255).toInt()
            guiGraphics.fill(0, 0, screenW, screenH, (aInt shl 24) or (r shl 16) or (g shl 8) or b)
        }

        
        
        
        
        
        
        
        
        
        
        val horizonFraction = (0.5f - camera.xRot() / (VERT_HALF_FOV * 2f)).coerceIn(0f, 1f)
        val horizonY = (horizonFraction * screenH).toInt()
        if (horizonY > 0) {
            
            
            
            val skyAlpha = effectiveFactor.pow(0.15).toFloat().coerceIn(0f, 0.95f)
            val sInt = (skyAlpha * 255).toInt()
            val skyColorFull = (sInt shl 24) or (r shl 16) or (g shl 8) or b
            guiGraphics.fillGradient(0, 0, screenW, horizonY, skyColorFull, 0x00000000)
        }
        
    }
}
