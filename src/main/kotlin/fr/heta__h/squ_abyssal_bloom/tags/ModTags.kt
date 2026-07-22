package fr.heta__h.squ_abyssal_bloom.tags

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.biome.Biome

object ModTags {
    object EntityTypes {
        val FISH_SCHOOL_FRIENDLY: TagKey<EntityType<*>> = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "fish_school_friendly")
        )
    }

    object Items {
        val RED_SLOBBERER_FOOD: TagKey<Item> = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "red_slobberer_food")
        )
    }

    object Biomes {
        val IS_ABYSSAL: TagKey<Biome> = TagKey.create(
            Registries.BIOME,
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "is_abyssal")
        )

        val IS_DEEP_OCEAN: TagKey<Biome> = TagKey.create(
            Registries.BIOME,
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "is_deep_ocean")
        )
    }
}
