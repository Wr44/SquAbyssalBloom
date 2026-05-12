package fr.heta__h.squ_abyssal_bloom.event.nautilus.enchantment

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantable
import net.minecraft.world.item.enchantment.Repairable
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object NautilusArmorEnchantability {

    @SubscribeEvent
    fun onModifyDefaultComponents(event: ModifyDefaultComponentsEvent) {
        event.modify(Items.COPPER_NAUTILUS_ARMOR) { builder ->
            builder.set(DataComponents.ENCHANTABLE, Enchantable(8))
            builder.set(DataComponents.REPAIRABLE, Repairable(HolderSet.direct(BuiltInRegistries.ITEM.wrapAsHolder(Items.COPPER_INGOT))))
            builder.set(DataComponents.MAX_DAMAGE, 241)
            builder.set(DataComponents.DAMAGE, 0)
        }
        event.modify(Items.IRON_NAUTILUS_ARMOR) { builder ->
            builder.set(DataComponents.ENCHANTABLE, Enchantable(9))
            builder.set(DataComponents.REPAIRABLE, Repairable(HolderSet.direct(BuiltInRegistries.ITEM.wrapAsHolder(Items.IRON_INGOT))))
            builder.set(DataComponents.MAX_DAMAGE, 240)
            builder.set(DataComponents.DAMAGE, 0)
        }
        event.modify(Items.GOLDEN_NAUTILUS_ARMOR) { builder ->
            builder.set(DataComponents.ENCHANTABLE, Enchantable(25))
            builder.set(DataComponents.REPAIRABLE, Repairable(HolderSet.direct(BuiltInRegistries.ITEM.wrapAsHolder(Items.GOLD_INGOT))))
            builder.set(DataComponents.MAX_DAMAGE, 112)
            builder.set(DataComponents.DAMAGE, 0)
        }
        event.modify(Items.DIAMOND_NAUTILUS_ARMOR) { builder ->
            builder.set(DataComponents.ENCHANTABLE, Enchantable(10))
            builder.set(DataComponents.REPAIRABLE, Repairable(HolderSet.direct(BuiltInRegistries.ITEM.wrapAsHolder(Items.DIAMOND))))
            builder.set(DataComponents.MAX_DAMAGE, 528)
            builder.set(DataComponents.DAMAGE, 0)
        }
        event.modify(Items.NETHERITE_NAUTILUS_ARMOR) { builder ->
            builder.set(DataComponents.ENCHANTABLE, Enchantable(15))
            builder.set(DataComponents.REPAIRABLE,
                Repairable(HolderSet.direct(BuiltInRegistries.ITEM.wrapAsHolder(Items.NETHERITE_INGOT)))
            )
            builder.set(DataComponents.MAX_DAMAGE, 592)
            builder.set(DataComponents.DAMAGE, 0)
        }
    }
}