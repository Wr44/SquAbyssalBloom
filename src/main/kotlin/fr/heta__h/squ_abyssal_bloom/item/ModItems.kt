package fr.heta__h.squ_abyssal_bloom.item

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.item.abyssal_guardian_focalist.AbyssalGuardianFocalistItem
import fr.heta__h.squ_abyssal_bloom.item.bubble_spitter.BubbleSpitterItem
import fr.heta__h.squ_abyssal_bloom.item.respiration_bubble.RespirationBubbleItem
import fr.heta__h.squ_abyssal_bloom.item.lifeline_bubble.LifelineBubbleItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.SpawnEggItem
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister


object ModItems {
    val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(SquAbyssalBloom.ID)

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

    val LIFELINE_BUBBLE : DeferredItem<Item> = ITEMS.registerItem(
        "lifeline_bubble"
    ) { properties -> LifelineBubbleItem(properties) }

    val RESPIRATION_BUBBLE : DeferredItem<Item> = ITEMS.registerItem(
        "respiration_bubble"
    ) { properties -> RespirationBubbleItem(properties) }

    val NAUTILUS_LAMP : DeferredItem<Item> = ITEMS.registerItem(
        "nautilus_lamp"
    ) { properties -> Item(properties) }

    val BARBED_NAUTILUS_SCALE: DeferredItem<Item> = ITEMS.registerItem(
        "barbed_nautilus_scale"
    ) { properties -> Item(properties
        .durability(325)
        .enchantable(10)
        .repairable(PRISMARINE_SPIKE.get())
    ) }

    val BUBBLE_SPITTER = ITEMS.registerItem(
        "bubble_spitter"
    ) { properties ->
        BubbleSpitterItem(
            properties
                .durability(375)
                .enchantable(10)
                .repairable(BRINE_BUBBLES.get())
        )
    }


    val MOBILE_CONDUIT = ITEMS.registerItem(
        "mobile_conduit"
    ) { properties -> Item(properties
        .rarity(Rarity.RARE)
        .stacksTo(1)
    ) }

    val ABYSSAL_GUARDIAN_FOCALIST: DeferredItem<Item> = ITEMS.registerItem(
        "abyssal_guardian_focalist"
    ) { properties -> AbyssalGuardianFocalistItem(properties.rarity(Rarity.RARE)
        .durability(215)
        .enchantable(15)
        .repairable(PRISMARINE_SPIKE.get())
    ) }


    // Blocks
    val ASTRAL_PRISMARINE = ITEMS.registerSimpleBlockItem(ModBlocks.ASTRAL_PRISMARINE)

    // Fictive items for testing
    val BARBED_NAUTILUS_SCALE_DISPLAY: DeferredItem<Item> = ITEMS.registerItem(
        "barbed_nautilus_scale_display"
    ) { properties -> Item(properties) }

    fun register(eventBus: IEventBus) {
        ITEMS.register(eventBus)
    }
}