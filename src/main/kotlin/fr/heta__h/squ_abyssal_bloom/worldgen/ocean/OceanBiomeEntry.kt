package fr.heta__h.squ_abyssal_bloom.worldgen.ocean

import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Climate

data class OceanBiomeEntry(
    val zone: OceanZone,
    val tempBand: Int,
    val key: ResourceKey<Biome>,
    val referencePoints: List<Climate.ParameterPoint>
) {
    fun fitness(target: Climate.TargetPoint): Long = OceanClimateUtils.fitness(referencePoints, target)
}
