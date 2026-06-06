package fr.heta__h.squ_abyssal_bloom.worldgen.ocean

import net.minecraft.world.level.biome.Climate

object OceanClimateUtils {

    fun fitness(points: List<Climate.ParameterPoint>, target: Climate.TargetPoint): Long {
        if (points.isEmpty()) return Long.MAX_VALUE
        return points.minOf { fitness(it, target) }
    }

    private fun fitness(point: Climate.ParameterPoint, target: Climate.TargetPoint): Long {
        return point.temperature().distance(target.temperature())
            .plus(point.humidity().distance(target.humidity()))
            .plus(point.continentalness().distance(target.continentalness()))
            .plus(point.erosion().distance(target.erosion()))
            .plus(point.depth().distance(target.depth()))
            .plus(point.weirdness().distance(target.weirdness()))
    }
}
