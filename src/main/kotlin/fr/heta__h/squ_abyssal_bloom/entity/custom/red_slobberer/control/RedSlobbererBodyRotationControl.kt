package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.control.BodyRotationControl

class RedSlobbererBodyRotationControl(
    private val redSlobberer: RedSlobbererEntity
) : BodyRotationControl(redSlobberer) {

    private companion object {
        const val MIN_ACTIVE_SPEED = 0.001f
        const val BODY_TURN_DEGREES = 2.25f
        const val MAX_HEAD_OFFSET_DEGREES = 18.0f
        const val MOVEMENT_EPSILON_SQR = 1.0E-5
    }

    private var initialized = false
    private var stableBodyYaw = 0.0f

    override fun clientTick() {
        if (!initialized) {
            stableBodyYaw = redSlobberer.yBodyRot
            initialized = true
        }

        val dx = redSlobberer.x - redSlobberer.xo
        val dz = redSlobberer.z - redSlobberer.zo
        val isCrawling = redSlobberer.isInLocomotionPropulsionPhase && (
            dx * dx + dz * dz > MOVEMENT_EPSILON_SQR ||
                redSlobberer.speed > MIN_ACTIVE_SPEED
            )

        if (isCrawling) {
            stableBodyYaw = Mth.approachDegrees(
                stableBodyYaw,
                redSlobberer.yRot,
                BODY_TURN_DEGREES
            )
        }

        redSlobberer.yBodyRot = stableBodyYaw
        val headOffset = Mth.wrapDegrees(redSlobberer.yHeadRot - stableBodyYaw).coerceIn(
            -MAX_HEAD_OFFSET_DEGREES,
            MAX_HEAD_OFFSET_DEGREES
        )
        redSlobberer.yHeadRot = stableBodyYaw + headOffset
    }

}
