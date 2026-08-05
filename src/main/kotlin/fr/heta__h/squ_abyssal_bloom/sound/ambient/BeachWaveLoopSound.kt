package fr.heta__h.squ_abyssal_bloom.sound.ambient

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BiomeTags
import net.minecraft.util.Mth.lerp
import net.minecraft.util.RandomSource

class BeachWaveLoopSound(
    private val minecraft: Minecraft
) : AbstractTickableSoundInstance(
    ModSounds.BEACH_WAVE_LOOP.get(),
    SoundSource.AMBIENT,
    RandomSource.create()
) {

    init {
        looping = true
        delay = 0

        relative = false
        attenuation = SoundInstance.Attenuation.NONE

        minecraft.player?.let { player ->
            x = player.x
            y = player.y
            z = player.z
        }

        volume = 0.03f
        pitch = 1.0f
    }

    override fun canStartSilent(): Boolean = true

    override fun tick() {
        val level = minecraft.level
        val player = minecraft.player

        if (level == null || player == null || !ModConfig.enableBeachWaveSound) {
            stop()
            return
        }

        x = player.x
        y = player.y
        z = player.z

        val isOnBeach = level
            .getBiome(player.blockPosition())
            .`is`(BiomeTags.IS_BEACH)

        val targetVolume = if (isOnBeach) {
            0.1f
        } else {
            0.0f
        }

        volume = lerp(
            0.035f,
            volume,
            targetVolume
        )

        if (!isOnBeach && volume <= 0.002f) {
            stop()
        }
    }
}