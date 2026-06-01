package fr.heta__h.squ_abyssal_bloom.event.trident

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Repairable
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent


@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object TridentRepairs {
    @SubscribeEvent
    fun onModifyComponents(event: ModifyDefaultComponentsEvent) {
        event.modify(Items.TRIDENT) { builder, _, _ ->
            builder.set(
                DataComponents.REPAIRABLE,
                Repairable(
                    HolderSet.direct(
                        BuiltInRegistries.ITEM.wrapAsHolder(ModItems.PRISMARINE_SPIKE.get())
                    )
                )
            )
        }
    }
}