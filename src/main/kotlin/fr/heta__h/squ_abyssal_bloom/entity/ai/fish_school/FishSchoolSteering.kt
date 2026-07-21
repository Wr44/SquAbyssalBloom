package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluenceContext
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

object FishSchoolSteering {
    fun calculateDesiredVelocity(
        level: ServerLevel,
        state: FishCollectiveState,
        settings: FishCollectiveSettings
    ): Vec3 {
        val fish = state.fish
        val currentVelocity = fish.deltaMovement
        val currentDirection = horizontalUnit(currentVelocity.x, currentVelocity.z)

        var separationX = 0.0
        var separationY = 0.0
        var separationZ = 0.0
        var alignmentX = 0.0
        var alignmentY = 0.0
        var alignmentZ = 0.0
        var horizontalAlignmentSamples = 0
        var socialNeighborWeight = 0.0
        var cohesionX = 0.0
        var cohesionY = 0.0
        var cohesionZ = 0.0

        val predictedFishX = fish.x + currentVelocity.x * COLLISION_PREDICTION_TICKS
        val predictedFishY = fish.y + currentVelocity.y * COLLISION_PREDICTION_TICKS
        val predictedFishZ = fish.z + currentVelocity.z * COLLISION_PREDICTION_TICKS

        for (neighbor in state.neighbors) {
            val neighborVelocity = neighbor.deltaMovement
            val socialAffinity = FishSchoolCompatibility.socialAffinity(fish, neighbor, settings)
            var awayX = predictedFishX -
                (neighbor.x + neighborVelocity.x * COLLISION_PREDICTION_TICKS)
            var awayY = predictedFishY -
                (neighbor.y + neighborVelocity.y * COLLISION_PREDICTION_TICKS)
            var awayZ = predictedFishZ -
                (neighbor.z + neighborVelocity.z * COLLISION_PREDICTION_TICKS)
            var distanceSqr = awayX * awayX + awayY * awayY + awayZ * awayZ
            if (distanceSqr <= MINIMUM_DIRECTION_LENGTH_SQR) {
                awayX = fish.x - neighbor.x
                awayY = fish.y - neighbor.y
                awayZ = fish.z - neighbor.z
                distanceSqr = awayX * awayX + awayY * awayY + awayZ * awayZ
                if (distanceSqr <= MINIMUM_DIRECTION_LENGTH_SQR) {
                    awayX = cos(state.movementPhase)
                    awayY = 0.0
                    awayZ = sin(state.movementPhase)
                    distanceSqr = MINIMUM_SEPARATION_DISTANCE_SQR
                }
            }
            if (distanceSqr < SEPARATION_RADIUS_SQR) {
                val proximity =
                    (1.0 - distanceSqr / SEPARATION_RADIUS_SQR).coerceIn(0.0, 1.0)
                val smoothFalloff = proximity * proximity
                val inverseDistanceSqr = 1.0 / max(distanceSqr, MINIMUM_SEPARATION_DISTANCE_SQR)
                separationX += awayX * inverseDistanceSqr * smoothFalloff
                separationY += awayY * inverseDistanceSqr * smoothFalloff
                separationZ += awayZ * inverseDistanceSqr * smoothFalloff
            }

            val neighborHorizontalSpeedSqr =
                neighborVelocity.x * neighborVelocity.x + neighborVelocity.z * neighborVelocity.z
            if (neighborHorizontalSpeedSqr > MINIMUM_DIRECTION_LENGTH_SQR) {
                val inverseNeighborSpeed = 1.0 / sqrt(neighborHorizontalSpeedSqr)
                alignmentX += neighborVelocity.x * inverseNeighborSpeed * socialAffinity
                alignmentZ += neighborVelocity.z * inverseNeighborSpeed * socialAffinity
                horizontalAlignmentSamples++
            }
            alignmentY += neighborVelocity.y * socialAffinity
            cohesionX += neighbor.x * socialAffinity
            cohesionY += neighbor.y * socialAffinity
            cohesionZ += neighbor.z * socialAffinity
            socialNeighborWeight += socialAffinity
        }

        var alignment = Vec3.ZERO
        var cohesion = Vec3.ZERO
        var centroidDirection = Vec3.ZERO
        if (state.neighbors.isNotEmpty()) {
            val inverseAlignmentSamples = if (horizontalAlignmentSamples > 0) {
                1.0 / horizontalAlignmentSamples.toDouble()
            } else {
                0.0
            }
            val inverseSocialNeighborWeight = 1.0 / socialNeighborWeight
            val averageSocialAffinity =
                socialNeighborWeight / state.neighbors.size.toDouble()
            alignment = Vec3(
                alignmentX * inverseAlignmentSamples,
                (
                    alignmentY * inverseSocialNeighborWeight - currentVelocity.y
                    ) * ALIGNMENT_VERTICAL_SCALE * averageSocialAffinity,
                alignmentZ * inverseAlignmentSamples
            )

            val localCenterOffset = Vec3(
                cohesionX * inverseSocialNeighborWeight - fish.x,
                cohesionY * inverseSocialNeighborWeight - fish.y,
                cohesionZ * inverseSocialNeighborWeight - fish.z
            )
            val horizontalCohesion = horizontalUnit(localCenterOffset.x, localCenterOffset.z)
            centroidDirection = horizontalCohesion
            val horizontalCenterDistance = sqrt(
                localCenterOffset.x * localCenterOffset.x +
                    localCenterOffset.z * localCenterOffset.z
            )
            val horizontalCohesionStrength =
                (horizontalCenterDistance / COHESION_FULL_STRENGTH_DISTANCE).coerceIn(0.0, 1.0)
            cohesion = Vec3(
                horizontalCohesion.x * horizontalCohesionStrength * averageSocialAffinity,
                localCenterOffset.y
                    .coerceIn(-MAXIMUM_LOCAL_VERTICAL_ERROR, MAXIMUM_LOCAL_VERTICAL_ERROR) *
                    averageSocialAffinity,
                horizontalCohesion.z * horizontalCohesionStrength * averageSocialAffinity
            )
        }

        val separation = limitMagnitude(
            Vec3(separationX, separationY, separationZ),
            MAXIMUM_SEPARATION_FORCE
        )
        val entityAvoidance = calculateEntityAvoidance(
            fish,
            state.nearbyLargeEntities
        )

        var influenceX = 0.0
        var influenceY = 0.0
        var influenceZ = 0.0
        val influenceContext = FishSchoolInfluenceContext(
            gameTime = level.gameTime,
            threatIntensity = state.threatIntensity
        )
        for (influence in state.influences) {
            if (influence.source.isRemoved) continue
            val contribution = influence.computeInfluence(fish, influenceContext)
            influenceX += contribution.x
            influenceY += contribution.y
            influenceZ += contribution.z
        }
        val environmentalInfluence = limitMagnitude(
            Vec3(influenceX, influenceY, influenceZ),
            MAXIMUM_ENVIRONMENTAL_INFLUENCE
        )

        val threatIntensity = state.threatIntensity.coerceIn(0.0, 1.0)
        val aggregationInfluence = calculateAggregationInfluence(state, settings)
        val aggregationWeight = settings.cohesionWeight *
            AGGREGATION_COHESION_SCALE *
            (1.0 - threatIntensity * PANIC_AGGREGATION_REDUCTION)
        val herdInfluence = calculateHerdInfluence(state, centroidDirection, threatIntensity)
        val densityNoiseFactor = (
            1.0 / (1.0 + state.localCompatibleFishCount * NOISE_DENSITY_REDUCTION_PER_NEIGHBOR)
            ).coerceAtLeast(MINIMUM_DENSITY_NOISE_FACTOR)
        val noiseWeight = BASE_NOISE_WEIGHT *
            densityNoiseFactor *
            (1.0 - threatIntensity * PANIC_NOISE_REDUCTION)
        val ambientWander = calculateAmbientWander(fish, level.gameTime)
        val ambientWanderWeight = AMBIENT_WANDER_WEIGHT *
            (1.0 - threatIntensity * PANIC_AMBIENT_WANDER_REDUCTION)
        val preliminary = Vec3(
            separation.x * settings.separationWeight +
                alignment.x * settings.alignmentWeight +
                cohesion.x * settings.cohesionWeight +
                entityAvoidance.x * settings.obstacleAvoidanceWeight +
                herdInfluence.x * settings.threatAvoidanceWeight +
                environmentalInfluence.x +
                aggregationInfluence.x * aggregationWeight +
                ambientWander.x * ambientWanderWeight +
                state.noiseDirection.x * noiseWeight,
            0.0,
            separation.z * settings.separationWeight +
                alignment.z * settings.alignmentWeight +
                cohesion.z * settings.cohesionWeight +
                entityAvoidance.z * settings.obstacleAvoidanceWeight +
                herdInfluence.z * settings.threatAvoidanceWeight +
                environmentalInfluence.z +
                aggregationInfluence.z * aggregationWeight +
                ambientWander.z * ambientWanderWeight +
                state.noiseDirection.z * noiseWeight
        )
        val intendedDirection = horizontalUnit(preliminary.x, preliminary.z)
        val probeDirection = if (currentDirection.lengthSqr() > MINIMUM_DIRECTION_LENGTH_SQR) {
            horizontalUnit(
                currentDirection.x * CURRENT_DIRECTION_PROBE_WEIGHT +
                    intendedDirection.x * INTENDED_DIRECTION_PROBE_WEIGHT,
                currentDirection.z * CURRENT_DIRECTION_PROBE_WEIGHT +
                    intendedDirection.z * INTENDED_DIRECTION_PROBE_WEIGHT
            )
        } else {
            intendedDirection
        }
        val obstacleAvoidance = calculateBlockAvoidance(
            level,
            state,
            probeDirection
        )
        val avoidanceUrgency = (
            horizontalLength(obstacleAvoidance) + horizontalLength(entityAvoidance)
            ).coerceIn(0.0, 1.0)
        val alignmentWeight = settings.alignmentWeight *
            (1.0 + threatIntensity * PANIC_ALIGNMENT_INCREASE) *
            (1.0 - avoidanceUrgency * AVOIDANCE_ALIGNMENT_REDUCTION) *
            (if (state.directlyThreatened) DIRECT_THREAT_ALIGNMENT_FACTOR else 1.0)
        val cohesionWeight = settings.cohesionWeight

        var desiredX = separation.x * settings.separationWeight +
            alignment.x * alignmentWeight +
            cohesion.x * cohesionWeight +
            (entityAvoidance.x + obstacleAvoidance.x) * settings.obstacleAvoidanceWeight +
            herdInfluence.x * settings.threatAvoidanceWeight +
            environmentalInfluence.x +
            aggregationInfluence.x * aggregationWeight +
            ambientWander.x * ambientWanderWeight +
            state.noiseDirection.x * noiseWeight
        var desiredZ = separation.z * settings.separationWeight +
            alignment.z * alignmentWeight +
            cohesion.z * cohesionWeight +
            (entityAvoidance.z + obstacleAvoidance.z) * settings.obstacleAvoidanceWeight +
            herdInfluence.z * settings.threatAvoidanceWeight +
            environmentalInfluence.z +
            aggregationInfluence.z * aggregationWeight +
            ambientWander.z * ambientWanderWeight +
            state.noiseDirection.z * noiseWeight

        var desiredDirection = horizontalUnit(desiredX, desiredZ)
        if (desiredDirection.lengthSqr() <= MINIMUM_DIRECTION_LENGTH_SQR) {
            desiredDirection = currentDirection
                .takeIf { it.lengthSqr() > MINIMUM_DIRECTION_LENGTH_SQR }
                ?: horizontalUnit(state.noiseDirection.x, state.noiseDirection.z)
        }

        val wiggleAngle = sin(state.swimWigglePhase) * MAXIMUM_SWIM_WIGGLE_ANGLE
        state.debugLastWiggleAngle = wiggleAngle
        desiredDirection = rotateHorizontal(desiredDirection, wiggleAngle)

        val normalSpeed = settings.maximumFishSpeed *
            NORMAL_CRUISING_SPEED_FACTOR *
            state.individualSpeedFactor
        var targetSpeed = normalSpeed +
            (settings.panicFishSpeed - normalSpeed) * threatIntensity
        targetSpeed *= 1.0 - avoidanceUrgency * OBSTACLE_SPEED_REDUCTION

        val socialVertical = (separation.y * settings.separationWeight +
            alignment.y * alignmentWeight +
            cohesion.y * cohesionWeight +
            environmentalInfluence.y + state.noiseDirection.y * noiseWeight) *
            SOCIAL_VERTICAL_FORCE_SCALE
        val socialVerticalVelocity = (socialVertical * settings.verticalMovementWeight)
            .coerceIn(-MAXIMUM_SOCIAL_VERTICAL_SPEED, MAXIMUM_SOCIAL_VERTICAL_SPEED)
        var desiredY = currentVelocity.y * VERTICAL_MOMENTUM_RETENTION +
            socialVerticalVelocity +
            calculateAmbientVerticalDrift(fish, level.gameTime) * settings.verticalDriftSpeed +
            obstacleAvoidance.y * settings.obstacleAvoidanceWeight
        desiredY = desiredY.coerceIn(-MAXIMUM_VERTICAL_SPEED, MAXIMUM_VERTICAL_SPEED)

        val horizontalSpeed = sqrt(max(0.0, targetSpeed * targetSpeed - desiredY * desiredY))
        desiredX = desiredDirection.x * horizontalSpeed
        desiredZ = desiredDirection.z * horizontalSpeed
        return Vec3(desiredX, desiredY, desiredZ)
    }

