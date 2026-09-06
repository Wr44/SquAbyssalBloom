package fr.heta__h.squ_abyssal_bloom.effect

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.effect.effects.GuardiansRetribution
import fr.heta__h.squ_abyssal_bloom.effect.effects.PressureEffect
import fr.heta__h.squ_abyssal_bloom.effect.effects.PropulsionEffect
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffect
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModEffects {
    val MOB_EFFECTS: DeferredRegister<MobEffect> =
        DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, SquAbyssalBloom.ID)

    val GUARDIAN_S_REDISTRIBUTION: Holder<MobEffect> =
        MOB_EFFECTS.register("guardian_s_redistribution") { _ -> GuardiansRetribution() as MobEffect }

    val PRESSURE: Holder<MobEffect> =
        MOB_EFFECTS.register("pressure") { _ -> PressureEffect() as MobEffect }

    val PROPULSION: Holder<MobEffect> =
        MOB_EFFECTS.register("propulsion") { _ -> PropulsionEffect() as MobEffect }

    fun register(eventBus: IEventBus) {
        MOB_EFFECTS.register(eventBus)
    }
}
