package fr.heta__h.squ_abyssal_bloom.util

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssDepthStart
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssMaxDepth
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min

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

    fun findLocalWaterFloor(level: LevelReader, start: BlockPos): BlockPos? {
        val cursor = start.mutable()

        while (cursor.y > level.minY) {
            if (!level.getFluidState(cursor).`is`(FluidTags.WATER)) return null

            val supportPos = cursor.below()
            val supportShape = level.getBlockState(supportPos).getCollisionShape(level, supportPos)
            if (!supportShape.isEmpty) return cursor.immutable()

            cursor.setY(cursor.y - 1)
        }

        return null
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


    fun hasCollisionFreeAquaticCorridor(
        mob: Mob,
        destination: Vec3,
        maxProbeDistance: Double = 6.0,
        sampleSpacing: Double = 0.75
    ): Boolean {
        val offset = destination.subtract(mob.position())
        val totalDistance = offset.length()
        if (totalDistance < 1.0e-4) return true

        val direction = offset.scale(1.0 / totalDistance)
        val probeDistance = min(totalDistance, maxProbeDistance)
        var sampledDistance = min(sampleSpacing, probeDistance)

        while (sampledDistance <= probeDistance + 1.0e-6) {
            val displacement = direction.scale(sampledDistance)
            if (!mob.level().noCollision(mob, mob.boundingBox.move(displacement))) return false

            val samplePos = BlockPos.containing(mob.position().add(displacement))
            if (!mob.level().getFluidState(samplePos).`is`(FluidTags.WATER)) return false

            if (sampledDistance == probeDistance) break
            sampledDistance = min(sampledDistance + sampleSpacing, probeDistance)
        }

        return true
    }


    fun getEnchantLevel(
        stack: ItemStack,
        level: Level,
        enchantName: String,
        namespace: String = SquAbyssalBloom.ID
    ): Int {
        val registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        val key = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(namespace, enchantName))

        return registry.get(key).getOrNull()?.let { holder ->
            stack.getEnchantmentLevel(holder)
        } ?: 0
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
            if (!extra.isEmpty && extra.item == NautilusLayerItems.LAMP) return 1.0
        }
        return 0.0
    }


    fun isNautilusExtraEquipment(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false

        val item = stack.item
        return item in listOf(
            NautilusLayerItems.LAMP,
            NautilusLayerItems.SHIELD,
            NautilusLayerItems.BUBBLE,
            NautilusLayerItems.CHEST,
            NautilusLayerItems.CONDUIT
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
            if (extra.item == NautilusLayerItems.LAMP) {
                val dist = entity.position().distanceTo(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))
                val influence = (1.0 - dist / maxRange).coerceIn(0.0, 1.0) * maxInfluence
                if (influence > maxFound) maxFound = influence
            }
        }
        return maxFound
    }

    fun preferWaterWalkTarget(pos: BlockPos, level: LevelReader, fallback: () -> Float): Float {
        return if (level.getFluidState(pos).`is`(FluidTags.WATER)) 10.0f else fallback()
    }

    fun getCombinedLampInfluence(entity: LivingEntity?, level: Level, pos: BlockPos, maxRange: Double, maxInfluence: Double): Double {
        entity ?: return 0.0
        return maxOf(getRiderLampInfluence(entity), getNautilusLampInfluence(level, pos, maxRange, maxInfluence))
    }

    fun smoothstep(t: Double): Double {
        return t * t * (3.0 - 2.0 * t)
    }

    fun minOfPositive(first: Long, second: Long): Long {
        if (first <= 0L) return second
        if (second <= 0L) return first
        return minOf(first, second)
    }

    fun sweepStaleUuidTicks(map: MutableMap<UUID, Long>, now: Long, maxAge: Long) {
        map.entries.removeIf { (_, tick) -> now - tick > maxAge }
    }

    fun getDepth(level: Level, pos: BlockPos): Double {
        if (!level.getFluidState(pos).`is`(FluidTags.WATER)) return 0.0

        val mutPos = BlockPos.MutableBlockPos()

        val offsets = intArrayOf(
            0, 0,
            8, 0, -8, 0, 0, 8, 0, -8,
            16, 0, -16, 0, 0, 16, 0, -16,
            24, 0, -24, 0, 0, 24, 0, -24
        )

        for (i in offsets.indices step 2) {
            val cx = pos.x + offsets[i]
            val cz = pos.z + offsets[i + 1]

            val topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz) - 1
            mutPos.set(cx, topY, cz)

            if (level.getFluidState(mutPos).`is`(FluidTags.WATER)) {

                var isFloatingStructure = false
                for (y in topY downTo pos.y) {
                    mutPos.set(cx, y, cz)
                    if (level.getBlockState(mutPos).isAir) {
                        isFloatingStructure = true
                        break
                    }
                }

                if (!isFloatingStructure) {
                    return (topY - pos.y).toDouble()
                }
            }
        }

        mutPos.set(pos)
        var surfaceY = pos.y
        while (surfaceY < level.maxY) {
            mutPos.y = surfaceY + 1
            if (!level.getFluidState(mutPos).`is`(FluidTags.WATER)) {
                break
            }
            surfaceY++
        }

        return (surfaceY - pos.y).toDouble()
    }

}
