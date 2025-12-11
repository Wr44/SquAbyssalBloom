package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.pow

class BarnacleEntity(type: EntityType<out Monster>, level: Level) : Monster(type, level) {

    var stillMouthCloseAnimationState = AnimationState()
    var stillMouthOpenAnimationState = AnimationState()
    var openMouthAnimationState = AnimationState()
    var closeMouthAnimationState = AnimationState()
    var moveStillAnimationState = AnimationState()
    var moveRushAnimationState = AnimationState()
    var fleeStillAnimationState = AnimationState()
    var fleeRushAnimationState = AnimationState()
    var swallowAnimationState = AnimationState()
    var swallowStartAnimationState = AnimationState()
    var swallowStopAnimationState = AnimationState()

    private var timeExposedInAir = 0
    private var moveStillStartTick = 0
    private var moveRushStartTick = 0
    private var direction: Vec3 = Vec3.ZERO

    private val moveStillDuration = ceil(BarnacleAnimation.move_still.lengthInSeconds * 60).toInt()
    private val moveRushDuration = ceil(BarnacleAnimation.move_rush.lengthInSeconds * 60).toInt()

    private var lastGoalState = -1
    private var lastRushPhase: Boolean? = null

    companion object {
        private val CURRENT_GOAL_STATE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.INT)
        private val RUSH_PHASE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.BOOLEAN)

        fun createAttributes(): AttributeSupplier.Builder =
            createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 1.5)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CURRENT_GOAL_STATE, 0)
        builder.define(RUSH_PHASE, false)
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, BarnacleFleeGoal())
        goalSelector.addGoal(1, BarnacleAttackGoal())
        goalSelector.addGoal(2, BarnacleGrabGoal())
        goalSelector.addGoal(3, BarnacleSwimTowardsGoal())
        goalSelector.addGoal(4, BarnacleIdleGoal())
    }

    override fun tick() {
        super.tick()

        if (level().isClientSide) {
            isNoGravity = true

            val currentGoal = entityData.get(CURRENT_GOAL_STATE)
            val currentRush = entityData.get(RUSH_PHASE)

            if (currentGoal != lastGoalState) {
                resetAnimationStates()
                print("Changing animation to goal state $currentGoal\n")
                lastGoalState = currentGoal
            } else if (currentGoal == 4 && currentRush != lastRushPhase) {
                print("Changing animation to rush phase $currentRush\n")
                resetAnimationStates()
                lastRushPhase = currentRush
            }

            when (currentGoal) {
                0 -> if (currentRush) moveRushAnimationState.startIfStopped(tickCount)
                else moveStillAnimationState.startIfStopped(tickCount)
                1 -> fleeRushAnimationState.startIfStopped(tickCount)
                2 -> openMouthAnimationState.startIfStopped(tickCount)
                3 -> swallowStartAnimationState.startIfStopped(tickCount)
                4 -> moveRushAnimationState.startIfStopped(tickCount)
            }
        }
    }

    private fun resetAnimationStates() {
        stillMouthCloseAnimationState.stop()
        stillMouthOpenAnimationState.stop()
        openMouthAnimationState.stop()
        closeMouthAnimationState.stop()
        moveStillAnimationState.stop()
        moveRushAnimationState.stop()
        fleeStillAnimationState.stop()
        fleeRushAnimationState.stop()
        swallowAnimationState.stop()
        swallowStartAnimationState.stop()
        swallowStopAnimationState.stop()
    }

    override fun aiStep() {
        super.aiStep()
        if (isUnderWater) {
            timeExposedInAir = 0
            airSupply = maxAirSupply
        } else {
            timeExposedInAir++
            if (timeExposedInAir >= 200) {
                hurt(damageSources().drown(), 1.0f)
            }
        }
    }

    inner class BarnacleFleeGoal : Goal() {
        override fun canUse(): Boolean {
            entityData.set(CURRENT_GOAL_STATE, 1)
            return false
        }
    }

    inner class BarnacleAttackGoal : Goal() {
        override fun canUse(): Boolean {
            entityData.set(CURRENT_GOAL_STATE, 2)
            return false
        }
    }

    inner class BarnacleGrabGoal : Goal() {
        override fun canUse(): Boolean {
            entityData.set(CURRENT_GOAL_STATE, 3)
            return false
        }
    }

    inner class BarnacleSwimTowardsGoal : Goal() {
        override fun canUse(): Boolean {
            entityData.set(CURRENT_GOAL_STATE, 4)
            return false
        }
    }

    inner class BarnacleIdleGoal : Goal() {

        override fun canUse(): Boolean {
            entityData.set(CURRENT_GOAL_STATE, 0)
            return true
        }

        override fun requiresUpdateEveryTick(): Boolean = true

        private fun barnacleSpeed(t: Double, tMax: Double, vMax: Double, k: Double = 2.0): Double {
            val ratio = (t / tMax).coerceIn(0.0, 1.0)
            return vMax * (1.0 - ratio).pow(k)
        }

        private fun getRandomDirection(): Vec3 {
            val x = (this@BarnacleEntity.random.nextDouble() * 2) - 1
            val y = ((this@BarnacleEntity.random.nextDouble() * 2) - 1) / 2
            val z = (this@BarnacleEntity.random.nextDouble() * 2) - 1
            return Vec3(x, y, z).normalize()
        }

        override fun start() {
            resetAnimationStates()
            direction = getRandomDirection()
            moveStillStartTick = tickCount
            entityData.set(RUSH_PHASE, false)
        }

        override fun tick() {
            if (!level().isClientSide) {
                val isRush = entityData.get(RUSH_PHASE)

                if (isRush) {
                    if (tickCount - moveRushStartTick >= moveRushDuration) {
                        entityData.set(RUSH_PHASE, false)
                        moveStillStartTick = tickCount
                        if (random.nextDouble() < 0.2) direction = getRandomDirection()
                    } else {
                        val t = tickCount - moveRushStartTick
                        deltaMovement = direction.scale(barnacleSpeed(t.toDouble(), moveRushDuration.toDouble(), 1.5))
                    }
                } else {
                    if (tickCount - moveStillStartTick >= moveStillDuration) {
                        entityData.set(RUSH_PHASE, true)
                        moveRushStartTick = tickCount
                    }
                }
            }
        }
    }
}
