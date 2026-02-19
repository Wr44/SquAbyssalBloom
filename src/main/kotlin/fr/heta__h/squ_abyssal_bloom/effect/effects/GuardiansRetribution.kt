package fr.heta__h.squ_abyssal_bloom.effect.effects

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory

class GuardiansRetribution : MobEffect(MobEffectCategory.BENEFICIAL,0xde6f26) {

    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }
}