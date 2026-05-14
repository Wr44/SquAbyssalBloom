package fr.heta__h.squ_abyssal_bloom.effect

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.effect.effects.GuardiansRetribution
import fr.heta__h.squ_abyssal_bloom.effect.effects.PressureEffect
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffect
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModEffects {
    val MOB_EFFECTS: DeferredRegister<MobEffect> =
        DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Squ_abyssal_bloom.ID)

    val GUARDIAN_S_REDISTRIBUTION: Holder<MobEffect> =
        MOB_EFFECTS.register("guardian_s_redistribution") { _ -> GuardiansRetribution() as MobEffect }

    val PRESSURE: Holder<MobEffect> =
        MOB_EFFECTS.register("pressure") { _ -> PressureEffect() as MobEffect }

    fun register(eventBus: IEventBus) {
        MOB_EFFECTS.register(eventBus)
    }
}