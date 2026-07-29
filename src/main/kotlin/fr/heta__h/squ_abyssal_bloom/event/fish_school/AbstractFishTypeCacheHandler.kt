package fr.heta__h.squ_abyssal_bloom.event.fish_school

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.cache.AbstractFishTypeCache
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object AbstractFishTypeCacheHandler {

    @SubscribeEvent
    fun onLoggingIn(event: ClientPlayerNetworkEvent.LoggingIn) {
        AbstractFishTypeCache.classifyAll()
    }
}
