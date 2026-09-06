package fr.heta__h.squ_abyssal_bloom.sound.ambient

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.abyssal_depth.AbyssDepthCache
import fr.heta__h.squ_abyssal_bloom.render.abyssal_depth.AbyssDepthProfile
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.level.material.Fluids

class AbyssalLoopSound(
    private val minecraft: Minecraft
) : AbstractTickableSoundInstance(
    ModSounds.ABYSSAL_LOOP.get(),
    SoundSource.AMBIENT,
    RandomSource.create()
) {
    init {
        looping = true
        delay = 0
        relative = true
        attenuation = SoundInstance.Attenuation.NONE
        volume = 0.0f
        pitch = 1.0f
    }

    override fun canStartSilent(): Boolean = true

    override fun tick() {
        val level = minecraft.level
        val player = minecraft.player
        if (level == null || player == null || !ModConfig.enableAbyssalAmbientSound) {
            stop()
            return
        }

        val eyePos = BlockPos.containing(player.eyePosition)
        val underwater = level.getFluidState(eyePos).`is`(Fluids.WATER)
        if (underwater) AbyssDepthCache.refreshIfNeeded(level, eyePos)

        val target = if (underwater) {
            (AbyssDepthProfile.entry * MAX_VOLUME).toFloat()
        } else {
            0.0f
        }

        volume = Mth.lerp(FADE_RATE, volume, target)
        if (!underwater && volume <= STOP_VOLUME) stop()
    }

    fun stopImmediately() = stop()

    companion object {
        private const val MAX_VOLUME = 0.35
        private const val FADE_RATE = 0.025f
        private const val STOP_VOLUME = 0.001f
    }
}
