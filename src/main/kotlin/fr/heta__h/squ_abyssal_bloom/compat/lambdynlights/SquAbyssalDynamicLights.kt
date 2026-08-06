package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer
import dev.lambdaurora.lambdynlights.api.item.ItemLightSource
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity.LuminescentBubbleLuminance
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity.NautilusLampLuminance
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.advancements.criterion.DataComponentMatchers
import net.minecraft.advancements.criterion.ItemPredicate
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EntityType

class SquAbyssalDynamicLights : DynamicLightsInitializer {

    override fun onInitializeDynamicLights(context: DynamicLightsContext) {
        context.entityLightSourceManager().onRegisterEvent().register { ctx ->

            ctx.register(EntityType.NAUTILUS, NautilusLampLuminance.INSTANCE)
            ctx.register(EntityType.ZOMBIE_NAUTILUS, NautilusLampLuminance.INSTANCE)
            ctx.register(ModEntities.BUBBLE.get(), LuminescentBubbleLuminance.INSTANCE)
        }

        context.itemLightSourceManager().onRegisterEvent().register { ctx ->
            ctx.register(ModItems.BIOLUMINESCENT_CRYSTAL.get(), 10)
            ctx.register(ModItems.NAUTILUS_LAMP.get(), 15)
            ctx.register(ModItems.PLANKTON_BOTTLE.get(), 10)
            ctx.register(
                ItemLightSource(
                    ItemPredicate.Builder.item()
                        .of(ctx.registryLookup().lookupOrThrow(Registries.ITEM), ModItems.BUBBLE_SPITTER.get())
                        .withComponents(
                            DataComponentMatchers.Builder.components()
                                .any<DataComponentType<Boolean>>(ModDataComponents.PLANKTON_LUMINESCENCE.get())
                                .build()
                        )
                        .build(),
                    12
                )
            )
        }
    }
}