package fr.heta__h.squ_abyssal_bloom.tags

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome

object ModTags {
    object Biomes {
        val IS_ABYSSAL: TagKey<Biome> = TagKey.create(
            Registries.BIOME,
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "is_abyssal")
        )
    }
}