package fr.heta__h.squ_abyssal_bloom.util

import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssDepthStart
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssMaxDepth
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

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

    fun isLargeBodyWater(level: Level, pos: BlockPos, radius: Int): Boolean {
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

    fun playSoundLocal(entity: LivingEntity, sound: SoundEvent, source: SoundSource, volume: Float, pitch: Float) {
        if (entity is ServerPlayer) {
            entity.connection.send(ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                source,
                entity.x, entity.y, entity.z,
                volume,
                pitch,
                entity.random.nextLong()
            ))
        }
    }

    fun getRiderLampInfluence(entity: LivingEntity): Double {
        val vehicle = entity.vehicle
        if (vehicle is AbstractNautilus) {
            val extra = vehicle.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
            if (!extra.isEmpty && isNautilusExtraEquipment(extra)) return 1.0
        }
        return 0.0
    }

    fun findWaterSurfaceBelow(level: Level, camPos: Vec3, maxScan: Int = 64): Int? {
        val startY = camPos.y.toInt()
        for (dy in 0..maxScan) {
            val pos = BlockPos(camPos.x.toInt(), startY - dy, camPos.z.toInt())
            if (level.getFluidState(pos).`is`(Fluids.WATER)) return pos.y
        }
        return null
    }

    fun isNautilusExtraEquipment(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false

        val item = stack.item
        return item == Items.CONDUIT
    }

    fun getNautilusLampInfluence(level: Level, pos: BlockPos, maxRange: Double, maxInfluence: Double): Double {
        val aabb = AABB(
            pos.x - maxRange, pos.y - maxRange, pos.z - maxRange,
            pos.x + maxRange, pos.y + maxRange, pos.z + maxRange
        )
        val nautili = level.getEntitiesOfClass(AbstractNautilus::class.java, aabb)
        var maxFound = 0.0
        for (entity in nautili) {
            val extra = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
            if (!extra.isEmpty) {
                val dist = entity.position().distanceTo(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))
                val influence = (1.0 - dist / maxRange).coerceIn(0.0, 1.0) * maxInfluence
                if (influence > maxFound) maxFound = influence
            }
        }
        return maxFound
    }

}