    private fun calculateEntityAvoidance(
        fish: AbstractFish,
        nearbyLargeEntities: List<LivingEntity>
    ): Vec3 {
        var avoidanceX = 0.0
        var avoidanceZ = 0.0
        val predictedFishX = fish.x + fish.deltaMovement.x * ENTITY_COLLISION_PREDICTION_TICKS
        val predictedFishZ = fish.z + fish.deltaMovement.z * ENTITY_COLLISION_PREDICTION_TICKS

        for (entity in nearbyLargeEntities) {
            if (!entity.isAlive || entity.isRemoved) continue
            val predictedEntityX = entity.x +
                entity.deltaMovement.x * ENTITY_COLLISION_PREDICTION_TICKS
            val predictedEntityZ = entity.z +
                entity.deltaMovement.z * ENTITY_COLLISION_PREDICTION_TICKS
            val dx = predictedFishX - predictedEntityX
            val dz = predictedFishZ - predictedEntityZ
            val distance = sqrt(dx * dx + dz * dz)
            val requiredDistance =
                (fish.bbWidth + entity.bbWidth) * 0.5 + LARGE_ENTITY_CLEARANCE
            val influenceDistance = requiredDistance + LARGE_ENTITY_ANTICIPATION_DISTANCE
            if (distance >= influenceDistance) continue

            val safeDistance = max(distance, MINIMUM_ENTITY_DISTANCE)
            val strength = ((influenceDistance - distance) / influenceDistance).let { it * it }
            avoidanceX += dx / safeDistance * strength
            avoidanceZ += dz / safeDistance * strength
        }
        return limitMagnitude(Vec3(avoidanceX, 0.0, avoidanceZ), MAXIMUM_ENTITY_AVOIDANCE)
    }

