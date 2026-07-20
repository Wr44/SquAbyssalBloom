package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleBehaviorState
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.ceil

class BarnacleFleeBehaviorGoal(private val barnacle: BarnacleEntity) : Goal() {
    private val moveStillDuration =
        ceil(BarnacleEntity.ANIM_FLEE_STILL_S * BarnacleEntity.FLEE_ANIM_FPS).toInt()
    private val moveRushDuration =
        ceil(BarnacleEntity.ANIM_FLEE_RUSH_S * BarnacleEntity.FLEE_ANIM_FPS).toInt()
    private val fleeRadiusSqr = BarnacleEntity.FLEE_RADIUS * BarnacleEntity.FLEE_RADIUS

    private var isPlaying = false
    private var previousThreat: LivingEntity = barnacle
    private var currentThreat: LivingEntity? = null

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean {
        currentThreat = findThreat()
        return isPlaying ||
            (barnacle.isUnderWater && !barnacle.mouthOpen && barnacle.isHealthCritical && currentThreat != null)
    }

    override fun start() {
        barnacle.behaviorPathController.stop()
        currentThreat?.let(::setFleeDirection)
    }

    override fun tick() {
        barnacle.ensureGoalState(BarnacleBehaviorState.FLEE)
        currentThreat = findThreat()
        val threat = currentThreat ?: previousThreat
        previousThreat = threat

        if (barnacle.rushPhase) tickRush(threat) else tickStill(threat)
    }

    override fun stop() {
        barnacle.behaviorPathController.stop()
        barnacle.rushPhase = false
        barnacle.idlePhase = false
        barnacle.deltaMovement = Vec3.ZERO
        isPlaying = false
    }

    private fun findThreat(): LivingEntity? {
        val mobThreat = barnacle.lastHurtByMob?.takeIf {
            it.isAlive && it !is Player && barnacle.distanceToSqr(it) < fleeRadiusSqr
        }
        val playerThreat = barnacle.level().getNearestPlayer(barnacle, BarnacleEntity.FLEE_RADIUS)?.takeIf {
            !it.isCreative && !it.isSpectator && !it.hasEffect(MobEffects.INVISIBILITY) && it.isAlive
        }

        return when {
            mobThreat != null && playerThreat != null ->
                if (barnacle.distanceToSqr(mobThreat) < barnacle.distanceToSqr(playerThreat)) mobThreat else playerThreat
            mobThreat != null -> mobThreat
            else -> playerThreat
        }
    }

    private fun setFleeDirection(threat: LivingEntity) {
        val baseDirection = barnacle.position().subtract(threat.position()).normalize()
        val jitter = Vec3(
            barnacle.random.nextDouble() - BarnacleEntity.FLEE_JITTER_OFFSET,
            barnacle.random.nextDouble() - BarnacleEntity.FLEE_JITTER_OFFSET,
            barnacle.random.nextDouble() - BarnacleEntity.FLEE_JITTER_OFFSET
        ).scale(BarnacleEntity.FLEE_JITTER_SCALE)
        barnacle.setMovementDirection(baseDirection.add(jitter).normalize())
    }

    private fun tickStill(threat: LivingEntity) {
        val elapsed = barnacle.tickCount - barnacle.behaviorAnimationStartTick
        barnacle.behaviorPathController.pause()
        barnacle.deltaMovement = barnacle.deltaMovement.scale(BarnacleEntity.FLEE_STILL_FRICTION)
        setFleeDirection(threat)

        if (elapsed >= moveStillDuration) {
            isPlaying = true
            barnacle.spawnInk()
            barnacle.rushPhase = true
            barnacle.behaviorAnimationStartTick = barnacle.tickCount
        }
    }

    private fun tickRush(threat: LivingEntity) {
        val elapsed = barnacle.tickCount - barnacle.behaviorAnimationStartTick
        if (elapsed >= moveRushDuration) {
            isPlaying = false
            barnacle.rushPhase = false
            barnacle.behaviorPathController.stop()
            barnacle.deltaMovement = Vec3.ZERO
            barnacle.behaviorAnimationStartTick = barnacle.tickCount
            return
        }

        setFleeDirection(threat)
        val direction = barnacle.getMovementDirection()
        val speed = barnacle.barnacleSpeed(
            elapsed.toFloat(),
            moveRushDuration.toFloat(),
            BarnacleEntity.FLEE_MAX_SPEED,
            BarnacleEntity.FLEE_SPEED_K
        )
        barnacle.behaviorPathController.moveAway(direction, speed.toDouble())
    }
}
