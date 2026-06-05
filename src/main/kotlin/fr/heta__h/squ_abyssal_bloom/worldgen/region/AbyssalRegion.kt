package fr.heta__h.squ_abyssal_bloom.worldgen.region

import com.mojang.datafixers.util.Pair
import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Climate
import terrablender.api.ParameterUtils
import terrablender.api.Region
import terrablender.api.RegionType
import java.util.function.Consumer

private val ABYSSAL_CONTINENTALNESS = Climate.Parameter.span(-1.5f, -0.7f)

class AbyssalRegion(location: Identifier, weight: Int) : Region(location, RegionType.OVERWORLD, weight) {

    override fun addBiomes(
        registry: Registry<Biome>,
        mapper: Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>
    ) {
        ParameterUtils.ParameterPointListBuilder()
            .continentalness(ABYSSAL_CONTINENTALNESS)
            .depth(ParameterUtils.Depth.SURFACE, ParameterUtils.Depth.FLOOR)
            .build()
            .forEach { point -> mapper.accept(Pair.of(point, ModBiomes.ABYSSAL_OCEAN)) }

        addModifiedVanillaOverworldBiomes(mapper) { }
    }
}