    private fun calculateBlockAvoidance(
        level: ServerLevel,
        state: FishCollectiveState,
        requestedDirection: Vec3
    ): Vec3 {
        val fish = state.fish
        val forward = requestedDirection.takeIf {
            it.lengthSqr() > MINIMUM_DIRECTION_LENGTH_SQR
        } ?: horizontalUnit(fish.deltaMovement.x, fish.deltaMovement.z)
        if (forward.lengthSqr() <= MINIMUM_DIRECTION_LENGTH_SQR) return Vec3.ZERO

        val forwardClearance = probeClearance(level, fish, forward)
        var horizontalAvoidance = Vec3.ZERO
        if (forwardClearance < 1.0) {
            var bestDirection = forward
            var bestScore = forwardClearance
            var foundAlternative = false
            val preferPositiveTurn = sin(state.movementPhase) >= 0.0

            for (baseAngle in ALTERNATIVE_PROBE_ANGLES) {
                val firstAngle = if (preferPositiveTurn) baseAngle else -baseAngle
                val secondAngle = -firstAngle

                val firstDirection = rotateHorizontal(forward, firstAngle)
                val firstClearance = probeClearance(level, fish, firstDirection)
                val firstScore = firstClearance - abs(firstAngle) * PROBE_TURN_PENALTY
                if (firstScore > bestScore) {
                    bestScore = firstScore
                    bestDirection = firstDirection
                    foundAlternative = true
                }

                val secondDirection = rotateHorizontal(forward, secondAngle)
                val secondClearance = probeClearance(level, fish, secondDirection)
                val secondScore = secondClearance - abs(secondAngle) * PROBE_TURN_PENALTY
                if (secondScore > bestScore) {
                    bestScore = secondScore
                    bestDirection = secondDirection
                    foundAlternative = true
                }
            }

            val urgency = (1.0 - forwardClearance).coerceIn(0.0, 1.0)
            horizontalAvoidance = if (!foundAlternative) {
                Vec3(-forward.x * BLOCKED_REVERSE_WEIGHT, 0.0, -forward.z * BLOCKED_REVERSE_WEIGHT)
            } else {
                bestDirection.subtract(forward).scale(urgency)
            }
        }

        val verticalProbeX = forward.x * VERTICAL_PROBE_FORWARD_DISTANCE
        val verticalProbeZ = forward.z * VERTICAL_PROBE_FORWARD_DISTANCE
        var verticalAvoidance = 0.0
        if (!level.noBlockCollision(
                fish,
                fish.boundingBox.move(verticalProbeX, -DOWNWARD_COLLISION_PROBE, verticalProbeZ)
            )
        ) {
            verticalAvoidance += FLOOR_VERTICAL_CORRECTION
        }
        if (!level.noBlockCollision(
                fish,
                fish.boundingBox.move(verticalProbeX, UPWARD_COLLISION_PROBE, verticalProbeZ)
            )
        ) {
            verticalAvoidance -= CEILING_VERTICAL_CORRECTION
        }

        val floorProbe = BlockPos.containing(
            fish.x + verticalProbeX,
            fish.boundingBox.minY - FLOOR_FLUID_PROBE_DISTANCE,
            fish.z + verticalProbeZ
        )
        if (!level.getFluidState(floorProbe).`is`(FluidTags.WATER)) {
            verticalAvoidance += FLOOR_VERTICAL_CORRECTION
        }
        val surfaceProbe = BlockPos.containing(
            fish.x + verticalProbeX,
            fish.boundingBox.maxY + SURFACE_FLUID_PROBE_DISTANCE,
            fish.z + verticalProbeZ
        )
        if (!level.getFluidState(surfaceProbe).`is`(FluidTags.WATER)) {
            verticalAvoidance -= SURFACE_VERTICAL_CORRECTION
        }

        return Vec3(
            horizontalAvoidance.x,
            verticalAvoidance.coerceIn(-MAXIMUM_VERTICAL_AVOIDANCE, MAXIMUM_VERTICAL_AVOIDANCE),
            horizontalAvoidance.z
        )
    }

