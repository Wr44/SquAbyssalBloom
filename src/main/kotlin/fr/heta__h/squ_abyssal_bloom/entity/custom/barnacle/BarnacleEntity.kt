package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Mth
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

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
    private var directionMob: Vec3 = Vec3.ZERO

    private val moveStillDuration = ceil(BarnacleAnimation.move_still.lengthInSeconds * 30).toInt()
    private val moveRushDuration = ceil(BarnacleAnimation.move_rush.lengthInSeconds * 30).toInt()

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
        updateBodyRotation()
        if (level().isClientSide) {
            isNoGravity = true
            val currentGoal = entityData.get(CURRENT_GOAL_STATE)
            val currentRush = entityData.get(RUSH_PHASE)
            if (currentGoal != lastGoalState) {
                resetAnimationStates()
                lastGoalState = currentGoal
            } else if (currentGoal == 4 && currentRush != lastRushPhase) {
                resetAnimationStates()
                lastRushPhase = currentRush
            }
            when (currentGoal) {
                4 -> if (currentRush) moveRushAnimationState.startIfStopped(tickCount)
                else moveStillAnimationState.startIfStopped(tickCount)
                3 -> moveStillAnimationState.startIfStopped(tickCount)
                2 -> openMouthAnimationState.startIfStopped(tickCount)
                1 -> swallowAnimationState.startIfStopped(tickCount)
                0 -> fleeStillAnimationState.startIfStopped(tickCount)
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

    private fun getRandomDirection(): Vec3 {
        val x = (this.random.nextDouble() * 2) - 1
        val y = (this.random.nextDouble() * 2) - 1
        val z = (this.random.nextDouble() * 2) - 1
        return Vec3(x, y, z).normalize()
    }

    fun updateBodyRotation() {
        val movement = deltaMovement
        val distSqr = movement.lengthSqr()

        
        if (distSqr < 0.01) return

        
        val targetYaw = (atan2(movement.z, movement.x) * (180.0 / Math.PI)).toFloat() - 90f
        yHeadRot = rotLerpDegrees(yHeadRot, targetYaw + 20f, 3f) 
        yRot = yHeadRot 
        
        val deltaYaw = Mth.wrapDegrees(yHeadRot - yBodyRot)
        if (deltaYaw < -20f) yBodyRot -= 4f
        else if (deltaYaw > 20f) yBodyRot += 4f

        
        val horizontalDist = sqrt(movement.x * movement.x + movement.z * movement.z).coerceAtLeast(0.001)
        val targetPitch = (-atan2(movement.y, horizontalDist) * (180.0 / Math.PI)).toFloat()
        xRot = rotLerpDegrees(xRot, targetPitch + 10f, 3f) 
    }

    private fun rotLerpDegrees(current: Float, target: Float, maxChange: Float): Float {
        val delta = Mth.wrapDegrees(target - current)
        return current + delta.coerceIn(-maxChange, maxChange)
    }

    private fun barnacleSpeed(t: Double, tMax: Double, vMax: Double, k: Double = 2.0): Double {
        val ratio = (t / tMax).coerceIn(0.0, 1.0)
        return vMax * (1.0 - ratio).pow(k)
    }

    inner class BarnacleFleeGoal : Goal() {
        override fun canUse(): Boolean = false
    }

    inner class BarnacleAttackGoal : Goal() {
        override fun canUse(): Boolean = false
    }

    inner class BarnacleGrabGoal : Goal() {
        override fun canUse(): Boolean = false
    }

    inner class BarnacleSwimTowardsGoal : Goal() {
        override fun canUse(): Boolean = false
    }

    inner class BarnacleIdleGoal : Goal() {

        override fun canUse(): Boolean {
            entityData.set(CURRENT_GOAL_STATE, 4)
            return true
        }

        override fun requiresUpdateEveryTick(): Boolean = true

        override fun start() {
            resetAnimationStates()
            directionMob = getRandomDirection()
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
                        directionMob = getRandomDirection()
                    } else {
                        val t = tickCount - moveRushStartTick
                        deltaMovement = directionMob.scale(barnacleSpeed(t.toDouble(), moveRushDuration.toDouble(), 1.5))
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
