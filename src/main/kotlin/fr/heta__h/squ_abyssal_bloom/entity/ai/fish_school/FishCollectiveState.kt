package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluence
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FishCollectiveState(
    val fish: AbstractFish
) {

    companion object {
        const val PHASE_MASK = 0xFFFFL
        const val MINIMUM_DIRECTION_LENGTH_SQR = 1.0E-10
        const val MINIMUM_THREAT_SIGNAL = 1.0E-3
        const val MAXIMUM_NOISE_VERTICAL_COMPONENT = 0.14
        const val MINIMUM_NOISE_LIFETIME_TICKS = 20
        const val NOISE_LIFETIME_VARIATION_TICKS = 21
        const val MAXIMUM_NOISE_TURN_PER_REFRESH = PI / 3.0
        const val INDIVIDUAL_SPEED_VARIATION = 0.1
        const val MINIMUM_WIGGLE_PERIOD_TICKS = 26.0
        const val WIGGLE_PERIOD_VARIATION_TICKS = 8.0
        const val MINIMUM_ACTUAL_MOVEMENT_LENGTH_SQR = 1.0E-6
        const val MINIMUM_HORIZONTAL_MOVEMENT = 1.0E-4
        const val MAXIMUM_FISH_PITCH = 60.0f
        const val MAXIMUM_PITCH_CHANGE = 4.0f
    }

    val neighbors: MutableList<AbstractFish> = ArrayList()
    val nearbyCollisionFish: MutableList<AbstractFish> = ArrayList()
    val influences: MutableList<FishSchoolInfluence> = ArrayList()
    val nearbyLargeEntities: MutableList<LivingEntity> = ArrayList()
    var aggregationNeighbor: AbstractFish? = null

    val isUnderCustomControl: Boolean
        get() = collectiveActive || hasActiveAggregationNeighbor || hasActiveInfluence

    private val hasActiveAggregationNeighbor: Boolean
        get() {
            val neighbor = aggregationNeighbor ?: return false
            return neighbor.isAlive &&
                neighbor.isInWater &&
                !neighbor.isRemoved &&
                neighbor.level() === fish.level()
        }

    private val hasActiveInfluence: Boolean
        get() = influences.any { it.isActive(fish) }

    var desiredVelocity: Vec3 = Vec3.ZERO
    var lastSteeringTick: Long = Long.MIN_VALUE
    var debugLastWiggleAngle: Double = 0.0
    var debugLastAppliedTurnDegrees: Double = Double.NaN
    var debugLastWrittenHeadingDegrees: Double = Double.NaN
    var lastObservedTick: Long = Long.MIN_VALUE
        private set

    var collectiveActive: Boolean = false
        private set

    var movementControllerRunning: Boolean = false

    var smoothedBodyPitch: Float = 0.0f
        private set

    var localCompatibleFishCount: Int = 0
        private set

    val activationProgressTicks: Int
        get() = activationTicks

    val deactivationProgressTicks: Int
        get() = deactivationTicks

    var threatDirection: Vec3 = Vec3.ZERO
        private set

    var threatIntensity: Double = 0.0
        private set

    var directlyThreatened: Boolean = false
        private set

    var directThreatDirection: Vec3 = Vec3.ZERO
        private set

    var directThreatIntensity: Double = 0.0
        private set

    var noiseDirection: Vec3 = Vec3.ZERO
        private set

    var swimWigglePhase: Double = 0.0
        private set

    var avoidanceCoordinationDirection: Vec3 = Vec3.ZERO
        private set

    var obstacleAvoidanceDirection: Vec3 = Vec3.ZERO
        private set

    var obstacleAvoidanceRecordedTick: Long = Long.MIN_VALUE
        private set

    val movementPhase: Double = run {
        val mixedUuid = ModUtilities.mixedUuidBits(fish.uuid)
        ModUtilities.uuidFractionAsAngle(mixedUuid, mask = PHASE_MASK)
    }

    val individualSpeedFactor: Double = run {
        val mixedUuid = ModUtilities.mixedUuidBits(fish.uuid)
        val normalized = ModUtilities.normalizedUuidFraction(mixedUuid, shift = 16, mask = PHASE_MASK)
        1.0 + (normalized - 0.5) * 2.0 * INDIVIDUAL_SPEED_VARIATION
    }

    val swimWiggleAngularStep: Double = run {
        val mixedUuid = ModUtilities.mixedUuidBits(fish.uuid)
        val normalized = ModUtilities.normalizedUuidFraction(mixedUuid, shift = 32, mask = PHASE_MASK)
        2.0 * PI / (MINIMUM_WIGGLE_PERIOD_TICKS + normalized * WIGGLE_PERIOD_VARIATION_TICKS)
    }

    private var initialized = false
    private var hasRefreshedNeighborhood = false
    private var nextNeighborhoodRefreshTick = Long.MAX_VALUE
    private var nextThreatRefreshTick = Long.MAX_VALUE
    private var nextLongRangeRefreshTick = Long.MAX_VALUE
    private var lastNeighborhoodRefreshTick = Long.MIN_VALUE
    private var nextNoiseRefreshTick = Long.MIN_VALUE
    private var noiseAngle = 0.0
    private var lastWiggleAdvanceTick = Long.MIN_VALUE
    private var obstacleAvoidanceExpiresAtTick = Long.MIN_VALUE
    private var activationTicks = 0
    private var deactivationTicks = 0

    fun observe(
        gameTime: Long,
        neighborRefreshInterval: Int,
        threatRefreshInterval: Int,
        longRangeRefreshInterval: Int
    ) {
        lastObservedTick = gameTime
        if (initialized) return

        val mixedUuid = ModUtilities.mixedUuidBits(fish.uuid)
        nextNeighborhoodRefreshTick = gameTime + staggeredOffset(
            mixedUuid,
            neighborRefreshInterval,
            0
        )
        nextThreatRefreshTick = gameTime + staggeredOffset(
            mixedUuid,
            threatRefreshInterval,
            21
        )
        nextLongRangeRefreshTick = gameTime + staggeredOffset(
            mixedUuid,
            longRangeRefreshInterval,
            42
        )
        lastNeighborhoodRefreshTick = gameTime
        noiseAngle = movementPhase
        swimWigglePhase = movementPhase
        noiseDirection = Vec3(cos(noiseAngle), 0.0, sin(noiseAngle))
        nextNoiseRefreshTick = gameTime
        initialized = true
    }

    fun needsNeighborhoodRefresh(gameTime: Long): Boolean {
        return gameTime >= nextNeighborhoodRefreshTick
    }

    fun completeNeighborhoodRefresh(
        gameTime: Long,
        localNeighborCount: Int,
        settings: FishCollectiveSettings
    ) {
        localCompatibleFishCount = localNeighborCount
        val elapsedTicks = if (hasRefreshedNeighborhood) {
            (gameTime - lastNeighborhoodRefreshTick)
                .coerceIn(1L, (settings.neighborRefreshInterval * 2L).coerceAtLeast(1L))
                .toInt()
        } else {
            1
        }

        if (collectiveActive) {
            activationTicks = 0
            if (localNeighborCount <= settings.deactivationThreshold) {
                deactivationTicks += elapsedTicks
                if (deactivationTicks >= settings.deactivationDelayTicks) {
                    collectiveActive = false
                    deactivationTicks = 0
                    desiredVelocity = Vec3.ZERO
                }
            } else {
                deactivationTicks = 0
            }
        } else {
            deactivationTicks = 0
            if (localNeighborCount >= settings.activationThreshold) {
                activationTicks += elapsedTicks
                if (activationTicks >= settings.activationDelayTicks) {
                    collectiveActive = true
                    activationTicks = 0
                }
            } else {
                activationTicks = 0
            }
        }

        hasRefreshedNeighborhood = true
        lastNeighborhoodRefreshTick = gameTime
        nextNeighborhoodRefreshTick = gameTime + settings.neighborRefreshInterval.coerceAtLeast(1)
    }

    fun needsThreatRefresh(gameTime: Long): Boolean {
        return gameTime >= nextThreatRefreshTick
    }

    fun completeThreatRefresh(
        gameTime: Long,
        direction: Vec3,
        intensity: Double,
        refreshInterval: Int
    ) {
        val normalizedDirection = unit(direction)
        if (
            intensity <= MINIMUM_THREAT_SIGNAL ||
            normalizedDirection.lengthSqr() <= MINIMUM_DIRECTION_LENGTH_SQR
        ) {
            directThreatDirection = Vec3.ZERO
            directThreatIntensity = 0.0
        } else {
            directThreatDirection = normalizedDirection
            directThreatIntensity = intensity.coerceIn(0.0, 1.0)
        }
        nextThreatRefreshTick = gameTime + refreshInterval.coerceAtLeast(1)
    }

    fun needsLongRangeRefresh(gameTime: Long): Boolean {
        return gameTime >= nextLongRangeRefreshTick
    }

    fun completeLongRangeRefresh(gameTime: Long, refreshInterval: Int) {
        nextLongRangeRefreshTick = gameTime + refreshInterval.coerceAtLeast(1)
    }

    fun applyThreatSignal(
        direction: Vec3,
        intensity: Double,
        isDirect: Boolean
    ) {
        val normalizedDirection = unit(direction)
        if (
            intensity <= MINIMUM_THREAT_SIGNAL ||
            normalizedDirection.lengthSqr() <= MINIMUM_DIRECTION_LENGTH_SQR
        ) {
            threatDirection = Vec3.ZERO
            threatIntensity = 0.0
            directlyThreatened = false
            return
        }
        threatDirection = normalizedDirection
        threatIntensity = intensity.coerceIn(0.0, 1.0)
        directlyThreatened = isDirect
    }

    fun activeObstacleAvoidance(gameTime: Long): Vec3 {
        if (gameTime > obstacleAvoidanceExpiresAtTick) {
            clearObstacleAvoidance()
        }
        return obstacleAvoidanceDirection
    }

    fun rememberObstacleAvoidance(
        direction: Vec3,
        gameTime: Long,
        lifetimeTicks: Int
    ) {
        val normalizedDirection = unit(direction)
        if (normalizedDirection.lengthSqr() <= MINIMUM_DIRECTION_LENGTH_SQR) {
            clearObstacleAvoidance()
            return
        }
        obstacleAvoidanceDirection = normalizedDirection
        obstacleAvoidanceRecordedTick = gameTime
        obstacleAvoidanceExpiresAtTick = gameTime + lifetimeTicks.coerceAtLeast(1)
    }

    fun setAvoidanceCoordinationDirection(direction: Vec3) {
        avoidanceCoordinationDirection = unit(direction)
    }

    fun clearObstacleAvoidance() {
        obstacleAvoidanceDirection = Vec3.ZERO
        obstacleAvoidanceRecordedTick = Long.MIN_VALUE
        obstacleAvoidanceExpiresAtTick = Long.MIN_VALUE
    }

    fun refreshNoiseIfNeeded(gameTime: Long) {
        if (gameTime < nextNoiseRefreshTick) return

        val turn = (fish.random.nextDouble() - 0.5) * MAXIMUM_NOISE_TURN_PER_REFRESH
        noiseAngle += turn
        val verticalNoise = (fish.random.nextDouble() - 0.5) * MAXIMUM_NOISE_VERTICAL_COMPONENT
        noiseDirection = Vec3(cos(noiseAngle), verticalNoise, sin(noiseAngle))
        nextNoiseRefreshTick = gameTime + MINIMUM_NOISE_LIFETIME_TICKS +
            fish.random.nextInt(NOISE_LIFETIME_VARIATION_TICKS)
    }

    fun advanceSwimWiggle(gameTime: Long) {
        if (gameTime == lastWiggleAdvanceTick) return
        lastWiggleAdvanceTick = gameTime
        swimWigglePhase += swimWiggleAngularStep
    }

    fun updateBodyOrientation(
        maximumYawChange: Float,
        debugEnabled: Boolean
    ) {
        val movementX = fish.x - fish.xo
        val movementY = fish.y - fish.yo
        val movementZ = fish.z - fish.zo
        val pitchBefore = smoothedBodyPitch
        val movementLengthSqr =
            movementX * movementX +
                movementY * movementY +
                movementZ * movementZ
        if (movementLengthSqr < MINIMUM_ACTUAL_MOVEMENT_LENGTH_SQR) {
            smoothedBodyPitch = Mth.approachDegrees(
                smoothedBodyPitch,
                0.0f,
                MAXIMUM_PITCH_CHANGE
            )
            fish.xRot = smoothedBodyPitch
            if (debugEnabled) {
                FishCollectiveDebugger.logBodyOrientation(
                    fish = fish,
                    gameTime = fish.level().gameTime,
                    movementX = movementX,
                    movementY = movementY,
                    movementZ = movementZ,
                    targetPitch = 0.0f,
                    pitchBefore = pitchBefore,
                    pitchAfter = fish.xRot,
                    targetYaw = null,
                    stationary = true
                )
            }
            return
        }

        val horizontalMovement = sqrt(
            movementX * movementX + movementZ * movementZ
        )
        var targetYaw: Float? = null
        if (horizontalMovement > MINIMUM_HORIZONTAL_MOVEMENT) {
            val calculatedTargetYaw = (
                atan2(movementZ, movementX) * 180.0 / Math.PI
                ).toFloat() - 90.0f
            targetYaw = calculatedTargetYaw
            fish.yRot = Mth.approachDegrees(
                fish.yRot,
                calculatedTargetYaw,
                maximumYawChange
            )
            fish.yBodyRot = Mth.approachDegrees(
                fish.yBodyRot,
                fish.yRot,
                maximumYawChange
            )
            fish.yHeadRot = Mth.approachDegrees(
                fish.yHeadRot,
                fish.yRot,
                maximumYawChange
            )
        }

        val targetPitch = (
            -(atan2(movementY, horizontalMovement) * 180.0 / Math.PI)
            ).toFloat().coerceIn(-MAXIMUM_FISH_PITCH, MAXIMUM_FISH_PITCH)
        smoothedBodyPitch = Mth.approachDegrees(
            smoothedBodyPitch,
            targetPitch,
            MAXIMUM_PITCH_CHANGE
        )
        fish.xRot = smoothedBodyPitch
        if (debugEnabled) {
            FishCollectiveDebugger.logBodyOrientation(
                fish = fish,
                gameTime = fish.level().gameTime,
                movementX = movementX,
                movementY = movementY,
                movementZ = movementZ,
                targetPitch = targetPitch,
                pitchBefore = pitchBefore,
                pitchAfter = fish.xRot,
                targetYaw = targetYaw,
                stationary = false
            )
        }
    }

    fun resetBodyOrientation() {
        smoothedBodyPitch = 0.0f
        fish.xRot = 0.0f
    }

    private fun staggeredOffset(
        mixedUuid: Long,
        interval: Int,
        rotation: Int
    ): Long {
        val safeInterval = interval.coerceAtLeast(1)
        val rotated = java.lang.Long.rotateRight(mixedUuid, rotation)
        return Math.floorMod(rotated, safeInterval.toLong())
    }

    private fun unit(vector: Vec3): Vec3 {
        val lengthSqr = vector.lengthSqr()
        if (lengthSqr <= MINIMUM_DIRECTION_LENGTH_SQR) return Vec3.ZERO
        return vector.scale(1.0 / sqrt(lengthSqr))
    }
}
