package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RedSlobbererBabyClimbAlignment(
    private val redSlobberer: RedSlobbererEntity
) {

    private companion object {
        const val PROBE_FORWARD_MARGIN = 0.05
        const val PROBE_HEIGHT_FRACTION = 0.5
        const val MAX_CLIMB_HEIGHT = 1.05
        const val CLIMB_RISE_PER_TICK = 0.1
        const val EXIT_EPSILON = 0.02
        const val MAX_NORMAL_ROTATION_DEGREES_PER_TICK = 12.0
        const val MIN_NORMAL_ROTATION_DEGREES = 0.5
        const val MIN_VECTOR_LENGTH_SQR = 1.0E-8

        val UP: Vec3 = Vec3(0.0, 1.0, 0.0)
        val MAX_NORMAL_ROTATION_RADIANS_PER_TICK: Double = MAX_NORMAL_ROTATION_DEGREES_PER_TICK * PI / 180.0
        val MIN_NORMAL_ROTATION_RADIANS: Double = MIN_NORMAL_ROTATION_DEGREES * PI / 180.0
    }

    private enum class State {
        HORIZONTAL,
        CLIMBING
    }

    private var state = State.HORIZONTAL
    private var targetTopY = 0.0
    private var climbWallNormal: Vec3 = UP

    private var previousNormal: Vec3 = UP
    private var currentNormal: Vec3 = UP

    fun tick() {
        previousNormal = currentNormal

        if (state == State.CLIMBING && redSlobberer.boundingBox.minY >= targetTopY - EXIT_EPSILON) {
            state = State.HORIZONTAL
        }
        if (state == State.HORIZONTAL && redSlobberer.isInLocomotionPropulsionPhase) {
            tryStartClimb()
        }

        val targetNormal = if (state == State.CLIMBING) climbWallNormal else UP
        currentNormal = smoothNormal(currentNormal, targetNormal)
    }

    fun getInterpolatedNormal(partialTick: Float): Vec3 {
        val alpha = partialTick.toDouble().coerceIn(0.0, 1.0)
        val interpolated = previousNormal.scale(1.0 - alpha).add(currentNormal.scale(alpha))
        return if (interpolated.lengthSqr() > MIN_VECTOR_LENGTH_SQR) interpolated.normalize() else UP
    }

    fun createTangentMovement(input: Vec3, speed: Float, yawDegrees: Float): Vec3? {
        return if (state == State.CLIMBING) Vec3(0.0, CLIMB_RISE_PER_TICK, 0.0) else null
    }

    private fun tryStartClimb() {
        val level = redSlobberer.level()
        val yawRadians = redSlobberer.yRot * PI / 180.0
        val dirX = -sin(yawRadians)
        val dirZ = cos(yawRadians)

        val probeDistance = redSlobberer.bbWidth / 2.0 + PROBE_FORWARD_MARGIN
        val bodyBottom = redSlobberer.boundingBox.minY
        val probeY = bodyBottom + redSlobberer.bbHeight * PROBE_HEIGHT_FRACTION
        val wallPos = BlockPos.containing(
            redSlobberer.x + dirX * probeDistance,
            probeY,
            redSlobberer.z + dirZ * probeDistance
        )

        if (level.getBlockState(wallPos).getCollisionShape(level, wallPos).isEmpty) return

        val ledgeTopY = wallPos.y + 1.0
        val rise = ledgeTopY - bodyBottom
        if (rise <= EXIT_EPSILON || rise > MAX_CLIMB_HEIGHT) return

        val abovePos = wallPos.above()
        if (!level.getBlockState(abovePos).getCollisionShape(level, abovePos).isEmpty) return

        targetTopY = ledgeTopY
        climbWallNormal = Vec3(-dirX, 0.0, -dirZ)
        state = State.CLIMBING
    }

    private fun smoothNormal(from: Vec3, target: Vec3): Vec3 {
        val angle = acos(from.dot(target).coerceIn(-1.0, 1.0))
        if (angle <= MIN_NORMAL_ROTATION_RADIANS) return from

        val blend = min(1.0, MAX_NORMAL_ROTATION_RADIANS_PER_TICK / angle)
        val blended = from.scale(1.0 - blend).add(target.scale(blend))
        return if (blended.lengthSqr() > MIN_VECTOR_LENGTH_SQR) blended.normalize() else target
    }
}
