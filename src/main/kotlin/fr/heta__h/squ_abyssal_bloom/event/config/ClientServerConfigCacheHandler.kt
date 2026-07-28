package fr.heta__h.squ_abyssal_bloom.event.config

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object ClientServerConfigCacheHandler {

    @SubscribeEvent
    fun onLoggingOut(_event: ClientPlayerNetworkEvent.LoggingOut) {
        ServerConfigCache.clear()
    }
}
