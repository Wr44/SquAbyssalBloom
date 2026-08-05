package fr.heta__h.squ_abyssal_bloom.event.sound

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.sound.ambient.BeachWaveLoopSound
import net.minecraft.client.Minecraft
import net.minecraft.tags.BiomeTags
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object BeachWaveSoundManager {

    private var currentSound: BeachWaveLoopSound? = null
    private var checkCooldown = 0

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level
        val player = minecraft.player

        if (level == null || player == null) {
            currentSound = null
            return
        }

        if (checkCooldown > 0) {
            checkCooldown--
            return
        }

        checkCooldown = 10

        val isOnBeach = level
            .getBiome(player.blockPosition())
            .`is`(BiomeTags.IS_BEACH)

        val sound = currentSound

        if (
            ModConfig.enableBeachWaveSound &&
            isOnBeach &&
            (sound == null || sound.isStopped)
        ) {
            val newSound = BeachWaveLoopSound(minecraft)

            currentSound = newSound
            minecraft.soundManager.play(newSound)
        }

        if (sound?.isStopped == true) {
            currentSound = null
        }
    }
}