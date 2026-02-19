package fr.heta__h.squ_abyssal_bloom.effect.potion

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects.GUARDIAN_S_REDISTRIBUTION
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.Potions
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent
import net.neoforged.neoforge.registries.DeferredRegister

object ModPotions {
    val POTIONS: DeferredRegister<Potion> =
        DeferredRegister.create(BuiltInRegistries.POTION, Squ_abyssal_bloom.ID)

    val GUARDIAN_S_REDISTRIBUTION: Holder<Potion> =
        POTIONS.register("guardian_s_redistribution") { reg -> Potion(
            reg.path,
            MobEffectInstance(ModEffects.GUARDIAN_S_REDISTRIBUTION, 3600))
        }

    fun register(eventBus: IEventBus) {
        POTIONS.register(eventBus)
    }

    fun registerBrewingRecipes(event: RegisterBrewingRecipesEvent) {
        val builder = event.builder

        builder.addMix(
            Potions.AWKWARD,
            ModItems.GUARDIAN_EYE.get(),
            GUARDIAN_S_REDISTRIBUTION
        )
    }
}