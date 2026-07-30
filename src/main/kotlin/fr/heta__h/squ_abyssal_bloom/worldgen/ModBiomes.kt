package fr.heta__h.squ_abyssal_bloom.worldgen

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.worldgen.region.AbyssalRegion
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import terrablender.api.Regions

object ModBiomes {

    // Deep

    val BLOOD_VALLEY = ResourceKey.create(
        Registries.BIOME,
        Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "blood_valley")
    )


    // Abyssal
    val ABYSSAL_OCEAN = ResourceKey.create(
        Registries.BIOME,
        Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "abyssal_ocean")
    )

    val ABYSSAL_PLAINS = ResourceKey.create(
        Registries.BIOME,
        Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "abyssal_plains")
    )

    fun registerRegions(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            SquAbyssalBloom.LOGGER.info("Registering AbyssalRegion...")
            Regions.register(
                AbyssalRegion(
                    Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "abyssal_region"),
                    10
                )
            )
            SquAbyssalBloom.LOGGER.info("AbyssalRegion registered!")
        }
    }
}