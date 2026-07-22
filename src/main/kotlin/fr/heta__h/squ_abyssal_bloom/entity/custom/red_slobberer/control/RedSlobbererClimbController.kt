package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

class RedSlobbererClimbController(
    private val redSlobberer: RedSlobbererEntity
) {

    private companion object {
        const val ATTACHING_MODEL_PITCH = -4.0f
        const val CLIMBING_MODEL_PITCH = -13.0f
        const val CRESTING_MODEL_PITCH = -6.0f

        const val MIN_ELEVATED_NODE_HEIGHT = 0.3
        const val MAX_CLIMB_DURATION_TICKS = 260
        const val NATIVE_STEP_RELEASE_DELAY_TICKS = 1

        const val APPROACH_SPEED_MULTIPLIER = 0.45
        const val CONTACT_SPEED_MULTIPLIER = 0.16
        const val MAX_ATTACH_APPROACH_TICKS = 40
        const val CONTACT_DURATION_TICKS = 6
        const val ATTACH_VERTICAL_RESPONSIVENESS = 0.35

        const val CLIMB_SPEED_MULTIPLIER = 0.24
        const val MIN_COMMITTED_CLIMB_HEIGHT = 0.2
        const val CLIMB_HEIGHT_RESPONSE = 0.14
        const val MIN_CLIMB_SPEED = 0.02
        const val MAX_CLIMB_SPEED = 0.045
        const val CLIMB_VERTICAL_RESPONSIVENESS = 0.32

        const val CREST_CLEARANCE_HEIGHT = 0.0625
        const val CREST_START_DISTANCE = 0.12
        const val CREST_FINISH_DISTANCE = 0.04
        const val CREST_SPEED_MULTIPLIER = 0.75
        const val CREST_BODY_WIDTH_FACTOR = 0.35f
        const val MIN_CREST_TRAVEL_DISTANCE = 0.5
        const val MAX_CREST_TRAVEL_DISTANCE = 1.5
        const val CREST_HEIGHT_RESPONSE = 0.1
        const val MIN_CREST_SPEED = 0.025
        const val MAX_CREST_SPEED = 0.04
        const val CREST_VERTICAL_RESPONSIVENESS = 0.3
        const val CLEAR_CREST_DURATION_TICKS = 5
        const val MAX_CREST_DURATION_TICKS = 140
    }

    var phase: RedSlobbererClimbPhase = RedSlobbererClimbPhase.NONE
        private set

    var horizontalSpeedMultiplier: Double = 1.0
        private set

    val preventsNativeStep: Boolean
        get() = phase != RedSlobbererClimbPhase.NONE ||
            redSlobberer.tickCount <= suppressNativeStepUntilTick

    private var climbStartY = 0.0
    private var climbTargetY = 0.0
    private var approachTicks = 0
    private var contactTicks = 0
    private var crestStartX = 0.0
    private var crestStartZ = 0.0
    private var crestTicks = 0
    private var clearCrestTicks = 0
    private var totalClimbTicks = 0
    private var suppressNativeStepUntilTick = -1

    fun tick(climbRequested: Boolean, obstacleAhead: Boolean, wantedY: Double) {
        if (phase == RedSlobbererClimbPhase.NONE) {
            if (!climbRequested) return
            begin(wantedY)
        }

        totalClimbTicks++
        if (totalClimbTicks > MAX_CLIMB_DURATION_TICKS) {
            abort()
            return
        }

        climbTargetY = maxOf(
            climbTargetY,
            wantedY.coerceAtMost(climbStartY + redSlobberer.maximumClimbHeight)
        )

        when (phase) {
            RedSlobbererClimbPhase.ATTACHING -> tickAttaching(climbRequested, obstacleAhead)
            RedSlobbererClimbPhase.CLIMBING -> tickClimbing(obstacleAhead)
            RedSlobbererClimbPhase.CRESTING -> tickCresting(obstacleAhead)
            RedSlobbererClimbPhase.NONE -> Unit
        }
    }

    fun stop() {
        if (phase == RedSlobbererClimbPhase.NONE) return
        suppressNativeStepUntilTick = redSlobberer.tickCount + NATIVE_STEP_RELEASE_DELAY_TICKS
        phase = RedSlobbererClimbPhase.NONE
        horizontalSpeedMultiplier = 1.0
        approachTicks = 0
        contactTicks = 0
        crestStartX = 0.0
        crestStartZ = 0.0
        crestTicks = 0
        clearCrestTicks = 0
        totalClimbTicks = 0
        redSlobberer.setClimbVisualPitchTarget(0.0f)
    }

    private fun begin(wantedY: Double) {
        climbStartY = redSlobberer.y
        climbTargetY = wantedY.coerceIn(
            climbStartY + MIN_ELEVATED_NODE_HEIGHT,
            climbStartY + redSlobberer.maximumClimbHeight
        )
        phase = RedSlobbererClimbPhase.ATTACHING
        horizontalSpeedMultiplier = APPROACH_SPEED_MULTIPLIER
        approachTicks = 0
        contactTicks = 0
        crestStartX = 0.0
        crestStartZ = 0.0
        crestTicks = 0
        clearCrestTicks = 0
        totalClimbTicks = 0
        redSlobberer.setClimbVisualPitchTarget(ATTACHING_MODEL_PITCH)
    }

    private fun tickAttaching(climbRequested: Boolean, obstacleAhead: Boolean) {
        approachVerticalVelocity(0.0, ATTACH_VERTICAL_RESPONSIVENESS)

        if (!climbRequested && !obstacleAhead) {
            stop()
            return
        }

        if (obstacleAhead) {
            horizontalSpeedMultiplier = CONTACT_SPEED_MULTIPLIER
            approachTicks = 0
            contactTicks++
            if (contactTicks >= CONTACT_DURATION_TICKS) {
                phase = RedSlobbererClimbPhase.CLIMBING
                redSlobberer.setClimbVisualPitchTarget(CLIMBING_MODEL_PITCH)
            }
        } else {
            horizontalSpeedMultiplier = APPROACH_SPEED_MULTIPLIER
            approachTicks++
            contactTicks = 0
            if (approachTicks >= MAX_ATTACH_APPROACH_TICKS) {
                abort()
            }
        }
    }

    private fun tickClimbing(obstacleAhead: Boolean) {
        horizontalSpeedMultiplier = CLIMB_SPEED_MULTIPLIER
        if (!obstacleAhead && redSlobberer.y - climbStartY < MIN_COMMITTED_CLIMB_HEIGHT) {
            abort()
            return
        }

        val remainingHeight = climbTargetY + CREST_CLEARANCE_HEIGHT - redSlobberer.y
        if (remainingHeight <= CREST_START_DISTANCE) {
            phase = RedSlobbererClimbPhase.CRESTING
            crestStartX = redSlobberer.x
            crestStartZ = redSlobberer.z
            crestTicks = 0
            clearCrestTicks = 0
            redSlobberer.setClimbVisualPitchTarget(CRESTING_MODEL_PITCH)
            return
        }

        val targetVerticalSpeed = (remainingHeight * CLIMB_HEIGHT_RESPONSE)
            .coerceIn(MIN_CLIMB_SPEED, MAX_CLIMB_SPEED)
        approachVerticalVelocity(targetVerticalSpeed, CLIMB_VERTICAL_RESPONSIVENESS)
    }

    private fun tickCresting(obstacleAhead: Boolean) {
        horizontalSpeedMultiplier = CREST_SPEED_MULTIPLIER
        crestTicks++

        val remainingHeight = climbTargetY + CREST_CLEARANCE_HEIGHT - redSlobberer.y
        val targetVerticalSpeed = if (remainingHeight > 0.0) {
            (remainingHeight * CREST_HEIGHT_RESPONSE).coerceIn(MIN_CREST_SPEED, MAX_CREST_SPEED)
        } else {
            0.0
        }
        approachVerticalVelocity(targetVerticalSpeed, CREST_VERTICAL_RESPONSIVENESS)

        val crestDx = redSlobberer.x - crestStartX
        val crestDz = redSlobberer.z - crestStartZ
        val requiredCrestDistance = (redSlobberer.bbWidth * CREST_BODY_WIDTH_FACTOR)
            .toDouble()
            .coerceIn(MIN_CREST_TRAVEL_DISTANCE, MAX_CREST_TRAVEL_DISTANCE)
        val bodyHasCrossedCrest = crestDx * crestDx + crestDz * crestDz >=
            requiredCrestDistance * requiredCrestDistance

        if (
            bodyHasCrossedCrest &&
            !obstacleAhead &&
            abs(remainingHeight) <= CREST_FINISH_DISTANCE
        ) {
            clearCrestTicks++
        } else {
            clearCrestTicks = 0
        }

        if (clearCrestTicks >= CLEAR_CREST_DURATION_TICKS || crestTicks >= MAX_CREST_DURATION_TICKS) {
            stop()
        }
    }

    private fun approachVerticalVelocity(target: Double, responsiveness: Double) {
        val movement = redSlobberer.deltaMovement
        val nextY = movement.y + (target - movement.y) * responsiveness
        redSlobberer.deltaMovement = Vec3(movement.x, nextY, movement.z)
    }

    private fun abort() {
        val movement = redSlobberer.deltaMovement
        redSlobberer.deltaMovement = Vec3(movement.x, movement.y.coerceAtMost(0.0), movement.z)
        redSlobberer.navigation.stop()
        stop()
    }
}
