package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.FishCollectiveManager
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.FishThreatClassifier
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.util.ProblemReporter
import net.minecraft.world.entity.EntityProcessor
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin


class RedSlobbererFishRefugeStorage(
    private val redSlobberer: RedSlobbererEntity
) {

    companion object {
        private const val TAG_STORED_FISH = "RedSlobbererStoredRefugeFish"
        private const val TAG_FISH_DATA = "FishData"
        private const val TAG_SHELTERED_TICKS = "ShelteredTicks"
        private const val TAG_UNSAFE_TICKS = "RedSlobbererRefugeUnsafeTicks"
        private const val TAG_QUIET_TICKS = "RedSlobbererRefugeQuietTicks"

        private const val REENTRY_COOLDOWN_TAG = "squ_abyssal_bloom:red_slobberer_refuge_cooldown_until"
        private const val MAXIMUM_LOADED_FISH = 64
        private const val THREAT_SCAN_INTERVAL_TICKS = 10L
        private const val ENTRANCE_CLEARANCE = 0.35
        private const val ENTRANCE_HEIGHT = 0.75
        private const val ENTRANCE_POSITION_ATTEMPTS = 8
        private const val CAPTURE_DISTANCE = 0.85
        private const val CAPTURE_DISTANCE_SQR = CAPTURE_DISTANCE * CAPTURE_DISTANCE
        private const val RELEASE_ANGLE_ATTEMPTS = 12
        private const val RELEASE_RING_ATTEMPTS = 3
        private const val RELEASE_RING_STEP = 0.7
        private const val RELEASE_VERTICAL_MOTION = 0.025
        private const val RELEASE_HORIZONTAL_MOTION = 0.045
        private const val NORMAL_REENTRY_COOLDOWN_TICKS = 60L
        private const val MINIMUM_PANIC_TO_ENTER = 0.03
        private const val ANGLE_MASK = 0xFFFFL

        private val RELEASE_HEIGHTS = doubleArrayOf(0.75, 1.15, 0.4, 1.65)

        fun isOnReentryCooldown(fish: AbstractFish): Boolean {
            return fish.persistentData.getLongOr(REENTRY_COOLDOWN_TAG, 0L) >
                fish.level().gameTime
        }
    }

    private data class StoredFish(
        val data: CompoundTag,
        var shelteredTicks: Int
    )

    private val storedFish = mutableListOf<StoredFish>()
    private var unsafeTicks = 0
    private var quietTicks = 0
    private var nextThreatScanGameTime = Long.MIN_VALUE
    private var hasNearbyThreat = true
    private var releaseRequested = false

    val size: Int
        get() = storedFish.size

    fun canAccept(fish: AbstractFish): Boolean {
        val level = redSlobberer.level()
        return fish.level() === level &&
            redSlobberer.isAlive &&
            redSlobberer.isUnderWater &&
            !redSlobberer.isBaby &&
            fish.isAlive &&
            fish.isInWater &&
            !fish.isPassenger &&
            !fish.isVehicle &&
            !fish.isRemoved &&
            unsafeTicks <= 0 &&
            storedFish.size < configuredCapacity() &&
            !isOnReentryCooldown(fish)
    }

    fun entranceFor(fish: AbstractFish): Vec3 {
        val bodyRadius = redSlobberer.bbWidth.toDouble() * 0.5
        val entranceRadius = bodyRadius + fish.bbWidth.toDouble() * 0.5 + ENTRANCE_CLEARANCE
        val baseAngle = refugeAngle(fish)
        var fallback = entranceAt(baseAngle, entranceRadius)
        val level = redSlobberer.level()

        repeat(ENTRANCE_POSITION_ATTEMPTS) { attempt ->
            val alternatingOffset = if (attempt == 0) {
                0
            } else {
                (attempt + 1) / 2 * if (attempt % 2 == 1) 1 else -1
            }
            val angle = baseAngle + alternatingOffset * (Math.PI * 2.0 / ENTRANCE_POSITION_ATTEMPTS)
            val candidate = entranceAt(angle, entranceRadius)
            if (attempt == 0) fallback = candidate
            val candidatePos = BlockPos.containing(candidate)
            if (!level.isLoaded(candidatePos)) return@repeat
            if (!level.getFluidState(candidatePos).`is`(FluidTags.WATER)) return@repeat
            val displacement = candidate.subtract(fish.position())
            if (!level.noCollision(fish, fish.boundingBox.move(displacement))) return@repeat
            return candidate
        }
        return fallback
    }

    fun tryStore(
        level: ServerLevel,
        fish: AbstractFish,
        panic: Double
    ): Boolean {
        if (panic <= MINIMUM_PANIC_TO_ENTER || !canAccept(fish)) return false
        val entrance = entranceFor(fish)
        if (fish.distanceToSqr(entrance) > CAPTURE_DISTANCE_SQR) return false

        val reporter = ProblemReporter.Collector()
        val output = TagValueOutput.createWithContext(reporter, level.registryAccess())
        if (!fish.save(output) || !reporter.isEmpty) {
            SquAbyssalBloom.LOGGER.warn(
                "Could not serialize fish {} into Red Slobberer refuge {}: {}",
                fish.uuid,
                redSlobberer.uuid,
                if (reporter.isEmpty) "entity refused serialization" else reporter.report
            )
            return false
        }

        val fishId = fish.id
        val fishUuid = fish.uuid
        storedFish.add(StoredFish(output.buildResult(), 0))
        quietTicks = 0
        hasNearbyThreat = true
        nextThreatScanGameTime = level.gameTime
        FishCollectiveManager.forLevel(level).forget(fish)
        fish.discard()

        playEnterEffects(level, entrance)
        RedSlobbererReefManager.forLevel(level).recordFishStored(
            fishId,
            fishUuid,
            redSlobberer,
            storedFish.size
        )
        return true
    }

    fun tick(level: ServerLevel) {
        if (unsafeTicks > 0) unsafeTicks--
        if (storedFish.isEmpty()) {
            quietTicks = 0
            releaseRequested = false
            return
        }

        storedFish.forEach { stored ->
            stored.shelteredTicks = (stored.shelteredTicks + 1).coerceAtMost(Int.MAX_VALUE)
        }

        if (releaseRequested || !redSlobberer.isAlive) {
            releaseAll(level, ReleaseReason.SHELTER_DESTROYED)
            return
        }

        if (level.gameTime >= nextThreatScanGameTime) {
            nextThreatScanGameTime = level.gameTime + THREAT_SCAN_INTERVAL_TICKS
            hasNearbyThreat = hasThreatNearby(level)
        }
        quietTicks = if (hasNearbyThreat) 0 else (quietTicks + 1).coerceAtMost(Int.MAX_VALUE)

        val minimumStay = configuredMinimumStay()
        val maximumStay = configuredMaximumStay().coerceAtLeast(minimumStay)
        val quietRelease = configuredQuietRelease()
        releaseMatching(level, ReleaseReason.MAXIMUM_STAY) { stored ->
            stored.shelteredTicks >= maximumStay
        }
        if (!hasNearbyThreat && quietTicks >= quietRelease) {
            releaseMatching(level, ReleaseReason.AREA_CALM) { stored ->
                stored.shelteredTicks >= minimumStay
            }
        }
    }

    fun invalidateAfterAttack(level: ServerLevel) {
        unsafeTicks = configuredUnsafeCooldown()
        quietTicks = 0
        hasNearbyThreat = true
        releaseRequested = storedFish.isNotEmpty()
        RedSlobbererReefManager.forLevel(level).recordRefugeInvalidated(
            redSlobberer,
            storedFish.size,
            unsafeTicks
        )
        releaseAll(level, ReleaseReason.SHELTER_ATTACKED)
    }

    fun releaseOnDestruction(level: ServerLevel) {
        releaseRequested = storedFish.isNotEmpty()
        releaseAll(level, ReleaseReason.SHELTER_DESTROYED)
    }

    fun save(output: ValueOutput) {
        output.putInt(TAG_UNSAFE_TICKS, unsafeTicks)
        output.putInt(TAG_QUIET_TICKS, quietTicks)
        if (storedFish.isEmpty()) return

        val outputList = output.childrenList(TAG_STORED_FISH)
        for (stored in storedFish) {
            val child = outputList.addChild()
            child.putInt(TAG_SHELTERED_TICKS, stored.shelteredTicks)
            child.store(TAG_FISH_DATA, CompoundTag.CODEC, stored.data)
        }
    }

    fun load(input: ValueInput) {
        storedFish.clear()
        unsafeTicks = input.getIntOr(TAG_UNSAFE_TICKS, 0).coerceAtLeast(0)
        quietTicks = input.getIntOr(TAG_QUIET_TICKS, 0).coerceAtLeast(0)
        hasNearbyThreat = true
        nextThreatScanGameTime = Long.MIN_VALUE
        releaseRequested = false

        var loadedCount = 0
        for (child in input.childrenListOrEmpty(TAG_STORED_FISH)) {
            if (loadedCount >= MAXIMUM_LOADED_FISH) break
            val data = child.read(TAG_FISH_DATA, CompoundTag.CODEC).orElse(null) ?: continue
            storedFish.add(
                StoredFish(
                    data = data,
                    shelteredTicks = child.getIntOr(TAG_SHELTERED_TICKS, 0).coerceAtLeast(0)
                )
            )
            loadedCount++
        }
        releaseRequested = unsafeTicks > 0 && storedFish.isNotEmpty()
    }

    private fun releaseAll(level: ServerLevel, reason: ReleaseReason) {
        releaseMatching(level, reason) { true }
        releaseRequested = storedFish.isNotEmpty()
    }

    private fun releaseMatching(
        level: ServerLevel,
        reason: ReleaseReason,
        predicate: (StoredFish) -> Boolean
    ) {
        val releasePositions = mutableListOf<Vec3>()
        val iterator = storedFish.iterator()
        while (iterator.hasNext()) {
            val stored = iterator.next()
            if (!predicate(stored)) continue
            val releasePosition = restoreFish(level, stored, reason) ?: continue
            iterator.remove()
            releasePositions.add(releasePosition)
        }
        val released = releasePositions.size
        if (released <= 0) return

        playExitEffects(level, releasePositions)
        RedSlobbererReefManager.forLevel(level).recordFishReleased(
            redSlobberer,
            released,
            storedFish.size,
            reason.debugName
        )
    }

    private fun restoreFish(
        level: ServerLevel,
        stored: StoredFish,
        reason: ReleaseReason
    ): Vec3? {
        val restored = try {
            EntityType.loadEntityRecursive(
                stored.data.copy(),
                level,
                EntitySpawnReason.LOAD,
                EntityProcessor.NOP
            )
        } catch (exception: RuntimeException) {
            SquAbyssalBloom.LOGGER.error(
                "Could not restore fish from Red Slobberer refuge {}",
                redSlobberer.uuid,
                exception
            )
            null
        }
        val fish = restored as? AbstractFish
        if (fish == null) {
            SquAbyssalBloom.LOGGER.warn(
                "Stored refuge entity in Red Slobberer {} is not a fish; keeping its data",
                redSlobberer.uuid
            )
            return null
        }

        val releasePosition = findReleasePosition(level, fish)
        fish.setPos(releasePosition)
        val outward = releasePosition.subtract(redSlobberer.position())
            .multiply(1.0, 0.0, 1.0)
            .normalize()
        fish.deltaMovement = Vec3(
            outward.x * RELEASE_HORIZONTAL_MOTION,
            RELEASE_VERTICAL_MOTION,
            outward.z * RELEASE_HORIZONTAL_MOTION
        )
        fish.airSupply = fish.maxAirSupply
        val cooldown = when (reason) {
            ReleaseReason.AREA_CALM -> NORMAL_REENTRY_COOLDOWN_TICKS
            else -> configuredUnsafeCooldown().toLong()
        }
        fish.persistentData.putLong(REENTRY_COOLDOWN_TAG, level.gameTime + cooldown)
        FishCollectiveManager.forLevel(level).forget(fish)
        if (level.addFreshEntity(fish)) return releasePosition

        SquAbyssalBloom.LOGGER.warn(
            "Could not add restored fish {} from Red Slobberer refuge {} back to the level",
            fish.uuid,
            redSlobberer.uuid
        )
        return null
    }

    private fun findReleasePosition(level: ServerLevel, fish: AbstractFish): Vec3 {
        val bodyRadius = redSlobberer.bbWidth.toDouble() * 0.5
        val baseRadius = bodyRadius + fish.bbWidth.toDouble() * 0.5 + ENTRANCE_CLEARANCE
        val baseAngle = refugeAngle(fish)

        repeat(RELEASE_RING_ATTEMPTS) { ring ->
            val radius = baseRadius + ring * RELEASE_RING_STEP
            repeat(RELEASE_ANGLE_ATTEMPTS) { angleIndex ->
                val angle = baseAngle + angleIndex * (Math.PI * 2.0 / RELEASE_ANGLE_ATTEMPTS)
                for (height in RELEASE_HEIGHTS) {
                    val candidate = Vec3(
                        redSlobberer.x + cos(angle) * radius,
                        redSlobberer.boundingBox.minY + height,
                        redSlobberer.z + sin(angle) * radius
                    )
                    val blockPos = BlockPos.containing(candidate)
                    if (!level.isLoaded(blockPos)) continue
                    if (!level.getFluidState(blockPos).`is`(FluidTags.WATER)) continue
                    fish.setPos(candidate)
                    if (level.noCollision(fish)) return candidate
                }
            }
        }

        return entranceAt(baseAngle, baseRadius)
    }

    private fun hasThreatNearby(level: ServerLevel): Boolean {
        val radius = ModServerConfig.FISH_SCHOOL_THREAT_DETECTION_RADIUS.get().coerceAtLeast(1.0)
        return level.getEntitiesOfClass(
            LivingEntity::class.java,
            redSlobberer.boundingBox.inflate(radius)
        ) { candidate ->
            candidate !== redSlobberer && FishThreatClassifier.isPotentialThreat(candidate)
        }.isNotEmpty()
    }

    private fun entranceAt(angle: Double, radius: Double): Vec3 {
        return Vec3(
            redSlobberer.x + cos(angle) * radius,
            redSlobberer.boundingBox.minY + ENTRANCE_HEIGHT,
            redSlobberer.z + sin(angle) * radius
        )
    }

    private fun refugeAngle(fish: AbstractFish): Double {
        val mixedUuid = fish.uuid.mostSignificantBits xor redSlobberer.uuid.leastSignificantBits
        val normalized = (mixedUuid and ANGLE_MASK).toDouble() / ANGLE_MASK.toDouble()
        return normalized * Math.PI * 2.0
    }

    private fun playEnterEffects(level: ServerLevel, entrance: Vec3) {
        level.playSound(
            null,
            entrance.x,
            entrance.y,
            entrance.z,
            SoundEvents.BEEHIVE_ENTER,
            SoundSource.NEUTRAL,
            0.8f,
            0.9f + level.random.nextFloat() * 0.2f
        )
        level.sendParticles(
            ParticleTypes.BUBBLE_POP,
            entrance.x,
            entrance.y,
            entrance.z,
            7,
            0.15,
            0.15,
            0.15,
            0.02
        )
        level.sendParticles(
            ParticleTypes.POOF,
            entrance.x,
            entrance.y,
            entrance.z,
            4,
            0.12,
            0.1,
            0.12,
            0.015
        )

        val refugeCenter = Vec3(
            redSlobberer.x,
            redSlobberer.boundingBox.minY + ENTRANCE_HEIGHT,
            redSlobberer.z
        )
        for (step in 1..5) {
            val trailPosition = entrance.lerp(refugeCenter, step / 6.0)
            level.sendParticles(
                if (step % 2 == 0) ParticleTypes.NAUTILUS else ParticleTypes.BUBBLE,
                trailPosition.x,
                trailPosition.y,
                trailPosition.z,
                2,
                0.06,
                0.06,
                0.06,
                0.012
            )
        }
    }

    private fun playExitEffects(level: ServerLevel, releasePositions: List<Vec3>) {
        val released = releasePositions.size
        val position = Vec3(
            redSlobberer.x,
            redSlobberer.boundingBox.minY + ENTRANCE_HEIGHT,
            redSlobberer.z
        )
        level.playSound(
            null,
            position.x,
            position.y,
            position.z,
            SoundEvents.BEEHIVE_EXIT,
            SoundSource.NEUTRAL,
            0.8f,
            0.9f + level.random.nextFloat() * 0.2f
        )
        level.sendParticles(
            ParticleTypes.NAUTILUS,
            position.x,
            position.y,
            position.z,
            (released * 2).coerceAtMost(24),
            redSlobberer.bbWidth * 0.55,
            0.35,
            redSlobberer.bbWidth * 0.55,
            0.02
        )

        releasePositions.take(20).forEach { releasePosition ->
            level.sendParticles(
                ParticleTypes.BUBBLE_POP,
                releasePosition.x,
                releasePosition.y,
                releasePosition.z,
                6,
                0.16,
                0.14,
                0.16,
                0.025
            )
            level.sendParticles(
                ParticleTypes.POOF,
                releasePosition.x,
                releasePosition.y,
                releasePosition.z,
                3,
                0.1,
                0.08,
                0.1,
                0.012
            )
        }
    }

    private fun configuredCapacity(): Int =
        ModServerConfig.RED_SLOBBERER_FISH_REFUGE_CAPACITY.get().coerceAtLeast(0)

    private fun configuredMinimumStay(): Int =
        ModServerConfig.RED_SLOBBERER_FISH_REFUGE_MINIMUM_STAY_TICKS.get().coerceAtLeast(0)

    private fun configuredMaximumStay(): Int =
        ModServerConfig.RED_SLOBBERER_FISH_REFUGE_MAXIMUM_STAY_TICKS.get().coerceAtLeast(1)

    private fun configuredQuietRelease(): Int =
        ModServerConfig.RED_SLOBBERER_FISH_REFUGE_QUIET_RELEASE_TICKS.get().coerceAtLeast(0)

    private fun configuredUnsafeCooldown(): Int =
        ModServerConfig.RED_SLOBBERER_FISH_REFUGE_UNSAFE_COOLDOWN_TICKS.get().coerceAtLeast(0)

    private enum class ReleaseReason(val debugName: String) {
        AREA_CALM("area_calm"),
        MAXIMUM_STAY("maximum_stay"),
        SHELTER_ATTACKED("shelter_attacked"),
        SHELTER_DESTROYED("shelter_destroyed")
    }
}