    private fun probeClearance(
        level: ServerLevel,
        fish: AbstractFish,
        direction: Vec3
    ): Double {
        var clearProbeCount = 0
        for (distance in HORIZONTAL_PROBE_DISTANCES) {
            val offsetX = direction.x * distance
            val offsetZ = direction.z * distance
            if (!level.noBlockCollision(fish, fish.boundingBox.move(offsetX, 0.0, offsetZ))) {
                break
            }

            val waterProbe = BlockPos.containing(
                fish.x + offsetX,
                fish.eyeY,
                fish.z + offsetZ
            )
            if (!level.getFluidState(waterProbe).`is`(FluidTags.WATER)) break
            clearProbeCount++
        }
        return clearProbeCount.toDouble() / HORIZONTAL_PROBE_DISTANCES.size.toDouble()
    }

    private fun rotateHorizontal(direction: Vec3, angle: Double): Vec3 {
        val currentAngle = atan2(direction.z, direction.x) + angle
        return Vec3(cos(currentAngle), 0.0, sin(currentAngle))
    }

    private fun calculateAmbientWander(
        fish: AbstractFish,
        gameTime: Long
    ): Vec3 {
        val time = gameTime.toDouble()
        val flowX = cos(
            fish.z * AMBIENT_WANDER_SPATIAL_FREQUENCY +
                time * AMBIENT_WANDER_X_TIME_FREQUENCY
        )
        val flowZ = sin(
            fish.x * AMBIENT_WANDER_SPATIAL_FREQUENCY -
                time * AMBIENT_WANDER_Z_TIME_FREQUENCY +
                AMBIENT_WANDER_PHASE_OFFSET
        )
        return horizontalUnit(flowX, flowZ)
    }

