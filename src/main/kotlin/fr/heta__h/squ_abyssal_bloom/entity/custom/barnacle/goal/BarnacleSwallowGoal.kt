package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleBehaviorState
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.ceil

class BarnacleSwallowGoal(private val barnacle: BarnacleEntity) : Goal() {
    private val startDuration =
        ceil(BarnacleEntity.ANIM_SWALLOW_START_S * BarnacleEntity.SWALLOW_START_FPS).toInt()
    private val stopDuration =
        ceil(BarnacleEntity.ANIM_SWALLOW_STOP_S * BarnacleEntity.SWALLOW_STOP_FPS).toInt()
    private val swallowDuration =
        ceil(BarnacleEntity.ANIM_SWALLOW_S * BarnacleEntity.SWALLOW_FPS).toInt()

    private var finishing = false
    private var capturingDrops = false

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean = barnacle.isUnderWater &&
        (barnacle.swallowing || finishing ||
            (barnacle.myTarget != null && barnacle.mouthOpen && !barnacle.isHealthCritical))

    override fun canContinueToUse(): Boolean = canUse()

    override fun start() {
        barnacle.behaviorPathController.stop()
        barnacle.ensureGoalState(BarnacleBehaviorState.SWALLOW)
        barnacle.behaviorAnimationStartTick = barnacle.tickCount
        finishing = false
        barnacle.myTarget?.let {
            barnacle.setMovementDirection(barnacle.getMyDir(it) ?: barnacle.getRandomDirection())
        }
    }

    override fun tick() {
        val elapsed = barnacle.tickCount - barnacle.behaviorAnimationStartTick
        val target = barnacle.myTarget

        if (target == null || barnacle.isHealthCritical || (!target.isAlive && barnacle.swallowing)) {
            tickFinish(elapsed)
            return
        }

        if (!barnacle.swallowing) {
            if (elapsed >= startDuration) {
                barnacle.behaviorAnimationStartTick = barnacle.tickCount
                barnacle.swallowing = true
                if (!capturingDrops) {
                    target.captureDrops(mutableListOf())
                    capturingDrops = true
                }
                barnacle.holdTarget(target, barnacle.holdDistance)
            } else {
                val progress = elapsed.toDouble() / startDuration
                val distance = BarnacleEntity.ATTACK_START_DIST +
                    (barnacle.holdDistance - BarnacleEntity.ATTACK_START_DIST) * progress
                barnacle.holdTarget(target, distance)
            }
        } else {
            barnacle.deltaMovement = Vec3.ZERO
            barnacle.holdTarget(target, barnacle.holdDistance)
            if (elapsed % ceil(swallowDuration / 2.0).toInt() == 0) {
                target.hurtServer(
                    barnacle.level() as ServerLevel,
                    barnacle.swallowDamageSource(),
                    BarnacleEntity.SWALLOW_DAMAGE
                )
            }
        }
    }

    private fun tickFinish(elapsed: Int) {
        if (!finishing) {
            barnacle.playSound(ModSounds.BARNACLE_CLOSE_MOUTH.get())
            finishing = true
            barnacle.behaviorAnimationStartTick = barnacle.tickCount
            barnacle.swallowing = false
            if (barnacle.isHealthCritical) {
                barnacle.mouthOpen = false
                finishing = false
            }
            return
        }

        barnacle.deltaMovement = Vec3.ZERO
        if (barnacle.hasStomachContents) {
            if (elapsed >= BarnacleEntity.SPIT_ITEM_DELAY_TICKS && barnacle.spitNextStomachItem()) {
                barnacle.behaviorAnimationStartTick = barnacle.tickCount
            }
        } else if (elapsed >= stopDuration / 2) {
            finishing = false
            barnacle.mouthOpen = false
        }
    }

    override fun stop() {
        barnacle.swallowing = false
        capturingDrops = false
        finishing = false
        if (barnacle.isHealthCritical) barnacle.mouthOpen = false
    }
}
