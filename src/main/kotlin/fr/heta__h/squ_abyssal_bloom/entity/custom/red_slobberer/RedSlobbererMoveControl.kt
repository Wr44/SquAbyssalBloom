package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer

import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import kotlin.math.ceil
import kotlin.math.sqrt

class RedSlobbererMoveControl(
    private val redSlobberer: RedSlobbererEntity
) : MoveControl(redSlobberer) {

    private val climbController = RedSlobbererClimbController(redSlobberer)
    private var headingInitialized = false
    private var smoothedDirectionX = 0.0
    private var smoothedDirectionZ = 1.0
    private var lockedClimbDirectionX = 0.0
    private var lockedClimbDirectionZ = 1.0

    val preventsNativeStep: Boolean
        get() = climbController.preventsNativeStep

    override fun tick() {
        if (operation == Operation.STRAFE) {
            climbController.stop()
            super.tick()
            return
        }

        if (climbController.phase != RedSlobbererClimbPhase.NONE) {
            operation = Operation.WAIT
            continueActiveClimb()
            return
        }

        if (operation != Operation.MOVE_TO) {
            operation = Operation.WAIT
            redSlobberer.speed = 0.0f
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
            return
        }

        updateSmoothedHeading(dx, dz, horizontalDistanceSqr)
        rotateTowardSmoothedHeading()

        val obstacleAhead = isObstacleAhead(smoothedDirectionX, smoothedDirectionZ)
        val collisionClimbTargetY = if (obstacleAhead && !elevatedPathNode) {
            findCollisionClimbTargetY(smoothedDirectionX, smoothedDirectionZ)
        } else {
            null
        }
        val climbRequested = obstacleAhead && (elevatedPathNode || collisionClimbTargetY != null)
        val climbTargetY = if (elevatedPathNode) wantedY else collisionClimbTargetY ?: wantedY

        if (climbRequested) {
            lockedClimbDirectionX = smoothedDirectionX
            lockedClimbDirectionZ = smoothedDirectionZ
        }

        climbController.tick(climbRequested, obstacleAhead, climbTargetY)
        applyMovementSpeed()
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
                climbController.horizontalSpeedMultiplier
            ).toFloat()
    }

    private fun isObstacleAhead(directionX: Double, directionZ: Double): Boolean {
        val halfWidth = redSlobberer.bbWidth * 0.5
        val forwardDistance = halfWidth + FRONT_PROBE_MARGIN
        val sideX = -directionZ
        val sideZ = directionX
        val probeY = redSlobberer.boundingBox.minY + FOOT_PROBE_HEIGHT

        for (sideFactor in FRONT_PROBE_SIDE_FACTORS) {
            val sideDistance = halfWidth * sideFactor
            val probePos = BlockPos.containing(
                redSlobberer.x + directionX * forwardDistance + sideX * sideDistance,
                probeY,
                redSlobberer.z + directionZ * forwardDistance + sideZ * sideDistance
            )
            if (!redSlobberer.level().isLoaded(probePos)) continue
            val state = redSlobberer.level().getBlockState(probePos)
            if (!state.getCollisionShape(redSlobberer.level(), probePos).isEmpty) return true
        }

        return false
    }

    private fun findCollisionClimbTargetY(directionX: Double, directionZ: Double): Double? {
        val probeX = directionX * COLLISION_CLEARANCE_PROBE_DISTANCE
        val probeZ = directionZ * COLLISION_CLEARANCE_PROBE_DISTANCE

        val clearanceSteps = ceil(
            redSlobberer.maximumClimbHeight / CLIMB_CLEARANCE_STEP
        ).toInt()
        for (heightStep in 1..clearanceSteps) {
            val lift = minOf(
                heightStep * CLIMB_CLEARANCE_STEP,
                redSlobberer.maximumClimbHeight
            )
            if (redSlobberer.level().noBlockCollision(
                    redSlobberer,
                    redSlobberer.boundingBox.move(probeX, lift, probeZ)
                )
            ) {
                return redSlobberer.y + lift
            }
        }

        return null
    }

    private companion object {
        val FRONT_PROBE_SIDE_FACTORS = doubleArrayOf(-0.8, -0.4, 0.0, 0.4, 0.8)

        const val MIN_HORIZONTAL_DISTANCE_SQR = 2.5000003E-7
        const val MIN_HEADING_DISTANCE_SQR = 0.36
        const val HEADING_SMOOTHING = 0.14
        const val REVERSE_HEADING_SMOOTHING = 0.06
        const val MAX_PATH_TURN_DEGREES = 3.0f
        const val REVERSE_DIRECTION_DOT_THRESHOLD = -0.35
        const val MIN_CLIMB_NODE_OFFSET = 0.3
        const val FRONT_PROBE_MARGIN = 0.45
        const val FOOT_PROBE_HEIGHT = 0.2
        const val COLLISION_CLEARANCE_PROBE_DISTANCE = 0.45
        const val CLIMB_CLEARANCE_STEP = 0.125
    }
}