    private fun calculateHerdInfluence(
        state: FishCollectiveState,
        centroidDirection: Vec3,
        threatIntensity: Double
    ): Vec3 {
        if (threatIntensity <= 0.0) return Vec3.ZERO

        val compression = threatIntensity * (1.0 - threatIntensity) * HERD_COMPRESSION
        return Vec3(
            state.threatDirection.x * threatIntensity + centroidDirection.x * compression,
            0.0,
            state.threatDirection.z * threatIntensity + centroidDirection.z * compression
        )
    }

    private fun calculateAmbientVerticalDrift(
        fish: AbstractFish,
        gameTime: Long
    ): Double {
        val time = gameTime.toDouble()
        return sin(
            fish.x * AMBIENT_VERTICAL_SPATIAL_FREQUENCY +
                fish.z * AMBIENT_VERTICAL_SPATIAL_FREQUENCY +
                time * AMBIENT_VERTICAL_TIME_FREQUENCY +
                AMBIENT_VERTICAL_PHASE_OFFSET
        )
    }

    private fun calculateAggregationInfluence(
        state: FishCollectiveState,
        settings: FishCollectiveSettings
    ): Vec3 {
        val fish = state.fish
        val aggregationNeighbor = state.aggregationNeighbor ?: return Vec3.ZERO
        if (
            !aggregationNeighbor.isAlive ||
            !aggregationNeighbor.isInWater ||
            aggregationNeighbor.level() !== fish.level()
        ) {
            return Vec3.ZERO
        }

        val offsetX = aggregationNeighbor.x - fish.x
        val offsetZ = aggregationNeighbor.z - fish.z
        val horizontalDistance = sqrt(offsetX * offsetX + offsetZ * offsetZ)
        if (
            horizontalDistance <= settings.neighborSearchRadius ||
            horizontalDistance > settings.aggregationRadius ||
            horizontalDistance <= MINIMUM_AGGREGATION_DISTANCE
        ) {
            return Vec3.ZERO
        }

        val aggregationSpan =
            (settings.aggregationRadius - settings.neighborSearchRadius)
                .coerceAtLeast(MINIMUM_AGGREGATION_DISTANCE)
        val distanceProgress = (
            (horizontalDistance - settings.neighborSearchRadius) / aggregationSpan
            ).coerceIn(0.0, 1.0)
        val strength = MINIMUM_AGGREGATION_STRENGTH +
            (1.0 - MINIMUM_AGGREGATION_STRENGTH) * distanceProgress
        val affinity = FishSchoolCompatibility.socialAffinity(
            fish,
            aggregationNeighbor,
            settings
        )
        return Vec3(
            offsetX / horizontalDistance * strength * affinity,
            0.0,
            offsetZ / horizontalDistance * strength * affinity
        )
    }

