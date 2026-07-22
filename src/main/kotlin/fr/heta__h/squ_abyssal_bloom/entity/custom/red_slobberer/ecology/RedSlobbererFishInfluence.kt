package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluence
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluenceContext
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.util.Mth
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RedSlobbererFishInfluence(
    private val redSlobberer: RedSlobbererEntity,
    private val isReefRefuge: Boolean,
    private val refugeInfluenceDistance: Double
) : FishSchoolInfluence {

    private companion object {
        const val NORMAL_INFLUENCE_DISTANCE = 16.0
        const val MINIMUM_BODY_CLEARANCE = 1.15
        const val CALM_PREFERRED_BODY_DISTANCE = 5.5
        const val NORMAL_EDGE_TAPER_WIDTH = 4.0
        const val REFUGE_EDGE_TAPER_WIDTH = 7.0
        const val STRONG_REPULSION = 2.4
        const val RING_CORRECTION = 0.38
        const val LONG_RANGE_ATTRACTION = 1.1
        const val RECRUITMENT_RAMP = 2.0
        const val TANGENTIAL_FLOW = 0.32
        const val CALM_HEIGHT_OFFSET = 1.5
        const val MAXIMUM_HEIGHT_ERROR = 4.0
        const val VERTICAL_HOLD = 1.4
        const val MINIMUM_REFUGE_SIGNAL = 0.03
        const val FULL_REFUGE_SIGNAL = 0.30
        const val REFUGE_CAPTURE_RADIUS = 0.85
        const val REFUGE_APPROACH_STRENGTH = 4.2
        const val REFUGE_HOLD_STRENGTH = 1.1
        const val REFUGE_VERTICAL_HOLD = 2.2
        const val MAXIMUM_THREAT_ESCAPE_SUPPRESSION = 1.0
        const val MAXIMUM_SEPARATION_SUPPRESSION = 0.78
        const val MAXIMUM_REFUGE_VERTICAL_SPEED_BONUS = 0.025
        const val MINIMUM_VECTOR_LENGTH = 1.0E-5
        const val ANGLE_MASK = 0xFFFFL
    }

    override val source: Entity
        get() = redSlobberer

    override fun computeInfluence(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Vec3 {
        if (!redSlobberer.isAlive || !redSlobberer.isUnderWater) return Vec3.ZERO

        val panic = if (isReefRefuge && redSlobberer.canShelterFish(fish)) {
            context.threatIntensity.coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val refugeBlend = refugeBlend(panic)
        val maximumInfluenceDistance = if (isReefRefuge) {
            refugeInfluenceDistance
        } else {
            NORMAL_INFLUENCE_DISTANCE
        }
        val maximumInfluenceDistanceSqr = maximumInfluenceDistance * maximumInfluenceDistance

        val centerOffsetX = fish.x - redSlobberer.x
        val centerOffsetZ = fish.z - redSlobberer.z
        val centerDistanceSqr = centerOffsetX * centerOffsetX + centerOffsetZ * centerOffsetZ
        if (centerDistanceSqr >= maximumInfluenceDistanceSqr) return Vec3.ZERO

        if (refugeBlend > 0.0) {
            val entrance = redSlobberer.fishRefugeEntrance(fish)
            (fish.level() as? ServerLevel)?.let { level ->
                RedSlobbererReefManager.forLevel(level)
                    .recordRefugeInfluence(fish, redSlobberer, panic, entrance)
                if (redSlobberer.tryShelterFish(level, fish, panic)) {
                    return Vec3.ZERO
                }
            }
            return computeRefugeApproach(
                fish,
                sqrt(centerDistanceSqr),
                maximumInfluenceDistance,
                refugeBlend,
                entrance
            )
        }

        val box = redSlobberer.boundingBox
        val nearestBodyX = Mth.clamp(fish.x, box.minX, box.maxX)
        val nearestBodyZ = Mth.clamp(fish.z, box.minZ, box.maxZ)
        var radialX = fish.x - nearestBodyX
        var radialZ = fish.z - nearestBodyZ
        var bodyDistance = sqrt(radialX * radialX + radialZ * radialZ)

        if (bodyDistance <= MINIMUM_VECTOR_LENGTH) {
            val centerDistance = sqrt(centerDistanceSqr)
            if (centerDistance > MINIMUM_VECTOR_LENGTH) {
                radialX = centerOffsetX / centerDistance
                radialZ = centerOffsetZ / centerDistance
            } else {
                val angle = fallbackAngle(fish)
                radialX = cos(angle)
                radialZ = sin(angle)
            }
            bodyDistance = 0.0
        } else {
            radialX /= bodyDistance
            radialZ /= bodyDistance
        }

        val centerDistance = sqrt(centerDistanceSqr)
        val edgeTaperWidth = if (isReefRefuge) REFUGE_EDGE_TAPER_WIDTH else NORMAL_EDGE_TAPER_WIDTH
        val edgeTaper = ((maximumInfluenceDistance - centerDistance) / edgeTaperWidth)
            .coerceIn(0.0, 1.0)
        val radialStrength = when {
            bodyDistance < MINIMUM_BODY_CLEARANCE -> {
                val proximity = 1.0 - bodyDistance / MINIMUM_BODY_CLEARANCE
                STRONG_REPULSION * proximity * proximity
            }
            bodyDistance <= CALM_PREFERRED_BODY_DISTANCE ->
                (CALM_PREFERRED_BODY_DISTANCE - bodyDistance) / CALM_PREFERRED_BODY_DISTANCE *
                    RING_CORRECTION * edgeTaper
            else ->
                -((bodyDistance - CALM_PREFERRED_BODY_DISTANCE) / RECRUITMENT_RAMP)
                    .coerceAtMost(1.0) * LONG_RANGE_ATTRACTION * edgeTaper
        }

        val tangentSign = if (usesClockwiseOrbit(fish)) -1.0 else 1.0
        val tangentStrength = TANGENTIAL_FLOW * edgeTaper
        val heightError = (redSlobberer.boundingBox.minY + CALM_HEIGHT_OFFSET - fish.y)
            .coerceIn(-MAXIMUM_HEIGHT_ERROR, MAXIMUM_HEIGHT_ERROR)
        val verticalHold = heightError / MAXIMUM_HEIGHT_ERROR * VERTICAL_HOLD * edgeTaper

        return Vec3(
            radialX * radialStrength - radialZ * tangentStrength * tangentSign,
            verticalHold,
            radialZ * radialStrength + radialX * tangentStrength * tangentSign
        )
    }

    private fun computeRefugeApproach(
        fish: AbstractFish,
        centerDistance: Double,
        maximumInfluenceDistance: Double,
        blend: Double,
        target: Vec3
    ): Vec3 {
        val offsetX = target.x - fish.x
        val offsetZ = target.z - fish.z
        val targetDistance = sqrt(offsetX * offsetX + offsetZ * offsetZ)
        val edgeTaper = ((maximumInfluenceDistance - centerDistance) / REFUGE_EDGE_TAPER_WIDTH)
            .coerceIn(0.0, 1.0)

        val horizontalStrength = if (targetDistance <= REFUGE_CAPTURE_RADIUS) {
            targetDistance / REFUGE_CAPTURE_RADIUS * REFUGE_HOLD_STRENGTH
        } else {
            REFUGE_HOLD_STRENGTH +
                (REFUGE_APPROACH_STRENGTH - REFUGE_HOLD_STRENGTH) *
                ((targetDistance - REFUGE_CAPTURE_RADIUS) / 3.0).coerceIn(0.0, 1.0)
        }
        val inverseTargetDistance = if (targetDistance > MINIMUM_VECTOR_LENGTH) {
            1.0 / targetDistance
        } else {
            0.0
        }
        val heightError = (target.y - fish.y)
            .coerceIn(-MAXIMUM_HEIGHT_ERROR, MAXIMUM_HEIGHT_ERROR)
        val verticalHold = heightError / MAXIMUM_HEIGHT_ERROR * REFUGE_VERTICAL_HOLD

        return Vec3(
            offsetX * inverseTargetDistance * horizontalStrength * edgeTaper * blend,
            verticalHold * edgeTaper * blend,
            offsetZ * inverseTargetDistance * horizontalStrength * edgeTaper * blend
        )
    }

    override fun threatEscapeSuppression(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Double {
        if (!isReefRefuge || !redSlobberer.isAlive || !redSlobberer.isUnderWater) return 0.0
        if (fish.distanceToSqr(redSlobberer) > refugeInfluenceDistance * refugeInfluenceDistance) {
            return 0.0
        }
        return refugeBlend(context.threatIntensity) * MAXIMUM_THREAT_ESCAPE_SUPPRESSION
    }

    override fun sourceEntityAvoidanceSuppression(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Double {
        if (!canUseRefuge(fish)) return 0.0
        return refugeBlend(context.threatIntensity)
    }

    override fun separationSuppression(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Double {
        if (!canUseRefuge(fish)) return 0.0
        return refugeBlend(context.threatIntensity) * MAXIMUM_SEPARATION_SUPPRESSION
    }

    override fun additionalVerticalSpeed(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Double {
        if (
            !isReefRefuge ||
            !redSlobberer.isAlive ||
            !redSlobberer.isUnderWater ||
            fish.distanceToSqr(redSlobberer) > refugeInfluenceDistance * refugeInfluenceDistance
        ) {
            return 0.0
        }
        return refugeBlend(context.threatIntensity) * MAXIMUM_REFUGE_VERTICAL_SPEED_BONUS
    }

    private fun canUseRefuge(fish: AbstractFish): Boolean =
        isReefRefuge &&
            redSlobberer.isAlive &&
            redSlobberer.isUnderWater &&
            redSlobberer.canShelterFish(fish) &&
            fish.distanceToSqr(redSlobberer) <= refugeInfluenceDistance * refugeInfluenceDistance

    private fun refugeBlend(threatIntensity: Double): Double {
        val linear = ((threatIntensity - MINIMUM_REFUGE_SIGNAL) /
            (FULL_REFUGE_SIGNAL - MINIMUM_REFUGE_SIGNAL)).coerceIn(0.0, 1.0)
        return linear * linear * (3.0 - 2.0 * linear)
    }

    private fun usesClockwiseOrbit(fish: AbstractFish): Boolean {
        val mixedUuid = fish.uuid.leastSignificantBits xor redSlobberer.uuid.mostSignificantBits
        return mixedUuid and 1L == 0L
    }

    private fun fallbackAngle(fish: AbstractFish): Double {
        val mixedUuid = fish.uuid.mostSignificantBits xor redSlobberer.uuid.leastSignificantBits
        val normalized = (mixedUuid and ANGLE_MASK).toDouble() / ANGLE_MASK.toDouble()
        return normalized * Math.PI * 2.0
    }

}
