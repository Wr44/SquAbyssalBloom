package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.control.BodyRotationControl

class RedSlobbererBodyRotationControl(
    private val redSlobberer: RedSlobbererEntity
) : BodyRotationControl(redSlobberer) {

    private companion object {
        const val MAX_HEAD_OFFSET_DEGREES = 18.0f
    }

    private var initialized = false
    private var stableBodyYaw = 0.0f

    override fun clientTick() {
        if (!initialized) {
            stableBodyYaw = redSlobberer.yBodyRot
            initialized = true
        }

        if (redSlobberer.isInLocomotionChargePhase) {
            stableBodyYaw = Mth.approachDegrees(
                stableBodyYaw,
                redSlobberer.yRot,
                RedSlobbererEntity.LOCOMOTION_CHARGE_TURN_DEGREES
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
