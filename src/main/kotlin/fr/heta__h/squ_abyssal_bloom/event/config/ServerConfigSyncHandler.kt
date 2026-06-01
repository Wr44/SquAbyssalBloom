package fr.heta__h.squ_abyssal_bloom.event.config

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ServerConfigData
import fr.heta__h.squ_abyssal_bloom.network.config.S2CServerConfigPacket
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.network.PacketDistributor

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object ServerConfigSyncHandler {

    @SubscribeEvent
    fun onPlayerJoin(event: PlayerEvent.PlayerLoggedInEvent) {
        PacketDistributor.sendToPlayer(
            event.entity as? net.minecraft.server.level.ServerPlayer ?: return,
            S2CServerConfigPacket(ServerConfigData.fromSpec())
        )
    }
}