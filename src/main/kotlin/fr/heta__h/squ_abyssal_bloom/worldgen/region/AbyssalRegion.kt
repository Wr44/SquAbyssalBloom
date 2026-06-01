package fr.heta__h.squ_abyssal_bloom.worldgen.region

import com.mojang.datafixers.util.Pair
import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Climate
import net.minecraft.resources.Identifier
import terrablender.api.Region
import terrablender.api.RegionType

class AbyssalRegion(location: Identifier, weight: Int) : Region(location, RegionType.OVERWORLD, weight) {

    override fun addBiomes(registry: Registry<Biome>, mapper: java.util.function.Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>) {
        addBiome(mapper,
            Climate.Parameter.span(-1.05f, -0.455f),
            Climate.Parameter.span(-1.0f, 1.0f),
            Climate.Parameter.span(-1.05f, -0.455f),
            Climate.Parameter.span(-1.0f, 1.0f),
            Climate.Parameter.span(-1.0f, 1.0f),
            Climate.Parameter.point(0.0f),
            0f,
            ModBiomes.ABYSSAL_PLAINS
        )
    }
}