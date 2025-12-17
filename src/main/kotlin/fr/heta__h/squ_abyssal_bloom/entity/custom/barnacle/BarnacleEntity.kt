package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
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
    private var animationStartTick = 0
    private var lookControl = SmoothSwimmingLookControl(this, 10)

    private var lastGoalState = -1
    private var lastRushPhase: Boolean? = null
    private var lastIsIdle: Boolean? = null
    private var lastMouthOpen: Boolean? = null
    private var animationResetScheduled = false

    companion object {
        private val CURRENT_GOAL_STATE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.INT)
        private val RUSH_PHASE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val IS_IDLE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val MOUTH_OPEN: EntityDataAccessor<Boolean> =
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
        builder.define(IS_IDLE, false)
        builder.define(MOUTH_OPEN, false)
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
            if (!isUnderWater) {
                if (!animationResetScheduled) {
                    resetAnimationStates()
                    animationResetScheduled = true
                }
            } else {
                val currentGoal = entityData.get(CURRENT_GOAL_STATE)
                val currentRush = entityData.get(RUSH_PHASE)
                val currentIsIdle = entityData.get(IS_IDLE)
                val currentMouthOpen = entityData.get(MOUTH_OPEN)

                
                var needsReset = false

                if (currentGoal != lastGoalState) {
                    needsReset = true
                    lastGoalState = currentGoal
                    lastRushPhase = null
                    lastIsIdle = null
                    lastMouthOpen = null
                }

                
                if (currentGoal == 3 || currentGoal == 4) {
                    if (currentRush != lastRushPhase) {
                        needsReset = true
                        lastRushPhase = currentRush
                    }

                    if (currentGoal == 4 && currentIsIdle != lastIsIdle) {
                        needsReset = true
                        lastIsIdle = currentIsIdle
                    }
                }

                if (currentGoal == 2) {
                    if (currentMouthOpen != lastMouthOpen) {
                        if (currentMouthOpen == true) {
                            closeMouthAnimationState.stop()
                        } else {
                            openMouthAnimationState.stop()
                        }
                        lastMouthOpen = currentMouthOpen
                        needsReset = false
                    }
                }

                if (needsReset || animationResetScheduled) {
                    resetAnimationStates()
                    animationResetScheduled = false
                }

                when (currentGoal) {
                    4 -> {
                        if (currentIsIdle) {
                            stillMouthCloseAnimationState.startIfStopped(tickCount)
                        } else {
                            if (currentRush) moveRushAnimationState.startIfStopped(tickCount)
                            else moveStillAnimationState.startIfStopped(tickCount)
                        }
                    }

                    3 -> {
                        if (currentRush) moveRushAnimationState.startIfStopped(tickCount)
                        else moveStillAnimationState.startIfStopped(tickCount)
                    }

                    2 -> {
                        if (currentMouthOpen) {
                            closeMouthAnimationState.startIfStopped(tickCount)
                        } else {
                            openMouthAnimationState.startIfStopped(tickCount)
                        }
                    }

                    1 -> swallowAnimationState.startIfStopped(tickCount)
                    0 -> fleeStillAnimationState.startIfStopped(tickCount)
                }
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
            if (onGround() || deltaMovement.y == 0.0) {
                deltaMovement = deltaMovement.add(0.0, 0.45, 0.0)
                val horizontalDir = getRandomDirection().normalize()
                deltaMovement = deltaMovement.add(horizontalDir.x * 0.1, 0.0, horizontalDir.z * 0.1)
                yRot = atan2(horizontalDir.z, horizontalDir.x).toFloat() * (180f / Math.PI.toFloat()) - 90f
                setDirectionInData(horizontalDir)
                setOnGround(false)
                hasImpulse = true
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

    fun getTarget(radius: Double): LivingEntity? {
            val players = this@BarnacleEntity.level().getEntitiesOfClass(
                Player::class.java,
                this@BarnacleEntity.boundingBox.inflate(radius)
            ).filter { it.gameMode() == GameType.SURVIVAL }
                .sortedBy { it.distanceToSqr(this@BarnacleEntity) }

            val guardians = this@BarnacleEntity.level().getEntitiesOfClass(
                Guardian::class.java,
                this@BarnacleEntity.boundingBox.inflate(radius)
            ).sortedBy { it.distanceToSqr(this@BarnacleEntity) }

            return when {
                players.isNotEmpty() -> players.first()
                guardians.isNotEmpty() -> guardians.first()
                else -> null
            }
        }

    fun getDir(target: LivingEntity?) : Vec3? {
            val tgt = target ?: return null
            val targetPos = tgt.position().add(0.0, 1.0, 0.0)
            return targetPos.subtract(this@BarnacleEntity.position()).normalize()
        }

    override fun getAmbientSound(): SoundEvent? {
        return ModSounds.BARNACLE_AMBIENT.get()
    }

    override fun playAmbientSound() {
        val soundEvent = this.ambientSound
        if (soundEvent != null) {
            this.playSound(soundEvent, getSoundVolume()*1.5f, this.voicePitch)
        }
    }

    override fun getHurtSound(source: DamageSource): SoundEvent? = ModSounds.BARNACLE_HURT.get()


    override fun getDeathSound(): SoundEvent? = ModSounds.BARNACLE_DEATH.get()


    override fun getSoundVolume(): Float = 1.3f

    override fun getAmbientSoundInterval(): Int = 550


    private fun getRandomDirection(): Vec3 {
        val x = (this.random.nextDouble() * 2) - 1
        val y = (this.random.nextDouble() * 2) - 1
        val z = (this.random.nextDouble() * 2) - 1
        return Vec3(x, y, z).normalize()
    }

    private var lastYaw = 0f
    private var lastPitch = 0f
    private var lastUpdateTick: Int = 0

    fun updateBodyRotation() {

        if (!isUnderWater) {
            val dir = getDirectionFromData()
            if (dir.lengthSqr() < 1e-6) return
            val targetYawRad = atan2(dir.z, dir.x)
            var targetYaw = Math.toDegrees(targetYawRad).toFloat()
            targetYaw = (targetYaw - 90.0).toFloat()
            targetYaw = Mth.wrapDegrees(targetYaw)
        val deltaTicks = (tickCount - lastUpdateTick).coerceAtLeast(1)
        val lerpFactor = (deltaTicks / 5f).coerceIn(0f, 1f) 
        yRot = Mth.rotLerp(lerpFactor, lastYaw, targetYaw)
        xRot = 0f
        yHeadRot = yRot
        yBodyRot = yRot
        lastYaw = yRot
        lastPitch = xRot
        lastUpdateTick = tickCount
        return
        }

        val dir = getDirectionFromData()
        if (dir.lengthSqr() < 1e-6) return

        val horizontalLength = sqrt(dir.x * dir.x + dir.z * dir.z)
        val targetPitchRad = atan2(-dir.y, horizontalLength)
        val targetYawRad = atan2(dir.z, dir.x)

        var targetPitch = Math.toDegrees(targetPitchRad).toFloat()
        var targetYaw = Math.toDegrees(targetYawRad).toFloat()

        targetYaw = (targetYaw - 90.0).toFloat()
        targetYaw = Mth.wrapDegrees(targetYaw)
        targetPitch = Mth.wrapDegrees(targetPitch)

        val deltaTicks = (tickCount - lastUpdateTick).coerceAtLeast(1)
        val lerpFactor = (deltaTicks / 5f).coerceIn(0f, 1f) 

        yRot = Mth.rotLerp(lerpFactor, lastYaw, targetYaw)
        xRot = Mth.rotLerp(lerpFactor, lastPitch, targetPitch)
        yHeadRot = yRot
        yBodyRot = yRot

        lastYaw = yRot
        lastPitch = xRot
        lastUpdateTick = tickCount

        val lookDistance = 10.0
        val lookPos = position().add(
            dir.x * lookDistance,
            dir.y * lookDistance,
            dir.z * lookDistance
        )
        lookControl.setLookAt(lookPos.x, lookPos.y, lookPos.z)
        lookControl.tick()
    }


    private fun barnacleSpeed(t: Double, tMax: Double, vMax: Double, k: Double = 2.0): Double {
        val ratio = (t / tMax).coerceIn(0.0, 1.0)
        return vMax * (1.0 - ratio).pow(k)
    }

    private fun hasWaterAhead(
        direction: Vec3,
        distance: Int = 5
    ): Boolean {
        val world = level()
        val start = position()

        for (i in 1..distance) {
            val checkPos = start.add(
                direction.x * i,
                direction.y * i,
                direction.z * i
            )

            val blockPos = BlockPos.containing(checkPos)
            val blockState = world.getBlockState(blockPos)

            if (!blockState.fluidState.isSource) {
                return false
            }
        }
        return true
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
        override fun canUse(): Boolean = entityData.get(MOUTH_OPEN) && isUnderWater
    }

    inner class BarnacleGrabGoal : Goal() {
        private val openMouthDuration = ceil(BarnacleAnimation.mouth_open.lengthInSeconds * 21).toInt()
        private val closeMoutDuration = ceil(BarnacleAnimation.mouth_close.lengthInSeconds * 21).toInt()
        private val targetDistance = 4.0 
        private var initialVelocity = Vec3.ZERO

        val target: LivingEntity?
            get() = getTarget(5.5)
        val dir
            get() : Vec3? = getDir(target)

        override fun canUse(): Boolean = isUnderWater && (target != null || entityData.get(MOUTH_OPEN))
        override fun requiresUpdateEveryTick(): Boolean = true
        override fun isInterruptable(): Boolean = false

        override fun start() {
            if (!level().isClientSide) {
                entityData.set(CURRENT_GOAL_STATE, 2)
                animationStartTick = tickCount

                
                initialVelocity = deltaMovement

                if (target != null) {
                    setDirectionInData(dir ?: getRandomDirection().normalize())
                }
            }
        }

        override fun tick() {
            if (level().isClientSide) return

            val isMouthOpen = entityData.get(MOUTH_OPEN)
            val t = tickCount - animationStartTick
            val tgt = target

            
            if (tgt != null && tgt.isAlive && !isMouthOpen) {
                
                val directionToTarget = tgt.position().subtract(position()).normalize()
                val targetPosition = position().add(
                    directionToTarget.x * targetDistance,
                    directionToTarget.y * targetDistance,
                    directionToTarget.z * targetDistance
                )
                val movementNeeded = targetPosition.subtract(tgt.position())
                tgt.deltaMovement = movementNeeded.scale(0.3)
                tgt.hurtMarked = true

                
                if (t >= openMouthDuration) {
                    entityData.set(MOUTH_OPEN, true)
                    deltaMovement = Vec3.ZERO
                } else {
                    val slowdownFactor = 1.0 - (t.toDouble() / openMouthDuration.toDouble())
                    deltaMovement = Vec3(
                        initialVelocity.x * slowdownFactor,
                        initialVelocity.y * slowdownFactor,
                        initialVelocity.z * slowdownFactor
                    )
                }
            }
            
            else if (tgt == null && isMouthOpen) {
                if (t >= closeMoutDuration) {
                    
                    entityData.set(MOUTH_OPEN, false)
                }
                deltaMovement = Vec3.ZERO
            }
            
            else if (tgt != null && tgt.isAlive && isMouthOpen) {
                val directionToTarget = tgt.position().subtract(position()).normalize()
                val targetPosition = position().add(
                    directionToTarget.x * targetDistance,
                    directionToTarget.y * targetDistance,
                    directionToTarget.z * targetDistance
                )
                val movementNeeded = targetPosition.subtract(tgt.position())
                tgt.deltaMovement = movementNeeded.scale(0.3)
                tgt.hurtMarked = true

                deltaMovement = Vec3.ZERO
            }
            
            else {
                deltaMovement = Vec3.ZERO
            }
        }
    }

    inner class BarnacleSwimTowardsGoal : Goal() {

        private val moveStillDuration = ceil(BarnacleAnimation.move_still.lengthInSeconds * 22).toInt()
        private val moveRushDuration = ceil(BarnacleAnimation.move_rush.lengthInSeconds * 22).toInt()
        val target: LivingEntity?
            get() = getTarget(40.0)
        val dir
            get() : Vec3? = getDir(target)

        override fun canUse(): Boolean = target != null && isUnderWater
        override fun requiresUpdateEveryTick(): Boolean = true
        override fun isInterruptable(): Boolean = true

        override fun start() {
            if (!level().isClientSide) {
                entityData.set(RUSH_PHASE, false)
                entityData.set(CURRENT_GOAL_STATE, 3)
                animationStartTick = tickCount

                setDirectionInData(dir ?: getRandomDirection().normalize())
                deltaMovement = Vec3.ZERO
            } else {
                resetAnimationStates()
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
            val t = tickCount - animationStartTick

            if (t >= moveStillDuration) {
                entityData.set(RUSH_PHASE, true)
                animationStartTick = tickCount
            }

            deltaMovement = Vec3.ZERO
        }

        private fun handleRush() {
            val t = tickCount - animationStartTick

            if (t >= moveRushDuration) {
                entityData.set(RUSH_PHASE, false)
                animationStartTick = tickCount

                deltaMovement = Vec3.ZERO
                return
            }

            val direction = dir ?: getRandomDirection()
            setDirectionInData(direction)

            val speed = barnacleSpeed(t.toDouble(), moveRushDuration.toDouble(), 1.5, 3.0)
            deltaMovement = Vec3(
                direction.x * speed,
                direction.y * speed,
                direction.z * speed
            )
        }
    }


    inner class BarnacleIdleGoal : Goal() {

        override fun canUse(): Boolean = isUnderWater
        override fun requiresUpdateEveryTick(): Boolean = true
        override fun isInterruptable(): Boolean = true

        private val moveStillDuration = ceil(BarnacleAnimation.move_still.lengthInSeconds * 30).toInt()
        private val moveRushDuration = ceil(BarnacleAnimation.move_rush.lengthInSeconds * 30).toInt()
        private var stillDuration =  0

        override fun start() {
            if (!level().isClientSide) {
                entityData.set(CURRENT_GOAL_STATE, 4)
                entityData.set(RUSH_PHASE, false)
                entityData.set(IS_IDLE, false)
                animationStartTick = tickCount

                setDirectionInData(getRandomDirection().normalize())
                deltaMovement = Vec3.ZERO
            }
        }

        override fun tick() {
            if (level().isClientSide) return

            val isIdle = entityData.get(IS_IDLE)
            val isRush = entityData.get(RUSH_PHASE)

            if (!isIdle) {
                if (isRush) {
                    handleRush()
                } else {
                    handleIdle()
                }
            } else {
                handleStill()
            }
        }

        private fun handleStill() {
            if (tickCount - animationStartTick >= stillDuration) {
                animationStartTick = tickCount
                entityData.set(IS_IDLE, false)
            }
        }

        private fun handleIdle() {
            if (tickCount - animationStartTick >= moveStillDuration) {
                entityData.set(RUSH_PHASE, true)
                animationStartTick = tickCount
            }
        }

        private fun handleRush() {
            val t = tickCount - animationStartTick

            if (t >= moveRushDuration) {
                entityData.set(RUSH_PHASE, false)
                animationStartTick = tickCount

                deltaMovement = Vec3.ZERO

                val valRandom = random.nextDouble()
                if (valRandom < 0.33) setDirectionInData(getRandomDirection().normalize()) else if (valRandom < 0.55) {
                    entityData.set(IS_IDLE, true)
                    stillDuration = random.nextInt(60, 150)
                }
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
                }  else if (!hasWaterAhead(dir)) {
                    val newDir = getRandomDirection()
                        .let { Vec3(it.x, it.y.coerceIn(-0.4, 0.4), it.z) }
                        .normalize()

                    setDirectionInData(newDir)
                }

                val speed = barnacleSpeed(t.toDouble(), moveRushDuration.toDouble(), 1.0)
                deltaMovement = Vec3(
                    dir.x * speed,
                    dir.y * speed,
                    dir.z * speed
                )
            }
        }
    }
}
