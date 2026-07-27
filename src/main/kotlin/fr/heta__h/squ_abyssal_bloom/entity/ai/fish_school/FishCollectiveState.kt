package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluence
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

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
    }

    val neighbors: MutableList<AbstractFish> = ArrayList()
    val influences: MutableList<FishSchoolInfluence> = ArrayList()
    val nearbyLargeEntities: MutableList<LivingEntity> = ArrayList()
    var aggregationNeighbor: AbstractFish? = null

    val isUnderCustomControl: Boolean
        get() = collectiveActive || aggregationNeighbor != null || hasActiveInfluence

    private val hasActiveInfluence: Boolean
        get() = influences.any { !it.source.isRemoved && it.source.isAlive }

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

    var noiseDirection: Vec3 = Vec3.ZERO
        private set

    var swimWigglePhase: Double = 0.0
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
    private var lastNeighborhoodRefreshTick = Long.MIN_VALUE
    private var lastThreatDecayTick = Long.MIN_VALUE
    private var nextNoiseRefreshTick = Long.MIN_VALUE
    private var noiseAngle = 0.0
    private var lastWiggleAdvanceTick = Long.MIN_VALUE
    private var activationTicks = 0
    private var deactivationTicks = 0

    fun observe(gameTime: Long, refreshInterval: Int) {
        lastObservedTick = gameTime
        if (initialized) return

        val interval = refreshInterval.coerceAtLeast(1)
        val mixedUuid = ModUtilities.mixedUuidBits(fish.uuid)
        val offset = Math.floorMod(mixedUuid, interval.toLong())
        nextNeighborhoodRefreshTick = gameTime + offset
        lastNeighborhoodRefreshTick = gameTime
        lastThreatDecayTick = gameTime
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

    fun decayThreatSignal(gameTime: Long, decayPerTick: Double) {
        if (lastThreatDecayTick == Long.MIN_VALUE) {
            lastThreatDecayTick = gameTime
            return
        }
        val elapsedTicks = gameTime - lastThreatDecayTick
        if (elapsedTicks <= 0L) return

        threatIntensity = max(0.0, threatIntensity - decayPerTick * elapsedTicks)
        if (threatIntensity <= MINIMUM_THREAT_SIGNAL) {
            threatIntensity = 0.0
            threatDirection = Vec3.ZERO
        }
        lastThreatDecayTick = gameTime
    }

    fun beginThreatRefresh() {
        directlyThreatened = false
    }

    fun receiveDirectThreat(direction: Vec3, intensity: Double) {
        val normalizedDirection = horizontalUnit(direction)
        if (normalizedDirection.lengthSqr() <= MINIMUM_DIRECTION_LENGTH_SQR) return

        directlyThreatened = true
        threatDirection = normalizedDirection
        threatIntensity = max(threatIntensity, intensity.coerceIn(0.0, 1.0))
    }

    fun receivePropagatedThreat(direction: Vec3, intensity: Double) {
        if (intensity <= threatIntensity) return
        val normalizedDirection = horizontalUnit(direction)
        if (normalizedDirection.lengthSqr() <= MINIMUM_DIRECTION_LENGTH_SQR) return

        threatDirection = normalizedDirection
        threatIntensity = intensity.coerceIn(0.0, 1.0)
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

    private fun horizontalUnit(vector: Vec3): Vec3 {
        val horizontalLengthSqr = vector.x * vector.x + vector.z * vector.z
        if (horizontalLengthSqr <= MINIMUM_DIRECTION_LENGTH_SQR) return Vec3.ZERO
        val inverseLength = 1.0 / kotlin.math.sqrt(horizontalLengthSqr)
        return Vec3(vector.x * inverseLength, 0.0, vector.z * inverseLength)
    }
}
