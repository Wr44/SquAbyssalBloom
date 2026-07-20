package fr.heta__h.squ_abyssal_bloom.entity.ai

import net.minecraft.util.Mth
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.phys.Vec3

class SmoothCrawlMoveControl(
    mob: Mob,
    private val maxTurnDegrees: Float = 30.0f,
    private val maxClimbPerTick: Double = 0.1
) : MoveControl(mob) {

    class SmoothCrawlMoveControl(
        mob: Mob,
        private val maxTurnDegrees: Float = 30.0f,
        private val maxClimbPerTick: Double = 0.1,
        private val climbResponsiveness: Double = 0.3
    ) : MoveControl(mob) {

        override fun tick() {
            if (operation != Operation.MOVE_TO) {
                mob.zza = 0.0f
                return
            }

            val dx = wantedX - mob.x
            val dy = wantedY - mob.y
            val dz = wantedZ - mob.z
            val distSqr = dx * dx + dy * dy + dz * dz

            if (distSqr < 2.5000003E-7) {
                mob.zza = 0.0f
                return
            }

            val yaw = (Mth.atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90.0f
            mob.yRot = rotlerp(mob.yRot, yaw, maxTurnDegrees)
            mob.speed = (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED)).toFloat()

            if (dy > 0.0) {
                val climbSpeed = (dy * climbResponsiveness).coerceAtMost(maxClimbPerTick)
                mob.deltaMovement = Vec3(mob.deltaMovement.x, climbSpeed, mob.deltaMovement.z)
            }
        }
    }
}