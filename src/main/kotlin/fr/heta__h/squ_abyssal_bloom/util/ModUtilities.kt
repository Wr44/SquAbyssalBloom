package fr.heta__h.squ_abyssal_bloom.util

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids

object ModUtilities {
    fun findWaterSurface(level: Level, start: BlockPos, maxDistance: Int = 200): Double {
        val startY = start.y
        for (dy in 0..maxDistance) {
            val pos = start.above(dy)
            if (!level.getFluidState(pos).`is`(Fluids.WATER)) {
                return pos.y.toDouble() - startY.toDouble()
            }
        }
        return maxDistance.toDouble()
    }


    fun getDepthFactor(depth: Double): Double {
        return ((depth - ModConfig.abyssDepthStart) / (ModConfig.abyssMaxDepth - ModConfig.abyssDepthStart)).coerceIn(0.0, 1.0)
    }

    fun isLargeBodyWater( level: Level, pos: BlockPos, radius: Int): Boolean {
        var count = 0
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val checkPos = pos.offset(x, 0, z)
                if (level.getFluidState(checkPos).`is`(Fluids.WATER)) {
                    count ++
                }
            }
        }
        if (count < (radius * 2 + 1) * (radius * 2 + 1) * 0.4) {
            return false
        }
        return true
    }
}