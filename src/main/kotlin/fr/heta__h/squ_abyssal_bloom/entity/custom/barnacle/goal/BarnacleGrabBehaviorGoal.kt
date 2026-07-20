package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleBehaviorState
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.ceil

class BarnacleGrabBehaviorGoal(private val barnacle: BarnacleEntity) : Goal() {
    private val openDuration =
        ceil(BarnacleEntity.ANIM_MOUTH_OPEN_S * BarnacleEntity.GRAB_ANIM_FPS).toInt()
    private val closeDuration =
        ceil(BarnacleEntity.ANIM_MOUTH_CLOSE_S * BarnacleEntity.GRAB_ANIM_FPS).toInt()

    private var initialVelocity = Vec3.ZERO
    private var closing = false

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean = barnacle.isUnderWater &&
        ((barnacle.myTarget != null && !barnacle.isHealthCritical) || closing)

    override fun start() {
        barnacle.behaviorPathController.stop()
        barnacle.ensureGoalState(BarnacleBehaviorState.GRAB)
        barnacle.behaviorAnimationStartTick = barnacle.tickCount
        initialVelocity = barnacle.deltaMovement
        closing = false
        barnacle.myTarget?.let {
            barnacle.setMovementDirection(barnacle.getMyDir(it) ?: barnacle.getRandomDirection())
        }
    }

    override fun tick() {
        val elapsed = barnacle.tickCount - barnacle.behaviorAnimationStartTick
        val target = barnacle.myTarget

        if ((target == null || barnacle.isHealthCritical) && !closing) {
            closing = true
            barnacle.behaviorAnimationStartTick = barnacle.tickCount
            barnacle.mouthOpen = false
            barnacle.deltaMovement = Vec3.ZERO
            return
        }

        if (target != null && target.isAlive) {
            closing = false
            barnacle.holdTarget(target, BarnacleEntity.GRAB_HOLD_FACTOR)
            if (!barnacle.mouthOpen) {
                when {
                    elapsed == BarnacleEntity.GRAB_OPEN_SOUND_DELAY_TICKS ->
                        barnacle.playSound(ModSounds.BARNACLE_OPEN_MOUTH.get())

                    elapsed >= openDuration + BarnacleEntity.GRAB_HOLD_DURATION_TICKS -> {
                        barnacle.behaviorAnimationStartTick = barnacle.tickCount
                        barnacle.mouthOpen = true
                        barnacle.deltaMovement = Vec3.ZERO
                    }

                    elapsed >= openDuration -> barnacle.deltaMovement = Vec3.ZERO
                    else -> {
                        val slowdown = (1.0 - elapsed.toDouble() / openDuration).coerceAtLeast(0.0)
                        barnacle.deltaMovement = initialVelocity.scale(slowdown)
                    }
                }
            } else {
                barnacle.deltaMovement = Vec3.ZERO
            }
        } else if (closing) {
            barnacle.deltaMovement = Vec3.ZERO
            if (elapsed == BarnacleEntity.GRAB_CLOSE_SOUND_DELAY_TICKS) {
                barnacle.playSound(ModSounds.BARNACLE_CLOSE_MOUTH.get())
            }
            if (elapsed >= closeDuration) closing = false
        }
    }

    override fun stop() {
        closing = false
        if (!(barnacle.myTarget != null && barnacle.mouthOpen)) barnacle.mouthOpen = false
    }
}
