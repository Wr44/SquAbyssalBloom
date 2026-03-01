package fr.heta__h.squ_abyssal_bloom.util

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssDepthStart
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssMaxDepth
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.HitResult
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_CONTEXT

object ModUtilities {
    fun findWaterSurface(
        level: Level,
        start: BlockPos,
        maxErrors: Int = 20
    ): Int {
        val startY = start.y
        var errors = 0

        for (dy in 0..(level.maxY - startY - 1)) {
            val pos = start.above(dy)
            val above = pos.above()

            val aboveIsWater = level.getFluidState(above).`is`(Fluids.WATER)

            if (!aboveIsWater) {
                errors++
                if (errors > maxErrors) {
                    return dy - errors
                }
            } else {
                errors = 0
            }
        }

        return (level.maxY - 1) - startY
    }


    fun getDepthFactor(depth: Double): Double {
        return ((depth - abyssDepthStart) / (abyssMaxDepth - abyssDepthStart)).coerceIn(0.0, 1.0)
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

    fun hasClearPath(level: Level, from: LivingEntity, to: LivingEntity): Boolean {
        val start = from.eyePosition
        val end = to.eyePosition

        val hit = level.clip(
            ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                from
            )
        )

        return hit.type == HitResult.Type.MISS
    }

    fun gaussianWeight(dx: Int, dy: Int, dz: Int, sigma: Double): Double {
        val r2 = (dx*dx + dy*dy + dz*dz).toDouble()
        return kotlin.math.exp(-r2 / (2.0 * sigma*sigma))
    }

    fun smoothDepthGaussian(level: Level, pos: BlockPos, sigma: Double = 1.0): Double {
        var sum = 0.0
        var weightSum = 0.0

        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val neighborPos = pos.offset(dx, dy, dz)
                    val w = gaussianWeight(dx, dy, dz, sigma)
                    sum += w * getDepthFactor(findWaterSurface(level, neighborPos).toDouble())
                    weightSum += w
                }
            }
        }

        return sum / weightSum
    }


    fun getEnchantLevel(stack: ItemStack, level: Level, enchantName: String): Int {
        val registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
        val key = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.ENCHANTMENT,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("squ_abyssal_bloom", enchantName)
        )
        val holder = registry.get(key)
        return if (holder.isPresent) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder.get(), stack)
        } else 0
    }

}