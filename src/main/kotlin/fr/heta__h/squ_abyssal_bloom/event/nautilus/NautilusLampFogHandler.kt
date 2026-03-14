package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.material.FogType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ViewportEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object NautilusLampFogHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onRenderFog(event: ViewportEvent.RenderFog) {
        if (!ModConfig.enableNautilusLampFog) return
        if (event.camera.fluidInCamera != FogType.WATER) return
        val entity = event.camera.entity() as? LivingEntity ?: return
        val riderInfluence = ModUtilities.getRiderLampInfluence(entity)
        if (riderInfluence < 0.01) return

        event.nearPlaneDistance = ModConfig.nautilusLampFogStart.toFloat()
        event.farPlaneDistance = ModConfig.nautilusLampFogEnd.toFloat()
    }

}
