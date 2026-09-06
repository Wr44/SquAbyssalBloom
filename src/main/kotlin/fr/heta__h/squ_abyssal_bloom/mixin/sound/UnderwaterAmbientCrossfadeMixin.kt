package fr.heta__h.squ_abyssal_bloom.mixin.sound

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.abyssal_depth.AbyssDepthProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractSoundInstance
import net.minecraft.sounds.SoundEvents
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(AbstractSoundInstance::class)
abstract class UnderwaterAmbientCrossfadeMixin {
    @Inject(method = ["getVolume"], at = [At("RETURN")], cancellable = true)
    private fun crossfadeUnderwaterAmbience(callback: CallbackInfoReturnable<Float>) {
        if (!ModConfig.enableAbyssalAmbientSound) return

        val sound = this as AbstractSoundInstance
        val id = sound.identifier
        if (
            id != SoundEvents.AMBIENT_UNDERWATER_LOOP.location &&
            id != SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS.location &&
            id != SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE.location &&
            id != SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE.location
        ) return

        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        if (!player.isUnderWater) return

        val depth = AbyssDepthProfile.entry.toFloat()

        val vanillaGain = ((1.0f - depth) * (1.0f - depth)).coerceAtLeast(0.01f)
        callback.returnValue *= vanillaGain
    }
}
