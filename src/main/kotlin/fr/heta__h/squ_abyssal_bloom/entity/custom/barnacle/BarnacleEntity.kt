package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth.wrapDegrees
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
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
import java.util.*
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
    val myTarget: LivingEntity?
        get() = getMyTarget(5.5)
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
        private val TARGET: EntityDataAccessor<Optional<EntityReference<LivingEntity?>?>?> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE)
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
                .add(Attributes.FOLLOW_RANGE, 55.0)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CURRENT_GOAL_STATE, 0)
        builder.define(RUSH_PHASE, false)
        builder.define(IS_IDLE, false)
        builder.define(MOUTH_OPEN, false)
        builder.define(TARGET,
            Optional.ofNullable(myTarget?.let { EntityReference.of(it) } ) as Optional<EntityReference<LivingEntity?>?>
        )
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
                val rawTarget = entityData.get(TARGET)?.orElse(null)?.getEntity(level(), LivingEntity::class.java as Class<LivingEntity?>)
                val currentTarget = if (rawTarget != null && rawTarget.isAlive) rawTarget else null

                var needsReset = false
                var keepClosingDuringReset = false

                if (currentGoal != lastGoalState) {
                    needsReset = true

                    if (lastGoalState == 2) {
                        closeMouthAnimationState.startIfStopped(tickCount)
                        keepClosingDuringReset = true
                    }

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
                        lastMouthOpen = currentMouthOpen
                        needsReset = false
                    }
                }

                if (needsReset || animationResetScheduled) {
                    resetAnimationStates(keepClosingDuringReset)
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
                        val isMouthOpen = entityData.get(MOUTH_OPEN)
                        if (currentTarget != null) {
                            if (!isMouthOpen) {
                                openMouthAnimationState.startIfStopped(tickCount)
                                stillMouthOpenAnimationState.stop()
                            } else {
                                openMouthAnimationState.stop()
                                stillMouthOpenAnimationState.startIfStopped(tickCount)
                            }
                            closeMouthAnimationState.stop()
                        } else {
                            stillMouthOpenAnimationState.stop()
                            openMouthAnimationState.stop()
                            closeMouthAnimationState.startIfStopped(tickCount)
                        }
                    }

                    1 -> swallowAnimationState.startIfStopped(tickCount)
                    0 -> fleeStillAnimationState.startIfStopped(tickCount)
                }
            }
        } else {
            entityData.set(
                TARGET,
                Optional.ofNullable(myTarget?.let { EntityReference.of(it) } ) as Optional<EntityReference<LivingEntity?>?>
            )
        }
    }

    private fun resetAnimationStates(keepClosing: Boolean = false) {
        stillMouthCloseAnimationState.stop()
        stillMouthOpenAnimationState.stop()
        openMouthAnimationState.stop()

        if (!keepClosing) {
            closeMouthAnimationState.stop()
        }

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
                hurt(damageSources().drown(), 2.0f)
            }
            if (onGround() || deltaMovement.y == 0.0) {
                deltaMovement = deltaMovement.add(0.0, 0.45, 0.0)
                val horizontalDir = getRandomDirection().normalize()
                deltaMovement = deltaMovement.add(horizontalDir.x * 0.1, 0.0, horizontalDir.z * 0.1)
                yRot = atan2(horizontalDir.z, horizontalDir.x).toFloat() * (180f / Math.PI.toFloat()) - 90f
                setDirectionInData(horizontalDir)
                setOnGround(false)
                hasImpulse = true
                playSound(getFlopSound(), getSoundVolume(), this.voicePitch)
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

    fun getMyTarget(radius: Double): LivingEntity? {
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

    fun getMyDir(target: LivingEntity?) : Vec3? {
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

    fun getFlopSound(): SoundEvent {
        return SoundEvents.GUARDIAN_FLOP
    }

    override fun getSoundVolume(): Float = 1.3f

    override fun getAmbientSoundInterval(): Int = 550


    private fun getRandomDirection(): Vec3 {
        val x = (this.random.nextDouble() * 2) - 1
        val y = (this.random.nextDouble() * 2) - 1
        val z = (this.random.nextDouble() * 2) - 1
        return Vec3(x, y, z).normalize()
    }

    private fun updateBodyRotation() {
        val dir = getDirectionFromData()
        if (dir.lengthSqr() < 1e-6) return

        val targetYaw = (atan2(dir.z, dir.x) * (180.0 / Math.PI)).toFloat() - 90.0f
        val targetPitch = (atan2(dir.y, sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0 / Math.PI)).toFloat()

        this.yRot = rotlerp(this.yRot, targetYaw, 10f)
        this.xRot = rotlerp(this.xRot, -targetPitch, 10f)

        this.yBodyRot = this.yRot
        this.yHeadRot = this.yRot
    }

    private fun rotlerp(current: Float, target: Float, maxChange: Float): Float {
        var f = wrapDegrees(target - current)
        if (f > maxChange) f = maxChange
        if (f < -maxChange) f = -maxChange
        return current + f
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
        override fun canUse(): Boolean = false
    }

    inner class BarnacleGrabGoal : Goal() {
        private val openMouthDuration = ceil(BarnacleAnimation.mouth_open.lengthInSeconds * 21).toInt()
        private val closeMoutDuration = ceil(BarnacleAnimation.mouth_close.lengthInSeconds * 21).toInt()
        private val targetDistance = 4.0
        private var initialVelocity = Vec3.ZERO
        private var isClosing = false

        init {
            this.flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean = isUnderWater && (myTarget != null || isClosing)


        override fun start() {
            if (!level().isClientSide) {
                entityData.set(CURRENT_GOAL_STATE, 2)
                animationStartTick = tickCount
                initialVelocity = deltaMovement
                isClosing = false

                myTarget?.let { setDirectionInData(getMyDir(it) ?: getRandomDirection().normalize()) }
            }
        }

        override fun tick() {
            if (level().isClientSide) return

            val isMouthOpen = entityData.get(MOUTH_OPEN)
            val t = tickCount - animationStartTick
            val tgt = myTarget

            if (tgt == null && !isClosing) {
                isClosing = true
                animationStartTick = tickCount
                entityData.set(MOUTH_OPEN, false)
                deltaMovement = Vec3.ZERO
                return
            }

            if (tgt != null && tgt.isAlive) {
                isClosing = false

                maintenirCible(tgt)

                if (!isMouthOpen) {
                    if (t >= openMouthDuration) {
                        animationStartTick = tickCount
                        entityData.set(MOUTH_OPEN, true)
                        deltaMovement = Vec3.ZERO
                    } else {
                        val slowdownFactor = (1.0 - (t.toDouble() / openMouthDuration.toDouble())).coerceAtLeast(0.0)
                        deltaMovement = initialVelocity.scale(slowdownFactor)
                    }
                } else {
                    deltaMovement = Vec3.ZERO
                }
            }
            else if (isClosing) {
                deltaMovement = Vec3.ZERO

                if (t >= closeMoutDuration) {
                    isClosing = false
                }
            }

        }

        
        private fun maintenirCible(tgt: LivingEntity) {
            val viewVector = calculateViewVector(this@BarnacleEntity.xRot, this@BarnacleEntity.yRot)

            val realDistance = 4.0 + (3 / 2.0) + (tgt.bbWidth / 2.0)

            val holdPosition = this@BarnacleEntity.eyePosition.add(viewVector.scale(realDistance))

            val attractionVector = holdPosition.subtract(tgt.position())
            val distSq = attractionVector.lengthSqr()

            val maxSpeed = 1.2
            val pullStrength = 0.25

            var newVelocity = attractionVector.scale(pullStrength)

            if (newVelocity.lengthSqr() > maxSpeed * maxSpeed) {
                newVelocity = newVelocity.normalize().scale(maxSpeed)
            }

            if (!tgt.isInWater) {
                newVelocity = newVelocity.multiply(1.0, 0.8, 1.0)
            }

            tgt.deltaMovement = newVelocity
            tgt.hasImpulse = true
            tgt.fallDistance = 0.0

            if (tgt is ServerPlayer) {
                tgt.connection.send(ClientboundSetEntityMotionPacket(tgt))
            }

            if (distSq > 16.0) {
                tgt.setPos(holdPosition.x, holdPosition.y, holdPosition.z)
            }
        }

        private fun calculateViewVector(xRot: Float, yRot: Float): Vec3 {
            val f = xRot * (Math.PI.toFloat() / 180f)
            val g = -yRot * (Math.PI.toFloat() / 180f)
            val h = kotlin.math.cos(g)
            val i = kotlin.math.sin(g)
            val j = kotlin.math.cos(f)
            val k = kotlin.math.sin(f)
            return Vec3((i * j).toDouble(), (-k).toDouble(), (h * j).toDouble())
        }

        override fun stop() {
            if (!level().isClientSide) {
                isClosing = false
                entityData.set(MOUTH_OPEN, false)
            }
        }
    }

    inner class BarnacleSwimTowardsGoal : Goal() {

        private val moveStillDuration = ceil(BarnacleAnimation.move_still.lengthInSeconds * 22).toInt()
        private val moveRushDuration = ceil(BarnacleAnimation.move_rush.lengthInSeconds * 22).toInt()
        private val swimmTarget: LivingEntity?
            get() = getMyTarget(50.0)

        override fun canUse(): Boolean = swimmTarget != null && isUnderWater

        override fun requiresUpdateEveryTick(): Boolean = true
        override fun isInterruptable(): Boolean = true

        init {
            this.flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun start() {
            if (!level().isClientSide) {
                entityData.set(RUSH_PHASE, false)
                entityData.set(IS_IDLE, false)
                entityData.set(CURRENT_GOAL_STATE, 3)
                animationStartTick = tickCount

                setDirectionInData(getMyDir(swimmTarget) ?: getRandomDirection().normalize())
                deltaMovement = Vec3.ZERO
            } else {
                resetAnimationStates()
            }
        }

        override fun tick() {

            if (entityData.get(CURRENT_GOAL_STATE) != 3) {
                entityData.set(CURRENT_GOAL_STATE, 3)
            }

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

            val direction = getMyDir(swimmTarget) ?: getRandomDirection().normalize()
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

        init {
            this.flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

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
