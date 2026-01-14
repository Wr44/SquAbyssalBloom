package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth.wrapDegrees
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.decoration.Mannequin
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.abs
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
    private var lastSwallowing : Boolean? = null
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
        private val IS_SWALLOWING: EntityDataAccessor<Boolean> =
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
                .add(Attributes.ATTACK_DAMAGE, 3.0)
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
        builder.define(IS_SWALLOWING, false)
        builder.define(TARGET,
            Optional.empty<EntityReference<LivingEntity>>() as Optional<EntityReference<LivingEntity?>?>
        )
        builder.define(DIR_X, 0f)
        builder.define(DIR_Y, 0f)
        builder.define(DIR_Z, 1f)
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

        if (this.isUnderWater) updateBodyRotation()

        if (level().isClientSide) {

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
                val currentSwallowing = entityData.get(IS_SWALLOWING) 

                val rawTarget = entityData.get(TARGET)?.orElse(null)?.getEntity(level(), LivingEntity::class.java as Class<LivingEntity?>)
                val currentTarget = if (rawTarget != null && rawTarget.isAlive) rawTarget else null

                var needsReset = false
                var keepClosingDuringReset = false

                if (currentGoal != lastGoalState) {
                    needsReset = true

                    if (lastGoalState == 2 && currentGoal != 1) {
                        closeMouthAnimationState.startIfStopped(tickCount)
                        keepClosingDuringReset = true
                    }

                    lastGoalState = currentGoal
                    lastRushPhase = null
                    lastIsIdle = null
                    lastMouthOpen = null
                    lastSwallowing = null
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

                else if (currentGoal == 1) {
                    if (currentSwallowing != lastSwallowing) {
                        lastSwallowing = currentSwallowing
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

                    1 -> {
                        val isSwallowing = entityData.get(IS_SWALLOWING)

                        if (currentTarget != null) {
                            swallowStopAnimationState.stop()

                            if (!isSwallowing) {
                                swallowStartAnimationState.startIfStopped(tickCount)
                                swallowAnimationState.stop()
                            } else {
                                swallowStartAnimationState.stop()
                                swallowAnimationState.startIfStopped(tickCount)
                            }
                        }
                        else {
                            swallowStartAnimationState.stop()
                            swallowAnimationState.stop()
                            swallowStopAnimationState.startIfStopped(tickCount)
                        }
                    }

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

            if (onGround() && tickCount > 20) {
                deltaMovement = deltaMovement.add(0.0, 0.45, 0.0)
                val horizontalDir = getRandomDirection().normalize()
                deltaMovement = deltaMovement.add(horizontalDir.x * 0.1, 0.0, horizontalDir.z * 0.1)
                yRot = atan2(horizontalDir.z, horizontalDir.x).toFloat() * (180f / Math.PI.toFloat()) - 90f
                xRot = 0f
                setDirectionInData(horizontalDir)
                setOnGround(false)
                hasImpulse = true
                playSound(getFlopSound())
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

    private fun isClosestBarnacle(victim: LivingEntity, searchRadius: Double): Boolean {
        val myDistanceSqr = this.distanceToSqr(victim)

        val nearbyBarnacles = this.level().getEntitiesOfClass(
            BarnacleEntity::class.java,
            this.boundingBox.inflate(searchRadius)
        )

        for (otherBarnacle in nearbyBarnacles) {
            if (otherBarnacle == this) continue

            val otherDistanceSqr = otherBarnacle.distanceToSqr(victim)

            if (otherDistanceSqr < myDistanceSqr) {
                return false
            }

            if (abs(otherDistanceSqr - myDistanceSqr) < 0.0001 && otherBarnacle.id < this.id) {
                return false
            }
        }

        return true
    }


    fun getMyTarget(radius: Double): LivingEntity? {
        val attacker = this.lastHurtByMob
        if (attacker != null && attacker.isAlive) {
            if (this.distanceToSqr(attacker) <= radius * radius) {
                val isValidPlayer = if (attacker is Player) !attacker.isCreative && !attacker.isSpectator else true

                if (attacker != this && isValidPlayer) {
                    return attacker
                }
            }
        }

        val players = this.level().getEntitiesOfClass(
            Player::class.java,
            this.boundingBox.inflate(radius)
        ).filter { (it.gameMode() == GameType.SURVIVAL || it.gameMode() == GameType.ADVENTURE) && !it.hasEffect(MobEffects.INVISIBILITY) }

        val guardians = this.level().getEntitiesOfClass(
            Guardian::class.java,
            this.boundingBox.inflate(radius)
        )

        val mannequins = this.level().getEntitiesOfClass(
            Mannequin::class.java,
            this.boundingBox.inflate(radius)
        )

        val validPlayers = players.filter { isClosestBarnacle(it, radius) }
            .sortedBy { it.distanceToSqr(this) }

        val validGuardians = guardians.filter { isClosestBarnacle(it, radius) }
            .sortedBy { it.distanceToSqr(this) }

        val validMannequins = mannequins.filter { isClosestBarnacle(it, radius) }
            .sortedBy { it.distanceToSqr(this) }

        return when {
            validPlayers.isNotEmpty() -> validPlayers.first()
            validGuardians.isNotEmpty() -> validGuardians.first()
            validMannequins.isNotEmpty() -> validMannequins.first()
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

    private fun maintenirCible(tgt: LivingEntity, factor: Double) {
        val dir = getDirectionFromData() ?: return

        val holdPos = this.position().add(dir.scale(factor))

        tgt.deltaMovement = Vec3.ZERO
        tgt.hasImpulse = true
        tgt.fallDistance = 0.0
        tgt.addEffect(MobEffectInstance(MobEffects.BLINDNESS,40, 1, true, false))


        if (tgt is ServerPlayer) {
            tgt.connection.teleport(holdPos.x, holdPos.y, holdPos.z, tgt.yRot, tgt.xRot)
            tgt.connection.send(ClientboundSetEntityMotionPacket(tgt))
        } else {
            tgt.setPos(holdPos.x, holdPos.y, holdPos.z)
        }
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
        private val startSwallowDuration = ceil(BarnacleAnimation.swallow_start.lengthInSeconds * 21).toInt()
        private val stopSwallowDuration = ceil(BarnacleAnimation.swallow_stop.lengthInSeconds * 21).toInt()
        private val swallowDuration = ceil(BarnacleAnimation.swallow.lengthInSeconds * 20).toInt()

        private val closeSoundDelay = 2
        private val factor = Pair(4.5, 1.5)

        private var isFinishingAnimation: Boolean = false

        init {
            this.flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean {
            return isUnderWater && (entityData.get(IS_SWALLOWING) || (myTarget != null && entityData.get(MOUTH_OPEN)))
        }

        override fun start() {
            if (!level().isClientSide) {
                entityData.set(CURRENT_GOAL_STATE, 1)
                animationStartTick = tickCount

                myTarget?.let { setDirectionInData(getMyDir(it) ?: getRandomDirection().normalize()) }
            }
        }

        override fun tick() {
            if (level().isClientSide) return

            val t = tickCount - animationStartTick
            val isSwallowing = entityData.get(IS_SWALLOWING)
            val target = myTarget

            if (target == null || !target.isAlive) {
                if (!isFinishingAnimation) {
                    isFinishingAnimation = true
                    animationStartTick = tickCount
                    entityData.set(IS_SWALLOWING, false)
                    return
                }

                deltaMovement = Vec3.ZERO

                if (t == closeSoundDelay) {
                    playSound(ModSounds.BARNACLE_CLOSE_MOUTH.get())
                }

                if (t >= stopSwallowDuration) {
                    isFinishingAnimation = false
                }
                return
            }

            isFinishingAnimation = false

            if (!isSwallowing) {
                if (t >= startSwallowDuration) {
                    animationStartTick = tickCount
                    entityData.set(IS_SWALLOWING, true)

                    maintenirCible(target, factor.second)
                } else {
                    val progress = t.toDouble() / startSwallowDuration.toDouble()
                    val currentDist = factor.first + (factor.second - factor.first) * progress
                    maintenirCible(target, currentDist)
                }
            } else {
                deltaMovement = Vec3.ZERO

                maintenirCible(target, factor.second)

                if ((t % ceil((swallowDuration/2).toDouble())).toInt() == 0) {
                    target.hurt(damageSources().mobAttack(this@BarnacleEntity), 3f)
                }
            }
        }

        override fun stop() {
            if (!level().isClientSide) {
                entityData.set(IS_SWALLOWING, false)
                entityData.set(MOUTH_OPEN, false)
                isFinishingAnimation = false
            }
        }
    }

    inner class BarnacleGrabGoal : Goal() {
        private val openMouthDuration = ceil(BarnacleAnimation.mouth_open.lengthInSeconds * 21).toInt()
        private val closeMouthDuration = ceil(BarnacleAnimation.mouth_close.lengthInSeconds * 21).toInt()

        private val holdOpenDuration = 7

        private val factor = 4.5
        private val openSoundDelay = 2
        private val closeSoundDelay = openSoundDelay

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
                maintenirCible(tgt, factor)

                if (!isMouthOpen) {
                    if (t == openSoundDelay) {
                        playSound(ModSounds.BARNACLE_OPEN_MOUTH.get())
                    }

                    if (t >= openMouthDuration + holdOpenDuration) {
                        animationStartTick = tickCount
                        entityData.set(MOUTH_OPEN, true)
                        deltaMovement = Vec3.ZERO
                    }
                    else if (t >= openMouthDuration) {
                        deltaMovement = Vec3.ZERO
                    }
                    else {
                        val slowdownFactor = (1.0 - (t.toDouble() / openMouthDuration.toDouble())).coerceAtLeast(0.0)
                        deltaMovement = initialVelocity.scale(slowdownFactor)
                    }
                } else {
                    deltaMovement = Vec3.ZERO
                }
            }
            else if (isClosing) {
                deltaMovement = Vec3.ZERO
                if (t == closeSoundDelay) {
                    playSound(ModSounds.BARNACLE_CLOSE_MOUTH.get())
                }
                if (t >= closeMouthDuration) {
                    isClosing = false
                }
            }
        }

        override fun stop() {
            if (!level().isClientSide) {
                isClosing = false
                val handingOverToAttack = myTarget != null && entityData.get(MOUTH_OPEN)
                if (!handingOverToAttack) {
                    entityData.set(MOUTH_OPEN, false)
                }
            }
        }
    }

    inner class BarnacleSwimTowardsGoal : Goal() {

        private val moveStillDuration = ceil(BarnacleAnimation.move_still.lengthInSeconds * 22).toInt()
        private val moveRushDuration = ceil(BarnacleAnimation.move_rush.lengthInSeconds * 22).toInt()
        private val swimmTarget: LivingEntity?
            get() = getMyTarget(55.0)

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
