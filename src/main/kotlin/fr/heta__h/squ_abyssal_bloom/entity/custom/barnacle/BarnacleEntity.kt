package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl
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
    private var lookControl = SmoothSwimmingLookControl(this, 10)

    private val moveStillDuration = ceil(BarnacleAnimation.move_still.lengthInSeconds * 30).toInt()
    private val moveRushDuration = ceil(BarnacleAnimation.move_rush.lengthInSeconds * 30).toInt()

    private var lastGoalState = -1
    private var lastRushPhase: Boolean? = null

    companion object {
        private val CURRENT_GOAL_STATE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.INT)
        private val RUSH_PHASE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val DIR_X: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.FLOAT)
        private val DIR_Y: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.FLOAT)
        private val DIR_Z: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.FLOAT)

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
        builder.define(DIR_X, 0f)
        builder.define(DIR_Y, 0f)
        builder.define(DIR_Z, 0f)
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
            updateBodyRotation()
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

    override fun travel(travelVector: Vec3) {
        if (isEffectiveAi && isInWater) {
            moveRelative(0.01f, travelVector)
            move(MoverType.SELF, deltaMovement)
            deltaMovement = deltaMovement.scale(0.9)
        } else {
            super.travel(travelVector)
        }
    }


    private fun getRandomDirection(): Vec3 {
        val x = (this.random.nextDouble() * 2) - 1
        val y = (this.random.nextDouble() * 2) - 1
        val z = (this.random.nextDouble() * 2) - 1
        return Vec3(x, y, z).normalize()
    }

    fun updateBodyRotation() {
        var movement = deltaMovement
        if (movement.lengthSqr() < 0.01) movement = getDirectionFromData()

        val targetX = x + 100 * movement.x
        val targetY = y + 100 * movement.y
        val targetZ = z + 100 * movement.z
        lookControl.setLookAt(targetX, targetY, targetZ)
        lookControl.tick()
    }


    private fun barnacleSpeed(t: Double, tMax: Double, vMax: Double, k: Double = 2.0): Double {
        val ratio = (t / tMax).coerceIn(0.0, 1.0)
        return vMax * (1.0 - ratio).pow(k)
    }

    private fun setDirectionInData(direction: Vec3) {
        entityData.set(DIR_X, direction.x.toFloat())
        entityData.set(DIR_Y, direction.y.toFloat())
        entityData.set(DIR_Z, direction.z.toFloat())
    }

    private fun getDirectionFromData(): Vec3 {
        val x = entityData.get(DIR_X).toDouble()
        val y = entityData.get(DIR_Y).toDouble()
        val z = entityData.get(DIR_Z).toDouble()
        return Vec3(x, y, z)
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

        override fun canUse(): Boolean = true
        override fun canContinueToUse(): Boolean = true
        override fun requiresUpdateEveryTick(): Boolean = true
        override fun isInterruptable(): Boolean = true

        override fun start() {
            if (!level().isClientSide) {
                entityData.set(CURRENT_GOAL_STATE, 4)
                entityData.set(RUSH_PHASE, false)
                moveStillStartTick = tickCount

                setDirectionInData(getRandomDirection().normalize())
                deltaMovement = Vec3.ZERO
            }
        }

        override fun tick() {
            if (level().isClientSide) return

            val isRush = entityData.get(RUSH_PHASE)

            if (isRush) {
                handleRush()
            } else {
                handleIdle()
            }
        }

        private fun handleIdle() {
            if (tickCount - moveStillStartTick >= moveStillDuration) {
                entityData.set(RUSH_PHASE, true)
                moveRushStartTick = tickCount
            }
        }

        private fun handleRush() {
            val t = tickCount - moveRushStartTick

            if (t >= moveRushDuration) {
                entityData.set(RUSH_PHASE, false)
                moveStillStartTick = tickCount

                deltaMovement = Vec3.ZERO

                if (random.nextDouble() < 0.33) setDirectionInData(getRandomDirection().normalize())
                return
            }

            if (this@BarnacleEntity.isUnderWater) {
                val world = level()
                val maxDistance = 10
                val currentPos = blockPosition()

                var blocksUp = 0
                for (i in 1..maxDistance) {
                    val posAbove = currentPos.above(i)
                    val blockState = world.getBlockState(posAbove)
                    if (blockState.isAir) {
                        break
                    }
                    blocksUp++
                }

                var dir = getDirectionFromData()

                if (blocksUp < maxDistance) {
                    val adjustFactor = (maxDistance - blocksUp).toDouble() / maxDistance.toDouble()
                    val adjustedY = dir.y - adjustFactor * 0.5
                    dir = Vec3(dir.x, adjustedY, dir.z).normalize()
                    setDirectionInData(dir)
                }

                val speed = barnacleSpeed(t.toDouble(), moveRushDuration.toDouble(), 1.5)
                deltaMovement = Vec3(
                    dir.x * speed,
                    dir.y * speed,
                    dir.z * speed
                )
            }
        }
    }
}
