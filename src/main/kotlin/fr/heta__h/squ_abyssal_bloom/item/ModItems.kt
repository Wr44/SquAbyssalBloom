package fr.heta__h.squ_abyssal_bloom.item

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.item.abyssal_guardian_focalist.AbyssalGuardianFocalistItem
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.SpawnEggItem
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister


object ModItems {
    val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(Squ_abyssal_bloom.ID)

    val BARNACLE_SPAWN_EGG: DeferredItem<Item> = ITEMS.registerItem(
        "barnacle_spawn_egg"
    ) { properties -> SpawnEggItem(properties.spawnEgg(ModEntities.BARNACLE.get())) }

    val BRINE_SPAWN_EGG : DeferredItem<Item> = ITEMS.registerItem(
        "brine_spawn_egg"
    ) { properties -> SpawnEggItem(properties.spawnEgg(ModEntities.BRINE.get())) }

    val BARNACLE_TOOTH: DeferredItem<Item> = ITEMS.registerItem(
        "barnacle_tooth"
    ) { properties -> Item(properties) }

    val GUARDIAN_EYE: DeferredItem<Item> = ITEMS.registerItem(
        "guardian_eye"
    ) { properties -> Item(properties) }

    val PRISMARINE_BULB: DeferredItem<Item> = ITEMS.registerItem(
        "prismarine_bulb"
    ) { properties -> Item(properties) }

    val PRISMARINE_SPIKE : DeferredItem<Item> = ITEMS.registerItem(
        "prismarine_spike"
    ) { properties -> Item(properties) }

    val BRINE_BUBBLES : DeferredItem<Item> = ITEMS.registerItem(
        "brine_bubbles"
    ) { properties -> Item(properties) }

    val NAUTILUS_LAMP : DeferredItem<Item> = ITEMS.registerItem(
        "nautilus_lamp"
    ) { properties -> Item(properties) }

    val BARBED_NAUTILUS_SCALE: DeferredItem<Item> = ITEMS.registerItem(
        "barbed_nautilus_scale"
    ) { properties -> Item(properties
        .enchantable(10)
        .durability(200)
    )
    }

    val ABYSSAL_GUARDIAN_FOCALIST: DeferredItem<Item> = ITEMS.registerItem(
        "abyssal_guardian_focalist"
    ) { properties -> AbyssalGuardianFocalistItem(properties.rarity(Rarity.RARE)
        .durability(155)
        .enchantable(15)
    )
    }


    
    val BARBED_NAUTILUS_SCALE_DISPLAY: DeferredItem<Item> = ITEMS.registerItem(
        "barbed_nautilus_scale_display"
    ) { properties -> Item(properties) }

    fun register(eventBus: IEventBus) {
        ITEMS.register(eventBus)
    }
}