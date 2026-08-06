package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.item.plankton_bottle.PlanktonTintSource
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object PlanktonBottleColorHandler {

    @SubscribeEvent
    fun onRegisterItemTintSources(event: RegisterColorHandlersEvent.ItemTintSources) {
        event.register(PlanktonTintSource.ID, PlanktonTintSource.CODEC)
    }
}
