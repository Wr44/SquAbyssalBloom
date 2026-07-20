package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleBehaviorState
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.findWaterSurface
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.ceil

class BarnacleIdleBehaviorGoal(private val barnacle: BarnacleEntity) : Goal() {
    private val stillAnimationDuration =
        ceil(BarnacleEntity.ANIM_MOVE_STILL_S * BarnacleEntity.IDLE_ANIM_FPS).toInt()
    private val rushDuration =
        ceil(BarnacleEntity.ANIM_MOVE_RUSH_S * BarnacleEntity.IDLE_ANIM_FPS).toInt()
    private var longStillDuration = 0
    private var ticksSinceObstacleCheck = 0

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean = barnacle.isUnderWater
    override fun requiresUpdateEveryTick() = true

    override fun start() {
        barnacle.ensureGoalState(BarnacleBehaviorState.IDLE)
        barnacle.rushPhase = false
        barnacle.idlePhase = false
        barnacle.behaviorAnimationStartTick = barnacle.tickCount
        barnacle.behaviorPathController.stop()
        ticksSinceObstacleCheck = 10
        barnacle.setMovementDirection(barnacle.getRandomDirection())
        barnacle.deltaMovement = Vec3.ZERO
    }

    override fun tick() {
        when {
            barnacle.idlePhase -> tickLongStill()
            barnacle.rushPhase -> tickRush()
            else -> tickPreRush()
        }
    }

    override fun stop() {
        barnacle.behaviorPathController.stop()
    }

    private fun tickLongStill() {
        barnacle.behaviorPathController.pause()
        if (barnacle.tickCount - barnacle.behaviorAnimationStartTick >= longStillDuration) {
            barnacle.behaviorAnimationStartTick = barnacle.tickCount
            barnacle.idlePhase = false
        }
    }

    private fun tickPreRush() {
        barnacle.behaviorPathController.pause()
        if (barnacle.tickCount - barnacle.behaviorAnimationStartTick >= stillAnimationDuration) {
            barnacle.rushPhase = true
            barnacle.behaviorAnimationStartTick = barnacle.tickCount
            ticksSinceObstacleCheck = 10
        }
    }

    private fun tickRush() {
        val elapsed = barnacle.tickCount - barnacle.behaviorAnimationStartTick
        if (elapsed >= rushDuration) {
            barnacle.rushPhase = false
            barnacle.behaviorAnimationStartTick = barnacle.tickCount
            barnacle.behaviorPathController.stop()
            barnacle.deltaMovement = Vec3.ZERO

            when (barnacle.random.nextDouble()) {
                in 0.0..BarnacleEntity.IDLE_DIR_CHANCE_1 ->
                    barnacle.setMovementDirection(barnacle.getRandomDirection())

                in BarnacleEntity.IDLE_DIR_CHANCE_1..BarnacleEntity.IDLE_DIR_CHANCE_2 -> {
                    barnacle.idlePhase = true
                    longStillDuration = barnacle.random.nextInt(BarnacleEntity.IDLE_MIN_TICKS, BarnacleEntity.IDLE_MAX_TICKS)
                }
            }
            return
        }

        ticksSinceObstacleCheck++
        var direction = barnacle.getMovementDirection()
        if (ticksSinceObstacleCheck >= OBSTACLE_CHECK_TICKS) {
            direction = adjustDirectionForObstacles(direction)
            ticksSinceObstacleCheck = 0
        }

        val speed = barnacle.barnacleSpeed(
            elapsed.toFloat(),
            rushDuration.toFloat(),
            BarnacleEntity.IDLE_MAX_SPEED
        )
        barnacle.behaviorPathController.moveInDirection(direction, speed.toDouble())
    }

    private fun adjustDirectionForObstacles(input: Vec3): Vec3 {
        val blocksUp = findWaterSurface(barnacle.level(), barnacle.blockPosition(), 1)
        var direction = input

        if (blocksUp < BarnacleEntity.IDLE_WATER_CHECK_DIST) {
            val adjustment = (BarnacleEntity.IDLE_WATER_CHECK_DIST - blocksUp).toDouble() /
                BarnacleEntity.IDLE_WATER_CHECK_DIST
            direction = Vec3(direction.x, direction.y - adjustment * 0.5, direction.z).normalize()
            barnacle.setMovementDirection(direction)
        } else if (!barnacle.hasWaterAhead(direction)) {
            direction = Vec3(
                barnacle.random.nextDouble() - 0.5,
                barnacle.random.nextDouble().coerceIn(-0.4, 0.4),
                barnacle.random.nextDouble() - 0.5
            ).normalize()
            barnacle.setMovementDirection(direction)
        }

        return direction
    }

    private companion object {
        const val OBSTACLE_CHECK_TICKS = 10
    }
}
