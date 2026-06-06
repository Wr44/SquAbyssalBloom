package fr.heta__h.squ_abyssal_bloom.worldgen

import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache // Import du cache
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.biome.Climate

object AbyssalOceanBiomes {

    val ABYSSAL_OCEAN: ResourceKey<Biome> = ModBiomes.ABYSSAL_OCEAN

    val FULL_RANGE: Climate.Parameter = Climate.Parameter.span(-1.0f, 1.0f)
    val SURFACE_DEPTH: Climate.Parameter = Climate.Parameter.point(0.0f)
    val FLOOR_DEPTH: Climate.Parameter = Climate.Parameter.point(1.0f)

    val VANILLA_DEEP_CONT: Climate.Parameter = Climate.Parameter.span(-1.05f, -0.455f)
    val VANILLA_OCEAN_CONT: Climate.Parameter = Climate.Parameter.span(-0.455f, -0.19f)

    val TEMPERATURES: Array<Climate.Parameter> = arrayOf(
        Climate.Parameter.span(-1.0f, -0.45f),
        Climate.Parameter.span(-0.45f, -0.15f),
        Climate.Parameter.span(-0.15f, 0.2f),
        Climate.Parameter.span(0.2f, 0.55f),
        Climate.Parameter.span(0.55f, 1.0f)
    )

    val SHALLOW_OCEANS: Array<ResourceKey<Biome>> = arrayOf(
        Biomes.FROZEN_OCEAN, Biomes.COLD_OCEAN, Biomes.OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.WARM_OCEAN
    )

    val DEEP_OCEANS: Array<ResourceKey<Biome>> = arrayOf(
        Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN
    )

    fun shallowDeep() = ServerConfigCache.effectiveShallowDeep
    fun deepAbyssal() = ServerConfigCache.effectiveDeepAbyssal
}