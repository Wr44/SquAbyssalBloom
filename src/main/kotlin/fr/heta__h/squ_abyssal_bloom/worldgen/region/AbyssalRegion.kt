package fr.heta__h.squ_abyssal_bloom.worldgen.region

import com.mojang.datafixers.util.Pair
import fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean.OceanBiomeEntry
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

        OceanBiomeRegistry.abyssalEntries().forEach { entry ->
            emit(mapper, entry, AbyssalOceanBiomes.FULL_RANGE, abyssalCont)
        }

        OceanBiomeRegistry.deepEntries().forEach { entry ->
            emit(mapper, entry, AbyssalOceanBiomes.TEMPERATURES[entry.tempBand], deepCont)
        }

        OceanBiomeRegistry.shallowEntries().forEach { entry ->
            emit(mapper, entry, AbyssalOceanBiomes.TEMPERATURES[entry.tempBand], shallowCont)
        }

        addModifiedVanillaOverworldBiomes(mapper) { builder ->
            AbyssalOceanBiomes.TEMPERATURES.forEach { temp ->
                listOf(AbyssalOceanBiomes.SURFACE_DEPTH, AbyssalOceanBiomes.FLOOR_DEPTH).forEach { depth ->
                    builder.removeParameter(
                        Climate.parameters(temp, AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.vanillaDeepCont(), AbyssalOceanBiomes.FULL_RANGE, depth, AbyssalOceanBiomes.FULL_RANGE, 0f)
                    )
                    builder.removeParameter(
                        Climate.parameters(temp, AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.vanillaOceanCont(), AbyssalOceanBiomes.FULL_RANGE, depth, AbyssalOceanBiomes.FULL_RANGE, 0f)
                    )
                }
            }
        }
    }

    private fun emit(
        mapper: Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>,
        entry: OceanBiomeEntry,
        defaultTemp: Climate.Parameter,
        defaultCont: Climate.Parameter
    ) {
        val ov = AbyssalOceanBiomes.overrideFor(entry.key)
        val depths = if (ov != null) {
            listOf(AbyssalOceanBiomes.SURFACE_DEPTH)
        } else {
            listOf(AbyssalOceanBiomes.SURFACE_DEPTH, AbyssalOceanBiomes.FLOOR_DEPTH)
        }
        depths.forEach { depth ->
            addBiome(
                mapper,
                ov?.temperature ?: defaultTemp,
                ov?.humidity ?: AbyssalOceanBiomes.FULL_RANGE,
                ov?.continentalness ?: defaultCont,
                ov?.erosion ?: AbyssalOceanBiomes.FULL_RANGE,
                ov?.weirdness ?: AbyssalOceanBiomes.FULL_RANGE,
                ov?.depth ?: depth,
                ov?.offset ?: 0.0f,
                entry.key
            )
        }
    }
}