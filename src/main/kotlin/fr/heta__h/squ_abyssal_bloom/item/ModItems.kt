package fr.heta__h.squ_abyssal_bloom.item

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.item.abyssal_guardian_focalist.AbyssalGuardianFocalistItem
import fr.heta__h.squ_abyssal_bloom.item.bubble_spitter.BubbleSpitterItem
import fr.heta__h.squ_abyssal_bloom.item.respiration_bubble.RespirationBubbleItem
import fr.heta__h.squ_abyssal_bloom.item.lifeline_bubble.LifelineBubbleItem
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.MobBucketItem
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.item.StandingAndWallBlockItem
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.material.Fluids
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

    val RED_SLOBBERER_SPAWN_EGG: DeferredItem<Item> = ITEMS.registerItem(
        "red_slobberer_spawn_egg"
    )  { properties -> SpawnEggItem(properties.spawnEgg(ModEntities.RED_SLOBBERER.get())) }

    val BABY_RED_SLOBBERER_BUCKET: DeferredItem<Item> = ITEMS.registerItem(
        "baby_red_slobberer_bucket"
    ) { properties ->
        MobBucketItem(
            ModEntities.RED_SLOBBERER.get(),
            Fluids.WATER,
            SoundEvents.BUCKET_EMPTY_FISH,
            properties
                .stacksTo(1)
                .component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY)
        )
    }

    val MACKEREL_SPAWN_EGG: DeferredItem<Item> = ITEMS.registerItem(
        "mackerel_spawn_egg"
    ) { properties -> SpawnEggItem(properties.spawnEgg(ModEntities.MACKEREL.get())) }

    val MACKEREL_BUCKET: DeferredItem<Item> = ITEMS.registerItem(
        "mackerel_bucket"
    ) { properties ->
        MobBucketItem(
            ModEntities.MACKEREL.get(),
            Fluids.WATER,
            SoundEvents.BUCKET_EMPTY_FISH,
            properties
                .stacksTo(1)
                .component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY)
        )
    }

    val RAW_MACKEREL: DeferredItem<Item> = ITEMS.registerItem(
        "raw_mackerel"
    ) { properties -> Item(properties.food(FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build())) }

    val COOKED_MACKEREL: DeferredItem<Item> = ITEMS.registerItem(
        "cooked_mackerel"
    ) { properties -> Item(properties.food(FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build())) }

    val BARNACLE_TOOTH: DeferredItem<Item> = ITEMS.registerItem(
        "barnacle_tooth"
    ) { properties -> Item(properties) }

    val GUARDIAN_EYE: DeferredItem<Item> = ITEMS.registerItem(
        "guardian_eye"
    ) { properties -> Item(properties) }

    val PRISMARINE_BULB: DeferredItem<Item> = ITEMS.registerItem(
        "prismarine_bulb"
    ) { properties -> Item(properties) }

    val BIOLUMINESCENT_CRYSTAL: DeferredItem<Item> = ITEMS.registerItem(
    "bioluminescent_crystal"
    ) { properties -> Item(properties) }

    val PRISMARINE_SPIKE : DeferredItem<Item> = ITEMS.registerItem(
        "prismarine_spike"
    ) { properties -> Item(properties) }

    val CALCAREOUS_FRAGMENT: DeferredItem<Item> = ITEMS.registerItem(
        "calcareous_fragment"
    ) { properties -> Item(properties) }

    val MARINE_CEMENT: DeferredItem<Item> = ITEMS.registerItem(
        "marine_cement"
    ) { properties -> Item(properties) }

    val MARINE_BRICK: DeferredItem<Item> = ITEMS.registerItem(
        "marine_brick",
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
    val BIOLUMINESCENT_TORCH = ITEMS.registerItem(
        "bioluminescent_torch"
    ) { properties ->
        StandingAndWallBlockItem(
            ModBlocks.BIOLUMINESCENT_TORCH.get(),
            ModBlocks.BIOLUMINESCENT_WALL_TORCH.get(),
            Direction.DOWN,
            properties.useBlockDescriptionPrefix()
        )
    }

    val BIOLUMINESCENT_LANTERN = ITEMS.registerSimpleBlockItem(ModBlocks.BIOLUMINESCENT_LANTERN)

    val ASTRAL_PRISMARINE = ITEMS.registerSimpleBlockItem(ModBlocks.ASTRAL_PRISMARINE)

    val RHODOPHYTA = ITEMS.registerSimpleBlockItem(ModBlocks.RHODOPHYTA)

    val DEAD_RHODOPHYTA = ITEMS.registerSimpleBlockItem(ModBlocks.DEAD_RHODOPHYTA)

    val BLOOD_SEAGRASS = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_SEAGRASS)

    val TALL_BLOOD_SEAGRASS = ITEMS.registerSimpleBlockItem(ModBlocks.TALL_BLOOD_SEAGRASS)

    val MARINE_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.MARINE_BRICKS)

    val CHISELED_MARINE_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.CHISELED_MARINE_BRICKS)

    val MARINE_BRICKS_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.MARINE_BRICKS_STAIRS)

    val MARINE_BRICKS_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.MARINE_BRICKS_SLAB)

    val ALGEA_INFESTED_MARINE_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.ALGEA_INFESTED_MARINE_BRICKS)

    val ALGEA_INFESTED_MARINE_BRICKS_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.ALGEA_INFESTED_MARINE_BRICKS_STAIRS)

    val ALGEA_INFESTED_MARINE_BRICKS_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.ALGEA_INFESTED_MARINE_BRICKS_SLAB)

    val RHODOPHYTA_INFESTED_MARINE_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.RHODOPHYTA_INFESTED_MARINE_BRICKS)

    val RHODOPHYTA_INFESTED_MARINE_BRICKS_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.RHODOPHYTA_INFESTED_MARINE_BRICKS_STAIRS)

    val RHODOPHYTA_INFESTED_MARINE_BRICKS_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.RHODOPHYTA_INFESTED_MARINE_BRICKS_SLAB)

    val MARINE_BRICKS_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.MARINE_BRICKS_WALL)

    val ALGEA_INFESTED_MARINE_BRICKS_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.ALGEA_INFESTED_MARINE_BRICKS_WALL)

    val RHODOPHYTA_INFESTED_MARINE_BRICKS_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.RHODOPHYTA_INFESTED_MARINE_BRICKS_WALL)

    val BIOLUMINESCENT_CRYSTAL_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.BIOLUMINESCENT_CRYSTAL_BLOCK)

    // Fictive items for testing
    val BARBED_NAUTILUS_SCALE_DISPLAY: DeferredItem<Item> = ITEMS.registerItem(
        "barbed_nautilus_scale_display"
    ) { properties -> Item(properties) }

    fun register(eventBus: IEventBus) {
        ITEMS.register(eventBus)
    }
}
