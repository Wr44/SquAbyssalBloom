package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluenceRegistry
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.max
import kotlin.math.sqrt

class FishCollectiveManager private constructor(
    private val level: ServerLevel
) {
    private val statesByFish = HashMap<UUID, FishCollectiveState>()
    private val snapshotsByCell = HashMap<Long, FishLocalEntitySnapshot>()
    private var cachedSettings = FishCollectiveSettings.fromConfig()
    private var cachedSettingsTick = Long.MIN_VALUE
    private var nextCleanupTick = 0L
    private var nextDebugSummaryTick = 0L
    private var debugWasEnabled = false

    companion object {
        private val MANAGERS_BY_LEVEL = HashMap<ServerLevel, FishCollectiveManager>()

        private const val SNAPSHOT_CELL_SIZE = 8
        private const val SNAPSHOT_CELL_HALF_SIZE = SNAPSHOT_CELL_SIZE * 0.5
        private const val BLOCKER_SEARCH_RADIUS = 7.0
        private const val BLOCKER_SEARCH_RADIUS_SQR = BLOCKER_SEARCH_RADIUS * BLOCKER_SEARCH_RADIUS
        private const val LARGE_ENTITY_MINIMUM_WIDTH = 1.0f
        private const val MAXIMUM_NEARBY_LARGE_ENTITIES = 8
        private const val AGGREGATION_LOCAL_DENSITY_LIMIT = 100
        private const val MINIMUM_DIRECT_THREAT_INTENSITY = 0.35
        private const val MINIMUM_PROPAGATED_THREAT_INTENSITY = 0.01
        private const val MINIMUM_PANIC_COLLISION_SIGNAL = 0.03
        private const val PANIC_COLLISION_SEARCH_RADIUS = 1.5
        private const val FISH_THREAT_PREDICTION_TICKS = 2.0
        private const val THREAT_PREDICTION_TICKS = 5.0
        private const val MINIMUM_ESCAPE_VECTOR_LENGTH = 1.0E-4
        private const val MINIMUM_MOVING_THREAT_SPEED = 0.025
        private const val MOVING_THREAT_LATERAL_SPLIT_WEIGHT = 0.65
        private const val CLEANUP_INTERVAL_TICKS = 200L
        private const val STATE_RETENTION_TICKS = 400L
        private const val SETTINGS_REFRESH_INTERVAL_TICKS = 20L
        private const val DEBUG_SUMMARY_INTERVAL_TICKS = 100L

        fun forLevel(level: ServerLevel): FishCollectiveManager {
            return MANAGERS_BY_LEVEL.getOrPut(level) { FishCollectiveManager(level) }
        }

        fun releaseLevel(level: ServerLevel) {
            MANAGERS_BY_LEVEL.remove(level)
        }
    }

    fun shouldUseCollectiveMovement(fish: AbstractFish): Boolean {
        return updateState(fish, calculateSteering = false)?.isUnderCustomControl == true
    }

    fun desiredVelocity(fish: AbstractFish): Vec3? {
        val state = updateState(fish, calculateSteering = true) ?: return null
        return state.desiredVelocity.takeIf { state.isUnderCustomControl }
    }

    fun avoidPanicFishCollisions(
        fish: AbstractFish,
        proposedVelocity: Vec3
    ): Vec3 {
        val state = statesByFish[fish.uuid] ?: return proposedVelocity
        if (state.threatIntensity <= MINIMUM_PANIC_COLLISION_SIGNAL) return proposedVelocity

        val nearbyFish = level.getEntitiesOfClass(
            AbstractFish::class.java,
            fish.boundingBox.inflate(PANIC_COLLISION_SEARCH_RADIUS)
        ) { candidate ->
            candidate !== fish &&
                candidate.isAlive &&
                candidate.isInWater &&
                !candidate.isRemoved &&
                !candidate.isPassenger
        }
        return FishSchoolSteering.avoidPanicFishCollisions(
            fish,
            proposedVelocity,
            nearbyFish
        )
    }

    fun setMovementControllerRunning(
        fish: AbstractFish,
        running: Boolean
    ) {
        statesByFish[fish.uuid]?.movementControllerRunning = running
    }


    fun forget(fish: AbstractFish) {
        statesByFish.remove(fish.uuid)
    }

    fun debugRecordNavigationSuppression(fish: AbstractFish) {
        if (!currentSettings().debugEnabled) return
        val state = statesByFish[fish.uuid] ?: return
        FishCollectiveDebugger.recordNavigationSuppression(state, level.gameTime)
    }

    fun debugRecordVelocityApplication(
        fish: AbstractFish,
        currentVelocity: Vec3,
        turnLimitedVelocity: Vec3,
        writtenVelocity: Vec3
    ) {
        if (!currentSettings().debugEnabled) return
        val state = statesByFish[fish.uuid] ?: return
        FishCollectiveDebugger.recordVelocityApplication(
            state,
            currentVelocity,
            turnLimitedVelocity,
            writtenVelocity
        )
    }

    fun observe(fish: AbstractFish) {
        val state = updateState(fish, calculateSteering = false) ?: return
        val settings = currentSettings()
        if (!settings.debugEnabled) {
            if (debugWasEnabled) FishCollectiveDebugger.clearMovementTracks()
            debugWasEnabled = false
            return
        }

        val gameTime = level.gameTime
        if (!debugWasEnabled) {
            debugWasEnabled = true
            nextDebugSummaryTick = gameTime
            FishCollectiveDebugger.logEnabled(level)
        }

        FishCollectiveDebugger.recordMovementSample(state, gameTime)
        FishCollectiveDebugger.renderFish(level, state, gameTime)
        if (gameTime >= nextDebugSummaryTick) {
            nextDebugSummaryTick = gameTime + DEBUG_SUMMARY_INTERVAL_TICKS
            FishCollectiveDebugger.logSummary(level, statesByFish.values, gameTime)
        }
    }

    fun currentSettings(): FishCollectiveSettings {
        return settingsForTick(level.gameTime)
    }

    private fun updateState(
        fish: AbstractFish,
        calculateSteering: Boolean
    ): FishCollectiveState? {
        if (
            fish.level() !== level ||
            !fish.isAlive ||
            !fish.isInWater
        ) {
            return null
        }

        val gameTime = level.gameTime
        val settings = settingsForTick(gameTime)
        cleanupIfNeeded(gameTime)

        val state = statesByFish.getOrPut(fish.uuid) { FishCollectiveState(fish) }
        state.observe(gameTime, settings.neighborRefreshInterval)
        state.decayThreatSignal(gameTime, settings.threatSignalDecay)
        state.refreshNoiseIfNeeded(gameTime)
        state.advanceSwimWiggle(gameTime)

        if (state.needsNeighborhoodRefresh(gameTime)) {
            refreshLocalState(state, settings, gameTime)
        }

        if (
            calculateSteering &&
            state.isUnderCustomControl &&
            state.lastSteeringTick != gameTime
        ) {
            state.desiredVelocity = FishSchoolSteering.calculateDesiredVelocity(
                level,
                state,
                settings
            )
            state.lastSteeringTick = gameTime
        }
        return state
    }

    private fun refreshLocalState(
        state: FishCollectiveState,
        settings: FishCollectiveSettings,
        gameTime: Long
    ) {
        val fish = state.fish
        val searchRadius = max(
            max(
                max(settings.neighborSearchRadius, settings.aggregationRadius),
                settings.threatDetectionRadius
            ),
            max(BLOCKER_SEARCH_RADIUS, FishSchoolInfluenceRegistry.maximumSearchRadius())
        )
        val snapshot = localSnapshot(fish, searchRadius, settings, gameTime)

        val localNeighborCount = populateTopologicalNeighbors(state, snapshot.entities, settings)
        state.completeNeighborhoodRefresh(gameTime, localNeighborCount, settings)
        populateAggregationNeighbor(state, snapshot.entities, settings)
        updateThreatSignal(state, snapshot.entities, settings, gameTime)

        state.influences.clear()
        FishSchoolInfluenceRegistry.collect(fish, snapshot.entities, state.influences)
        populateLargeEntities(state, snapshot.entities)
    }

    private fun populateTopologicalNeighbors(
        state: FishCollectiveState,
        nearbyEntities: List<LivingEntity>,
        settings: FishCollectiveSettings
    ): Int {
        val fish = state.fish
        state.neighbors.clear()
        var localNeighborCount = 0

        for (entity in nearbyEntities) {
            val candidate = entity as? AbstractFish ?: continue
            if (
                candidate === fish ||
                !candidate.isAlive ||
                !candidate.isInWater
            ) {
                continue
            }

            val distanceSqr = fish.distanceToSqr(candidate)
            if (distanceSqr > settings.neighborSearchRadiusSqr) continue
            localNeighborCount++
            insertRelevantFish(
                state.neighbors,
                candidate,
                distanceSqr / FishSchoolCompatibility.socialAffinity(fish, candidate, settings),
                fish,
                settings
            )
        }
        return localNeighborCount
    }

    private fun populateAggregationNeighbor(
        state: FishCollectiveState,
        nearbyEntities: List<LivingEntity>,
        settings: FishCollectiveSettings
    ) {
        state.aggregationNeighbor = null
        if (
            state.localCompatibleFishCount >= AGGREGATION_LOCAL_DENSITY_LIMIT ||
            settings.aggregationRadius <= settings.neighborSearchRadius
        ) {
            return
        }

        val fish = state.fish
        var bestCandidate: AbstractFish? = null
        var bestRelevanceDistanceSqr = Double.MAX_VALUE
        for (entity in nearbyEntities) {
            val candidate = entity as? AbstractFish ?: continue
            if (
                candidate === fish ||
                !candidate.isAlive ||
                !candidate.isInWater ||
                statesByFish[candidate.uuid]?.collectiveActive != true
            ) {
                continue
            }

            val distanceSqr = fish.distanceToSqr(candidate)
            if (
                distanceSqr <= settings.neighborSearchRadiusSqr ||
                distanceSqr > settings.aggregationRadiusSqr
            ) {
                continue
            }

            val relevanceDistanceSqr = distanceSqr /
                FishSchoolCompatibility.socialAffinity(fish, candidate, settings)
            if (relevanceDistanceSqr < bestRelevanceDistanceSqr) {
                bestRelevanceDistanceSqr = relevanceDistanceSqr
                bestCandidate = candidate
            }
        }
        state.aggregationNeighbor = bestCandidate
    }

    private fun updateThreatSignal(
        state: FishCollectiveState,
        nearbyEntities: List<LivingEntity>,
        settings: FishCollectiveSettings,
        gameTime: Long
    ) {
        val fish = state.fish
        state.beginThreatRefresh()
        val threatRadiusSqr = settings.threatDetectionRadius * settings.threatDetectionRadius
        var nearestThreat: LivingEntity? = null
        var nearestThreatDistanceSqr = threatRadiusSqr

        for (candidate in nearbyEntities) {
            val distanceSqr = fish.distanceToSqr(candidate)
            if (distanceSqr > nearestThreatDistanceSqr) continue
            if (!FishThreatClassifier.isThreat(fish, candidate)) continue
            nearestThreat = candidate
            nearestThreatDistanceSqr = distanceSqr
        }

        if (nearestThreat != null) {
            val distance = sqrt(nearestThreatDistanceSqr)
            val edgeProximity = (1.0 - distance / settings.threatDetectionRadius)
                .coerceIn(0.0, 1.0)
            val intensity = if (edgeProximity < MINIMUM_DIRECT_THREAT_INTENSITY) {
                MINIMUM_DIRECT_THREAT_INTENSITY * ModUtilities.smoothstep(
                    (edgeProximity / MINIMUM_DIRECT_THREAT_INTENSITY).coerceIn(0.0, 1.0)
                )
            } else {
                edgeProximity
            }
            state.receiveDirectThreat(
                predictedThreatEscapeDirection(fish, nearestThreat, state),
                intensity
            )
            return
        }

        var propagatedX = 0.0
        var propagatedZ = 0.0
        var strongestPropagatedIntensity = 0.0
        for (neighbor in state.neighbors) {
            val neighborState = statesByFish[neighbor.uuid] ?: continue
            neighborState.decayThreatSignal(gameTime, settings.threatSignalDecay)
            val propagatedIntensity =
                neighborState.threatIntensity *
                    settings.threatPropagationSpeed *
                    FishSchoolCompatibility.socialAffinity(fish, neighbor, settings)
            if (propagatedIntensity <= MINIMUM_PROPAGATED_THREAT_INTENSITY) continue

            propagatedX += neighborState.threatDirection.x * propagatedIntensity
            propagatedZ += neighborState.threatDirection.z * propagatedIntensity
            if (propagatedIntensity > strongestPropagatedIntensity) {
                strongestPropagatedIntensity = propagatedIntensity
            }
        }

        if (strongestPropagatedIntensity > 0.0) {
            state.receivePropagatedThreat(
                Vec3(propagatedX, 0.0, propagatedZ),
                strongestPropagatedIntensity
            )
        }
    }

    private fun predictedThreatEscapeDirection(
        fish: AbstractFish,
        threat: LivingEntity,
        state: FishCollectiveState
    ): Vec3 {
        var awayX = fish.x + fish.deltaMovement.x * FISH_THREAT_PREDICTION_TICKS -
            (threat.x + threat.deltaMovement.x * THREAT_PREDICTION_TICKS)
        var awayZ = fish.z + fish.deltaMovement.z * FISH_THREAT_PREDICTION_TICKS -
            (threat.z + threat.deltaMovement.z * THREAT_PREDICTION_TICKS)
        val awayLength = sqrt(awayX * awayX + awayZ * awayZ)
        if (awayLength > MINIMUM_ESCAPE_VECTOR_LENGTH) {
            awayX /= awayLength
            awayZ /= awayLength
        }

        val threatVelocityX = threat.deltaMovement.x
        val threatVelocityZ = threat.deltaMovement.z
        val threatSpeed = sqrt(
            threatVelocityX * threatVelocityX + threatVelocityZ * threatVelocityZ
        )
        if (threatSpeed <= MINIMUM_MOVING_THREAT_SPEED) {
            return Vec3(awayX, 0.0, awayZ)
        }

        val leftX = -threatVelocityZ / threatSpeed
        val leftZ = threatVelocityX / threatSpeed
        val sideCrossProduct = threatVelocityX * (fish.z - threat.z) -
            threatVelocityZ * (fish.x - threat.x)
        val side = when {
            sideCrossProduct > MINIMUM_ESCAPE_VECTOR_LENGTH -> 1.0
            sideCrossProduct < -MINIMUM_ESCAPE_VECTOR_LENGTH -> -1.0
            state.movementPhase < Math.PI -> 1.0
            else -> -1.0
        }
        return Vec3(
            awayX + leftX * side * MOVING_THREAT_LATERAL_SPLIT_WEIGHT,
            0.0,
            awayZ + leftZ * side * MOVING_THREAT_LATERAL_SPLIT_WEIGHT
        )
    }

    private fun populateLargeEntities(
        state: FishCollectiveState,
        nearbyEntities: List<LivingEntity>
    ) {
        val fish = state.fish
        state.nearbyLargeEntities.clear()
        for (candidate in nearbyEntities) {
            if (
                candidate === fish ||
                candidate is AbstractFish ||
                candidate.bbWidth < LARGE_ENTITY_MINIMUM_WIDTH
            ) {
                continue
            }

            if (state.influences.any { it.source === candidate }) continue

            val distanceSqr = fish.distanceToSqr(candidate)
            if (distanceSqr > BLOCKER_SEARCH_RADIUS_SQR) continue
            insertNearestEntity(
                state.nearbyLargeEntities,
                candidate,
                distanceSqr,
                fish,
                MAXIMUM_NEARBY_LARGE_ENTITIES
            )
        }
    }

    private fun localSnapshot(
        fish: AbstractFish,
        searchRadius: Double,
        settings: FishCollectiveSettings,
        gameTime: Long
    ): FishLocalEntitySnapshot {
        val cellX = Math.floorDiv(fish.blockX, SNAPSHOT_CELL_SIZE)
        val cellY = Math.floorDiv(fish.blockY, SNAPSHOT_CELL_SIZE)
        val cellZ = Math.floorDiv(fish.blockZ, SNAPSHOT_CELL_SIZE)
        val cellKey = BlockPos.asLong(cellX, cellY, cellZ)
        val cached = snapshotsByCell[cellKey]
        if (
            cached != null &&
            cached.expiresAtTick > gameTime &&
            cached.searchRadius >= searchRadius
        ) {
            return cached
        }

        val centerX = cellX * SNAPSHOT_CELL_SIZE + SNAPSHOT_CELL_HALF_SIZE
        val centerY = cellY * SNAPSHOT_CELL_SIZE + SNAPSHOT_CELL_HALF_SIZE
        val centerZ = cellZ * SNAPSHOT_CELL_SIZE + SNAPSHOT_CELL_HALF_SIZE
        val halfExtent = SNAPSHOT_CELL_HALF_SIZE + searchRadius
        val searchBox = AABB(
            centerX - halfExtent,
            centerY - halfExtent,
            centerZ - halfExtent,
            centerX + halfExtent,
            centerY + halfExtent,
            centerZ + halfExtent
        )
        val entities = level.getEntitiesOfClass(
            LivingEntity::class.java,
            searchBox
        ) { it.isAlive }
        return FishLocalEntitySnapshot(
            expiresAtTick = gameTime + settings.neighborRefreshInterval.coerceAtLeast(1),
            searchRadius = searchRadius,
            entities = entities
        ).also { snapshotsByCell[cellKey] = it }
    }

    private fun insertRelevantFish(
        output: MutableList<AbstractFish>,
        candidate: AbstractFish,
        candidateRelevanceDistanceSqr: Double,
        observer: AbstractFish,
        settings: FishCollectiveSettings
    ) {
        var insertionIndex = 0
        while (
            insertionIndex < output.size &&
            observer.distanceToSqr(output[insertionIndex]) /
                FishSchoolCompatibility.socialAffinity(
                    observer,
                    output[insertionIndex],
                    settings
                ) <= candidateRelevanceDistanceSqr
        ) {
            insertionIndex++
        }
        if (insertionIndex >= settings.neighborCount) return

        output.add(insertionIndex, candidate)
        if (output.size > settings.neighborCount) output.removeAt(output.lastIndex)
    }

    private fun insertNearestEntity(
        output: MutableList<LivingEntity>,
        candidate: LivingEntity,
        candidateDistanceSqr: Double,
        observer: AbstractFish,
        maximumCount: Int
    ) {
        var insertionIndex = 0
        while (
            insertionIndex < output.size &&
            observer.distanceToSqr(output[insertionIndex]) <= candidateDistanceSqr
        ) {
            insertionIndex++
        }
        if (insertionIndex >= maximumCount) return

        output.add(insertionIndex, candidate)
        if (output.size > maximumCount) output.removeAt(output.lastIndex)
    }

    private fun settingsForTick(gameTime: Long): FishCollectiveSettings {
        if (
            cachedSettingsTick == Long.MIN_VALUE ||
            gameTime < cachedSettingsTick ||
            gameTime - cachedSettingsTick >= SETTINGS_REFRESH_INTERVAL_TICKS
        ) {
            cachedSettings = FishCollectiveSettings.fromConfig()
            cachedSettingsTick = gameTime
        }
        return cachedSettings
    }

    private fun cleanupIfNeeded(gameTime: Long) {
        if (gameTime < nextCleanupTick) return
        nextCleanupTick = gameTime + CLEANUP_INTERVAL_TICKS

        statesByFish.entries.removeIf { (_, state) ->
            state.fish.isRemoved ||
                !state.fish.isAlive ||
                state.fish.level() !== level ||
                gameTime - state.lastObservedTick > STATE_RETENTION_TICKS
        }
        snapshotsByCell.entries.removeIf { (_, snapshot) ->
            snapshot.expiresAtTick <= gameTime
        }
    }
}
