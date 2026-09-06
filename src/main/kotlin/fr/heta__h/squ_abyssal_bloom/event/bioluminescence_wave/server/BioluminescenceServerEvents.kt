package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.server

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceLevelManager
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceServerManager
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceServerSettings
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.level.ChunkWatchEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object BioluminescenceServerEvents {
    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        BioluminescenceServerManager.forServer(event.server).tick()
    }

    @SubscribeEvent
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val server = player.level().server
        BioluminescenceServerManager.forServer(server).onPlayerLogin(player)
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val server = player.level().server
        BioluminescenceServerManager.forServer(server).onPlayerLogout(player)
    }

    @SubscribeEvent
    fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val server = player.level().server
        val previousLevel = server.getLevel(event.from)
        BioluminescenceServerManager.forServer(server)
            .onPlayerChangedDimension(player, previousLevel)
    }

    @SubscribeEvent
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val manager = BioluminescenceLevelManager.forLevel(player.level())
        manager.forgetPlayer(player.uuid)
        manager.synchronizePlayer(player, BioluminescenceServerSettings.current())
    }

    @SubscribeEvent
    fun onChunkSent(event: ChunkWatchEvent.Sent) {
        BioluminescenceLevelManager.forLevel(event.level).synchronizeTrackedChunk(
            event.player,
            event.pos,
            BioluminescenceServerSettings.current()
        )
    }
}
