package fr.heta__h.squ_abyssal_bloom.worldgen.region

import com.mojang.datafixers.util.Pair
import fr.heta__h.squ_abyssal_bloom.worldgen.ocean.AbyssalOceanBiomes
import fr.heta__h.squ_abyssal_bloom.worldgen.ocean.OceanBiomeRegistry
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Climate
import terrablender.api.Region
import terrablender.api.RegionType
import java.util.function.Consumer

class AbyssalRegion(location: Identifier, weight: Int) : Region(location, RegionType.OVERWORLD, weight) {

    override fun addBiomes(
        registry: Registry<Biome>,
        mapper: Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>
    ) {
        OceanBiomeRegistry.bootstrap(registry)

        val abyssalCont = AbyssalOceanBiomes.abyssalContinentalness()
        val deepCont = AbyssalOceanBiomes.deepContinentalness()
        val shallowCont = AbyssalOceanBiomes.shallowContinentalness()

        listOf(AbyssalOceanBiomes.SURFACE_DEPTH, AbyssalOceanBiomes.FLOOR_DEPTH).forEach { targetDepth ->

            OceanBiomeRegistry.abyssalEntries().forEach { entry ->
                addBiome(
                    mapper,
                    AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.FULL_RANGE,
                    abyssalCont,
                    AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.FULL_RANGE,
                    targetDepth, 0.0f, entry.key
                )
            }

            OceanBiomeRegistry.deepEntries().forEach { entry ->
                addBiome(
                    mapper,
                    AbyssalOceanBiomes.TEMPERATURES[entry.tempBand], AbyssalOceanBiomes.FULL_RANGE,
                    deepCont,
                    AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.FULL_RANGE,
                    targetDepth, 0.0f, entry.key
                )
            }

            OceanBiomeRegistry.shallowEntries().forEach { entry ->
                addBiome(
                    mapper,
                    AbyssalOceanBiomes.TEMPERATURES[entry.tempBand], AbyssalOceanBiomes.FULL_RANGE,
                    shallowCont,
                    AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.FULL_RANGE,
                    targetDepth, 0.0f, entry.key
                )
            }
        }

        addModifiedVanillaOverworldBiomes(mapper) { builder ->
            AbyssalOceanBiomes.TEMPERATURES.forEach { temp ->
                listOf(AbyssalOceanBiomes.SURFACE_DEPTH, AbyssalOceanBiomes.FLOOR_DEPTH).forEach { depth ->
                    builder.removeParameter(
                        Climate.parameters(temp, AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.VANILLA_DEEP_CONT, AbyssalOceanBiomes.FULL_RANGE, depth, AbyssalOceanBiomes.FULL_RANGE, 0f)
                    )
                    builder.removeParameter(
                        Climate.parameters(temp, AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.VANILLA_OCEAN_CONT, AbyssalOceanBiomes.FULL_RANGE, depth, AbyssalOceanBiomes.FULL_RANGE, 0f)
                    )
                }
            }
        }
    }
}