    private fun horizontalUnit(x: Double, z: Double): Vec3 {
        val lengthSqr = x * x + z * z
        if (lengthSqr <= MINIMUM_DIRECTION_LENGTH_SQR) return Vec3.ZERO
        val inverseLength = 1.0 / sqrt(lengthSqr)
        return Vec3(x * inverseLength, 0.0, z * inverseLength)
    }

    private fun horizontalLength(vector: Vec3): Double {
        return sqrt(vector.x * vector.x + vector.z * vector.z)
    }

    private fun limitMagnitude(vector: Vec3, maximumLength: Double): Vec3 {
        val lengthSqr = vector.lengthSqr()
        if (lengthSqr <= maximumLength * maximumLength) return vector
        return vector.scale(maximumLength / sqrt(lengthSqr))
    }

    private val HORIZONTAL_PROBE_DISTANCES = doubleArrayOf(0.7, 1.25, 1.85)
    private val ALTERNATIVE_PROBE_ANGLES = doubleArrayOf(
        PI / 7.2,
        PI / 4.0,
        PI / 2.4
    )

    private const val MINIMUM_DIRECTION_LENGTH_SQR = 1.0E-10
    private const val COLLISION_PREDICTION_TICKS = 3.0
    private const val ENTITY_COLLISION_PREDICTION_TICKS = 2.0
    private const val SEPARATION_RADIUS_SQR = 5.0625
    private const val MINIMUM_SEPARATION_DISTANCE_SQR = 0.09
    private const val MAXIMUM_SEPARATION_FORCE = 2.4
    private const val ALIGNMENT_VERTICAL_SCALE = 3.0
    private const val MAXIMUM_LOCAL_VERTICAL_ERROR = 1.0
    private const val COHESION_FULL_STRENGTH_DISTANCE = 3.0
    private const val MAXIMUM_ENVIRONMENTAL_INFLUENCE = 2.0
    private const val BASE_NOISE_WEIGHT = 0.14
    private const val MAXIMUM_SWIM_WIGGLE_ANGLE = PI / 8.0
    private const val NOISE_DENSITY_REDUCTION_PER_NEIGHBOR = 0.08
    private const val MINIMUM_DENSITY_NOISE_FACTOR = 0.5
    private const val AMBIENT_WANDER_WEIGHT = 0.16
    private const val PANIC_AMBIENT_WANDER_REDUCTION = 0.9
    private const val AMBIENT_WANDER_SPATIAL_FREQUENCY = 0.025
    private const val AMBIENT_WANDER_X_TIME_FREQUENCY = 0.0023
    private const val AMBIENT_WANDER_Z_TIME_FREQUENCY = 0.0017
    private const val AMBIENT_WANDER_PHASE_OFFSET = PI / 3.0
    private const val AMBIENT_VERTICAL_SPATIAL_FREQUENCY = 0.02
    private const val AMBIENT_VERTICAL_TIME_FREQUENCY = 0.006
    private const val AMBIENT_VERTICAL_PHASE_OFFSET = PI / 5.0
    private const val AGGREGATION_COHESION_SCALE = 0.65
    private const val PANIC_AGGREGATION_REDUCTION = 0.5
    private const val MINIMUM_AGGREGATION_STRENGTH = 0.25
    private const val MINIMUM_AGGREGATION_DISTANCE = 1.0E-4
    private const val PANIC_NOISE_REDUCTION = 0.9
    private const val PANIC_ALIGNMENT_INCREASE = 0.45
    private const val HERD_COMPRESSION = 0.6
    private const val AVOIDANCE_ALIGNMENT_REDUCTION = 0.75
    private const val DIRECT_THREAT_ALIGNMENT_FACTOR = 0.45
    private const val NORMAL_CRUISING_SPEED_FACTOR = 0.72
    private const val OBSTACLE_SPEED_REDUCTION = 0.18
    private const val SOCIAL_VERTICAL_FORCE_SCALE = 0.1
    private const val MAXIMUM_SOCIAL_VERTICAL_SPEED = 0.012
    private const val VERTICAL_MOMENTUM_RETENTION = 0.65
    private const val MAXIMUM_VERTICAL_SPEED = 0.028
    private const val LARGE_ENTITY_CLEARANCE = 0.8
    private const val LARGE_ENTITY_ANTICIPATION_DISTANCE = 1.5
    private const val MINIMUM_ENTITY_DISTANCE = 0.25
    private const val MAXIMUM_ENTITY_AVOIDANCE = 1.8
    private const val CURRENT_DIRECTION_PROBE_WEIGHT = 0.75
    private const val INTENDED_DIRECTION_PROBE_WEIGHT = 0.25
    private const val PROBE_TURN_PENALTY = 0.08
    private const val BLOCKED_REVERSE_WEIGHT = 0.55
    private const val VERTICAL_PROBE_FORWARD_DISTANCE = 0.65
    private const val DOWNWARD_COLLISION_PROBE = 0.28
    private const val UPWARD_COLLISION_PROBE = 0.35
    private const val FLOOR_FLUID_PROBE_DISTANCE = 0.35
    private const val SURFACE_FLUID_PROBE_DISTANCE = 0.55
    private const val FLOOR_VERTICAL_CORRECTION = 0.055
    private const val CEILING_VERTICAL_CORRECTION = 0.065
    private const val SURFACE_VERTICAL_CORRECTION = 0.075
    private const val MAXIMUM_VERTICAL_AVOIDANCE = 0.1
}
