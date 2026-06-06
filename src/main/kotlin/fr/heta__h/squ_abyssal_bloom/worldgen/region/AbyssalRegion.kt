package fr.heta__h.squ_abyssal_bloom.worldgen.region

import com.mojang.datafixers.util.Pair
import fr.heta__h.squ_abyssal_bloom.config.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.biome.Climate
import terrablender.api.Region
import terrablender.api.RegionType
import java.util.function.Consumer

private val FULL_RANGE = Climate.Parameter.span(-1.0f, 1.0f)
private val VANILLA_DEEP_CONT = Climate.Parameter.span(-1.05f, -0.455f)
private val VANILLA_OCEAN_CONT = Climate.Parameter.span(-0.455f, -0.19f)
private val SURFACE_DEPTH = Climate.Parameter.point(0.0f)
private val FLOOR_DEPTH = Climate.Parameter.point(1.0f)

private val TEMPERATURES = arrayOf(
    Climate.Parameter.span(-1.0f, -0.45f),
    Climate.Parameter.span(-0.45f, -0.15f),
    Climate.Parameter.span(-0.15f, 0.2f),
    Climate.Parameter.span(0.2f, 0.55f),
    Climate.Parameter.span(0.55f, 1.0f)
)

private val SHALLOW_OCEANS = arrayOf(
    Biomes.FROZEN_OCEAN,
    Biomes.COLD_OCEAN,
    Biomes.OCEAN,
    Biomes.LUKEWARM_OCEAN,
    Biomes.WARM_OCEAN
)

private val DEEP_OCEANS = arrayOf(
    Biomes.DEEP_FROZEN_OCEAN,
    Biomes.DEEP_COLD_OCEAN,
    Biomes.DEEP_OCEAN,
    Biomes.DEEP_LUKEWARM_OCEAN,
    Biomes.WARM_OCEAN
)

class AbyssalRegion(location: Identifier, weight: Int) : Region(location, RegionType.OVERWORLD, weight) {

    override fun addBiomes(
        registry: Registry<Biome>,
        mapper: Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>
    ) {
        val shallowDeep = ModServerConfig.SHALLOW_DEEP_BOUNDARY.get().toFloat()
        val deepAbyssal = ModServerConfig.DEEP_ABYSSAL_BOUNDARY.get().toFloat()

        val abyssalCont = Climate.Parameter.span(-1.05f, deepAbyssal)
        val deepCont = Climate.Parameter.span(deepAbyssal, shallowDeep)
        val shallowCont = Climate.Parameter.span(shallowDeep, -0.19f)

        listOf(SURFACE_DEPTH, FLOOR_DEPTH).forEach { depth ->
            addBiome(mapper, FULL_RANGE, FULL_RANGE, abyssalCont, FULL_RANGE, FULL_RANGE, depth, 0.0f, ModBiomes.ABYSSAL_OCEAN)
            TEMPERATURES.forEachIndexed { i, temp ->
                addBiome(mapper, temp, FULL_RANGE, deepCont, FULL_RANGE, FULL_RANGE, depth, 0.0f, DEEP_OCEANS[i])
                addBiome(mapper, temp, FULL_RANGE, shallowCont, FULL_RANGE, FULL_RANGE, depth, 0.0f, SHALLOW_OCEANS[i])
            }
        }

        addModifiedVanillaOverworldBiomes(mapper) { builder ->
            TEMPERATURES.forEach { temp ->
                listOf(SURFACE_DEPTH, FLOOR_DEPTH).forEach { depth ->
                    builder.removeParameter(
                        Climate.parameters(temp, FULL_RANGE, VANILLA_DEEP_CONT, FULL_RANGE, depth, FULL_RANGE, 0f)
                    )
                    builder.removeParameter(
                        Climate.parameters(temp, FULL_RANGE, VANILLA_OCEAN_CONT, FULL_RANGE, depth, FULL_RANGE, 0f)
                    )
                }
            }
        }
    }
}