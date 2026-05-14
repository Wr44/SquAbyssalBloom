package fr.heta__h.squ_abyssal_bloom.effect

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.Potions
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent
import net.neoforged.neoforge.registries.DeferredRegister

object ModPotions {
    val POTIONS: DeferredRegister<Potion> =
        DeferredRegister.create(BuiltInRegistries.POTION, SquAbyssalBloom.ID)

    val GUARDIAN_S_REDISTRIBUTION: Holder<Potion> =
        POTIONS.register("guardian_s_redistribution") { _ -> Potion(
            "guardian_s_redistribution",
            MobEffectInstance(ModEffects.GUARDIAN_S_REDISTRIBUTION, 1800)
        )
        }

    val LONG_GUARDIAN_S_REDISTRIBUTION: Holder<Potion> =
        POTIONS.register("long_guardian_s_redistribution") { _ -> Potion(
            "guardian_s_redistribution",
            MobEffectInstance(ModEffects.GUARDIAN_S_REDISTRIBUTION, 4800))
        }

    val STRONG_GUARDIAN_S_REDISTRIBUTION: Holder<Potion> =
        POTIONS.register("strong_guardian_s_redistribution") { _ -> Potion(
            "guardian_s_redistribution",
            MobEffectInstance(ModEffects.GUARDIAN_S_REDISTRIBUTION, 900, 1))
        }

    val LONG_STRONG_GUARDIAN_S_REDISTRIBUTION: Holder<Potion> =
        POTIONS.register("long_strong_guardian_s_redistribution") { _ -> Potion(
            "guardian_s_redistribution",
            MobEffectInstance(ModEffects.GUARDIAN_S_REDISTRIBUTION, 2400, 1))
        }

    val STRONGER_GUARDIAN_S_REDISTRIBUTION: Holder<Potion> =
        POTIONS.register("stronger_guardian_s_redistribution") { _ -> Potion(
            "guardian_s_redistribution",
            MobEffectInstance(ModEffects.GUARDIAN_S_REDISTRIBUTION, 450, 2))
        }

    val LONG_STRONGER_GUARDIAN_S_REDISTRIBUTION: Holder<Potion> =
        POTIONS.register("long_stronger_guardian_s_redistribution") { _ -> Potion(
            "guardian_s_redistribution",
            MobEffectInstance(ModEffects.GUARDIAN_S_REDISTRIBUTION, 1200, 2))
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

        builder.addMix(
            GUARDIAN_S_REDISTRIBUTION,
            Items.REDSTONE,
            LONG_GUARDIAN_S_REDISTRIBUTION
        )

        builder.addMix(
            GUARDIAN_S_REDISTRIBUTION,
            Items.GLOWSTONE_DUST,
            STRONG_GUARDIAN_S_REDISTRIBUTION
        )

        builder.addMix(
            STRONG_GUARDIAN_S_REDISTRIBUTION,
            Items.REDSTONE,
            LONG_STRONG_GUARDIAN_S_REDISTRIBUTION
        )

        builder.addMix(
            STRONG_GUARDIAN_S_REDISTRIBUTION,
            Items.GLOWSTONE_DUST,
            STRONGER_GUARDIAN_S_REDISTRIBUTION
        )

        builder.addMix(
            STRONGER_GUARDIAN_S_REDISTRIBUTION,
            Items.REDSTONE,
            LONG_STRONGER_GUARDIAN_S_REDISTRIBUTION
        )
    }
}