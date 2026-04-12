package fr.heta__h.squ_abyssal_bloom.util

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssDepthStart
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssMaxDepth
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull

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


    fun calculatePhysicalDepth(level: Level, start: BlockPos): Double {
        var surfaceY = start.y

        for (y in level.maxY downTo start.y) {
            val pos = BlockPos(start.x, y, start.z)

            if (level.getFluidState(pos).`is`(Fluids.WATER)) {
                surfaceY = y
                break
            }
        }

        val depth = surfaceY - start.y
        return maxOf(0, depth).toDouble()
    }

    fun getDepthFactor(depth: Double): Double {
        return ((depth - abyssDepthStart) / (abyssMaxDepth - abyssDepthStart)).coerceIn(0.0, 1.0)
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


    fun getEnchantLevel(
        stack: ItemStack,
        level: Level,
        enchantName: String,
        namespace: String = Squ_abyssal_bloom.ID
    ): Int {
        val registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)

        val key = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(namespace, enchantName)
        )

        val holder = registry.get(key).getOrNull()

        return if (holder != null) {
            EnchantmentHelper.getItemEnchantmentLevel(holder, stack)
        } else {
            0
        }
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
            if (!extra.isEmpty && extra.item == NautilusLayer.NAUTILUS_LAMP) return 1.0
        }
        return 0.0
    }


    fun isNautilusExtraEquipment(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false

        val item = stack.item
        return item in listOf(
            NautilusLayer.NAUTILUS_LAMP,
            NautilusLayer.SHIELD
        )
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
            if (extra.item == NautilusLayer.NAUTILUS_LAMP) {
                val dist = entity.position().distanceTo(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))
                val influence = (1.0 - dist / maxRange).coerceIn(0.0, 1.0) * maxInfluence
                if (influence > maxFound) maxFound = influence
            }
        }
        return maxFound
    }

    fun isWaterAbove(level: Level, pos: BlockPos, number: Int): Boolean {
        for (i in 1..number) {
            val checkPos = pos.above(i)
            if (level.getFluidState(checkPos).`is`(Fluids.WATER)) {
                return true
            }
        }
        return false
    }


    fun isDeepUnderwaterAirPocket(level: Level, pos: BlockPos): Boolean {
        if (getDepth(level, pos) < 5.0) return false

        var currentY = pos.y
        var solidThickness = 0

        while (currentY <= level.seaLevel + 10) {
            val checkPos = BlockPos(pos.x, currentY, pos.z)
            val state = level.getBlockState(checkPos)
            val fluid = level.getFluidState(checkPos)

            if (fluid.`is`(FluidTags.WATER)) {
                return solidThickness <= 4
            }

            if (!state.isAir && !state.canBeReplaced()) {
                solidThickness++
            } else {
                solidThickness = 0
            }

            if (solidThickness > 4) {
                return false
            }

            currentY++
        }

        return false
    }


    fun getDepth(level: Level, pos: BlockPos): Double {
        val surfaceY = findRegionalWaterSurface(level, pos)
        return if (surfaceY != -999 && pos.y < surfaceY) {
            (surfaceY - pos.y).toDouble()
        } else {
            0.0
        }
    }


    fun findRegionalWaterSurface(level: Level, pos: BlockPos): Int {
        var surface = checkHeightmapForWater(level, pos.x, pos.z)
        if (surface != -999) return surface

        val offset = 12

        surface = checkHeightmapForWater(level, pos.x + offset, pos.z)
        if (surface != -999) return surface

        surface = checkHeightmapForWater(level, pos.x - offset, pos.z)
        if (surface != -999) return surface

        surface = checkHeightmapForWater(level, pos.x, pos.z + offset)
        if (surface != -999) return surface

        surface = checkHeightmapForWater(level, pos.x, pos.z - offset)
        if (surface != -999) return surface

        return -999
    }


    private fun checkHeightmapForWater(level: Level, x: Int, z: Int): Int {
        val topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z)

        val state = level.getBlockState(BlockPos(x, topY - 1, z))
        if (state.fluidState.`is`(net.minecraft.tags.FluidTags.WATER)) {
            return topY - 1
        }
        return -999
    }

}