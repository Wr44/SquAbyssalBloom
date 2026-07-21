package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleBehaviorState
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.ceil

class BarnaclePursueGoal(private val barnacle: BarnacleEntity) : Goal() {
    private val stillDuration =
        ceil(BarnacleEntity.ANIM_MOVE_STILL_S * BarnacleEntity.SWIM_ANIM_FPS).toInt()
    private val rushDuration =
        ceil(BarnacleEntity.ANIM_MOVE_RUSH_S * BarnacleEntity.SWIM_ANIM_FPS).toInt()
    private var lockedTarget: LivingEntity? = null

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean {
        val candidate = barnacle.trackedTarget
        if (candidate != null && barnacle.isUnderWater && !barnacle.isHealthCritical) {
            lockedTarget = candidate
            return true
        }
        return false
    }

    override fun canContinueToUse(): Boolean {
        val current = lockedTarget ?: return false
        if (current is Player &&
            (current.gameMode() == GameType.CREATIVE || current.gameMode() == GameType.SPECTATOR ||
                current.hasEffect(MobEffects.INVISIBILITY))
        ) return false

        return current.isAlive && current === barnacle.trackedTarget &&
            barnacle.isUnderWater && !barnacle.isHealthCritical
    }

    override fun requiresUpdateEveryTick() = true

    override fun start() {
        barnacle.ensureGoalState(BarnacleBehaviorState.PURSUE)
        barnacle.rushPhase = false
        barnacle.idlePhase = false
        barnacle.behaviorAnimationStartTick = barnacle.tickCount
        barnacle.behaviorPathController.stop()
        barnacle.setMovementDirection(barnacle.getMyDir(lockedTarget) ?: barnacle.getRandomDirection())
        barnacle.deltaMovement = Vec3.ZERO
    }

    override fun tick() {
        barnacle.ensureGoalState(BarnacleBehaviorState.PURSUE)
        if (barnacle.rushPhase) tickRush() else tickStill()
    }

    override fun stop() {
        barnacle.behaviorPathController.stop()
        lockedTarget = null
        barnacle.deltaMovement = Vec3.ZERO
    }

    private fun tickStill() {
        barnacle.behaviorPathController.pause()
        if (barnacle.tickCount - barnacle.behaviorAnimationStartTick >= stillDuration) {
            barnacle.rushPhase = true
            barnacle.behaviorAnimationStartTick = barnacle.tickCount
        }
        barnacle.deltaMovement = Vec3.ZERO
    }

    private fun tickRush() {
        val elapsed = barnacle.tickCount - barnacle.behaviorAnimationStartTick
        if (elapsed >= rushDuration) {
            barnacle.rushPhase = false
            barnacle.behaviorAnimationStartTick = barnacle.tickCount
            barnacle.behaviorPathController.stop()
            barnacle.deltaMovement = Vec3.ZERO
            return
        }

        val target = lockedTarget
        val direction = barnacle.getMyDir(target) ?: barnacle.getRandomDirection()
        val speed = barnacle.barnacleSpeed(
            elapsed.toFloat(),
            rushDuration.toFloat(),
            barnacle.pursuitSpeed,
            BarnacleEntity.SWIM_SPEED_K
        )

        if (target != null) {
            barnacle.behaviorPathController.moveToward(target, speed.toDouble(), direction)
        } else {
            barnacle.behaviorPathController.moveInDirection(direction, speed.toDouble())
        }
    }
}
