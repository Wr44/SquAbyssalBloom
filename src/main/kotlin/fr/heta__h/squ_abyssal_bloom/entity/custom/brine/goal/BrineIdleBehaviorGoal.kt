package fr.heta__h.squ_abyssal_bloom.entity.custom.brine.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.atan2
import kotlin.math.sqrt

class BrineIdleBehaviorGoal(private val brine: BrineEntity) : Goal() {
    private var stillPhase = true
    private var phaseTicks = 0
    private var phaseDuration = 0
    private var targetVelocity = Vec3.ZERO

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse() = brine.isInWater && brine.target == null
    override fun requiresUpdateEveryTick() = true

    override fun start() {
        brine.behaviorPathController.stop()
        enterStillPhase()
    }

    override fun tick() {
        if (stillPhase) tickStill() else tickMove()
    }

    override fun stop() {
        brine.behaviorPathController.stop()
        brine.deltaMovement = Vec3.ZERO
    }

    private fun enterStillPhase() {
        stillPhase = true
        phaseTicks = 0
        phaseDuration = BrineEntity.STILL_MIN_TICKS +
            brine.random.nextInt(BrineEntity.STILL_MAX_TICKS - BrineEntity.STILL_MIN_TICKS)
    }

    private fun enterMovePhase() {
        stillPhase = false
        phaseTicks = 0
        phaseDuration = BrineEntity.MOVE_MIN_TICKS +
            brine.random.nextInt(BrineEntity.MOVE_MAX_TICKS - BrineEntity.MOVE_MIN_TICKS)
        targetVelocity = Vec3(
            (brine.random.nextDouble() - 0.5) * 2.0,
            (brine.random.nextDouble() - 0.5) * BrineEntity.FLOAT_Y_SCALE,
            (brine.random.nextDouble() - 0.5) * 2.0
        ).normalize().scale(BrineEntity.FLOAT_SPEED)
    }

    private fun tickStill() {
        brine.brakeHorizontal()
        brine.yRot += BrineEntity.STILL_ROTATION_SPEED
        brine.yBodyRot = brine.yRot
        brine.yHeadRot = brine.yRot
        if (++phaseTicks >= phaseDuration) enterMovePhase()
    }

    private fun tickMove() {
        if (++phaseTicks >= phaseDuration) {
            enterStillPhase()
            return
        }

        brine.deltaMovement = Vec3(
            brine.deltaMovement.x + (targetVelocity.x - brine.deltaMovement.x) * BrineEntity.FLOAT_LERP_FACTOR,
            brine.deltaMovement.y,
            brine.deltaMovement.z + (targetVelocity.z - brine.deltaMovement.z) * BrineEntity.FLOAT_LERP_FACTOR
        )
        val horizontalSpeed = sqrt(
            brine.deltaMovement.x * brine.deltaMovement.x + brine.deltaMovement.z * brine.deltaMovement.z
        )
        if (horizontalSpeed > BrineEntity.ROTATION_MOVING_THRESHOLD) {
            brine.yRot = (atan2(brine.deltaMovement.z, brine.deltaMovement.x) * (180.0 / Math.PI)).toFloat() - 90f
            brine.yBodyRot = brine.yRot
            brine.yHeadRot = brine.yRot
            brine.xRot = (atan2(brine.deltaMovement.y, horizontalSpeed) * -(180.0 / Math.PI)).toFloat()
        }
    }
}
