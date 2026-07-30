package fr.heta__h.squ_abyssal_bloom.entity.custom.brine.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

class BrineMoveControl(private val brine: BrineEntity) : MoveControl(brine) {
    private var requestTick = Int.MIN_VALUE
    private var requestedSpeed = 0.0

    fun requestPathVelocity(speed: Double) {
        requestTick = brine.tickCount
        requestedSpeed = speed
    }

    fun cancelRequest() {
        requestTick = Int.MIN_VALUE
        requestedSpeed = 0.0
        operation = Operation.WAIT
        brine.speed = 0.0f
    }

    override fun tick() {
        if (requestTick != brine.tickCount || requestedSpeed <= 0.0 || brine.knockbackTicks > 0) {
            operation = Operation.WAIT
            brine.speed = 0.0f
            return
        }
        if (operation != Operation.MOVE_TO || brine.navigation.isDone) {
            operation = Operation.WAIT
            return
        }

        val dx = wantedX - brine.x
        val dz = wantedZ - brine.z
        val horizontalDistance = sqrt(dx * dx + dz * dz)
        operation = Operation.WAIT

        val needsClimb = wantedY > brine.y + CLIMB_WAYPOINT_THRESHOLD
        if (needsClimb) {
            brine.climbingTicks = CLIMBING_MEMORY_TICKS
        } else if (brine.climbingTicks > 0) {
            brine.climbingTicks--
        }

        val verticalVelocity = if (brine.climbingTicks > 0) CLIMB_VELOCITY else brine.deltaMovement.y
        if (horizontalDistance <= BrineEntity.SHADOW_STOP_DIST) {
            brine.brakeHorizontal()
            if (needsClimb) {
                brine.deltaMovement = Vec3(brine.deltaMovement.x, verticalVelocity, brine.deltaMovement.z)
            }
            return
        }

        brine.deltaMovement = Vec3(
            dx / horizontalDistance * requestedSpeed,
            verticalVelocity,
            dz / horizontalDistance * requestedSpeed
        )

        brine.yRot = (atan2(dz, dx) * (180.0 / Math.PI)).toFloat() - 90f
        brine.yBodyRot = brine.yRot
        brine.yHeadRot = brine.yRot
        brine.speed = requestedSpeed.toFloat()
    }

    private companion object {
        const val CLIMB_WAYPOINT_THRESHOLD = 0.1
        const val CLIMBING_MEMORY_TICKS = 6
        const val CLIMB_VELOCITY = 0.15
    }
}
