package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class BarnacleMoveControl(private val barnacle: BarnacleEntity) : MoveControl(barnacle) {
    private var requestTick = Int.MIN_VALUE
    private var requestedSpeed = 0.0
    private var fallbackDirection = Vec3.ZERO
    private var steeringDirection = Vec3.ZERO
    private var steeringAngularSpeed = 0.0
    private var previousSteeringTarget = Vec3.ZERO

    fun requestPathVelocity(speed: Double, fallbackDirection: Vec3) {
        requestTick = barnacle.tickCount
        requestedSpeed = speed
        this.fallbackDirection = fallbackDirection
    }

    fun cancelRequest() {
        requestTick = Int.MIN_VALUE
        requestedSpeed = 0.0
        operation = Operation.WAIT
        barnacle.speed = 0.0f
        steeringAngularSpeed = 0.0
        previousSteeringTarget = Vec3.ZERO
    }

    override fun tick() {
        if (requestTick != barnacle.tickCount || requestedSpeed <= 0.0) {
            operation = Operation.WAIT
            barnacle.speed = 0.0f
            return
        }

        val followingPath = operation == Operation.MOVE_TO && barnacle.navigation.isInProgress
        val requestedDirection = if (followingPath) {
            Vec3(wantedX - barnacle.x, wantedY - barnacle.y, wantedZ - barnacle.z)
        } else {
            fallbackDirection
        }
        operation = Operation.WAIT

        if (requestedDirection.lengthSqr() < MIN_SPEED_SQR) {
            barnacle.deltaMovement = Vec3.ZERO
            barnacle.speed = 0.0f
            steeringAngularSpeed = 0.0
            previousSteeringTarget = Vec3.ZERO
            return
        }

        val desiredDirection = requestedDirection.normalize()
        val direction: Vec3
        val appliedSpeed: Double

        if (followingPath) {
            val currentDirection = steeringDirection.takeIf { it.lengthSqr() >= MIN_SPEED_SQR }
                ?: barnacle.getMovementDirection().takeIf { it.lengthSqr() >= MIN_SPEED_SQR }
                ?: desiredDirection

            direction = turnTowards(currentDirection, desiredDirection)
            steeringDirection = direction

            // Never cross a nearby waypoint in one impulse. Slowing while the requested
            // turn is still sharp also prevents a smoothed heading from driving into it.
            val waypointSpeedLimit = requestedDirection.length() * WAYPOINT_APPROACH_FACTOR
            val turnAlignment = direction.dot(desiredDirection).coerceIn(0.0, 1.0)
            appliedSpeed = minOf(requestedSpeed, MAX_PATH_SPEED, waypointSpeedLimit) * turnAlignment
        } else {
            direction = desiredDirection
            steeringDirection = direction
            steeringAngularSpeed = 0.0
            previousSteeringTarget = Vec3.ZERO
            appliedSpeed = requestedSpeed
        }

        barnacle.setMovementDirection(direction)
        if (appliedSpeed <= MIN_SPEED) {
            barnacle.deltaMovement = Vec3.ZERO
            barnacle.speed = 0.0f
            return
        }

        barnacle.deltaMovement = direction.scale(appliedSpeed)
        barnacle.speed = appliedSpeed.toFloat()
    }

    /**
     * Spherical interpolation with angular acceleration avoids both node snapping and
     * the zero-vector/opposite-vector failure of a linear interpolation.
     */
    private fun turnTowards(current: Vec3, target: Vec3): Vec3 {
        val from = current.normalize()
        val to = target.normalize()

        if (previousSteeringTarget.lengthSqr() >= MIN_SPEED_SQR) {
            val targetChange = acos(previousSteeringTarget.normalize().dot(to).coerceIn(-1.0, 1.0))
            if (targetChange >= NODE_CHANGE_THRESHOLD_RADIANS) {
                steeringAngularSpeed = minOf(steeringAngularSpeed, NODE_ENTRY_TURN_SPEED_RADIANS)
            }
        }
        previousSteeringTarget = to

        val dot = from.dot(to).coerceIn(-1.0, 1.0)
        val angle = acos(dot)
        if (angle <= MIN_TURN_ANGLE_RADIANS) {
            steeringAngularSpeed = 0.0
            return to
        }

        // sqrt(2*a*d) is the maximum angular speed that can still decelerate to zero
        // over the remaining angle. It produces a progressive ease-in/ease-out turn.
        val brakingSpeed = sqrt(2.0 * PATH_TURN_ACCELERATION_RADIANS * angle)
        val targetAngularSpeed = minOf(MAX_PATH_TURN_SPEED_RADIANS, brakingSpeed)
        steeringAngularSpeed = approach(
            steeringAngularSpeed,
            targetAngularSpeed,
            PATH_TURN_ACCELERATION_RADIANS
        )
        val turnAmount = minOf(angle, steeringAngularSpeed)

        if (abs(PI - angle) < OPPOSITE_DIRECTION_EPSILON) {
            val referenceAxis = if (abs(from.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
            val axis = from.cross(referenceAxis).normalize()
            return from.scale(cos(turnAmount))
                .add(axis.cross(from).scale(sin(turnAmount)))
                .normalize()
        }

        val sinAngle = sin(angle)
        val fromWeight = sin(angle - turnAmount) / sinAngle
        val toWeight = sin(turnAmount) / sinAngle
        return from.scale(fromWeight).add(to.scale(toWeight)).normalize()
    }

    private fun approach(current: Double, target: Double, maxChange: Double): Double = when {
        current < target -> minOf(current + maxChange, target)
        current > target -> maxOf(current - maxChange, target)
        else -> target
    }

    private companion object {
        const val MAX_PATH_SPEED = 1.0
        const val WAYPOINT_APPROACH_FACTOR = 0.5
        val MAX_PATH_TURN_SPEED_RADIANS: Double = Math.toRadians(8.0)
        val PATH_TURN_ACCELERATION_RADIANS: Double = Math.toRadians(1.5)
        val NODE_ENTRY_TURN_SPEED_RADIANS: Double = Math.toRadians(1.5)
        val NODE_CHANGE_THRESHOLD_RADIANS: Double = Math.toRadians(12.0)
        const val MIN_TURN_ANGLE_RADIANS = 1.0e-6
        const val OPPOSITE_DIRECTION_EPSILON = 1.0e-5
    }
}
