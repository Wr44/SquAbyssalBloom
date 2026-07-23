package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.phys.Vec3

class RedSlobbererMoveControl(
    private val redSlobberer: RedSlobbererEntity
) : MoveControl(redSlobberer) {

    private companion object {
        const val MIN_HORIZONTAL_DISTANCE_SQR = 2.5000003E-7
        const val MAX_PATH_TURN_DEGREES = 8.0f
    }

    private var wasPropelling = false

    fun stopForDefense() {
        operation = Operation.WAIT
        redSlobberer.speed = 0.0f
        redSlobberer.xxa = 0.0f
        redSlobberer.yya = 0.0f
        redSlobberer.zza = 0.0f
        redSlobberer.deltaMovement = Vec3.ZERO
        wasPropelling = false
    }

    override fun tick() {
        if (redSlobberer.isDefenseImmobilized) {
            stopForDefense()
            return
        }

        when (operation) {
            Operation.STRAFE -> tickStrafe()
            Operation.MOVE_TO -> tickMoveTo()
            else -> waitForMovement()
        }
    }

    private fun tickStrafe() {
        if (!redSlobberer.updateLocomotionCycle(true)) {
            operation = Operation.WAIT
            holdDuringCharge()
            return
        }

        super.tick()
        redSlobberer.speed *=
            RedSlobbererEntity.LOCOMOTION_PROPULSION_SPEED_MULTIPLIER.toFloat()
        wasPropelling = true
    }

    private fun tickMoveTo() {
        operation = Operation.WAIT
        val dx = wantedX - redSlobberer.x
        val dz = wantedZ - redSlobberer.z
        if (dx * dx + dz * dz < MIN_HORIZONTAL_DISTANCE_SQR) {
            waitForMovement()
            return
        }

        if (!redSlobberer.updateLocomotionCycle(true)) {
            holdDuringCharge()
            return
        }

        val targetYaw = (
            Mth.atan2(dz, dx) * 180.0 / Math.PI
            ).toFloat() - 90.0f
        redSlobberer.yRot = rotlerp(
            redSlobberer.yRot,
            targetYaw,
            MAX_PATH_TURN_DEGREES
        )
        redSlobberer.xxa = 0.0f
        redSlobberer.yya = 0.0f
        redSlobberer.speed = (
            speedModifier *
                redSlobberer.getAttributeValue(Attributes.MOVEMENT_SPEED) *
                RedSlobbererEntity.LOCOMOTION_PROPULSION_SPEED_MULTIPLIER
            ).toFloat()
        wasPropelling = true
    }

    private fun waitForMovement() {
        operation = Operation.WAIT
        redSlobberer.speed = 0.0f
        redSlobberer.xxa = 0.0f
        redSlobberer.yya = 0.0f
        redSlobberer.updateLocomotionCycle(false)
        stopPropulsionMomentum()
    }

    private fun holdDuringCharge() {
        redSlobberer.speed = 0.0f
        redSlobberer.xxa = 0.0f
        redSlobberer.yya = 0.0f
        stopPropulsionMomentum()
    }

    private fun stopPropulsionMomentum() {
        if (!wasPropelling) return
        val movement = redSlobberer.deltaMovement
        redSlobberer.deltaMovement = Vec3(0.0, movement.y, 0.0)
        wasPropelling = false
    }
}
