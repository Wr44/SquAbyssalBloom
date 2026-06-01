package fr.heta__h.squ_abyssal_bloom.event.nautilus.bubble

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.data_component.bubble.SplatterTintSource

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object BubbleSpitterColorHandler {

    @SubscribeEvent
    fun onRegisterItemTintSources(event: RegisterColorHandlersEvent.ItemTintSources) {
        event.register(SplatterTintSource.ID, SplatterTintSource.CODEC)
    }
}