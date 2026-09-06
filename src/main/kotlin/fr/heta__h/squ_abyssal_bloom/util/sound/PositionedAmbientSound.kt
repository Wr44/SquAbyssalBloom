package fr.heta__h.squ_abyssal_bloom.util.sound

import net.minecraft.client.resources.sounds.AbstractSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3

class PositionedAmbientSound(
    event: SoundEvent,
    position: Vec3,
    soundVolume: Float = 1.0f,
    soundPitch: Float = 1.0f
) : AbstractSoundInstance(event, SoundSource.AMBIENT, RandomSource.create()) {
    init {
        x = position.x
        y = position.y
        z = position.z
        volume = soundVolume
        pitch = soundPitch
        looping = false
        relative = false
        attenuation = SoundInstance.Attenuation.LINEAR
    }
}