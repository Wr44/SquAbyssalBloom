package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluence
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluenceContext
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RedSlobbererFishInfluence(
    private val redSlobberer: RedSlobbererEntity
) : FishSchoolInfluence {

    private companion object {
        const val MAXIMUM_INFLUENCE_DISTANCE = 16.0
        const val MAXIMUM_INFLUENCE_DISTANCE_SQR =
            MAXIMUM_INFLUENCE_DISTANCE * MAXIMUM_INFLUENCE_DISTANCE
        const val MINIMUM_BODY_CLEARANCE = 1.15
        const val PREFERRED_BODY_DISTANCE = 5.5
        const val EDGE_TAPER_WIDTH = 4.0
        const val STRONG_REPULSION = 2.4
        const val RING_CORRECTION = 0.38
        const val LONG_RANGE_ATTRACTION = 1.1
        const val RECRUITMENT_RAMP = 2.0
        const val TANGENTIAL_FLOW = 0.32
        const val PANIC_REFUGE_INCREASE = 0.25
        const val PANIC_TANGENT_REDUCTION = 0.6
        const val PREFERRED_HEIGHT_OFFSET = 1.5
        const val MAXIMUM_HEIGHT_ERROR = 3.0
        const val VERTICAL_HOLD = 0.8
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

        val centerOffsetX = fish.x - redSlobberer.x
        val centerOffsetZ = fish.z - redSlobberer.z
        val centerDistanceSqr = centerOffsetX * centerOffsetX + centerOffsetZ * centerOffsetZ
        if (centerDistanceSqr >= MAXIMUM_INFLUENCE_DISTANCE_SQR) return Vec3.ZERO

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
        val edgeTaper = ((MAXIMUM_INFLUENCE_DISTANCE - centerDistance) / EDGE_TAPER_WIDTH)
            .coerceIn(0.0, 1.0)
        val radialStrength = when {
            bodyDistance < MINIMUM_BODY_CLEARANCE -> {
                val proximity = 1.0 - bodyDistance / MINIMUM_BODY_CLEARANCE
                STRONG_REPULSION * proximity * proximity
            }
            bodyDistance <= PREFERRED_BODY_DISTANCE ->
                (PREFERRED_BODY_DISTANCE - bodyDistance) / PREFERRED_BODY_DISTANCE *
                    RING_CORRECTION * edgeTaper
            else ->
                -((bodyDistance - PREFERRED_BODY_DISTANCE) / RECRUITMENT_RAMP)
                    .coerceAtMost(1.0) * LONG_RANGE_ATTRACTION * edgeTaper
        }

        val panic = context.threatIntensity.coerceIn(0.0, 1.0)
        val attractionScale = 1.0 + panic * PANIC_REFUGE_INCREASE
        val tangentScale = (1.0 - panic * PANIC_TANGENT_REDUCTION).coerceAtLeast(0.25)
        val tangentSign = if (usesClockwiseOrbit(fish)) -1.0 else 1.0
        val tangentStrength = TANGENTIAL_FLOW * edgeTaper * tangentScale
        val heightError = (redSlobberer.y + PREFERRED_HEIGHT_OFFSET - fish.y)
            .coerceIn(-MAXIMUM_HEIGHT_ERROR, MAXIMUM_HEIGHT_ERROR)
        val verticalHold = heightError / MAXIMUM_HEIGHT_ERROR * VERTICAL_HOLD * edgeTaper

        return Vec3(
            radialX * radialStrength * attractionScale - radialZ * tangentStrength * tangentSign,
            verticalHold,
            radialZ * radialStrength * attractionScale + radialX * tangentStrength * tangentSign
        )
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
