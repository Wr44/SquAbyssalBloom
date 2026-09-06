package fr.heta__h.squ_abyssal_bloom.util

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.ButtonOption
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionEventListener
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import dev.isxander.yacl3.gui.YACLScreen
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssDepthStart
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.abyssMaxDepth
import fr.heta__h.squ_abyssal_bloom.config.ModConfig.saveConfig
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigData
import fr.heta__h.squ_abyssal_bloom.config.server.types.BoolOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.DoubleOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.IntOption
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.CrystalJellyEntity
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.network.config.C2SServerConfigPacket
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import net.minecraft.client.Minecraft
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.animal.squid.Squid
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import net.minecraft.world.level.levelgen.synth.NormalNoise
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.ArrayDeque
import java.util.Locale
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ModUtilities {

    const val ASPHYXIATION_GRACE_TICKS = 320
    const val ASPHYXIATION_DAMAGE = 2.0f
    const val FULL_BRIGHT_LIGHTMAP = 15728880
    const val WHITE_RGB = 0xFFFFFF
    const val ASPHYXIATION_DAMAGE_INTERVAL = 20
    const val PROPULSION_WEAK_VOLUME = 0.45
    const val PROPULSION_STRONG_VOLUME = 1.0
    const val PROPULSION_WEAK_PITCH = 1.35
    const val PROPULSION_STRONG_PITCH = 0.8
    const val PROPULSION_PITCH_JITTER = 0.08

    fun tickOutOfWaterAsphyxiation(
        mob: LivingEntity,
        level: ServerLevel,
        ticksOutOfWater: Int,
        graceTicks: Int = ASPHYXIATION_GRACE_TICKS,
        damage: Float = ASPHYXIATION_DAMAGE
    ): Int {

        if (mob is BrineEntity && mob.isUnderWater) return 0

        if (mob.isInWater && mob !is BrineEntity) return 0

        val elapsed = ticksOutOfWater + 1
        val overdue = elapsed - graceTicks
        if (overdue >= 0 && overdue % ASPHYXIATION_DAMAGE_INTERVAL == 0) {
            mob.hurtServer(level, mob.damageSources().drown(), damage)
        }
        return elapsed
    }

    fun hasLoadedChunk(level: LevelReader, chunkX: Int, chunkZ: Int): Boolean {
        return level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null
    }

    fun horizontalPositionKey(x: Int, z: Int): Long {
        return (x.toLong() shl 32) xor (z.toLong() and 0xFFFFFFFFL)
    }

    fun horizontalDistanceSqr(
        firstX: Double,
        firstZ: Double,
        secondX: Double,
        secondZ: Double
    ): Double {
        val deltaX = firstX - secondX
        val deltaZ = firstZ - secondZ
        return deltaX * deltaX + deltaZ * deltaZ
    }

    fun graphDistances(
        size: Int,
        neighbors: IntArray,
        sources: Collection<Int>,
        neighborCount: Int = 4
    ): IntArray {
        val distances = IntArray(size) { -1 }
        val queue = ArrayDeque<Int>()
        for (source in sources) {
            if (distances[source] >= 0) continue
            distances[source] = 0
            queue.addLast(source)
        }
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            for (direction in 0 until neighborCount) {
                val neighbor = neighbors[index * neighborCount + direction]
                if (neighbor < 0 || distances[neighbor] >= 0) continue
                distances[neighbor] = distances[index] + 1
                queue.addLast(neighbor)
            }
        }
        return distances
    }

    fun isWaterBlock(level: LevelReader, pos: BlockPos): Boolean {
        return level.getFluidState(pos).type.isSame(Fluids.WATER)
    }

    fun findTopWaterBlock(level: LevelReader, waterBlock: BlockPos): BlockPos? {
        if (!isWaterBlock(level, waterBlock)) return null

        val cursor = BlockPos.MutableBlockPos()
        var topY = waterBlock.y

        while (topY < level.maxY - 1) {
            cursor.set(waterBlock.x, topY + 1, waterBlock.z)
            if (!isWaterBlock(level, cursor)) break
            topY++
        }

        return BlockPos(waterBlock.x, topY, waterBlock.z)
    }

    fun isWaterSurface(level: LevelReader, waterBlock: BlockPos): Boolean {
        return isWaterBlock(level, waterBlock) && !isWaterBlock(level, waterBlock.above())
    }

    fun isRenderableWaterSurface(level: LevelReader, waterBlock: BlockPos): Boolean {
        if (!isWaterSurface(level, waterBlock)) return false
        if (!level.getFluidState(waterBlock).isSource) return false
        if (level.getBlockState(waterBlock).isSolidRender) return false

        val above = waterBlock.above()
        if (level.getBlockState(above).isSolidRender) return false

        val highestBlockingY = level.getHeight(
            Heightmap.Types.MOTION_BLOCKING,
            waterBlock.x,
            waterBlock.z
        ) - 1

        if (highestBlockingY <= above.y) return true

        val cursor = BlockPos.MutableBlockPos()
        for (y in (above.y + 1)..highestBlockingY) {
            cursor.set(waterBlock.x, y, waterBlock.z)
            if (level.getBlockState(cursor).isSolidRender) return false
        }

        return true
    }

    fun getFluidSurfaceHeight(level: LevelReader, fluidBlock: BlockPos): Double {
        return fluidBlock.y + level.getFluidState(fluidBlock).getHeight(level, fluidBlock).toDouble()
    }

    fun findNearbyRenderableWaterSurface(
        level: LevelReader,
        center: BlockPos,
        horizontalRadius: Int,
        verticalRadius: Int,
        predicate: (BlockPos) -> Boolean = { true }
    ): BlockPos? {
        val minimumY = maxOf(level.minY, center.y - verticalRadius)
        val maximumY = minOf(level.maxY - 1, center.y + verticalRadius)
        val cursor = BlockPos.MutableBlockPos()
        val visitedColumns = hashSetOf<Long>()

        for (radius in 0..horizontalRadius) {
            for (offsetX in -radius..radius) {
                for (offsetZ in -radius..radius) {
                    if (maxOf(abs(offsetX), abs(offsetZ)) != radius) continue
                    val x = center.x + offsetX
                    val z = center.z + offsetZ
                    if (!hasLoadedChunk(level, x shr 4, z shr 4)) continue

                    for (y in maximumY downTo minimumY) {
                        cursor.set(x, y, z)
                        if (!isWaterBlock(level, cursor)) continue
                        val surface = findTopWaterBlock(level, cursor) ?: continue
                        val columnKey = horizontalPositionKey(surface.x, surface.z)
                        if (!visitedColumns.add(columnKey)) break
                        if (isRenderableWaterSurface(level, surface) && predicate(surface)) {
                            return surface
                        }
                        break
                    }
                }
            }
        }
        return null
    }

    fun isOverworldLikeDimension(level: Level): Boolean {
        val type = level.dimensionType()
        return type.hasSkyLight() &&
            !type.hasCeiling() &&
            type.skybox() == net.minecraft.world.level.dimension.DimensionType.Skybox.OVERWORLD
    }

    fun findFluidBlockBelow(
        level: LevelReader,
        x: Int,
        z: Int,
        startY: Int,
        fluidTag: TagKey<Fluid>,
        minimumY: Int = level.minY,
        maxConsecutiveSolidBlocks: Int? = null
    ): BlockPos? {
        return findMatchingFluidBlockBelow(
            level = level,
            x = x, z = z, startY = startY,
            minimumY = minimumY,
            maxConsecutiveSolidBlocks = maxConsecutiveSolidBlocks
        ) { fluidState ->
            fluidState.`is`(fluidTag)
        }
    }

    fun findWaterBlockBelow(
        level: LevelReader,
        x: Int,
        z: Int,
        startY: Int,
        minimumY: Int = level.minY
    ): BlockPos? {
        return findMatchingFluidBlockBelow(
            level = level,
            x = x, z = z, startY = startY,
            minimumY = minimumY,
            maxConsecutiveSolidBlocks = null
        ) { fluidState ->
            fluidState.type.isSame(Fluids.WATER)
        }
    }

    private inline fun findMatchingFluidBlockBelow(
        level: LevelReader,
        x: Int,
        z: Int,
        startY: Int,
        minimumY: Int,
        maxConsecutiveSolidBlocks: Int?,
        matches: (FluidState) -> Boolean
    ): BlockPos? {
        require(maxConsecutiveSolidBlocks == null || maxConsecutiveSolidBlocks >= 0)

        val lowestY = maxOf(level.minY, minimumY)
        if (startY < lowestY) return null

        val cursor = BlockPos.MutableBlockPos()
        var solidStreak = 0

        for (y in startY downTo lowestY) {
            cursor.set(x, y, z)

            if (matches(level.getFluidState(cursor))) {
                return cursor.immutable()
            }

            if (maxConsecutiveSolidBlocks != null) {
                if (level.getBlockState(cursor).blocksMotion()) {
                    solidStreak++
                    if (solidStreak > maxConsecutiveSolidBlocks) return null
                } else {
                    solidStreak = 0
                }
            }
        }

        return null
    }

    fun findWaterSurface(
        level: Level,
        start: BlockPos,
        maxErrors: Int = 20
    ): Int {
        val startY = start.y
        var errors = 0

        for (dy in 0..<level.maxY - startY) {
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


    fun decayingImpulseSpeed(t: Float, tMax: Float, vMax: Float, k: Float = 2.0f): Float {
        val ratio = (t / tMax).coerceIn(0.0f, 1.0f)
        return vMax * (1.0f - ratio).pow(k)
    }

    fun getDepthFactor(depth: Double): Double {
        return ((depth - abyssDepthStart) / (abyssMaxDepth - abyssDepthStart)).coerceIn(0.0, 1.0)
    }

    fun hasClearPath(level: Level, from: LivingEntity, to: LivingEntity): Boolean =
        hasClearPath(level, from.eyePosition, to.eyePosition, from)

    fun hasClearPath(level: Level, from: Vec3, to: Vec3, source: Entity): Boolean {
        val hit = level.clip(
            ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                source
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
    


    fun playPositionedSound(
        level: Level,
        sound: SoundEvent,
        worldX: Double,
        worldY: Double,
        worldZ: Double,
        volume: Float,
        pitchSpread: Float = 0.0f,
        source: SoundSource = SoundSource.AMBIENT
    ) {
        val random = level.random
        val pitch = 1.0f + (random.nextFloat() - random.nextFloat()) * pitchSpread
        level.playLocalSound(worldX, worldY, worldZ, sound, source, volume, pitch, false)
    }

    fun playWaterPropulsion(
        entity: Entity,
        sound: SoundEvent,
        strength: Double,
        source: SoundSource,
        volumeScale: Double = 1.0
    ) {
        val level = entity.level()
        val amount = strength.coerceIn(0.0, 1.0)
        val random = level.random
        val volume = (lerp(PROPULSION_WEAK_VOLUME, PROPULSION_STRONG_VOLUME, amount) * volumeScale)
            .coerceAtMost(1.0)
            .toFloat()
        val pitch = (lerp(PROPULSION_WEAK_PITCH, PROPULSION_STRONG_PITCH, amount) +
            (random.nextDouble() - random.nextDouble()) * PROPULSION_PITCH_JITTER).toFloat()

        if (level.isClientSide) {
            level.playLocalSound(entity.x, entity.y, entity.z, sound, source, volume, pitch, false)
        } else {
            level.playSound(null, entity.x, entity.y, entity.z, sound, source, volume, pitch)
        }
    }

    fun isNautilusExtraEquipment(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return stack.typeHolder().`is`(ModTags.Items.NAUTILUS_EQUIPMENT)
    }

    fun getFogRepellerInfluence(
        entity: LivingEntity?,
        level: Level,
        pos: BlockPos,
        maxRange: Double,
        maxInfluence: Double
    ): Double {
        return maxOf(
            entity?.let(::getFogRepellerInfluence) ?: 0.0,
            getFogRepellerInfluence(level, pos, maxRange, maxInfluence)
        )
    }

    private fun distanceFalloffInfluence(
        distanceSquared: Double,
        rangeSquared: Double,
        maxRange: Double,
        maxInfluence: Double
    ): Double {
        if (distanceSquared > rangeSquared) return 0.0
        val distance = sqrt(distanceSquared)
        return (1.0 - distance / maxRange).coerceIn(0.0, 1.0) * maxInfluence
    }

    fun getFogRepellerInfluence(
        level: Level,
        pos: BlockPos,
        maxRange: Double,
        maxInfluence: Double
    ): Double {
        val center = Vec3.atCenterOf(pos)
        val rangeSquared = maxRange * maxRange

        var maxFound = 0.0

        val aabb = AABB(
            pos.x - maxRange,
            pos.y - maxRange,
            pos.z - maxRange,
            pos.x + maxRange,
            pos.y + maxRange,
            pos.z + maxRange
        )

        for (nautilus in level.getEntitiesOfClass(AbstractNautilus::class.java, aabb)) {
            val extra = nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)

            if (isFogRepellerStack(extra)) {
                val distanceSquared = nautilus.position().distanceToSqr(center)
                maxFound = maxOf(maxFound, distanceFalloffInfluence(distanceSquared, rangeSquared, maxRange, maxInfluence))
            }
        }

        for (bubble in level.getEntitiesOfClass(BubbleProjectile::class.java, aabb)) {
            if (!bubble.isLuminescent) continue
            val distanceSquared = bubble.position().distanceToSqr(center)
            maxFound = maxOf(maxFound, distanceFalloffInfluence(distanceSquared, rangeSquared, maxRange, maxInfluence))
        }

        for (crystalJelly in level.getEntitiesOfClass(CrystalJellyEntity::class.java, aabb)) {
            val distanceSquared = crystalJelly.position().distanceToSqr(center)
            val influence = distanceFalloffInfluence(distanceSquared, rangeSquared, maxRange, maxInfluence)
            maxFound = maxOf(maxFound, influence * crystalJelly.glowStrength)
        }

        if (ModCompat.hasDynLights) {
            for (itemEntity in level.getEntitiesOfClass(ItemEntity::class.java, aabb)) {
                val itemStrength = fogRepellerStrength(itemEntity.item, level)
                if (itemStrength <= 0.0) continue
                val distanceSquared = itemEntity.position().distanceToSqr(center)
                val influence = distanceFalloffInfluence(distanceSquared, rangeSquared, maxRange, maxInfluence)
                maxFound = maxOf(maxFound, influence * itemStrength)
            }

            for (itemFrame in level.getEntitiesOfClass(ItemFrame::class.java, aabb)) {
                val frameStrength = fogRepellerStrength(itemFrame.item, level)
                if (frameStrength <= 0.0) continue
                val distanceSquared = itemFrame.position().distanceToSqr(center)
                val influence = distanceFalloffInfluence(distanceSquared, rangeSquared, maxRange, maxInfluence)
                maxFound = maxOf(maxFound, influence * frameStrength)
            }
        }

        if (maxFound >= maxInfluence) return maxInfluence

        val blockRange = kotlin.math.ceil(maxRange).toInt()
        return scanFogRepellerBlocks(level, center, pos, blockRange, rangeSquared, maxRange, maxInfluence, maxFound)
    }

    private fun scanFogRepellerBlocks(
        level: Level,
        center: Vec3,
        pos: BlockPos,
        blockRange: Int,
        rangeSquared: Double,
        maxRange: Double,
        maxInfluence: Double,
        initial: Double
    ): Double {
        var maxFound = initial

        val minX = pos.x - blockRange
        val maxX = pos.x + blockRange
        val minY = (pos.y - blockRange).coerceAtLeast(level.minY)
        val maxY = (pos.y + blockRange).coerceAtMost(level.maxY - 1)
        val minZ = pos.z - blockRange
        val maxZ = pos.z + blockRange

        if (minY > maxY) return maxFound

        val chunkSource = level.chunkSource

        for (chunkX in (minX shr 4)..(maxX shr 4)) {
            for (chunkZ in (minZ shr 4)..(maxZ shr 4)) {
                val chunk = chunkSource.getChunkNow(chunkX, chunkZ) ?: continue
                val chunkMinBlockX = chunk.pos.minBlockX
                val chunkMinBlockZ = chunk.pos.minBlockZ

                val loX = (minX - chunkMinBlockX).coerceAtLeast(0)
                val hiX = (maxX - chunkMinBlockX).coerceAtMost(15)
                val loZ = (minZ - chunkMinBlockZ).coerceAtLeast(0)
                val hiZ = (maxZ - chunkMinBlockZ).coerceAtMost(15)
                if (loX > hiX || loZ > hiZ) continue

                val sections = chunk.sections
                val minSectionIndex = chunk.getSectionIndex(minY).coerceAtLeast(0)
                val maxSectionIndex = chunk.getSectionIndex(maxY).coerceAtMost(sections.size - 1)

                for (sectionIndex in minSectionIndex..maxSectionIndex) {
                    val section = sections[sectionIndex]
                    if (section.hasOnlyAir()) continue
                    if (!section.maybeHas { it.`is`(ModTags.Blocks.FOG_REPELLER) }) continue

                    val sectionWorldMinY = chunk.getSectionYFromSectionIndex(sectionIndex) shl 4
                    val loY = (minY - sectionWorldMinY).coerceAtLeast(0)
                    val hiY = (maxY - sectionWorldMinY).coerceAtMost(15)
                    if (loY > hiY) continue

                    for (localX in loX..hiX) {
                        for (localY in loY..hiY) {
                            for (localZ in loZ..hiZ) {
                                val state = section.getBlockState(localX, localY, localZ)
                                if (!state.`is`(ModTags.Blocks.FOG_REPELLER)) continue

                                val lampPos = BlockPos(
                                    chunkMinBlockX + localX,
                                    sectionWorldMinY + localY,
                                    chunkMinBlockZ + localZ
                                )
                                val distanceSquared = center.distanceToSqr(Vec3.atCenterOf(lampPos))
                                val influence = distanceFalloffInfluence(distanceSquared, rangeSquared, maxRange, maxInfluence)

                                maxFound = maxOf(maxFound, influence)
                                if (maxFound >= maxInfluence) return maxInfluence
                            }
                        }
                    }
                }
            }
        }

        return maxFound
    }

    fun getFogRepellerInfluence(entity: LivingEntity): Double {
        val mainHand = entity.mainHandItem
        val offHand = entity.offhandItem

        if (ModCompat.hasDynLights) {
            val handStrength = maxOf(
                fogRepellerStrength(mainHand, entity.level()),
                fogRepellerStrength(offHand, entity.level())
            )
            if (handStrength > 0.0) return handStrength
        }

        val vehicle = entity.vehicle

        if (vehicle is AbstractNautilus) {
            val mountStrength = fogRepellerStrength(vehicle.getData(ModAttachments.NAUTILUS_EXTRA_SLOT), entity.level())
            if (mountStrength > 0.0) return mountStrength
        }

        return 0.0
    }

    fun isFogRepellerStack(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return stack.`is`(ModTags.Items.FOG_REPELLER) || stack.getOrDefault(ModDataComponents.PLANKTON_LUMINESCENCE.get(), false)
    }

    fun fogRepellerStrength(stack: ItemStack, level: Level): Double {
        if (!isFogRepellerStack(stack)) return 0.0
        if (!stack.`is`(ModItems.CRYSTAL_JELLY_BUCKET.get())) return 1.0

        val bucketData = stack.get(DataComponents.BUCKET_ENTITY_DATA) ?: return 1.0
        val readyGameTime = bucketData.copyTag().getLongOr(CrystalJellyEntity.TAG_BOTTLE_READY, 0L)
        return CrystalJellyEntity.glowStrengthAt(level, readyGameTime).toDouble()
    }

    fun preferWaterWalkTarget(pos: BlockPos, level: LevelReader, fallback: () -> Float): Float {
        return if (level.getFluidState(pos).`is`(FluidTags.WATER)) 10.0f else fallback()
    }
    

    fun smooth(t: Float): Float {
        return t * t * (3.0f - 2.0f * t)
    }

    fun smooth(t: Double): Double {
        return t * t * (3.0 - 2.0 * t)
    }

    fun smooth(edge0: Double, edge1: Double, value: Double): Double {
        if (edge0 == edge1) {
            return if (value < edge0) 0.0 else 1.0
        }

        val normalized = ((value - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return smooth(normalized)
    }

    fun lerp(first: Double, second: Double, amount: Double): Double {
        return first + (second - first) * amount
    }

    fun smoothTowards(current: Double, target: Double, dt: Double, rate: Double = 2.0): Double {
        return current + (target - current) * (rate * dt)
    }

    fun lerpColor(first: Int, second: Int, amount: Double): Int {
        val red = lerpChannel(first shr 16 and 0xFF, second shr 16 and 0xFF, amount)
        val green = lerpChannel(first shr 8 and 0xFF, second shr 8 and 0xFF, amount)
        val blue = lerpChannel(first and 0xFF, second and 0xFF, amount)
        return (red shl 16) or (green shl 8) or blue
    }

    fun lerpChannel(first: Int, second: Int, amount: Double): Int {
        return (first + (second - first) * amount.coerceIn(0.0, 1.0))
            .roundToInt().coerceIn(0, 255)
    }

    fun minOfPositive(first: Long, second: Long): Long {
        if (first <= 0L) return second
        if (second <= 0L) return first
        return minOf(first, second)
    }

    fun sweepStaleUuidTicks(map: MutableMap<UUID, Long>, now: Long, maxAge: Long) {
        map.entries.removeIf { (_, tick) ->
            val age = now - tick
            age !in 0L..maxAge
        }
    }

    fun mixedUuidBits(uuid: UUID): Long {
        return uuid.mostSignificantBits xor uuid.leastSignificantBits
    }

    fun stableUnitValue(value: Long): Double {
        return ((value ushr 40) and 0xFFFFFFL).toDouble() / 0xFFFFFFL.toDouble()
    }

    fun createNormalNoise(
        parameters: NormalNoise.NoiseParameters,
        seed: Long,
        salt: Long
    ): NormalNoise {
        val random = XoroshiroRandomSource(RandomSupport.mixStafford13(seed xor salt))
        return NormalNoise.create(random, parameters)
    }

    fun sampleNoise2d(noise: NormalNoise, x: Double, z: Double): Double {
        return (0.5 + noise.getValue(x, 0.0, z) * 0.5).coerceIn(0.0, 1.0)
    }

    fun normalizedUuidFraction(bits: Long, shift: Int = 0, mask: Long = 0xFFFFL): Double {
        return ((bits ushr shift) and mask).toDouble() / mask.toDouble()
    }

    fun uuidFractionAsAngle(bits: Long, shift: Int = 0, mask: Long = 0xFFFFL): Double {
        return normalizedUuidFraction(bits, shift, mask) * Math.PI * 2.0
    }

    fun isQualifyingWaterMover(entity: Entity): Boolean {
        return entity.isAlive && !entity.isSpectator && !entity.isPassenger &&
            entity !is AbstractFish && entity !is Squid && entity !is CrystalJellyEntity &&
            (entity.isInWater || entity is AbstractBoat)
    }

    fun isQualifyingWaterMovingPlayer(player: Player): Boolean {
        return player.isAlive && !player.isSpectator &&
            (player.isInWater || player.vehicle is AbstractBoat)
    }

    fun horizontalMovementSpeed(entity: Entity): Double {

        val positionDeltaX = entity.x - entity.xo
        val positionDeltaZ = entity.z - entity.zo
        val movement = entity.deltaMovement

        return max(
            sqrt(positionDeltaX * positionDeltaX + positionDeltaZ * positionDeltaZ),
            sqrt(movement.x * movement.x + movement.z * movement.z)
        )
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

    fun formatTicksAsDuration(ticks: Int): String {
        var totalSeconds = ticks / 20
        val days = totalSeconds / 86400
        totalSeconds %= 86400
        val hours = totalSeconds / 3600
        totalSeconds %= 3600
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        val parts = mutableListOf<String>()
        if (days > 0) parts.add(formatUnit("days", days))
        if (hours > 0) parts.add(formatUnit("hours", hours))
        if (minutes > 0) parts.add(formatUnit("minutes", minutes))
        if (seconds > 0 || parts.isEmpty()) parts.add(formatUnit("seconds", seconds))
        return parts.joinToString(" ")
    }

    private fun formatUnit(unit: String, value: Any): String =
        Component.translatable("config.squ_abyssal_bloom.format.$unit", value).string

    fun ticksFormat(): (Int) -> Component = { ticks -> Component.literal(formatTicksAsDuration(ticks)) }

    fun unitInt(unit: String): (Int) -> Component = { value ->
        Component.translatable("config.squ_abyssal_bloom.format.$unit", value)
    }

    fun unitDouble(unit: String, decimals: Int = 1): (Double) -> Component = { value ->
        Component.translatable(
            "config.squ_abyssal_bloom.format.$unit",
            String.format(Locale.ROOT, "%.${decimals}f", value)
        )
    }

    fun plainDouble(decimals: Int): (Double) -> Component = { value ->
        Component.literal(String.format(Locale.ROOT, "%.${decimals}f", value))
    }

    fun blocksFormatInt(): (Int) -> Component = unitInt("blocks")

    fun blocksFormatDouble(): (Double) -> Component = unitDouble("blocks", 1)

    fun percentFormat(decimals: Int = 0): (Double) -> Component = { value ->
        Component.translatable(
            "config.squ_abyssal_bloom.format.percent",
            String.format(Locale.ROOT, "%.${decimals}f", value * 100.0)
        )
    }

    fun percentPointsFormat(decimals: Int = 0): (Double) -> Component = { value ->
        Component.translatable(
            "config.squ_abyssal_bloom.format.percent",
            String.format(Locale.ROOT, "%.${decimals}f", value)
        )
    }

    fun serverDouble(
        opt: DoubleOption,
        range: ClosedFloatingPointRange<Double> = opt.min..opt.max,
        step: Double = 0.1,
        format: ((Double) -> Component)? = null
    ): Option<Double> =
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.${opt.key}"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.${opt.key}.desc")))
            .binding(Binding.generic(opt.default, { ServerConfigCache.current(opt) }, { ServerConfigCache.set(opt, it) }))
            .controller { o ->
                var c = DoubleSliderControllerBuilder.create(o).range(range.start, range.endInclusive).step(step)
                if (format != null) c = c.formatValue(format)
                c
            }
            .build()

    fun serverInt(
        opt: IntOption,
        range: IntRange = opt.min..opt.max,
        step: Int = 1,
        format: ((Int) -> Component)? = null
    ): Option<Int> =
        Option.createBuilder<Int>()
            .name(Component.translatable("config.squ_abyssal_bloom.${opt.key}"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.${opt.key}.desc")))
            .binding(Binding.generic(opt.default, { ServerConfigCache.current(opt) }, { ServerConfigCache.set(opt, it) }))
            .controller { o ->
                var c = IntegerSliderControllerBuilder.create(o).range(range.first, range.last).step(step)
                if (format != null) c = c.formatValue(format)
                c
            }
            .build()

    fun <T : Comparable<T>> bindOrderedPair(minimum: Option<T>, maximum: Option<T>) {
        minimum.addEventListener { option, event ->
            if (event == OptionEventListener.Event.STATE_CHANGE &&
                maximum.pendingValue() < option.pendingValue()
            ) maximum.requestSet(option.pendingValue())
        }
        maximum.addEventListener { option, event ->
            if (event == OptionEventListener.Event.STATE_CHANGE &&
                minimum.pendingValue() > option.pendingValue()
            ) minimum.requestSet(option.pendingValue())
        }
    }

    fun serverBool(opt: BoolOption): Option<Boolean> =
        Option.createBuilder<Boolean>()
            .name(Component.translatable("config.squ_abyssal_bloom.${opt.key}"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.${opt.key}.desc")))
            .binding(Binding.generic(opt.default, { ServerConfigCache.current(opt) }, { ServerConfigCache.set(opt, it) }))
            .controller(TickBoxControllerBuilder::create)
            .build()



    fun persistServerConfigChanges(initialServerConfig: ServerConfigData) {
        if (ServerConfigCache.isSingleplayer()) {
            ServerConfigCache.toData().applyToSpec()
        } else {
            val currentServerConfig = ServerConfigCache.toData()
            if (currentServerConfig != initialServerConfig && Minecraft.getInstance().connection != null) {
                ClientPacketDistributor.sendToServer(C2SServerConfigPacket(currentServerConfig))
            }
        }
    }

    fun subScreenButton(
        name: Component,
        description: OptionDescription,
        screenTitle: Component,
        buildGroups: (ConfigCategory.Builder) -> Unit
    ): Option<*> =
        ButtonOption.createBuilder()
            .name(name)
            .description(description)
            .text(Component.translatable("config.squ_abyssal_bloom.subScreenButton"))
            .action { screen: YACLScreen ->
                val initialServerConfig = ServerConfigCache.toData()
                val categoryBuilder = ConfigCategory.createBuilder().name(screenTitle)
                buildGroups(categoryBuilder)
                val subScreen = YetAnotherConfigLib.createBuilder()
                    .title(screenTitle)
                    .save {
                        saveConfig()
                        persistServerConfigChanges(initialServerConfig)
                    }
                    .category(categoryBuilder.build())
                    .build()
                    .generateScreen(screen)
                Minecraft.getInstance().setScreen(subScreen)
            }
            .build()
}
