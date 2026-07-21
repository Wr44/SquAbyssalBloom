package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object FishCollectiveDebugger {
    private val movementTracks = HashMap<UUID, MovementTrack>()

    fun recordMovementSample(state: FishCollectiveState, gameTime: Long) {
        val track = movementTracks.getOrPut(state.fish.uuid) { MovementTrack() }
        if (track.windowStartTick == Long.MIN_VALUE) {
            track.beginWindow(gameTime, state.swimWigglePhase)
        }
        track.sampleTicks++
        if (state.movementControllerRunning) track.controlledTicks++
        if (gameTime - state.lastSteeringTick <= 1L) {
            val wiggleDegrees = Math.toDegrees(state.debugLastWiggleAngle)
            track.minWiggleDegrees = min(track.minWiggleDegrees, wiggleDegrees)
            track.maxWiggleDegrees = max(track.maxWiggleDegrees, wiggleDegrees)
        }

        val desiredHeading = headingDegrees(state.desiredVelocity.x, state.desiredVelocity.z)
        val actualHeading = headingDegrees(
            state.fish.deltaMovement.x,
            state.fish.deltaMovement.z
        )
        val fishX = state.fish.x
        val fishZ = state.fish.z
        val positionHeading = headingDegrees(fishX - track.lastX, fishZ - track.lastZ)
        if (gameTime - track.lastSampleTick == 1L) {
            if (!desiredHeading.isNaN() && !track.lastDesiredHeading.isNaN()) {
                track.recordDesiredTurn(
                    Mth.wrapDegrees(desiredHeading - track.lastDesiredHeading)
                )
            }
            if (!actualHeading.isNaN() && !track.lastActualHeading.isNaN()) {
                track.recordActualTurn(
                    Mth.wrapDegrees(actualHeading - track.lastActualHeading)
                )
            }
            if (state.movementControllerRunning) {
                if (!state.debugLastAppliedTurnDegrees.isNaN()) {
                    track.recordAppliedTurn(state.debugLastAppliedTurnDegrees)
                }
                if (!actualHeading.isNaN() && !state.debugLastWrittenHeadingDegrees.isNaN()) {
                    track.recordExternalDrift(
                        Mth.wrapDegrees(actualHeading - state.debugLastWrittenHeadingDegrees)
                    )
                }
            }
            if (!positionHeading.isNaN() && !track.lastPositionHeading.isNaN()) {
                track.recordPositionTurn(
                    Mth.wrapDegrees(positionHeading - track.lastPositionHeading)
                )
            }
        }
        track.lastSampleTick = gameTime
        track.lastDesiredHeading = desiredHeading
        track.lastActualHeading = actualHeading
        track.lastPositionHeading = positionHeading
        track.lastX = fishX
        track.lastZ = fishZ
        logCollectiveEvents(state, track, gameTime)
    }

    private fun logCollectiveEvents(
        state: FishCollectiveState,
        track: MovementTrack,
        gameTime: Long
    ) {
        val fish = state.fish
        val active = state.collectiveActive
        val previousActive = track.prevCollectiveActive
        track.prevCollectiveActive = active
        if (previousActive != null && previousActive != active) {
            SquAbyssalBloom.LOGGER.info(
                "[Fish school debug] event={} fishId={} type={} localFish={} neighbors={} threat={}",
                if (active) "collectiveActivated" else "collectiveDeactivated",
                fish.id,
                typeName(state),
                state.localCompatibleFishCount,
                state.neighbors.size,
                roundedDecimal(state.threatIntensity, 100.0)
            )
        }

        val target = state.aggregationNeighbor
        val targetId = target?.id ?: -1
        val previousTargetId = track.prevAggregationTargetId
        track.prevAggregationTargetId = targetId
        if (
            previousTargetId != Int.MIN_VALUE &&
            targetId != previousTargetId &&
            gameTime - track.lastAggregationEventTick >= EVENT_LOG_THROTTLE_TICKS
        ) {
            track.lastAggregationEventTick = gameTime
            if (target != null) {
                SquAbyssalBloom.LOGGER.info(
                    "[Fish school debug] event=aggregationLink fishId={} type={} targetId={} distance={} localFish={} active={}",
                    fish.id,
                    typeName(state),
                    targetId,
                    roundedDecimal(fish.distanceTo(target).toDouble(), 10.0),
                    state.localCompatibleFishCount,
                    active
                )
            } else {
                SquAbyssalBloom.LOGGER.info(
                    "[Fish school debug] event=aggregationEnd fishId={} type={} localFish={} active={}",
                    fish.id,
                    typeName(state),
                    state.localCompatibleFishCount,
                    active
                )
            }
        }

        val directlyThreatened = state.directlyThreatened
        val intensity = state.threatIntensity
        val previousDirect = track.prevDirectlyThreatened
        val previousIntensity = track.prevThreatIntensity
        track.prevDirectlyThreatened = directlyThreatened
        track.prevThreatIntensity = intensity
        if (gameTime - track.lastThreatEventTick < EVENT_LOG_THROTTLE_TICKS) return

        if (directlyThreatened != previousDirect) {
            track.lastThreatEventTick = gameTime
            SquAbyssalBloom.LOGGER.info(
                "[Fish school debug] event={} fishId={} type={} intensity={} escapeX={} escapeZ={}",
                if (directlyThreatened) "directThreatStart" else "directThreatEnd",
                fish.id,
                typeName(state),
                roundedDecimal(intensity, 100.0),
                roundedDecimal(state.threatDirection.x, 100.0),
                roundedDecimal(state.threatDirection.z, 100.0)
            )
            return
        }
        if (directlyThreatened) return

        if (intensity > MINIMUM_VISIBLE_THREAT && previousIntensity <= MINIMUM_VISIBLE_THREAT) {
            track.lastThreatEventTick = gameTime
            SquAbyssalBloom.LOGGER.info(
                "[Fish school debug] event=propagatedThreat fishId={} type={} intensity={} escapeX={} escapeZ={}",
                fish.id,
                typeName(state),
                roundedDecimal(intensity, 100.0),
                roundedDecimal(state.threatDirection.x, 100.0),
                roundedDecimal(state.threatDirection.z, 100.0)
            )
        } else if (intensity <= MINIMUM_VISIBLE_THREAT && previousIntensity > MINIMUM_VISIBLE_THREAT) {
            track.lastThreatEventTick = gameTime
            SquAbyssalBloom.LOGGER.info(
                "[Fish school debug] event=threatCleared fishId={} type={} localFish={} active={}",
                fish.id,
                typeName(state),
                state.localCompatibleFishCount,
                active
            )
        }
    }

    private fun typeName(state: FishCollectiveState): String {
        return state.fish.type.description.string
    }

    fun clearMovementTracks() {
        movementTracks.clear()
    }

    fun recordNavigationSuppression(state: FishCollectiveState, gameTime: Long) {
        val track = movementTracks.getOrPut(state.fish.uuid) { MovementTrack() }
        if (gameTime - track.lastNavigationEventTick < EVENT_LOG_THROTTLE_TICKS) return

        track.lastNavigationEventTick = gameTime
        SquAbyssalBloom.LOGGER.info(
            "[Fish school debug] event=navigationSuppressed fishId={} type={} active={} localFish={}",
            state.fish.id,
            typeName(state),
            state.collectiveActive,
            state.localCompatibleFishCount
        )
    }

    fun recordVelocityApplication(
        state: FishCollectiveState,
        currentVelocity: Vec3,
        turnLimitedVelocity: Vec3,
        writtenVelocity: Vec3
    ) {
        val currentHeading = headingDegrees(currentVelocity.x, currentVelocity.z)
        val limitedHeading = headingDegrees(turnLimitedVelocity.x, turnLimitedVelocity.z)
        state.debugLastAppliedTurnDegrees =
            if (currentHeading.isNaN() || limitedHeading.isNaN()) {
                Double.NaN
            } else {
                Mth.wrapDegrees(limitedHeading - currentHeading)
            }
        state.debugLastWrittenHeadingDegrees = headingDegrees(writtenVelocity.x, writtenVelocity.z)
    }

    fun renderFish(
        level: ServerLevel,
        state: FishCollectiveState,
        gameTime: Long
    ) {
        val fish = state.fish
        val mixedUuid = fish.uuid.mostSignificantBits xor fish.uuid.leastSignificantBits
        val updateOffset = Math.floorMod(mixedUuid, PARTICLE_INTERVAL_TICKS)
        if (Math.floorMod(gameTime + updateOffset, PARTICLE_INTERVAL_TICKS) != 0L) return

        val statusParticle = when {
            state.directlyThreatened -> ParticleTypes.ANGRY_VILLAGER
            state.threatIntensity > MINIMUM_VISIBLE_THREAT -> ParticleTypes.WITCH
            state.collectiveActive && state.deactivationProgressTicks > 0 -> ParticleTypes.WAX_OFF
            state.collectiveActive -> ParticleTypes.HAPPY_VILLAGER
            state.activationProgressTicks > 0 -> ParticleTypes.WAX_ON
            else -> ParticleTypes.SMOKE
        }
        level.sendParticles(
            statusParticle,
            fish.x,
            fish.eyeY + STATUS_PARTICLE_HEIGHT,
            fish.z,
            1,
            0.0,
            0.0,
            0.0,
            0.0
        )

        renderNeighborLinks(level, state)
        if (
            state.collectiveActive &&
            state.lastSteeringTick != Long.MIN_VALUE &&
            gameTime - state.lastSteeringTick <= MAXIMUM_STEERING_AGE_TICKS
        ) {
            renderSteeringDirection(level, state)
        }
    }

    fun logEnabled(level: ServerLevel) {
        SquAbyssalBloom.LOGGER.info(
            "[Fish school debug] Enabled for {}. Status particles: smoke=vanilla, orange=activation pending, cyan=deactivation pending, green=collective, purple=propagated threat, red=direct threat; sparks=neighbors; end rods=steering.",
            level.dimension()
        )
        SquAbyssalBloom.LOGGER.info(
            "[Fish school debug] Wiggle telemetry: per-fish lines with each summary. Expected while controlled: phaseStepPerTick~0.18-0.24 (individual per fish), wiggleDeg~[-22.5..22.5], desiredTurnDeg and actualTurnDeg oscillating between negative and positive values."
        )
        SquAbyssalBloom.LOGGER.info(
            "[Fish school debug] Transition events: collectiveActivated/collectiveDeactivated, aggregationLink/aggregationEnd (school merging), directThreatStart/directThreatEnd, propagatedThreat, threatCleared."
        )
    }

    fun logSummary(
        level: ServerLevel,
        states: Collection<FishCollectiveState>,
        gameTime: Long
    ) {
        var observedFish = 0
        var collectiveFish = 0
        var controlledFish = 0
        var collectiveWithoutControl = 0
        var activationPendingFish = 0
        var deactivationPendingFish = 0
        var threatSignalFish = 0
        var totalLocalFish = 0
        var totalTopologicalNeighbors = 0
        var crossSpeciesLinks = 0
        var aggregationTargets = 0
        val currentStates = ArrayList<FishCollectiveState>()

        for (state in states) {
            val fish = state.fish
            if (
                !fish.isAlive ||
                !fish.isInWater ||
                fish.level() !== level ||
                gameTime - state.lastObservedTick > MAXIMUM_OBSERVATION_AGE_TICKS
            ) {
                continue
            }

            currentStates.add(state)
            observedFish++
            if (state.collectiveActive) collectiveFish++
            if (state.movementControllerRunning) controlledFish++
            if (state.collectiveActive && !state.movementControllerRunning) {
                collectiveWithoutControl++
            }
            if (!state.collectiveActive && state.activationProgressTicks > 0) {
                activationPendingFish++
            }
            if (state.collectiveActive && state.deactivationProgressTicks > 0) {
                deactivationPendingFish++
            }
            if (state.threatIntensity > MINIMUM_VISIBLE_THREAT) threatSignalFish++
            totalLocalFish += state.localCompatibleFishCount
            totalTopologicalNeighbors += state.neighbors.size
            crossSpeciesLinks += state.neighbors.count { neighbor ->
                neighbor.type !== fish.type
            }
            if (state.aggregationNeighbor?.isAlive == true) aggregationTargets++
        }

        val averageLocalFish = roundedAverage(totalLocalFish, observedFish)
        val averageTopologicalNeighbors = roundedAverage(totalTopologicalNeighbors, observedFish)
        val componentSizes = collectiveComponentSizes(currentStates)
        SquAbyssalBloom.LOGGER.info(
            "[Fish school debug] level={} observed={} collective={} controlled={} collectiveWithoutControl={} vanilla={} activationPending={} deactivationPending={} avgLocalFish={} avgTopologicalNeighbors={} crossSpeciesLinks={} aggregationTargets={} threatSignals={} connectedSchools={} largestSchool={} schoolSizes={}",
            level.dimension(),
            observedFish,
            collectiveFish,
            controlledFish,
            collectiveWithoutControl,
            observedFish - collectiveFish,
            activationPendingFish,
            deactivationPendingFish,
            averageLocalFish,
            averageTopologicalNeighbors,
            crossSpeciesLinks,
            aggregationTargets,
            threatSignalFish,
            componentSizes.size,
            componentSizes.firstOrNull() ?: 0,
            formatComponentSizes(componentSizes)
        )
        logWiggleTelemetry(currentStates, gameTime)
    }

    private fun logWiggleTelemetry(
        currentStates: List<FishCollectiveState>,
        gameTime: Long
    ) {
        val statesByUuid = HashMap<UUID, FishCollectiveState>()
        for (state in currentStates) statesByUuid[state.fish.uuid] = state
        movementTracks.keys.retainAll(statesByUuid.keys)

        val loggedEntries = movementTracks.entries
            .filter { it.value.sampleTicks >= MINIMUM_WIGGLE_SAMPLES }
            .sortedByDescending { it.value.controlledTicks }
            .take(MAXIMUM_WIGGLE_LOGGED)
        for ((uuid, track) in loggedEntries) {
            val state = statesByUuid[uuid] ?: continue
            val fish = state.fish
            val windowTicks = (gameTime - track.windowStartTick).coerceAtLeast(1L)
            val phaseStep = (state.swimWigglePhase - track.windowStartPhase) / windowTicks
            val steeringAge = if (state.lastSteeringTick == Long.MIN_VALUE) {
                -1L
            } else {
                gameTime - state.lastSteeringTick
            }
            val horizontalSpeed = sqrt(
                fish.deltaMovement.x * fish.deltaMovement.x +
                    fish.deltaMovement.z * fish.deltaMovement.z
            )
            SquAbyssalBloom.LOGGER.info(
                "[Fish school debug] wiggle fishId={} type={} ctrlTicks={}/{} steeringAge={} phaseStepPerTick={} wiggleDeg={} desiredTurnDeg={} avgAbsDesiredTurn={} actualTurnDeg={} avgAbsActualTurn={} appliedTurnDeg={} avgAbsAppliedTurn={} driftDeg={} avgAbsDrift={} posTurnDeg={} avgAbsPosTurn={} speed={}",
                fish.id,
                typeName(state),
                track.controlledTicks,
                track.sampleTicks,
                steeringAge,
                roundedDecimal(phaseStep, 1000.0),
                formatRange(track.minWiggleDegrees, track.maxWiggleDegrees),
                formatRange(track.minDesiredTurn, track.maxDesiredTurn),
                roundedDecimal(track.averageAbsoluteDesiredTurn(), 10.0),
                formatRange(track.minActualTurn, track.maxActualTurn),
                roundedDecimal(track.averageAbsoluteActualTurn(), 10.0),
                formatRange(track.minAppliedTurn, track.maxAppliedTurn),
                roundedDecimal(track.averageAbsoluteAppliedTurn(), 10.0),
                formatRange(track.minExternalDrift, track.maxExternalDrift),
                roundedDecimal(track.averageAbsoluteExternalDrift(), 10.0),
                formatRange(track.minPositionTurn, track.maxPositionTurn),
                roundedDecimal(track.averageAbsolutePositionTurn(), 10.0),
                roundedDecimal(horizontalSpeed, 1000.0)
            )
        }

        for ((uuid, track) in movementTracks) {
            val state = statesByUuid[uuid] ?: continue
            track.beginWindow(gameTime, state.swimWigglePhase)
        }
    }

    private fun collectiveComponentSizes(
        states: List<FishCollectiveState>
    ): List<Int> {
        val collectiveStates = HashMap<UUID, FishCollectiveState>()
        for (state in states) {
            if (state.collectiveActive) collectiveStates[state.fish.uuid] = state
        }
        if (collectiveStates.isEmpty()) return emptyList()

        val connections = HashMap<UUID, MutableSet<UUID>>()
        for ((fishId, state) in collectiveStates) {
            val fishConnections = connections.getOrPut(fishId) { HashSet() }
            for (neighbor in state.neighbors) {
                val neighborId = neighbor.uuid
                if (!collectiveStates.containsKey(neighborId)) continue

                fishConnections.add(neighborId)
                connections.getOrPut(neighborId) { HashSet() }.add(fishId)
            }
        }

        val visited = HashSet<UUID>()
        val pending = ArrayDeque<UUID>()
        val componentSizes = ArrayList<Int>()
        for (fishId in collectiveStates.keys) {
            if (!visited.add(fishId)) continue

            pending.addLast(fishId)
            var componentSize = 0
            while (pending.isNotEmpty()) {
                val currentId = pending.removeFirst()
                componentSize++
                for (connectedId in connections[currentId].orEmpty()) {
                    if (visited.add(connectedId)) pending.addLast(connectedId)
                }
            }
            componentSizes.add(componentSize)
        }
        componentSizes.sortDescending()
        return componentSizes
    }

    private fun formatComponentSizes(componentSizes: List<Int>): String {
        val displayedSizes = componentSizes.take(MAXIMUM_LOGGED_COMPONENTS)
            .joinToString(separator = ",")
        return if (componentSizes.size > MAXIMUM_LOGGED_COMPONENTS) {
            "[$displayedSizes,...]"
        } else {
            "[$displayedSizes]"
        }
    }

    private fun renderNeighborLinks(
        level: ServerLevel,
        state: FishCollectiveState
    ) {
        val fish = state.fish
        val neighborCount = minOf(state.neighbors.size, MAXIMUM_DEBUG_NEIGHBORS)
        for (neighborIndex in 0 until neighborCount) {
            val neighbor = state.neighbors[neighborIndex]
            if (!neighbor.isAlive || neighbor.level() !== level) continue

            for (fraction in LINK_SAMPLE_FRACTIONS) {
                level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    fish.x + (neighbor.x - fish.x) * fraction,
                    fish.eyeY + (neighbor.eyeY - fish.eyeY) * fraction,
                    fish.z + (neighbor.z - fish.z) * fraction,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
                )
            }
        }
    }

    private fun renderSteeringDirection(
        level: ServerLevel,
        state: FishCollectiveState
    ) {
        val velocity = state.desiredVelocity
        val velocityLengthSqr = velocity.lengthSqr()
        if (velocityLengthSqr <= MINIMUM_VECTOR_LENGTH_SQR) return

        val inverseLength = 1.0 / sqrt(velocityLengthSqr)
        val directionX = velocity.x * inverseLength
        val directionY = velocity.y * inverseLength
        val directionZ = velocity.z * inverseLength
        val fish = state.fish

        for (distance in DIRECTION_SAMPLE_DISTANCES) {
            level.sendParticles(
                ParticleTypes.END_ROD,
                fish.x + directionX * distance,
                fish.eyeY + directionY * distance,
                fish.z + directionZ * distance,
                1,
                0.0,
                0.0,
                0.0,
                0.0
            )
        }
    }

    private fun roundedAverage(total: Int, count: Int): Double {
        if (count <= 0) return 0.0
        return kotlin.math.round(total.toDouble() * 100.0 / count.toDouble()) / 100.0
    }

    private fun roundedDecimal(value: Double, scale: Double): Double {
        return kotlin.math.round(value * scale) / scale
    }

    private fun formatRange(minimum: Double, maximum: Double): String {
        if (minimum > maximum) return "[none]"
        return "[${roundedDecimal(minimum, 10.0)}..${roundedDecimal(maximum, 10.0)}]"
    }

    private fun headingDegrees(x: Double, z: Double): Double {
        if (x * x + z * z <= MINIMUM_VECTOR_LENGTH_SQR) return Double.NaN
        return Math.toDegrees(atan2(z, x))
    }

    private class MovementTrack {
        var windowStartTick = Long.MIN_VALUE
        var windowStartPhase = 0.0
        var lastSampleTick = Long.MIN_VALUE
        var lastDesiredHeading = Double.NaN
        var lastActualHeading = Double.NaN
        var lastPositionHeading = Double.NaN
        var lastX = Double.NaN
        var lastZ = Double.NaN
        var sampleTicks = 0
        var controlledTicks = 0
        var prevCollectiveActive: Boolean? = null
        var prevAggregationTargetId = Int.MIN_VALUE
        var prevDirectlyThreatened = false
        var prevThreatIntensity = 0.0
        var lastAggregationEventTick = Long.MIN_VALUE / 2
        var lastThreatEventTick = Long.MIN_VALUE / 2
        var lastNavigationEventTick = Long.MIN_VALUE / 2
        var minWiggleDegrees = Double.POSITIVE_INFINITY
        var maxWiggleDegrees = Double.NEGATIVE_INFINITY
        var minDesiredTurn = Double.POSITIVE_INFINITY
        var maxDesiredTurn = Double.NEGATIVE_INFINITY
        var minActualTurn = Double.POSITIVE_INFINITY
        var maxActualTurn = Double.NEGATIVE_INFINITY
        var minAppliedTurn = Double.POSITIVE_INFINITY
        var maxAppliedTurn = Double.NEGATIVE_INFINITY
        var minExternalDrift = Double.POSITIVE_INFINITY
        var maxExternalDrift = Double.NEGATIVE_INFINITY
        var minPositionTurn = Double.POSITIVE_INFINITY
        var maxPositionTurn = Double.NEGATIVE_INFINITY
        private var totalAbsoluteDesiredTurn = 0.0
        private var desiredTurnSamples = 0
        private var totalAbsoluteActualTurn = 0.0
        private var actualTurnSamples = 0
        private var totalAbsoluteAppliedTurn = 0.0
        private var appliedTurnSamples = 0
        private var totalAbsoluteExternalDrift = 0.0
        private var externalDriftSamples = 0
        private var totalAbsolutePositionTurn = 0.0
        private var positionTurnSamples = 0

        fun beginWindow(gameTime: Long, phase: Double) {
            windowStartTick = gameTime
            windowStartPhase = phase
            sampleTicks = 0
            controlledTicks = 0
            minWiggleDegrees = Double.POSITIVE_INFINITY
            maxWiggleDegrees = Double.NEGATIVE_INFINITY
            minDesiredTurn = Double.POSITIVE_INFINITY
            maxDesiredTurn = Double.NEGATIVE_INFINITY
            minActualTurn = Double.POSITIVE_INFINITY
            maxActualTurn = Double.NEGATIVE_INFINITY
            minAppliedTurn = Double.POSITIVE_INFINITY
            maxAppliedTurn = Double.NEGATIVE_INFINITY
            minExternalDrift = Double.POSITIVE_INFINITY
            maxExternalDrift = Double.NEGATIVE_INFINITY
            minPositionTurn = Double.POSITIVE_INFINITY
            maxPositionTurn = Double.NEGATIVE_INFINITY
            totalAbsoluteDesiredTurn = 0.0
            desiredTurnSamples = 0
            totalAbsoluteActualTurn = 0.0
            actualTurnSamples = 0
            totalAbsoluteAppliedTurn = 0.0
            appliedTurnSamples = 0
            totalAbsoluteExternalDrift = 0.0
            externalDriftSamples = 0
            totalAbsolutePositionTurn = 0.0
            positionTurnSamples = 0
        }

        fun recordDesiredTurn(turnDegrees: Double) {
            minDesiredTurn = min(minDesiredTurn, turnDegrees)
            maxDesiredTurn = max(maxDesiredTurn, turnDegrees)
            totalAbsoluteDesiredTurn += abs(turnDegrees)
            desiredTurnSamples++
        }

        fun recordActualTurn(turnDegrees: Double) {
            minActualTurn = min(minActualTurn, turnDegrees)
            maxActualTurn = max(maxActualTurn, turnDegrees)
            totalAbsoluteActualTurn += abs(turnDegrees)
            actualTurnSamples++
        }

        fun averageAbsoluteDesiredTurn(): Double {
            if (desiredTurnSamples <= 0) return 0.0
            return totalAbsoluteDesiredTurn / desiredTurnSamples
        }

        fun averageAbsoluteActualTurn(): Double {
            if (actualTurnSamples <= 0) return 0.0
            return totalAbsoluteActualTurn / actualTurnSamples
        }

        fun recordAppliedTurn(turnDegrees: Double) {
            minAppliedTurn = min(minAppliedTurn, turnDegrees)
            maxAppliedTurn = max(maxAppliedTurn, turnDegrees)
            totalAbsoluteAppliedTurn += abs(turnDegrees)
            appliedTurnSamples++
        }

        fun recordExternalDrift(driftDegrees: Double) {
            minExternalDrift = min(minExternalDrift, driftDegrees)
            maxExternalDrift = max(maxExternalDrift, driftDegrees)
            totalAbsoluteExternalDrift += abs(driftDegrees)
            externalDriftSamples++
        }

        fun recordPositionTurn(turnDegrees: Double) {
            minPositionTurn = min(minPositionTurn, turnDegrees)
            maxPositionTurn = max(maxPositionTurn, turnDegrees)
            totalAbsolutePositionTurn += abs(turnDegrees)
            positionTurnSamples++
        }

        fun averageAbsoluteAppliedTurn(): Double {
            if (appliedTurnSamples <= 0) return 0.0
            return totalAbsoluteAppliedTurn / appliedTurnSamples
        }

        fun averageAbsoluteExternalDrift(): Double {
            if (externalDriftSamples <= 0) return 0.0
            return totalAbsoluteExternalDrift / externalDriftSamples
        }

        fun averageAbsolutePositionTurn(): Double {
            if (positionTurnSamples <= 0) return 0.0
            return totalAbsolutePositionTurn / positionTurnSamples
        }
    }

    private val LINK_SAMPLE_FRACTIONS = doubleArrayOf(0.33, 0.66)
    private val DIRECTION_SAMPLE_DISTANCES = doubleArrayOf(0.4, 0.8, 1.2, 1.6)

    private const val PARTICLE_INTERVAL_TICKS = 10L
    private const val MINIMUM_WIGGLE_SAMPLES = 20
    private const val MAXIMUM_WIGGLE_LOGGED = 4
    private const val EVENT_LOG_THROTTLE_TICKS = 20L
    private const val MAXIMUM_DEBUG_NEIGHBORS = 4
    private const val MAXIMUM_LOGGED_COMPONENTS = 8
    private const val MAXIMUM_STEERING_AGE_TICKS = 2L
    private const val MAXIMUM_OBSERVATION_AGE_TICKS = 2L
    private const val MINIMUM_VISIBLE_THREAT = 0.01
    private const val MINIMUM_VECTOR_LENGTH_SQR = 1.0E-8
    private const val STATUS_PARTICLE_HEIGHT = 0.25
}
