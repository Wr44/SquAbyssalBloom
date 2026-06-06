package fr.heta__h.squ_abyssal_bloom.worldgen.region

import com.mojang.datafixers.util.Pair
import fr.heta__h.squ_abyssal_bloom.worldgen.AbyssalOceanBiomes
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
        if (registry.containsKey(AbyssalOceanBiomes.ABYSSAL_OCEAN)) {
            AbyssalOceanBiomes.registerAbyssalOceanHolder(registry.getOrThrow(AbyssalOceanBiomes.ABYSSAL_OCEAN))
        }

        val abyssalCont = AbyssalOceanBiomes.abyssalContinentalness()
        val deepCont = AbyssalOceanBiomes.deepContinentalness()
        val shallowCont = AbyssalOceanBiomes.shallowContinentalness()

        listOf(AbyssalOceanBiomes.SURFACE_DEPTH, AbyssalOceanBiomes.FLOOR_DEPTH).forEach { targetDepth ->

            addBiome(mapper,
                AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.FULL_RANGE,
                abyssalCont,
                AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.FULL_RANGE,
                targetDepth, 0.0f, AbyssalOceanBiomes.ABYSSAL_OCEAN)

            AbyssalOceanBiomes.TEMPERATURES.forEachIndexed { i, temp ->
                addBiome(mapper,
                    temp, AbyssalOceanBiomes.FULL_RANGE,
                    deepCont,
                    AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.FULL_RANGE,
                    targetDepth, 0.0f, AbyssalOceanBiomes.DEEP_OCEANS[i])

                addBiome(mapper,
                    temp, AbyssalOceanBiomes.FULL_RANGE,
                    shallowCont,
                    AbyssalOceanBiomes.FULL_RANGE, AbyssalOceanBiomes.FULL_RANGE,
                    targetDepth, 0.0f, AbyssalOceanBiomes.SHALLOW_OCEANS[i])
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
