package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
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
import kotlin.math.sin
import kotlin.math.sqrt

class FishCollectiveManager private constructor(
    private val level: ServerLevel
) {
    private val statesByFish = HashMap<UUID, FishCollectiveState>()
    private val neighborhoodSnapshotsByCell = HashMap<Long, FishLocalEntitySnapshot>()
    private val threatSnapshotsByCell = HashMap<Long, FishLocalEntitySnapshot>()
    private val longRangeSnapshotsByCell = HashMap<Long, FishLocalEntitySnapshot>()
    private var cachedSettings = FishCollectiveSettings.fromConfig()
    private var cachedSettingsTick = Long.MIN_VALUE
    private var lastThreatSimulationTick = Long.MIN_VALUE
    private var nextCleanupTick = 0L
    private var nextDebugSummaryTick = 0L
    private var debugWasEnabled = false

    companion object {
        private val MANAGERS_BY_LEVEL = HashMap<ServerLevel, FishCollectiveManager>()

        private const val SNAPSHOT_CELL_SIZE = 8
        private const val SNAPSHOT_CELL_HALF_SIZE = SNAPSHOT_CELL_SIZE * 0.5
        private const val BLOCKER_SEARCH_RADIUS = 7.0
        private const val BLOCKER_SEARCH_RADIUS_SQR = BLOCKER_SEARCH_RADIUS * BLOCKER_SEARCH_RADIUS
        private const val LARGE_ENTITY_MINIMUM_SPAN = 1.0f
        private const val MAXIMUM_NEARBY_LARGE_ENTITIES = 8
        private const val MAXIMUM_NEARBY_COLLISION_FISH = 16
        private const val AGGREGATION_LOCAL_DENSITY_LIMIT = 100
        private const val MINIMUM_DIRECT_THREAT_INTENSITY = 0.35
        private const val MINIMUM_PROPAGATED_THREAT_INTENSITY = 0.01
        private const val MINIMUM_PANIC_COLLISION_SIGNAL = 0.03
        private const val PANIC_COLLISION_SEARCH_RADIUS = 1.5
        private const val PANIC_COLLISION_RELATIVE_SPEED_FACTOR = 2.0
        private const val FISH_THREAT_PREDICTION_TICKS = 2.0
        private const val THREAT_PREDICTION_TICKS = 5.0
        private const val MINIMUM_VECTOR_LENGTH_SQR = 1.0E-8
        private const val MINIMUM_MOVING_THREAT_SPEED = 0.025
        private const val MOVING_THREAT_LATERAL_SPLIT_WEIGHT = 0.65
        private const val AVOIDANCE_COORDINATION_DISTANCE_BIAS = 0.5
        private const val CURRENT_STATE_MAXIMUM_AGE_TICKS = 2L
        private const val CLEANUP_INTERVAL_TICKS = 200L
        private const val STATE_RETENTION_TICKS = 400L
        private const val SETTINGS_REFRESH_INTERVAL_TICKS = 20L
        private const val DEBUG_SUMMARY_INTERVAL_TICKS = 100L

        private val UP_DIRECTION = Vec3(0.0, 1.0, 0.0)
        private val X_DIRECTION = Vec3(1.0, 0.0, 0.0)

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

        return FishSchoolSteering.avoidPanicFishCollisions(
            fish,
            proposedVelocity,
            state.nearbyCollisionFish
        )
    }

    fun setMovementControllerRunning(
        fish: AbstractFish,
        running: Boolean
    ) {
        val state = statesByFish[fish.uuid]
        if (state == null) {
            if (!running) fish.xRot = 0.0f
            return
        }

        state.movementControllerRunning = running
        if (!running) state.resetBodyOrientation()
    }

    fun updateBodyOrientation(fish: AbstractFish) {
        val state = statesByFish[fish.uuid] ?: return
        if (!state.movementControllerRunning) return
        val settings = currentSettings()
        state.updateBodyOrientation(
            settings.maximumTurnRate.toFloat(),
            settings.debugEnabled
        )
    }

    fun forget(fish: AbstractFish) {
        val state = statesByFish.remove(fish.uuid)
        if (state == null) {
            fish.xRot = 0.0f
        } else {
            state.resetBodyOrientation()
        }
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
            !ModServerConfig.FISH_SCHOOL_ENABLED.get() ||
            fish.level() !== level ||
            !fish.isAlive ||
            !fish.isInWater
        ) {
            forget(fish)
            return null
        }

        val gameTime = level.gameTime
        val settings = settingsForTick(gameTime)
        cleanupIfNeeded(gameTime)

        val state = statesByFish.getOrPut(fish.uuid) { FishCollectiveState(fish) }
        state.observe(
            gameTime,
            settings.neighborRefreshInterval,
            settings.threatRefreshInterval,
            settings.longRangeRefreshInterval
        )
        state.refreshNoiseIfNeeded(gameTime)
        state.advanceSwimWiggle(gameTime)

        if (lastThreatSimulationTick != gameTime) {
            advanceThreatSimulation(gameTime, settings)
        }
        if (state.needsNeighborhoodRefresh(gameTime)) {
            refreshNeighborhoodState(state, settings, gameTime)
        }
        if (state.needsLongRangeRefresh(gameTime)) {
            refreshLongRangeState(state, settings, gameTime)
        }
        validateAggregationNeighbor(state, settings)
        updateAvoidanceCoordination(state, settings, gameTime)

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

    private fun refreshNeighborhoodState(
        state: FishCollectiveState,
        settings: FishCollectiveSettings,
        gameTime: Long
    ) {
        val searchRadius = max(settings.neighborSearchRadius, BLOCKER_SEARCH_RADIUS)
        val snapshot = entitySnapshot(
            neighborhoodSnapshotsByCell,
            state.fish,
            searchRadius,
            settings.neighborRefreshInterval,
            gameTime
        )

        val localNeighborCount = populateTopologicalNeighbors(
            state,
            snapshot.entities,
            settings
        )
        state.completeNeighborhoodRefresh(gameTime, localNeighborCount, settings)
        validateAggregationNeighbor(state, settings)
        populateCollisionFish(state, snapshot.entities, settings)
        populateLargeEntities(state, snapshot.entities)
    }

    private fun refreshLongRangeState(
        state: FishCollectiveState,
        settings: FishCollectiveSettings,
        gameTime: Long
    ) {
        val searchRadius = max(
            settings.aggregationRadius,
            FishSchoolInfluenceRegistry.maximumSearchRadius()
        )
        val snapshot = entitySnapshot(
            longRangeSnapshotsByCell,
            state.fish,
            searchRadius,
            settings.longRangeRefreshInterval,
            gameTime
        )

        populateAggregationNeighbor(state, snapshot.entities, settings)
        state.influences.clear()
        FishSchoolInfluenceRegistry.collect(state.fish, snapshot.entities, state.influences)
        state.completeLongRangeRefresh(gameTime, settings.longRangeRefreshInterval)
    }

    private fun advanceThreatSimulation(
        gameTime: Long,
        settings: FishCollectiveSettings
    ) {
        lastThreatSimulationTick = gameTime
        val currentStates = statesByFish.values.filter { state ->
            val fish = state.fish
            fish.isAlive &&
                fish.isInWater &&
                !fish.isRemoved &&
                fish.level() === level &&
                gameTime - state.lastObservedTick <= CURRENT_STATE_MAXIMUM_AGE_TICKS
        }
        if (currentStates.isEmpty()) return

        for (state in currentStates) {
            if (!state.needsThreatRefresh(gameTime)) continue
            val observation = detectDirectThreat(state, settings, gameTime)
            state.completeThreatRefresh(
                gameTime,
                observation?.direction ?: Vec3.ZERO,
                observation?.intensity ?: 0.0,
                settings.threatRefreshInterval
            )
        }

        val previousSignals = HashMap<UUID, ThreatSignalSnapshot>(currentStates.size)
        for (state in currentStates) {
            previousSignals[state.fish.uuid] = ThreatSignalSnapshot(
                state.threatDirection,
                state.threatIntensity
            )
        }

        val updates = ArrayList<ThreatSignalUpdate>(currentStates.size)
        for (state in currentStates) {
            val previousSignal = previousSignals[state.fish.uuid]
                ?: ThreatSignalSnapshot(Vec3.ZERO, 0.0)
            val decayedIntensity =
                (previousSignal.intensity - settings.threatSignalDecay).coerceAtLeast(0.0)

            if (state.directThreatIntensity > FishCollectiveState.MINIMUM_THREAT_SIGNAL) {
                updates.add(
                    ThreatSignalUpdate(
                        state,
                        state.directThreatDirection,
                        max(decayedIntensity, state.directThreatIntensity),
                        isDirect = true
                    )
                )
                continue
            }

            var propagatedX = 0.0
            var propagatedY = 0.0
            var propagatedZ = 0.0
            var strongestIntensity = 0.0
            var strongestDirection = Vec3.ZERO
            for (neighbor in state.neighbors) {
                val neighborSignal = previousSignals[neighbor.uuid] ?: continue
                val propagatedIntensity =
                    neighborSignal.intensity *
                        settings.threatPropagationSpeed *
                        FishSchoolCompatibility.socialAffinity(state.fish, neighbor, settings)
                if (propagatedIntensity <= MINIMUM_PROPAGATED_THREAT_INTENSITY) continue

                propagatedX += neighborSignal.direction.x * propagatedIntensity
                propagatedY += neighborSignal.direction.y * propagatedIntensity
                propagatedZ += neighborSignal.direction.z * propagatedIntensity
                if (propagatedIntensity > strongestIntensity) {
                    strongestIntensity = propagatedIntensity
                    strongestDirection = neighborSignal.direction
                }
            }

            if (strongestIntensity > decayedIntensity) {
                val combinedDirection = unit(
                    Vec3(propagatedX, propagatedY, propagatedZ)
                ).takeIf { it.lengthSqr() > MINIMUM_VECTOR_LENGTH_SQR }
                    ?: strongestDirection
                updates.add(
                    ThreatSignalUpdate(
                        state,
                        combinedDirection,
                        strongestIntensity,
                        isDirect = false
                    )
                )
            } else {
                updates.add(
                    ThreatSignalUpdate(
                        state,
                        previousSignal.direction,
                        decayedIntensity,
                        isDirect = false
                    )
                )
            }
        }

        for (update in updates) {
            update.state.applyThreatSignal(
                update.direction,
                update.intensity,
                update.isDirect
            )
        }
    }

    private fun detectDirectThreat(
        state: FishCollectiveState,
        settings: FishCollectiveSettings,
        gameTime: Long
    ): DirectThreatObservation? {
        val fish = state.fish
        val snapshot = entitySnapshot(
            threatSnapshotsByCell,
            fish,
            settings.threatDetectionRadius,
            settings.threatRefreshInterval,
            gameTime
        )
        val threatRadiusSqr = settings.threatDetectionRadius * settings.threatDetectionRadius
        var nearestThreat: LivingEntity? = null
        var nearestThreatDistanceSqr = threatRadiusSqr

        for (candidate in snapshot.entities) {
            val distanceSqr = centerDistanceSqr(fish, candidate)
            if (distanceSqr > nearestThreatDistanceSqr) continue
            if (!FishThreatClassifier.isThreat(fish, candidate)) continue
            if (!fish.hasLineOfSight(candidate)) continue
            nearestThreat = candidate
            nearestThreatDistanceSqr = distanceSqr
        }

        val threat = nearestThreat ?: return null
        val distance = sqrt(nearestThreatDistanceSqr)
        val edgeProximity = (1.0 - distance / settings.threatDetectionRadius)
            .coerceIn(0.0, 1.0)
        val intensity = if (edgeProximity < MINIMUM_DIRECT_THREAT_INTENSITY) {
            MINIMUM_DIRECT_THREAT_INTENSITY * ModUtilities.smooth(
                (edgeProximity / MINIMUM_DIRECT_THREAT_INTENSITY).coerceIn(0.0, 1.0)
            )
        } else {
            edgeProximity
        }
        return DirectThreatObservation(
            predictedThreatEscapeDirection(fish, threat, state),
            intensity
        )
    }

    private fun predictedThreatEscapeDirection(
        fish: AbstractFish,
        threat: LivingEntity,
        state: FishCollectiveState
    ): Vec3 {
        val predictedFish = Vec3(
            fish.x + fish.deltaMovement.x * FISH_THREAT_PREDICTION_TICKS,
            entityCenterY(fish) + fish.deltaMovement.y * FISH_THREAT_PREDICTION_TICKS,
            fish.z + fish.deltaMovement.z * FISH_THREAT_PREDICTION_TICKS
        )
        val predictedThreat = Vec3(
            threat.x + threat.deltaMovement.x * THREAT_PREDICTION_TICKS,
            entityCenterY(threat) + threat.deltaMovement.y * THREAT_PREDICTION_TICKS,
            threat.z + threat.deltaMovement.z * THREAT_PREDICTION_TICKS
        )
        var away = unit(predictedFish.subtract(predictedThreat))
        if (away.lengthSqr() <= MINIMUM_VECTOR_LENGTH_SQR) {
            val verticalSign = if (sin(state.movementPhase * 2.0) >= 0.0) 1.0 else -1.0
            away = unit(
                Vec3(
                    kotlin.math.cos(state.movementPhase),
                    verticalSign * 0.35,
                    sin(state.movementPhase)
                )
            )
        }

        val threatVelocity = threat.deltaMovement
        val threatSpeed = threatVelocity.length()
        if (threatSpeed <= MINIMUM_MOVING_THREAT_SPEED) return away

        val velocityDirection = threatVelocity.scale(1.0 / threatSpeed)
        var lateral = unit(velocityDirection.cross(UP_DIRECTION))
        if (lateral.lengthSqr() <= MINIMUM_VECTOR_LENGTH_SQR) {
            lateral = unit(velocityDirection.cross(X_DIRECTION))
        }
        if (lateral.lengthSqr() <= MINIMUM_VECTOR_LENGTH_SQR) return away

        val relativePosition = Vec3(
            fish.x - threat.x,
            entityCenterY(fish) - entityCenterY(threat),
            fish.z - threat.z
        )
        val sideProjection = relativePosition.dot(lateral)
        val side = when {
            sideProjection > sqrt(MINIMUM_VECTOR_LENGTH_SQR) -> 1.0
            sideProjection < -sqrt(MINIMUM_VECTOR_LENGTH_SQR) -> -1.0
            state.movementPhase < Math.PI -> 1.0
            else -> -1.0
        }
        return unit(
            away.add(lateral.scale(side * MOVING_THREAT_LATERAL_SPLIT_WEIGHT))
        )
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
                !candidate.isInWater ||
                candidate.isRemoved
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

    private fun populateCollisionFish(
        state: FishCollectiveState,
        nearbyEntities: List<LivingEntity>,
        settings: FishCollectiveSettings
    ) {
        val fish = state.fish
        state.nearbyCollisionFish.clear()
        val predictedSearchRadius = minOf(
            BLOCKER_SEARCH_RADIUS,
            PANIC_COLLISION_SEARCH_RADIUS +
                settings.panicFishSpeed *
                settings.neighborRefreshInterval *
                PANIC_COLLISION_RELATIVE_SPEED_FACTOR
        )
        val predictedSearchRadiusSqr = predictedSearchRadius * predictedSearchRadius
        for (entity in nearbyEntities) {
            val candidate = entity as? AbstractFish ?: continue
            if (
                candidate === fish ||
                !candidate.isAlive ||
                !candidate.isInWater ||
                candidate.isRemoved
            ) {
                continue
            }
            val distanceSqr = centerDistanceSqr(fish, candidate)
            if (distanceSqr > predictedSearchRadiusSqr) continue
            insertNearestFish(
                state.nearbyCollisionFish,
                candidate,
                distanceSqr,
                fish,
                MAXIMUM_NEARBY_COLLISION_FISH
            )
        }
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
                candidate.isRemoved ||
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

    private fun validateAggregationNeighbor(
        state: FishCollectiveState,
        settings: FishCollectiveSettings
    ) {
        val candidate = state.aggregationNeighbor ?: return
        val fish = state.fish
        val distanceSqr = fish.distanceToSqr(candidate)
        if (
            !candidate.isAlive ||
            !candidate.isInWater ||
            candidate.isRemoved ||
            candidate.level() !== level ||
            distanceSqr <= settings.neighborSearchRadiusSqr ||
            distanceSqr > settings.aggregationRadiusSqr
        ) {
            state.aggregationNeighbor = null
        }
    }

    private fun updateAvoidanceCoordination(
        state: FishCollectiveState,
        settings: FishCollectiveSettings,
        gameTime: Long
    ) {
        var directionX = 0.0
        var directionY = 0.0
        var directionZ = 0.0
        for (neighbor in state.neighbors) {
            val neighborState = statesByFish[neighbor.uuid] ?: continue
            if (neighborState.obstacleAvoidanceRecordedTick >= gameTime) continue
            val direction = neighborState.activeObstacleAvoidance(gameTime)
            if (direction.lengthSqr() <= MINIMUM_VECTOR_LENGTH_SQR) continue

            val distance = sqrt(state.fish.distanceToSqr(neighbor))
            val weight = FishSchoolCompatibility.socialAffinity(
                state.fish,
                neighbor,
                settings
            ) / (AVOIDANCE_COORDINATION_DISTANCE_BIAS + distance)
            directionX += direction.x * weight
            directionY += direction.y * weight
            directionZ += direction.z * weight
        }
        state.setAvoidanceCoordinationDirection(
            Vec3(directionX, directionY, directionZ)
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
                max(candidate.bbWidth, candidate.bbHeight) < LARGE_ENTITY_MINIMUM_SPAN
            ) {
                continue
            }

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

    private fun entitySnapshot(
        cache: MutableMap<Long, FishLocalEntitySnapshot>,
        fish: AbstractFish,
        searchRadius: Double,
        refreshInterval: Int,
        gameTime: Long
    ): FishLocalEntitySnapshot {
        val cellX = Math.floorDiv(fish.blockX, SNAPSHOT_CELL_SIZE)
        val cellY = Math.floorDiv(fish.blockY, SNAPSHOT_CELL_SIZE)
        val cellZ = Math.floorDiv(fish.blockZ, SNAPSHOT_CELL_SIZE)
        val cellKey = BlockPos.asLong(cellX, cellY, cellZ)
        val cached = cache[cellKey]
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
            expiresAtTick = gameTime + refreshInterval.coerceAtLeast(1),
            searchRadius = searchRadius,
            entities = entities
        ).also { cache[cellKey] = it }
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

    private fun insertNearestFish(
        output: MutableList<AbstractFish>,
        candidate: AbstractFish,
        candidateDistanceSqr: Double,
        observer: AbstractFish,
        maximumCount: Int
    ) {
        var insertionIndex = 0
        while (
            insertionIndex < output.size &&
            centerDistanceSqr(observer, output[insertionIndex]) <= candidateDistanceSqr
        ) {
            insertionIndex++
        }
        if (insertionIndex >= maximumCount) return

        output.add(insertionIndex, candidate)
        if (output.size > maximumCount) output.removeAt(output.lastIndex)
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

    private fun centerDistanceSqr(first: LivingEntity, second: LivingEntity): Double {
        val dx = first.x - second.x
        val dy = entityCenterY(first) - entityCenterY(second)
        val dz = first.z - second.z
        return dx * dx + dy * dy + dz * dz
    }

    private fun entityCenterY(entity: LivingEntity): Double {
        return (entity.boundingBox.minY + entity.boundingBox.maxY) * 0.5
    }

    private fun unit(vector: Vec3): Vec3 {
        val lengthSqr = vector.lengthSqr()
        if (lengthSqr <= MINIMUM_VECTOR_LENGTH_SQR) return Vec3.ZERO
        return vector.scale(1.0 / sqrt(lengthSqr))
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
            val shouldRemove =
                state.fish.isRemoved ||
                !state.fish.isAlive ||
                state.fish.level() !== level ||
                gameTime - state.lastObservedTick > STATE_RETENTION_TICKS
            if (shouldRemove) state.resetBodyOrientation()
            shouldRemove
        }
        neighborhoodSnapshotsByCell.entries.removeIf { (_, snapshot) ->
            snapshot.expiresAtTick <= gameTime
        }
        threatSnapshotsByCell.entries.removeIf { (_, snapshot) ->
            snapshot.expiresAtTick <= gameTime
        }
        longRangeSnapshotsByCell.entries.removeIf { (_, snapshot) ->
            snapshot.expiresAtTick <= gameTime
        }
    }

    private data class DirectThreatObservation(
        val direction: Vec3,
        val intensity: Double
    )

    private data class ThreatSignalSnapshot(
        val direction: Vec3,
        val intensity: Double
    )

    private data class ThreatSignalUpdate(
        val state: FishCollectiveState,
        val direction: Vec3,
        val intensity: Double,
        val isDirect: Boolean
    )
}
