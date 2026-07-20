package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer

import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl

/**
 * Ground-crawling movement for the Red Slobberer.
 *
 * Unlike vanilla [MoveControl], this controller never asks JumpControl to jump toward
 * a higher path node. Small terrain changes are handled by the entity's native step height.
 */
class RedSlobbererMoveControl(
    private val redSlobberer: RedSlobbererEntity,
    private val maxTurnDegrees: Float = 30.0f
) : MoveControl(redSlobberer) {

    override fun tick() {
        if (operation == Operation.STRAFE) {
            super.tick()
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

        if (horizontalDistanceSqr < MIN_HORIZONTAL_DISTANCE_SQR) {
            redSlobberer.speed = 0.0f
            return
        }

        val targetYaw = (Mth.atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90.0f
        redSlobberer.yRot = rotlerp(redSlobberer.yRot, targetYaw, maxTurnDegrees)
        redSlobberer.speed = (
            speedModifier * redSlobberer.getAttributeValue(Attributes.MOVEMENT_SPEED)
        ).toFloat()
    }

    private companion object {
        const val MIN_HORIZONTAL_DISTANCE_SQR = 2.5000003E-7
    }
}
