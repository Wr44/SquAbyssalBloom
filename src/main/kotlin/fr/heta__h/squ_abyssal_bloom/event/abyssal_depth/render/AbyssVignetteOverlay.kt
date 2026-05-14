package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.render

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.cache.AbyssDepthCache
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.material.FogType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderGuiEvent
import kotlin.math.pow

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object AbyssVignetteOverlay {

    private val VIGNETTE_LOCATION = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/gui/abyss_vignette.png")

    @SubscribeEvent
    fun onRenderGuiPre(event: RenderGuiEvent.Pre) {
        if (!ModConfig.enableDepthVignette || !ModConfig.enableAbyssFog) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        if (camera.fluidInCamera != FogType.WATER) return

        val entity = camera.entity() as? LivingEntity ?: return
        val level = entity.level()
        val camPos = BlockPos.containing(camera.position())

        AbyssDepthCache.refreshIfNeeded(level, camPos)

        val rawFactor = AbyssDepthCache.displayedDepthFactor
        val lampInfluence = maxOf(
            ModUtilities.getRiderLampInfluence(entity),
            ModUtilities.getNautilusLampInfluence(level, camPos, 16.0, 1.0)
        )
        val effectiveFactor = rawFactor * (1.0 - lampInfluence * ModConfig.nautilusLampInfluence)

        val alpha = (effectiveFactor.pow(0.8) * ModConfig.vignetteIntensity).toFloat().coerceIn(0f, 0.7f)
        if (alpha < 0.02f) return

        val guiGraphics = event.guiGraphics
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight

        val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
        val colorArgb = (alphaInt shl 24) or 0x00FFFFFF

        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            VIGNETTE_LOCATION,
            0, 0,
            0f, 0f,
            screenW, screenH,
            screenW, screenH,
            colorArgb
        )
    }
}
