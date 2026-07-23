package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RedSlobbererMoveControl(
    private val redSlobberer: RedSlobbererEntity
) : MoveControl(redSlobberer) {

    private companion object {
        const val MIN_HORIZONTAL_DISTANCE_SQR = 2.5000003E-7
        const val MIN_HEADING_DISTANCE_SQR = 0.36
        const val HEADING_SMOOTHING = 0.14
        const val REVERSE_HEADING_SMOOTHING = 0.06
        const val MAX_PATH_TURN_DEGREES = 3.0f
        const val REVERSE_DIRECTION_DOT_THRESHOLD = -0.35
        const val MIN_CLIMB_NODE_OFFSET = 0.3
        const val MIN_CLIMB_FACING_DOT = 0.94
        const val FORWARD_COLLISION_PROBE_DISTANCE = 0.45
        const val FORWARD_COLLISION_PROBE_HEIGHT = 0.1
        const val FORWARD_COLLISION_PROBE_HALF_WIDTH_FACTOR = 0.12
        const val MAX_FORWARD_COLLISION_PROBE_HALF_WIDTH = 0.5
        const val CLIMB_CLEARANCE_STEP = 0.125
    }

    private val climbController = RedSlobbererClimbController(redSlobberer)
    private var headingInitialized = false
    private var smoothedDirectionX = 0.0
    private var smoothedDirectionZ = 1.0
    private var lockedClimbDirectionX = 0.0
    private var lockedClimbDirectionZ = 1.0
    private var wasPropelling = false

    val preventsNativeStep: Boolean
        get() = climbController.preventsNativeStep

    val holdsVerticalPositionDuringCharge: Boolean
        get() = climbController.phase != RedSlobbererClimbPhase.NONE &&
            !redSlobberer.isInLocomotionPropulsionPhase

    val followsTerrainTangent: Boolean
        get() = climbController.phase == RedSlobbererClimbPhase.NONE

    fun stopForDefense() {
        operation = Operation.WAIT
        climbController.stop()
        redSlobberer.speed = 0.0f
        redSlobberer.deltaMovement = Vec3.ZERO
        wasPropelling = false
    }

    override fun tick() {
        if (redSlobberer.isDefenseImmobilized) {
            stopForDefense()
            return
        }

        if (operation == Operation.STRAFE) {
            climbController.stop()
            if (!redSlobberer.updateLocomotionCycle(true)) {
                operation = Operation.WAIT
                holdDuringCharge()
                return
            }
            super.tick()
            redSlobberer.speed *=
                RedSlobbererEntity.LOCOMOTION_PROPULSION_SPEED_MULTIPLIER.toFloat()
            wasPropelling = true
            return
        }

        if (climbController.phase != RedSlobbererClimbPhase.NONE) {
            operation = Operation.WAIT
            if (!redSlobberer.updateLocomotionCycle(true)) {
                holdDuringCharge()
                return
            }
            continueActiveClimb()
            wasPropelling = true
            return
        }

        if (operation != Operation.MOVE_TO) {
            operation = Operation.WAIT
            redSlobberer.speed = 0.0f
            redSlobberer.updateLocomotionCycle(false)
            stopPropulsionMomentum()
            return
        }

        operation = Operation.WAIT
        val dx = wantedX - redSlobberer.x
        val dz = wantedZ - redSlobberer.z
        val horizontalDistanceSqr = dx * dx + dz * dz
        val elevatedPathNode = wantedY - redSlobberer.y in
            MIN_CLIMB_NODE_OFFSET..redSlobberer.maximumClimbHeight

        if (horizontalDistanceSqr < MIN_HORIZONTAL_DISTANCE_SQR && !elevatedPathNode) {
            redSlobberer.speed = 0.0f
            redSlobberer.updateLocomotionCycle(false)
            stopPropulsionMomentum()
            return
        }

        updateSmoothedHeading(dx, dz, horizontalDistanceSqr)
        if (!redSlobberer.updateLocomotionCycle(true)) {
            holdDuringCharge()
            return
        }
        rotateTowardSmoothedHeading()

        val climbProbeDirection = getClimbProbeDirection(smoothedDirectionX, smoothedDirectionZ)
        if (climbProbeDirection == null) {
            applyMovementSpeed()
            wasPropelling = true
            return
        }

        val obstacleAhead = isObstacleAhead(climbProbeDirection.x, climbProbeDirection.z)
        val collisionClimbTargetY = if (obstacleAhead && !elevatedPathNode) {
            findCollisionClimbTargetY(climbProbeDirection.x, climbProbeDirection.z)
        } else {
            null
        }
        val climbRequested = obstacleAhead && (elevatedPathNode || collisionClimbTargetY != null)
        val climbTargetY = if (elevatedPathNode) wantedY else collisionClimbTargetY ?: wantedY

        if (climbRequested) {
            lockedClimbDirectionX = climbProbeDirection.x
            lockedClimbDirectionZ = climbProbeDirection.z
        }

        climbController.tick(climbRequested, obstacleAhead, climbTargetY)
        applyMovementSpeed()
        wasPropelling = true
    }

    private fun continueActiveClimb() {
        smoothedDirectionX = lockedClimbDirectionX
        smoothedDirectionZ = lockedClimbDirectionZ
        rotateTowardSmoothedHeading()

        val obstacleAhead = isObstacleAhead(lockedClimbDirectionX, lockedClimbDirectionZ)
        climbController.tick(true, obstacleAhead, wantedY)

        if (climbController.phase == RedSlobbererClimbPhase.NONE) {
            redSlobberer.speed = 0.0f
            return
        }

        applyMovementSpeed()
    }

    private fun updateSmoothedHeading(dx: Double, dz: Double, horizontalDistanceSqr: Double) {
        if (horizontalDistanceSqr < MIN_HEADING_DISTANCE_SQR && headingInitialized) return
        if (horizontalDistanceSqr < MIN_HORIZONTAL_DISTANCE_SQR) return

        val horizontalDistance = sqrt(horizontalDistanceSqr)
        val directionX = dx / horizontalDistance
        val directionZ = dz / horizontalDistance

        if (!headingInitialized) {
            smoothedDirectionX = directionX
            smoothedDirectionZ = directionZ
            headingInitialized = true
            return
        }

        val dot = smoothedDirectionX * directionX + smoothedDirectionZ * directionZ
        val smoothing = if (dot < REVERSE_DIRECTION_DOT_THRESHOLD) {
            REVERSE_HEADING_SMOOTHING
        } else {
            HEADING_SMOOTHING
        }
        val nextX = smoothedDirectionX + (directionX - smoothedDirectionX) * smoothing
        val nextZ = smoothedDirectionZ + (directionZ - smoothedDirectionZ) * smoothing
        val nextLengthSqr = nextX * nextX + nextZ * nextZ
        if (nextLengthSqr < MIN_HORIZONTAL_DISTANCE_SQR) return

        val nextLength = sqrt(nextLengthSqr)
        smoothedDirectionX = nextX / nextLength
        smoothedDirectionZ = nextZ / nextLength
    }

    private fun rotateTowardSmoothedHeading() {
        val targetYaw = (Mth.atan2(smoothedDirectionZ, smoothedDirectionX) * 180.0 / Math.PI)
            .toFloat() - 90.0f
        redSlobberer.yRot = rotlerp(
            redSlobberer.yRot,
            targetYaw,
            MAX_PATH_TURN_DEGREES
        )
    }

    private fun applyMovementSpeed() {
        redSlobberer.speed = (
            speedModifier *
                redSlobberer.getAttributeValue(Attributes.MOVEMENT_SPEED) *
                climbController.horizontalSpeedMultiplier *
                RedSlobbererEntity.LOCOMOTION_PROPULSION_SPEED_MULTIPLIER
            ).toFloat()
    }

    private fun holdDuringCharge() {
        redSlobberer.speed = 0.0f
        stopPropulsionMomentum()
    }

    private fun stopPropulsionMomentum() {
        if (!wasPropelling) return
        val movement = redSlobberer.deltaMovement
        val verticalMovement = if (climbController.phase == RedSlobbererClimbPhase.NONE) {
            movement.y
        } else {
            0.0
        }
        redSlobberer.deltaMovement = Vec3(0.0, verticalMovement, 0.0)
        wasPropelling = false
    }

    private fun getClimbProbeDirection(pathDirectionX: Double, pathDirectionZ: Double): Vec3? {
        val bodyYawRadians = Math.toRadians(redSlobberer.yBodyRot.toDouble())
        val bodyDirectionX = -sin(bodyYawRadians)
        val bodyDirectionZ = cos(bodyYawRadians)
        val pathFacingAlignment = bodyDirectionX * pathDirectionX + bodyDirectionZ * pathDirectionZ

        // The path can turn before this very wide body has physically rotated. Until the model is
        // facing the path, a block found in that direction is still beside the creature, not ahead.
        if (pathFacingAlignment < MIN_CLIMB_FACING_DOT) return null

        return Vec3(bodyDirectionX, 0.0, bodyDirectionZ)
    }

    private fun isObstacleAhead(directionX: Double, directionZ: Double): Boolean {
        // Probe only the creature's central path. Its wide body may brush a block that
        // is beside the route, but that contact must not be interpreted as a wall to climb.
        val halfProbeWidth = minOf(
            redSlobberer.bbWidth * FORWARD_COLLISION_PROBE_HALF_WIDTH_FACTOR,
            MAX_FORWARD_COLLISION_PROBE_HALF_WIDTH
        )

        return doubleArrayOf(-halfProbeWidth, 0.0, halfProbeWidth).any { lateralOffset ->
            hasBlockAhead(directionX, directionZ, lateralOffset)
        }
    }

    private fun findCollisionClimbTargetY(directionX: Double, directionZ: Double): Double? {
        val clearanceSteps = ceil(
            redSlobberer.maximumClimbHeight / CLIMB_CLEARANCE_STEP
        ).toInt()
        for (heightStep in 1..clearanceSteps) {
            val lift = minOf(
                heightStep * CLIMB_CLEARANCE_STEP,
                redSlobberer.maximumClimbHeight
            )
            if (hasFullBodyForwardClearance(directionX, directionZ, lift)) {
                return redSlobberer.y + lift
            }
        }

        return null
    }

    private fun hasBlockAhead(directionX: Double, directionZ: Double, lateralOffset: Double): Boolean {
        val lateralX = -directionZ * lateralOffset
        val lateralZ = directionX * lateralOffset
        val distanceToEdge = distanceToBoundingBoxEdge(directionX, directionZ, lateralX, lateralZ)
        if (!distanceToEdge.isFinite()) return false

        val probeDistance = distanceToEdge + FORWARD_COLLISION_PROBE_DISTANCE
        val from = Vec3(
            redSlobberer.x + lateralX,
            redSlobberer.boundingBox.minY + FORWARD_COLLISION_PROBE_HEIGHT,
            redSlobberer.z + lateralZ
        )
        val to = from.add(directionX * probeDistance, 0.0, directionZ * probeDistance)
        val hitResult = redSlobberer.level().clip(
            ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                redSlobberer
            )
        )
        return hitResult.type != HitResult.Type.MISS
    }

    private fun distanceToBoundingBoxEdge(
        directionX: Double,
        directionZ: Double,
        offsetX: Double,
        offsetZ: Double
    ): Double {
        val halfWidth = redSlobberer.bbWidth * 0.5
        val distanceToXEdge = when {
            directionX > 0.0 -> (halfWidth - offsetX) / directionX
            directionX < 0.0 -> (-halfWidth - offsetX) / directionX
            else -> Double.POSITIVE_INFINITY
        }
        val distanceToZEdge = when {
            directionZ > 0.0 -> (halfWidth - offsetZ) / directionZ
            directionZ < 0.0 -> (-halfWidth - offsetZ) / directionZ
            else -> Double.POSITIVE_INFINITY
        }
        return minOf(distanceToXEdge, distanceToZEdge)
    }

    private fun hasFullBodyForwardClearance(directionX: Double, directionZ: Double, lift: Double): Boolean {
        val probeX = directionX * FORWARD_COLLISION_PROBE_DISTANCE
        val probeZ = directionZ * FORWARD_COLLISION_PROBE_DISTANCE
        return redSlobberer.level().noBlockCollision(
            redSlobberer,
            redSlobberer.boundingBox.move(probeX, lift, probeZ)
        )
    }
}
