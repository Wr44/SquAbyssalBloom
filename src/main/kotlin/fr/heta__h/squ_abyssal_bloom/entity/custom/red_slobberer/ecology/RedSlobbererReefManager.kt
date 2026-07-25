package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.block.calcareous_deposit.CalcareousDepositBlock
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SeaPickleBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt


class RedSlobbererReefManager private constructor(
    private val level: ServerLevel
) {

    companion object {
        private val INSTANCES: MutableMap<ServerLevel, RedSlobbererReefManager> = WeakHashMap()

        @JvmStatic
        @Synchronized
        fun forLevel(level: ServerLevel): RedSlobbererReefManager {
            return INSTANCES.getOrPut(level) { RedSlobbererReefManager(level) }
        }

        @JvmStatic
        @Synchronized
        fun releaseLevel(level: ServerLevel) {
            INSTANCES.remove(level)
        }

        const val MEMBERSHIP_REFRESH_TICKS = 100L
        const val OBSERVATION_EXPIRY_TICKS = 3L
        const val MINIMUM_GROUP_SIZE = 2
        const val GROUP_HORIZONTAL_RADIUS = 32.0
        const val GROUP_HORIZONTAL_RADIUS_SQR = GROUP_HORIZONTAL_RADIUS * GROUP_HORIZONTAL_RADIUS
        const val GROUP_VERTICAL_RADIUS = 12.0
        const val REEF_REATTACH_RADIUS = 18.0
        const val REEF_REATTACH_RADIUS_SQR = REEF_REATTACH_RADIUS * REEF_REATTACH_RADIUS
        const val REEF_REATTACH_VERTICAL_RADIUS = 8.0
        const val ANCHOR_STABILITY_RADIUS = 12.0
        const val ANCHOR_STABILITY_VERTICAL_RADIUS = 6.0
        const val DISPERSION_STABILITY_RADIUS = 14.0
        const val MAXIMUM_MATURITY_MULTIPLIER = 8L
        const val INACTIVE_MATURITY_LOSS_PER_REFRESH = 5L
        const val EMPTY_REEF_RETENTION_TICKS = 24000L

        const val ECOLOGY_INTERVAL_TICKS = 100L
        const val BASE_FISH_COUNT = 2
        const val FISH_PER_GROUP_MEMBER = 2
        const val MAX_MATURE_REEF_FISH_BONUS = 4
        const val FISH_MATURITY_STEP_TICKS = 1200L
        const val FISH_COUNT_RADIUS = 24.0
        const val FISH_COUNT_VERTICAL_RADIUS = 10.0
        const val FISH_SPAWN_ATTEMPT_CHANCE = 0.65f
        const val FISH_SPAWN_POSITION_ATTEMPTS = 8
        const val FISH_SPAWN_RADIUS = 12
        const val MIN_FISH_SPAWN_DISTANCE_SQR = 25
        const val MIN_FISH_HEIGHT = 2
        const val FISH_HEIGHT_VARIATION = 5

        const val DECORATION_INTERVAL_TICKS = 1200L
        const val MAX_DECORATIONS_PER_REEF = 8
        const val DECORATION_POSITION_ATTEMPTS = 8
        const val DECORATION_RADIUS = 8
        const val DECORATION_SEARCH_ABOVE = 4
        const val DECORATION_VERTICAL_SEARCH = 10
        const val SEA_PICKLE_CHANCE = 0.6f
        const val MAX_NEW_PICKLES = 2

        const val BASE_MAXIMUM_DEPOSITS = 6
        const val DEPOSIT_POSITION_ATTEMPTS = 18
        const val DEPOSIT_RADIUS = 12
        const val MINIMUM_DEPOSIT_RADIUS_SQR = 9
        const val MAXIMUM_DEPOSIT_RADIUS_SQR = DEPOSIT_RADIUS * DEPOSIT_RADIUS
        const val DEPOSIT_SEARCH_ABOVE = 6
        const val DEPOSIT_VERTICAL_SEARCH = 16
        const val MINIMUM_DEPOSIT_SPACING_SQR = 16.0
        const val DEPOSIT_SPACING_VERTICAL_RANGE = 4
        const val FISH_REEF_VERTICAL_RADIUS = 12.0

        const val DEBUG_SUMMARY_INTERVAL_TICKS = 100L
        const val DEBUG_PARTICLE_INTERVAL_TICKS = 20L
        const val DEBUG_REFUGE_PARTICLE_INTERVAL_TICKS = 10L
        const val DEBUG_REFUGE_ACTIVE_WINDOW_TICKS = 30L
        const val DEBUG_REFUGE_EVENT_GAP_TICKS = 60L

        private val HORIZONTAL_DIRECTIONS = arrayOf(
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
        )

        private val LIVE_CORAL_DECORATIONS = arrayOf(
            Blocks.FIRE_CORAL,
            Blocks.FIRE_CORAL_FAN
        )
    }

    private data class Observation(
        var entity: RedSlobbererEntity,
        var lastSeenGameTime: Long
    )

    private data class DepositPlacementDiagnostics(
        var radiusRejectedAttempts: Int = 0,
        var unloadedColumns: Int = 0,
        var scannedPositions: Int = 0,
        var rejectedNonSourceWater: Int = 0,
        var rejectedSupport: Int = 0,
        var rejectedSpacing: Int = 0,
        var rejectedSurvival: Int = 0,
        var setBlockFailures: Int = 0,
        val rejectedSupportBlocks: MutableMap<String, Int> = hashMapOf()
    )

    private data class FishSpawnDiagnostics(
        var radiusRejectedAttempts: Int = 0,
        var unloadedPositions: Int = 0,
        var rejectedWater: Int = 0,
        var rejectedClearance: Int = 0,
        var entitySpawnFailures: Int = 0
    )

    private data class DecorationPlacementDiagnostics(
        var unloadedColumns: Int = 0,
        var scannedPositions: Int = 0,
        var rejectedNonWater: Int = 0,
        var rejectedSupport: Int = 0,
        var rejectedSurvival: Int = 0,
        var setBlockFailures: Int = 0
    )

    private data class ActiveFishRefuge(
        val shelterId: UUID,
        val lastInfluenceGameTime: Long
    )

    private val savedData = level.dataStorage.computeIfAbsent(RedSlobbererReefSavedData.TYPE)

    private val observations: MutableMap<UUID, Observation> = hashMapOf()
    private val memberToReef: MutableMap<UUID, UUID> = hashMapOf()

    private var lastTickGameTime = Long.MIN_VALUE
    private var nextMembershipRefreshGameTime = 0L

    private var debugWasEnabled = false
    private var nextDebugSummaryGameTime = 0L
    private var refugeScans = 0
    private var refugeScansWithoutNearbyRed = 0
    private var refugeCandidateRejectionsNoReef = 0
    private var refugeCandidateRejectionsInactiveReef = 0
    private var refugeCandidateRejectionsOutsideReef = 0
    private var refugeCandidateRejectionsUnavailable = 0
    private var refugeMatches = 0
    private var refugeThreatApplications = 0
    private var refugeNearNestEntranceApplications = 0
    private val activeRefugeByFish: MutableMap<UUID, ActiveFishRefuge> = hashMapOf()

    fun observe(redSlobberer: RedSlobbererEntity) {
        val gameTime = level.gameTime
        observations.compute(redSlobberer.uuid) { _, previous ->
            (previous ?: Observation(redSlobberer, gameTime)).also { observation ->
                observation.entity = redSlobberer
                observation.lastSeenGameTime = gameTime
            }
        }
        redSlobberer.consumeLegacyReefSnapshot()?.let { snapshot ->
            importLegacyReef(redSlobberer.uuid, snapshot, gameTime)
        }
        tickOnce(gameTime)
    }

    private fun importLegacyReef(
        memberId: UUID,
        snapshot: LegacyRedSlobbererReefSnapshot,
        gameTime: Long
    ) {
        val existing = savedData.reefs.values.asSequence()
            .filter { reef -> abs(reef.anchor.y - snapshot.anchor.y) <= 3 }
            .minByOrNull { reef -> reef.anchor.distSqr(snapshot.anchor) }
            ?.takeIf { reef -> reef.anchor.distSqr(snapshot.anchor) <= 36.0 }
        val reef = existing ?: ReefState(
            id = UUID.randomUUID(),
            anchor = snapshot.anchor,
            lastActiveGameTime = gameTime
        ).also { created -> savedData.reefs[created.id] = created }

        reef.maturityTicks = max(reef.maturityTicks, snapshot.residenceTicks.toLong())
        reef.placedDecorations = max(reef.placedDecorations, snapshot.placedDecorations)
        reef.lastActiveGameTime = gameTime
        memberToReef[memberId] = reef.id
        ensureSchedules(reef, gameTime)
        savedData.setDirty()
    }

    fun findNearestShelter(
        fish: AbstractFish,
        candidates: List<RedSlobbererEntity>
    ): RedSlobbererEntity? {
        val debug = debugEnabled()
        if (RedSlobbererFishRefugeStorage.isOnReentryCooldown(fish)) {
            if (debug) refugeCandidateRejectionsUnavailable += candidates.size
            return null
        }
        val reefRadius = configuredRefugeRadius()
        val reefRadiusSqr = reefRadius * reefRadius
        var nearest: RedSlobbererEntity? = null
        var nearestDistanceSqr = Double.MAX_VALUE

        for (candidate in candidates) {
            if (!candidate.isAlive || !candidate.isUnderWater) continue
            if (!candidate.canShelterFish(fish)) {
                if (debug) refugeCandidateRejectionsUnavailable++
                continue
            }
            val reefId = memberToReef[candidate.uuid]
            if (reefId == null) {
                if (debug) refugeCandidateRejectionsNoReef++
                continue
            }
            val reef = savedData.reefs[reefId]
            if (reef == null || reef.members.size < MINIMUM_GROUP_SIZE) {
                if (debug) refugeCandidateRejectionsInactiveReef++
                continue
            }
            val dx = reef.center.x - fish.x
            val dz = reef.center.z - fish.z
            if (
                dx * dx + dz * dz > reefRadiusSqr ||
                abs(reef.center.y - fish.y) > FISH_REEF_VERTICAL_RADIUS
            ) {
                if (debug) refugeCandidateRejectionsOutsideReef++
                continue
            }
            val distanceSqr = fish.distanceToSqr(candidate)
            if (distanceSqr < nearestDistanceSqr) {
                nearest = candidate
                nearestDistanceSqr = distanceSqr
            }
        }
        if (debug && nearest != null) refugeMatches++
        return nearest
    }

    /** Returns the fixed environmental anchor of the member's currently active collective reef. */
    fun activeReefAnchor(member: RedSlobbererEntity): BlockPos? {
        if (member.level() !== level || !member.isAlive) return null
        val reefId = memberToReef[member.uuid] ?: return null
        val reef = savedData.reefs[reefId] ?: return null
        if (reef.members.size < MINIMUM_GROUP_SIZE) return null
        return reef.anchor
    }

    fun recordRefugeScan(hasNearbyRedSlobberer: Boolean) {
        if (!debugEnabled()) return
        refugeScans++
        if (!hasNearbyRedSlobberer) refugeScansWithoutNearbyRed++
    }

    fun recordRefugeInfluence(
        fish: AbstractFish,
        shelter: RedSlobbererEntity,
        panic: Double,
        entrance: Vec3
    ) {
        if (panic <= 0.03) return
        val gameTime = level.gameTime
        val previousRefuge = activeRefugeByFish.put(
            fish.uuid,
            ActiveFishRefuge(shelter.uuid, gameTime)
        )
        if (!debugEnabled()) return
        if (
            previousRefuge == null ||
            previousRefuge.shelterId != shelter.uuid ||
            gameTime - previousRefuge.lastInfluenceGameTime > DEBUG_REFUGE_EVENT_GAP_TICKS
        ) {
            SquAbyssalBloom.LOGGER.info(
                "[Red Slobberer reef debug] event=fishRefugeApproach fishId={} shelterId={} panic={} distance={}",
                fish.id,
                shelter.id,
                debugDecimal(panic),
                debugDecimal(sqrt(fish.distanceToSqr(shelter)))
            )
        }
        if (Math.floorMod(gameTime + fish.id.toLong(), DEBUG_REFUGE_PARTICLE_INTERVAL_TICKS) != 0L) {
            return
        }

        refugeThreatApplications++
        if (fish.distanceToSqr(entrance) <= 1.0) refugeNearNestEntranceApplications++
        for (fraction in listOf(0.25, 0.5, 0.75)) {
            level.sendParticles(
                ParticleTypes.END_ROD,
                fish.x + (entrance.x - fish.x) * fraction,
                fish.y + (entrance.y - fish.y) * fraction,
                fish.z + (entrance.z - fish.z) * fraction,
                1,
                0.0,
                0.0,
                0.0,
                0.0
            )
        }
        level.sendParticles(
            ParticleTypes.NAUTILUS,
            fish.x,
            fish.eyeY,
            fish.z,
            1,
            0.05,
            0.05,
            0.05,
            0.0
        )
    }

    fun recordFishStored(
        fishId: Int,
        fishUuid: UUID,
        shelter: RedSlobbererEntity,
        storedCount: Int
    ) {
        activeRefugeByFish.remove(fishUuid)
        if (!debugEnabled()) return
        SquAbyssalBloom.LOGGER.info(
            "[Red Slobberer reef debug] event=fishRefugeStored fishId={} fishUuid={} shelterId={} stored={}",
            fishId,
            fishUuid,
            shelter.id,
            storedCount
        )
    }

    fun recordFishReleased(
        shelter: RedSlobbererEntity,
        releasedCount: Int,
        remainingCount: Int,
        reason: String
    ) {
        if (!debugEnabled()) return
        SquAbyssalBloom.LOGGER.info(
            "[Red Slobberer reef debug] event=fishRefugeReleased shelterId={} released={} remaining={} reason={}",
            shelter.id,
            releasedCount,
            remainingCount,
            reason
        )
    }

    fun recordRefugeInvalidated(
        shelter: RedSlobbererEntity,
        storedCount: Int,
        cooldownTicks: Int
    ) {
        if (!debugEnabled()) return
        SquAbyssalBloom.LOGGER.info(
            "[Red Slobberer reef debug] event=fishRefugeInvalidated shelterId={} storedBeforeRelease={} cooldown={}",
            shelter.id,
            storedCount,
            cooldownTicks
        )
    }

    private fun tickOnce(gameTime: Long) {
        if (lastTickGameTime == gameTime) return
        lastTickGameTime = gameTime

        if (gameTime >= nextMembershipRefreshGameTime) {
            nextMembershipRefreshGameTime = gameTime + MEMBERSHIP_REFRESH_TICKS
            refreshCollectiveReefs(gameTime)
        }

        for (reef in savedData.reefs.values) {
            ensureSchedules(reef, gameTime)

            if (gameTime >= reef.nextEcologyGameTime) {
                reef.nextEcologyGameTime = gameTime + ECOLOGY_INTERVAL_TICKS
                tickReefEcology(reef, gameTime)
                savedData.setDirty()
            }

            if (gameTime >= reef.nextDepositGameTime) {
                reef.nextDepositGameTime = gameTime + nextDepositDelay()
                tickCalcareousDeposits(reef, gameTime)
                savedData.setDirty()
            }
        }
        if (Math.floorMod(gameTime, DEBUG_SUMMARY_INTERVAL_TICKS) == 0L) {
            activeRefugeByFish.entries.removeIf { (_, activeRefuge) ->
                gameTime - activeRefuge.lastInfluenceGameTime >
                    DEBUG_REFUGE_EVENT_GAP_TICKS * 4L
            }
        }
        tickDebug(gameTime)
    }

    private fun refreshCollectiveReefs(gameTime: Long) {
        observations.entries.removeIf { (_, observation) ->
            gameTime - observation.lastSeenGameTime > OBSERVATION_EXPIRY_TICKS ||
                !observation.entity.isAlive ||
                observation.entity.isRemoved
        }

        val active = observations.values.asSequence()
            .map { observation -> observation.entity }
            .filter { redSlobberer -> redSlobberer.isAlive && redSlobberer.isUnderWater }
            .distinctBy { redSlobberer -> redSlobberer.uuid }
            .sortedBy { redSlobberer -> redSlobberer.uuid }
            .toList()
        val components = buildConnectedComponents(active)
            .filter { component -> component.size >= MINIMUM_GROUP_SIZE }
            .sortedWith(
                compareByDescending<List<RedSlobbererEntity>> { component -> component.size }
                    .thenBy { component -> component.first().uuid }
            )

        val previousMembership = HashMap(memberToReef)
        memberToReef.clear()
        savedData.reefs.values.forEach { reef ->
            reef.members = emptyList()
            reef.center = Vec3.atCenterOf(reef.anchor)
            reef.collectiveActivity = 0.0
            reef.stability = 0.0
        }

        val claimedReefs = hashSetOf<UUID>()
        for (component in components) {
            val center = calculateCenter(component)
            val reef = selectOrCreateReef(
                component,
                center,
                previousMembership,
                claimedReefs,
                gameTime
            )
            claimedReefs.add(reef.id)
            updateActiveReef(reef, component, center, gameTime)
            component.forEach { member -> memberToReef[member.uuid] = reef.id }
        }

        val maturityRequirement = ModServerConfig.RED_SLOBBERER_REEF_MATURITY_TICKS.get()
        val iterator = savedData.reefs.values.iterator()
        while (iterator.hasNext()) {
            val reef = iterator.next()
            if (reef.members.size >= MINIMUM_GROUP_SIZE) continue

            if (gameTime - reef.lastActiveGameTime > ReefState.DECLINE_GRACE_TICKS) {
                reef.maturityTicks = (reef.maturityTicks - INACTIVE_MATURITY_LOSS_PER_REFRESH)
                    .coerceAtLeast(0L)
            }
            val canForgetEmptyTrace = reef.maturityTicks == 0L &&
                gameTime - reef.lastActiveGameTime > EMPTY_REEF_RETENTION_TICKS
            if (canForgetEmptyTrace) iterator.remove()
        }

        val maximumMaturity = maturityRequirement.toLong() * MAXIMUM_MATURITY_MULTIPLIER
        savedData.reefs.values.forEach { reef ->
            reef.maturityTicks = reef.maturityTicks.coerceIn(0L, maximumMaturity)
        }
        savedData.setDirty()
    }

    private fun buildConnectedComponents(
        entities: List<RedSlobbererEntity>
    ): List<List<RedSlobbererEntity>> {
        val components = mutableListOf<List<RedSlobbererEntity>>()
        val visited = hashSetOf<UUID>()

        for (seed in entities) {
            if (!visited.add(seed.uuid)) continue
            val queue = ArrayDeque<RedSlobbererEntity>()
            val component = mutableListOf<RedSlobbererEntity>()
            queue.add(seed)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                component.add(current)
                for (candidate in entities) {
                    if (candidate.uuid in visited || !areCollectiveNeighbors(current, candidate)) continue
                    visited.add(candidate.uuid)
                    queue.add(candidate)
                }
            }
            components.add(component.sortedBy { member -> member.uuid })
        }
        return components
    }

    private fun areCollectiveNeighbors(
        first: RedSlobbererEntity,
        second: RedSlobbererEntity
    ): Boolean {
        val dx = first.x - second.x
        val dz = first.z - second.z
        return dx * dx + dz * dz <= GROUP_HORIZONTAL_RADIUS_SQR &&
            abs(first.y - second.y) <= GROUP_VERTICAL_RADIUS
    }

    private fun selectOrCreateReef(
        component: List<RedSlobbererEntity>,
        center: Vec3,
        previousMembership: Map<UUID, UUID>,
        claimedReefs: Set<UUID>,
        gameTime: Long
    ): ReefState {
        val overlapCounts = component.mapNotNull { member -> previousMembership[member.uuid] }
            .groupingBy { id -> id }
            .eachCount()
        val overlapping = overlapCounts.entries.asSequence()
            .mapNotNull { (id, count) ->
                savedData.reefs[id]?.takeIf { reef ->
                    id !in claimedReefs && canReattach(reef, center)
                }?.let { reef -> Triple(reef, count, reef.maturityTicks) }
            }
            .sortedWith(
                compareByDescending<Triple<ReefState, Int, Long>> { entry -> entry.second }
                    .thenByDescending { entry -> entry.third }
                    .thenBy { entry -> entry.first.id }
            )
            .firstOrNull()
            ?.first

        val nearby = overlapping ?: savedData.reefs.values.asSequence()
            .filter { reef -> reef.id !in claimedReefs && canReattach(reef, center) }
            .minByOrNull { reef -> horizontalDistanceSqr(reef.anchor, center) }

        if (nearby != null) {
            mergeOverlappingReefs(nearby, overlapCounts.keys, claimedReefs)
            return nearby
        }

        val id = UUID.randomUUID()
        return ReefState(
            id = id,
            anchor = BlockPos.containing(center),
            lastActiveGameTime = gameTime,
            nextEcologyGameTime = gameTime + stagger(id, ECOLOGY_INTERVAL_TICKS),
            nextDepositGameTime = gameTime + staggeredDepositDelay(id),
            nextDecorationGameTime = gameTime + DECORATION_INTERVAL_TICKS +
                stagger(id, DECORATION_INTERVAL_TICKS)
        ).also { reef -> savedData.reefs[id] = reef }
    }

    private fun mergeOverlappingReefs(
        target: ReefState,
        overlappingIds: Set<UUID>,
        claimedReefs: Set<UUID>
    ) {
        for (id in overlappingIds) {
            if (id == target.id || id in claimedReefs) continue
            val merged = savedData.reefs.remove(id) ?: continue
            target.maturityTicks = max(target.maturityTicks, merged.maturityTicks)
            target.lastActiveGameTime = max(target.lastActiveGameTime, merged.lastActiveGameTime)
            target.placedDecorations = max(target.placedDecorations, merged.placedDecorations)
            target.depositPositions.addAll(merged.depositPositions)
            if (merged.nextDepositGameTime > 0L) {
                target.nextDepositGameTime = ModUtilities.minOfPositive(
                    target.nextDepositGameTime,
                    merged.nextDepositGameTime
                )
            }
        }
    }

    private fun canReattach(reef: ReefState, center: Vec3): Boolean {
        return horizontalDistanceSqr(reef.anchor, center) <= REEF_REATTACH_RADIUS_SQR &&
            abs(reef.anchor.y + 0.5 - center.y) <= REEF_REATTACH_VERTICAL_RADIUS
    }

    private fun updateActiveReef(
        reef: ReefState,
        members: List<RedSlobbererEntity>,
        center: Vec3,
        gameTime: Long
    ) {
        reef.members = members
        reef.center = center
        reef.lastActiveGameTime = gameTime

        val anchorDistance = sqrt(horizontalDistanceSqr(reef.anchor, center))
        val verticalDistance = abs(reef.anchor.y + 0.5 - center.y)
        val dispersion = sqrt(
            members.sumOf { member ->
                val dx = member.x - center.x
                val dz = member.z - center.z
                dx * dx + dz * dz
            } / members.size.toDouble()
        )
        reef.stability = (
            1.0 -
                anchorDistance / ANCHOR_STABILITY_RADIUS * 0.55 -
                verticalDistance / ANCHOR_STABILITY_VERTICAL_RADIUS * 0.20 -
                dispersion / DISPERSION_STABILITY_RADIUS * 0.25
            ).coerceIn(0.0, 1.0)

        val sizeActivity = (0.65 + (members.size - MINIMUM_GROUP_SIZE) * 0.10)
            .coerceAtMost(1.0)
        val movementActivity = members.map { member ->
            val movement = member.deltaMovement
            (sqrt(movement.x * movement.x + movement.z * movement.z) / 0.10)
                .coerceIn(0.0, 1.0)
        }.average()
        reef.collectiveActivity = sizeActivity * (0.75 + movementActivity * 0.25)

        if (reef.stability >= configuredMinimumStability()) {
            val gainedTicks = (
                MEMBERSHIP_REFRESH_TICKS * reef.collectiveActivity * reef.stability
                ).roundToLong().coerceAtLeast(1L)
            reef.maturityTicks += gainedTicks
        }
    }

    private fun tickReefEcology(reef: ReefState, gameTime: Long) {
        if (reef.members.size < MINIMUM_GROUP_SIZE) return
        val maturityRequirement = ModServerConfig.RED_SLOBBERER_REEF_MATURITY_TICKS.get()
        maintainFishPopulation(reef, maturityRequirement)

        if (
            reef.stage(maturityRequirement, gameTime) == ReefStage.MATURE &&
            gameTime >= reef.nextDecorationGameTime
        ) {
            reef.nextDecorationGameTime = gameTime + DECORATION_INTERVAL_TICKS
            if (
                reef.placedDecorations < MAX_DECORATIONS_PER_REEF &&
                level.gameRules.get(GameRules.MOB_GRIEFING) &&
                level.random.nextFloat() < reef.collectiveActivity * reef.stability &&
                tryPlaceReefDecoration(reef)
            ) {
                reef.placedDecorations++
            }
        }
    }

    private fun maintainFishPopulation(reef: ReefState, maturityRequirement: Int) {
        if (!level.gameRules.get(GameRules.SPAWN_MOBS)) {
            if (debugEnabled()) {
                SquAbyssalBloom.LOGGER.info(
                    "[Red Slobberer reef debug] event=fishPopulation reef={} result=blocked reason=spawn_mobs=false",
                    shortReefId(reef)
                )
            }
            return
        }

        val maturityBonus = if (reef.maturityTicks < maturityRequirement) {
            0
        } else {
            (1L + (reef.maturityTicks - maturityRequirement) / FISH_MATURITY_STEP_TICKS)
                .coerceAtMost(MAX_MATURE_REEF_FISH_BONUS.toLong())
                .toInt()
        }
        val desiredFish = (
            BASE_FISH_COUNT + reef.members.size * FISH_PER_GROUP_MEMBER + maturityBonus
            ).coerceAtMost(ModServerConfig.RED_SLOBBERER_MAX_REEF_FISH.get())
        val center = reef.center
        val searchBox = AABB(
            center.x - FISH_COUNT_RADIUS,
            center.y - FISH_COUNT_VERTICAL_RADIUS,
            center.z - FISH_COUNT_RADIUS,
            center.x + FISH_COUNT_RADIUS,
            center.y + FISH_COUNT_VERTICAL_RADIUS,
            center.z + FISH_COUNT_RADIUS
        )
        val visibleFish = level.getEntitiesOfClass(AbstractFish::class.java, searchBox) { fish ->
            fish.isAlive && fish.isInWater
        }.size
        val shelteredFish = reef.members.sumOf { member -> member.shelteredFishCount }
        val accountedFish = visibleFish + shelteredFish

        if (accountedFish >= desiredFish) {
            if (debugEnabled()) {
                SquAbyssalBloom.LOGGER.info(
                    "[Red Slobberer reef debug] event=fishPopulation reef={} result=atCapacity visible={} sheltered={} total={}/{}",
                    shortReefId(reef),
                    visibleFish,
                    shelteredFish,
                    accountedFish,
                    desiredFish
                )
            }
            return
        }
        val spawnRoll = level.random.nextFloat()
        if (spawnRoll > FISH_SPAWN_ATTEMPT_CHANCE) {
            if (debugEnabled()) {
                SquAbyssalBloom.LOGGER.info(
                    "[Red Slobberer reef debug] event=fishPopulation reef={} result=chanceFailed visible={} sheltered={} total={}/{} chance={} roll={}",
                    shortReefId(reef),
                    visibleFish,
                    shelteredFish,
                    accountedFish,
                    desiredFish,
                    FISH_SPAWN_ATTEMPT_CHANCE,
                    debugDecimal(spawnRoll.toDouble())
                )
            }
            return
        }
        trySpawnFish(reef, center, accountedFish, desiredFish)
    }

    private fun trySpawnFish(
        reef: ReefState,
        center: Vec3,
        accountedFish: Int,
        desiredFish: Int
    ) {
        val diagnostics = FishSpawnDiagnostics()
        val centerY = floor(center.y).toInt()
        repeat(FISH_SPAWN_POSITION_ATTEMPTS) {
            val dx = level.random.nextInt(-FISH_SPAWN_RADIUS, FISH_SPAWN_RADIUS + 1)
            val dz = level.random.nextInt(-FISH_SPAWN_RADIUS, FISH_SPAWN_RADIUS + 1)
            if (dx * dx + dz * dz < MIN_FISH_SPAWN_DISTANCE_SQR) {
                diagnostics.radiusRejectedAttempts++
                return@repeat
            }

            val pos = BlockPos(
                floor(center.x).toInt() + dx,
                centerY + MIN_FISH_HEIGHT + level.random.nextInt(FISH_HEIGHT_VARIATION),
                floor(center.z).toInt() + dz
            )
            if (!level.isLoaded(pos)) {
                diagnostics.unloadedPositions++
                return@repeat
            }
            if (!level.getFluidState(pos).`is`(FluidTags.WATER)) {
                diagnostics.rejectedWater++
                return@repeat
            }
            if (!level.getFluidState(pos.above()).`is`(FluidTags.WATER)) {
                diagnostics.rejectedClearance++
                return@repeat
            }
            val spawned = EntityType.TROPICAL_FISH.spawn(level, pos, EntitySpawnReason.NATURAL)
            if (spawned != null) {
                if (debugEnabled()) {
                    SquAbyssalBloom.LOGGER.info(
                        "[Red Slobberer reef debug] event=fishSpawned reef={} fishId={} pos={} accountedBefore={}/{}",
                        shortReefId(reef),
                        spawned.id,
                        debugPos(pos),
                        accountedFish,
                        desiredFish
                    )
                }
                return
            }
            diagnostics.entitySpawnFailures++
        }
        if (debugEnabled()) {
            SquAbyssalBloom.LOGGER.info(
                "[Red Slobberer reef debug] event=fishSpawnFailed reef={} accounted={}/{} attempts={} radiusRejected={} unloaded={} notWater={} noClearance={} entityRejected={}",
                shortReefId(reef),
                accountedFish,
                desiredFish,
                FISH_SPAWN_POSITION_ATTEMPTS,
                diagnostics.radiusRejectedAttempts,
                diagnostics.unloadedPositions,
                diagnostics.rejectedWater,
                diagnostics.rejectedClearance,
                diagnostics.entitySpawnFailures
            )
        }
    }

    private fun tickCalcareousDeposits(reef: ReefState, gameTime: Long) {
        val removedDeposits = pruneMissingDeposits(reef)
        if (debugEnabled() && removedDeposits > 0) {
            SquAbyssalBloom.LOGGER.info(
                "[Red Slobberer reef debug] event=depositPruned reef={} removed={} remaining={}",
                shortReefId(reef),
                removedDeposits,
                reef.depositPositions.size
            )
        }
        val maturityRequirement = ModServerConfig.RED_SLOBBERER_REEF_MATURITY_TICKS.get()
        val blockers = depositBlockers(reef, maturityRequirement, gameTime)
        if (blockers.isNotEmpty()) {
            if (debugEnabled()) {
                SquAbyssalBloom.LOGGER.info(
                    "[Red Slobberer reef debug] event=depositCheck reef={} result=blocked reasons={}",
                    shortReefId(reef),
                    blockers.joinToString("+")
                )
            }
            return
        }

        val maturityRatio = reef.maturityTicks.toDouble() / maturityRequirement.coerceAtLeast(1)
        val ageFactor = (maturityRatio - 1.0).coerceIn(0.0, 2.0)
        val ecologicalFactor = reef.collectiveActivity * reef.stability

        val growable = reef.depositPositions.asSequence()
            .map(BlockPos::of)
            .filter { pos -> level.isLoaded(pos) }
            .filter { pos ->
                val state = level.getBlockState(pos)
                state.`is`(ModBlocks.CALCAREOUS_DEPOSIT.get()) &&
                    state.getValue(CalcareousDepositBlock.AGE) < CalcareousDepositBlock.MAX_AGE
            }
            .toList()
        val growthChance = (
            (ModServerConfig.RED_SLOBBERER_DEPOSIT_GROWTH_CHANCE.get() + ageFactor * 0.035) *
                ecologicalFactor
            ).coerceIn(0.0, 1.0)
        val growthRoll = if (growable.isNotEmpty()) level.random.nextDouble() else null
        if (growthRoll != null && growthRoll < growthChance) {
            val pos = growable[level.random.nextInt(growable.size)]
            val state = level.getBlockState(pos)
            val oldAge = state.getValue(CalcareousDepositBlock.AGE)
            val grownState = state.setValue(CalcareousDepositBlock.AGE, oldAge + 1)
            if (level.setBlock(
                pos,
                grownState,
                Block.UPDATE_ALL
            )) {
                playCalcareousGrowthEffects(pos, grownState)
                if (debugEnabled()) {
                    SquAbyssalBloom.LOGGER.info(
                        "[Red Slobberer reef debug] event=depositGrew reef={} pos={} age={}->{} chance={} roll={}",
                        shortReefId(reef),
                        debugPos(pos),
                        oldAge,
                        oldAge + 1,
                        debugDecimal(growthChance),
                        debugDecimal(growthRoll)
                    )
                }
                return
            }
        }

        val configuredMaximum = ModServerConfig.RED_SLOBBERER_MAX_CALCAREOUS_DEPOSITS.get()
        val maximumDeposits = (
            BASE_MAXIMUM_DEPOSITS +
                reef.members.size.coerceAtMost(4) +
                ageFactor.toInt()
            ).coerceAtMost(configuredMaximum)
        if (maximumDeposits <= 0 || reef.depositPositions.size >= maximumDeposits) {
            if (debugEnabled()) {
                SquAbyssalBloom.LOGGER.info(
                    "[Red Slobberer reef debug] event=depositCheck reef={} result=capacityReached deposits={}/{} growable={} growthChance={} growthRoll={}",
                    shortReefId(reef),
                    reef.depositPositions.size,
                    maximumDeposits,
                    growable.size,
                    debugDecimal(growthChance),
                    growthRoll?.let(::debugDecimal) ?: "none"
                )
            }
            return
        }

        val remainingCapacity =
            1.0 - reef.depositPositions.size.toDouble() / maximumDeposits.toDouble()
        val newDepositChance = (
            (ModServerConfig.RED_SLOBBERER_NEW_DEPOSIT_CHANCE.get() + ageFactor * 0.02) *
                ecologicalFactor * remainingCapacity * remainingCapacity
            ).coerceIn(0.0, 1.0)
        val newDepositRoll = level.random.nextDouble()
        if (newDepositRoll >= newDepositChance) {
            if (debugEnabled()) {
                SquAbyssalBloom.LOGGER.info(
                    "[Red Slobberer reef debug] event=depositCheck reef={} result=chanceFailed deposits={}/{} growable={} growthChance={} growthRoll={} newChance={} newRoll={}",
                    shortReefId(reef),
                    reef.depositPositions.size,
                    maximumDeposits,
                    growable.size,
                    debugDecimal(growthChance),
                    growthRoll?.let(::debugDecimal) ?: "none",
                    debugDecimal(newDepositChance),
                    debugDecimal(newDepositRoll)
                )
            }
            return
        }

        tryPlaceCalcareousDeposit(reef)?.let { placedPos ->
            reef.depositPositions.add(placedPos.asLong())
        }
    }

    private fun pruneMissingDeposits(reef: ReefState): Int {
        val previousSize = reef.depositPositions.size
        reef.depositPositions.removeIf { packedPos ->
            val pos = BlockPos.of(packedPos)
            level.isLoaded(pos) && !level.getBlockState(pos).`is`(ModBlocks.CALCAREOUS_DEPOSIT.get())
        }
        return previousSize - reef.depositPositions.size
    }

    private fun tryPlaceCalcareousDeposit(reef: ReefState): BlockPos? {
        val diagnostics = DepositPlacementDiagnostics()
        positionAttempt@ for (attempt in 0 until DEPOSIT_POSITION_ATTEMPTS) {
            val dx = level.random.nextInt(-DEPOSIT_RADIUS, DEPOSIT_RADIUS + 1)
            val dz = level.random.nextInt(-DEPOSIT_RADIUS, DEPOSIT_RADIUS + 1)
            val radiusSqr = dx * dx + dz * dz
            if (radiusSqr !in MINIMUM_DEPOSIT_RADIUS_SQR..MAXIMUM_DEPOSIT_RADIUS_SQR) {
                diagnostics.radiusRejectedAttempts++
                continue
            }

            val candidate = BlockPos.MutableBlockPos(
                reef.anchor.x + dx,
                reef.anchor.y + DEPOSIT_SEARCH_ABOVE,
                reef.anchor.z + dz
            )
            repeat(DEPOSIT_VERTICAL_SEARCH) {
                if (!level.isLoaded(candidate)) {
                    diagnostics.unloadedColumns++
                    continue@positionAttempt
                }
                diagnostics.scannedPositions++
                val candidateState = level.getBlockState(candidate)
                if (!candidateState.`is`(Blocks.WATER) || !level.getFluidState(candidate).isSource) {
                    diagnostics.rejectedNonSourceWater++
                } else {
                    val immutablePos = candidate.immutable()
                    val supportPos = immutablePos.below()
                    val support = level.getBlockState(supportPos)
                    if (
                        !support.`is`(ModTags.Blocks.CALCAREOUS_DEPOSIT_SUPPORTS) ||
                        !support.isFaceSturdy(level, supportPos, Direction.UP)
                    ) {
                        diagnostics.rejectedSupport++
                        val supportId = BuiltInRegistries.BLOCK.getKey(support.block).toString()
                        diagnostics.rejectedSupportBlocks.merge(supportId, 1, Int::plus)
                        candidate.move(Direction.DOWN)
                        return@repeat
                    }
                    if (isNearAnotherDeposit(immutablePos)) {
                        diagnostics.rejectedSpacing++
                        candidate.move(Direction.DOWN)
                        return@repeat
                    }
                    val placement = ModBlocks.CALCAREOUS_DEPOSIT.get().defaultBlockState()
                        .setValue(CalcareousDepositBlock.AGE, 0)
                        .setValue(
                            CalcareousDepositBlock.FACING,
                            HORIZONTAL_DIRECTIONS[level.random.nextInt(HORIZONTAL_DIRECTIONS.size)]
                        )
                        .setValue(CalcareousDepositBlock.WATERLOGGED, true)
                    if (!placement.canSurvive(level, immutablePos)) {
                        diagnostics.rejectedSurvival++
                    } else if (level.setBlock(immutablePos, placement, Block.UPDATE_ALL)) {
                        playCalcareousGrowthEffects(immutablePos, placement)
                        if (debugEnabled()) {
                            SquAbyssalBloom.LOGGER.info(
                                "[Red Slobberer reef debug] event=depositPlaced reef={} pos={} facing={} scanned={} chancePassed=true",
                                shortReefId(reef),
                                debugPos(immutablePos),
                                placement.getValue(CalcareousDepositBlock.FACING).serializedName,
                                diagnostics.scannedPositions
                            )
                        }
                        return immutablePos
                    } else {
                        diagnostics.setBlockFailures++
                    }
                }
                candidate.move(Direction.DOWN)
            }
        }
        if (debugEnabled()) {
            SquAbyssalBloom.LOGGER.info(
                "[Red Slobberer reef debug] event=depositPlacementFailed reef={} anchor={} attempts={} radiusRejected={} unloaded={} scanned={} nonSourceWater={} invalidSupport={} supportSamples={} tooClose={} cannotSurvive={} setBlockFailed={}",
                shortReefId(reef),
                debugPos(reef.anchor),
                DEPOSIT_POSITION_ATTEMPTS,
                diagnostics.radiusRejectedAttempts,
                diagnostics.unloadedColumns,
                diagnostics.scannedPositions,
                diagnostics.rejectedNonSourceWater,
                diagnostics.rejectedSupport,
                diagnostics.rejectedSupportBlocks.entries
                    .sortedByDescending { (_, count) -> count }
                    .take(4)
                    .joinToString(",") { (blockId, count) -> "$blockId:$count" }
                    .ifEmpty { "none" },
                diagnostics.rejectedSpacing,
                diagnostics.rejectedSurvival,
                diagnostics.setBlockFailures
            )
        }
        return null
    }

    private fun isNearAnotherDeposit(candidate: BlockPos): Boolean {
        val trackedDepositNearby = savedData.reefs.values.any { reef ->
            reef.depositPositions.any { packedPos ->
                val existing = BlockPos.of(packedPos)
                abs(existing.y - candidate.y) <= DEPOSIT_SPACING_VERTICAL_RANGE &&
                    existing.distSqr(candidate) < MINIMUM_DEPOSIT_SPACING_SQR
            }
        }
        if (trackedDepositNearby) return true

        for (dy in -DEPOSIT_SPACING_VERTICAL_RANGE..DEPOSIT_SPACING_VERTICAL_RANGE) {
            for (dx in -3..3) {
                for (dz in -3..3) {
                    if (dx * dx + dz * dz >= MINIMUM_DEPOSIT_SPACING_SQR) continue
                    if (
                        level.getBlockState(candidate.offset(dx, dy, dz))
                            .`is`(ModBlocks.CALCAREOUS_DEPOSIT.get())
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun tryPlaceReefDecoration(reef: ReefState): Boolean {
        val diagnostics = DecorationPlacementDiagnostics()
        val placeCoral = !hasNearbyLiveCoralDecoration(reef) ||
            level.random.nextFloat() >= SEA_PICKLE_CHANCE
        val coral = if (placeCoral) {
            LIVE_CORAL_DECORATIONS[level.random.nextInt(LIVE_CORAL_DECORATIONS.size)]
        } else {
            null
        }

        positionAttempt@ for (attempt in 0 until DECORATION_POSITION_ATTEMPTS) {
            val x = reef.anchor.x + level.random.nextInt(-DECORATION_RADIUS, DECORATION_RADIUS + 1)
            val z = reef.anchor.z + level.random.nextInt(-DECORATION_RADIUS, DECORATION_RADIUS + 1)
            val candidate = BlockPos.MutableBlockPos(x, reef.anchor.y + DECORATION_SEARCH_ABOVE, z)

            repeat(DECORATION_VERTICAL_SEARCH) {
                if (!level.isLoaded(candidate)) {
                    diagnostics.unloadedColumns++
                    continue@positionAttempt
                }
                diagnostics.scannedPositions++
                val supportPos = candidate.below()
                val supportState = level.getBlockState(supportPos)
                if (!level.getBlockState(candidate).`is`(Blocks.WATER)) {
                    diagnostics.rejectedNonWater++
                } else if (!supportState.isFaceSturdy(level, supportPos, Direction.UP)) {
                    diagnostics.rejectedSupport++
                } else {
                    val decoration = if (coral == null) {
                        Blocks.SEA_PICKLE.defaultBlockState().setValue(
                            SeaPickleBlock.PICKLES,
                            1 + level.random.nextInt(MAX_NEW_PICKLES)
                        )
                    } else {
                        coral.defaultBlockState()
                    }
                    val immutablePos = candidate.immutable()
                    if (!decoration.canSurvive(level, immutablePos)) {
                        diagnostics.rejectedSurvival++
                    } else if (level.setBlock(immutablePos, decoration, Block.UPDATE_ALL)) {
                        playDecorationGrowthEffects(immutablePos)
                        if (debugEnabled()) {
                            SquAbyssalBloom.LOGGER.info(
                                "[Red Slobberer reef debug] event=decorationPlaced reef={} type={} block={} pos={} scanned={}",
                                shortReefId(reef),
                                if (coral == null) "sea_pickle" else "live_coral",
                                BuiltInRegistries.BLOCK.getKey(decoration.block),
                                debugPos(immutablePos),
                                diagnostics.scannedPositions
                            )
                        }
                        return true
                    } else {
                        diagnostics.setBlockFailures++
                    }
                }
                candidate.move(Direction.DOWN)
            }
        }
        if (debugEnabled()) {
            SquAbyssalBloom.LOGGER.info(
                "[Red Slobberer reef debug] event=decorationPlacementFailed reef={} selected={} attempts={} unloaded={} scanned={} nonWater={} invalidSupport={} cannotSurvive={} setBlockFailed={}",
                shortReefId(reef),
                if (coral == null) "sea_pickle" else BuiltInRegistries.BLOCK.getKey(coral),
                DECORATION_POSITION_ATTEMPTS,
                diagnostics.unloadedColumns,
                diagnostics.scannedPositions,
                diagnostics.rejectedNonWater,
                diagnostics.rejectedSupport,
                diagnostics.rejectedSurvival,
                diagnostics.setBlockFailures
            )
        }
        return false
    }

    private fun playDecorationGrowthEffects(pos: BlockPos) {
        level.levelEvent(1505, pos, 15)
    }

    private fun playCalcareousGrowthEffects(pos: BlockPos, state: BlockState) {
        val age = state.getValue(CalcareousDepositBlock.AGE)
        level.sendParticles(
            BlockParticleOption(ParticleTypes.BLOCK, state),
            pos.x + 0.5,
            pos.y + 0.18 + age * 0.05,
            pos.z + 0.5,
            14 + age * 5,
            0.34,
            0.12 + age * 0.025,
            0.34,
            0.035
        )
        level.sendParticles(
            ParticleTypes.BUBBLE_POP,
            pos.x + 0.5,
            pos.y + 0.24,
            pos.z + 0.5,
            3 + age,
            0.24,
            0.08,
            0.24,
            0.018
        )
    }

    private fun hasNearbyLiveCoralDecoration(reef: ReefState): Boolean {
        for (dy in -DECORATION_VERTICAL_SEARCH..DECORATION_SEARCH_ABOVE) {
            for (dx in -DECORATION_RADIUS..DECORATION_RADIUS) {
                for (dz in -DECORATION_RADIUS..DECORATION_RADIUS) {
                    if (dx * dx + dz * dz > DECORATION_RADIUS * DECORATION_RADIUS) continue
                    val pos = reef.anchor.offset(dx, dy, dz)
                    if (!level.isLoaded(pos)) continue
                    val state = level.getBlockState(pos)
                    if (LIVE_CORAL_DECORATIONS.any { coral -> state.`is`(coral) }) return true
                }
            }
        }
        return false
    }

    private fun ensureSchedules(reef: ReefState, gameTime: Long) {
        var changed = false
        if (reef.nextEcologyGameTime <= 0L) {
            reef.nextEcologyGameTime = gameTime + stagger(reef.id, ECOLOGY_INTERVAL_TICKS)
            changed = true
        }
        val (_, maximumDepositInterval) = configuredDepositIntervals()
        if (
            reef.nextDepositGameTime <= 0L ||
            reef.nextDepositGameTime - gameTime > maximumDepositInterval
        ) {
            reef.nextDepositGameTime = gameTime + staggeredDepositDelay(reef.id)
            changed = true
        }
        if (reef.nextDecorationGameTime <= 0L) {
            reef.nextDecorationGameTime = gameTime + DECORATION_INTERVAL_TICKS +
                stagger(reef.id, DECORATION_INTERVAL_TICKS)
            changed = true
        }
        if (changed) savedData.setDirty()
    }

    private fun nextDepositDelay(): Long {
        val (minimum, maximum) = configuredDepositIntervals()
        val spread = (maximum - minimum + 1L).toInt()
        return minimum + level.random.nextInt(spread).toLong()
    }

    private fun staggeredDepositDelay(id: UUID): Long {
        val (minimum, maximum) = configuredDepositIntervals()
        return minimum + stagger(id, maximum - minimum + 1L)
    }

    private fun configuredDepositIntervals(): Pair<Long, Long> {
        val minimum = ModServerConfig.RED_SLOBBERER_DEPOSIT_MIN_INTERVAL_TICKS.get()
            .coerceAtLeast(20)
            .toLong()
        val maximum = ModServerConfig.RED_SLOBBERER_DEPOSIT_MAX_INTERVAL_TICKS.get()
            .coerceAtLeast(minimum.toInt())
            .toLong()
        return minimum to maximum
    }

    private fun configuredMinimumStability(): Double =
        ModServerConfig.RED_SLOBBERER_MINIMUM_REEF_STABILITY.get().coerceIn(0.0, 1.0)

    private fun configuredRefugeRadius(): Double =
        ModServerConfig.RED_SLOBBERER_FISH_REFUGE_RADIUS.get().coerceAtLeast(1.0)

    private fun debugEnabled(): Boolean = ModServerConfig.RED_SLOBBERER_REEF_DEBUG.get()

    private fun depositBlockers(
        reef: ReefState,
        maturityRequirement: Int,
        gameTime: Long
    ): List<String> {
        val blockers = mutableListOf<String>()
        val stage = reef.stage(maturityRequirement, gameTime)
        if (reef.members.size < MINIMUM_GROUP_SIZE) blockers.add("group_size<2")
        if (stage != ReefStage.MATURE) blockers.add("stage=${stage.name.lowercase()}")
        if (reef.stability < configuredMinimumStability()) blockers.add("low_stability")
        if (!level.gameRules.get(GameRules.MOB_GRIEFING)) blockers.add("mob_griefing=false")
        if (ModServerConfig.RED_SLOBBERER_MAX_CALCAREOUS_DEPOSITS.get() <= 0) {
            blockers.add("max_deposits=0")
        }
        return blockers
    }

    private fun tickDebug(gameTime: Long) {
        if (!debugEnabled()) {
            if (debugWasEnabled) {
                SquAbyssalBloom.LOGGER.info(
                    "[Red Slobberer reef debug] Disabled for {}",
                    level.dimension()
                )
            }
            debugWasEnabled = false
            return
        }

        if (!debugWasEnabled) {
            debugWasEnabled = true
            nextDebugSummaryGameTime = gameTime
            SquAbyssalBloom.LOGGER.info(
                "[Red Slobberer reef debug] Enabled for {}. end_rod=reef anchor or route to an exterior nest entrance, wax_on=tracked deposit, nautilus=fish approaching a refuge; copy all lines containing '[Red Slobberer reef debug]'.",
                level.dimension()
            )
        }

        if (Math.floorMod(gameTime, DEBUG_PARTICLE_INTERVAL_TICKS) == 0L) {
            renderDebugParticles(gameTime)
        }
        if (gameTime < nextDebugSummaryGameTime) return
        nextDebugSummaryGameTime = gameTime + DEBUG_SUMMARY_INTERVAL_TICKS

        val maturityRequirement = ModServerConfig.RED_SLOBBERER_REEF_MATURITY_TICKS.get()
        val activeRedSlobberers = observations.values.count { observation ->
            observation.entity.isAlive && observation.entity.isUnderWater
        }
        val activeReefs = savedData.reefs.values.count { reef ->
            reef.members.size >= MINIMUM_GROUP_SIZE
        }
        val (minimumDepositInterval, maximumDepositInterval) = configuredDepositIntervals()
        SquAbyssalBloom.LOGGER.info(
            "[Red Slobberer reef debug] level={} observedRed={} underwaterRed={} storedReefs={} activeReefs={} maturityRequired={} minimumStability={} depositInterval={}..{} mobGriefing={} spawnMobs={} fishSchoolDebug={}",
            level.dimension(),
            observations.size,
            activeRedSlobberers,
            savedData.reefs.size,
            activeReefs,
            maturityRequirement,
            debugDecimal(configuredMinimumStability()),
            minimumDepositInterval,
            maximumDepositInterval,
            level.gameRules.get(GameRules.MOB_GRIEFING),
            level.gameRules.get(GameRules.SPAWN_MOBS),
            ModServerConfig.FISH_SCHOOL_DEBUG.get()
        )

        savedData.reefs.values.asSequence()
            .sortedWith(
                compareByDescending<ReefState> { reef -> reef.members.size }
                    .thenByDescending { reef -> reef.maturityTicks }
                    .thenBy { reef -> reef.id }
            )
            .take(16)
            .forEach { reef ->
                val blockers = depositBlockers(reef, maturityRequirement, gameTime)
                SquAbyssalBloom.LOGGER.info(
                    "[Red Slobberer reef debug] reef={} stage={} anchor={} center={} members={} maturity={}/{} activity={} stability={} deposits={} decorations={} nextDepositIn={} blockers={}",
                    shortReefId(reef),
                    reef.stage(maturityRequirement, gameTime).name.lowercase(),
                    debugPos(reef.anchor),
                    debugVec(reef.center),
                    reef.members.size,
                    reef.maturityTicks,
                    maturityRequirement,
                    debugDecimal(reef.collectiveActivity),
                    debugDecimal(reef.stability),
                    reef.depositPositions.size,
                    reef.placedDecorations,
                    (reef.nextDepositGameTime - gameTime).coerceAtLeast(0L),
                    if (blockers.isEmpty()) "none" else blockers.joinToString("+")
                )
            }

        val activeRefugeFish = activeRefugeByFish.count { (_, activeRefuge) ->
            gameTime - activeRefuge.lastInfluenceGameTime <= DEBUG_REFUGE_ACTIVE_WINDOW_TICKS
        }
        val storedRefugeFish = observations.values.sumOf { observation ->
            observation.entity.shelteredFishCount
        }
        SquAbyssalBloom.LOGGER.info(
            "[Red Slobberer reef debug] refuge scans={} withoutNearbyRed={} noReefCandidate={} inactiveReefCandidate={} outsideReefCandidate={} unavailableCandidate={} sameReefMatches={} approachingFish={} storedFish={} refugeApplications={} nearNestEntrance={}",
            refugeScans,
            refugeScansWithoutNearbyRed,
            refugeCandidateRejectionsNoReef,
            refugeCandidateRejectionsInactiveReef,
            refugeCandidateRejectionsOutsideReef,
            refugeCandidateRejectionsUnavailable,
            refugeMatches,
            activeRefugeFish,
            storedRefugeFish,
            refugeThreatApplications,
            refugeNearNestEntranceApplications
        )
        refugeScans = 0
        refugeScansWithoutNearbyRed = 0
        refugeCandidateRejectionsNoReef = 0
        refugeCandidateRejectionsInactiveReef = 0
        refugeCandidateRejectionsOutsideReef = 0
        refugeCandidateRejectionsUnavailable = 0
        refugeMatches = 0
        refugeThreatApplications = 0
        refugeNearNestEntranceApplications = 0
    }

    private fun renderDebugParticles(gameTime: Long) {
        val maturityRequirement = ModServerConfig.RED_SLOBBERER_REEF_MATURITY_TICKS.get()
        for (reef in savedData.reefs.values) {
            if (reef.members.size >= MINIMUM_GROUP_SIZE) {
                val anchor = Vec3.atCenterOf(reef.anchor)
                level.sendParticles(
                    ParticleTypes.END_ROD,
                    anchor.x,
                    anchor.y,
                    anchor.z,
                    3,
                    0.2,
                    0.35,
                    0.2,
                    0.0
                )
                level.sendParticles(
                    when (reef.stage(maturityRequirement, gameTime)) {
                        ReefStage.MATURE -> ParticleTypes.HAPPY_VILLAGER
                        ReefStage.ESTABLISHED -> ParticleTypes.WAX_ON
                        ReefStage.COLONIZING -> ParticleTypes.BUBBLE_POP
                        ReefStage.DECLINING -> ParticleTypes.SMOKE
                    },
                    anchor.x,
                    anchor.y + 0.75,
                    anchor.z,
                    1,
                    0.1,
                    0.1,
                    0.1,
                    0.0
                )
            }
            for (packedPos in reef.depositPositions) {
                val pos = BlockPos.of(packedPos)
                if (!level.isLoaded(pos)) continue
                level.sendParticles(
                    ParticleTypes.WAX_ON,
                    pos.x + 0.5,
                    pos.y + 0.35,
                    pos.z + 0.5,
                    1,
                    0.1,
                    0.1,
                    0.1,
                    0.0
                )
            }
        }
    }

    private fun shortReefId(reef: ReefState): String = reef.id.toString().take(8)

    private fun debugDecimal(value: Double): Double =
        (value * 1000.0).roundToLong() / 1000.0

    private fun debugPos(pos: BlockPos): String = "${pos.x},${pos.y},${pos.z}"

    private fun debugVec(position: Vec3): String =
        "${debugDecimal(position.x)},${debugDecimal(position.y)},${debugDecimal(position.z)}"

    private fun calculateCenter(members: List<RedSlobbererEntity>): Vec3 {
        val inverseCount = 1.0 / members.size.toDouble()
        return Vec3(
            members.sumOf { member -> member.x } * inverseCount,
            members.sumOf { member -> member.y } * inverseCount,
            members.sumOf { member -> member.z } * inverseCount
        )
    }

    private fun horizontalDistanceSqr(anchor: BlockPos, position: Vec3): Double {
        val dx = anchor.x + 0.5 - position.x
        val dz = anchor.z + 0.5 - position.z
        return dx * dx + dz * dz
    }

    private fun stagger(id: UUID, range: Long): Long {
        if (range <= 1L) return 0L
        val mixed = id.mostSignificantBits xor id.leastSignificantBits
        return (mixed and Long.MAX_VALUE) % range
    }

}
