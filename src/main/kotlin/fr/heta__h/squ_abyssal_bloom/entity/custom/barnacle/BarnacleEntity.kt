package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.damage_type.ModDamagesTypes
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.control.BarnacleBehaviorState
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.control.BarnacleMoveControl
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.control.BarnaclePathController
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.control.BarnacleTargeting
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal.BarnacleFleeBehaviorGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal.BarnacleGoalPriorities
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal.BarnacleGrabBehaviorGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal.BarnacleIdleBehaviorGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal.BarnaclePursueGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal.BarnacleSwallowGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal.BarnacleTargetGoal
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.atan2
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
    var behaviorAnimationStartTick = 0
    private var regenCooldown = 0
    val behaviorPathController = BarnaclePathController(this)

    val myTarget: LivingEntity?
        get() = targetWithin(captureDistance)
    val trackedTarget: LivingEntity?
        get() = targetWithin(detectionRange)
    val captureDistance: Double
        get() = ModServerConfig.BARNACLE_CAPTURE_DISTANCE.get()
    val detectionRange: Double
        get() = ModServerConfig.BARNACLE_DETECTION_RANGE.get()
    val holdDistance: Double
        get() = ModServerConfig.BARNACLE_HOLD_DISTANCE.get()
    val pursuitSpeed: Float
        get() = ModServerConfig.BARNACLE_MOVEMENT_SPEED.get().toFloat()
    val fleeSpeed: Float
        get() = ModServerConfig.BARNACLE_FLEE_SPEED.get().toFloat()
    val isHoldingTarget: Boolean
        get() = entityData.get(MOUTH_OPEN) || entityData.get(IS_SWALLOWING)
    private val healthRatio: Float
        get() = this.health / this.maxHealth
    val isHealthCritical: Boolean
        get() = healthRatio <= CRITICAL_HEALTH_RATIO
    private var stomach: MutableList<ItemStack> = mutableListOf()


    private var lastGoalState: BarnacleBehaviorState? = null
    private var lastRushPhase: Boolean? = null
    private var lastIsIdle: Boolean? = null
    private var lastMouthOpen: Boolean? = null
    private var lastSwallowing: Boolean? = null
    private var animationResetScheduled = false

    init {
        this.xpReward = XP_REWARD
        this.moveControl = BarnacleMoveControl(this)
    }

    companion object {
        // BASE STATS
        private const val DEFAULT_FOLLOW_RANGE_ATTRIBUTE = 55.0
        const val XP_REWARD = 15
        const val CRITICAL_HEALTH_RATIO = 0.25f
        const val REGEN_HEAL_AMOUNT = 1.0f
        const val MAX_AIR_TICKS = 200
        const val DROWN_DAMAGE = 2.0f
        const val TARGET_SCAN_INTERVAL_TICKS = 5

        // FLOP
        const val FLOP_DELAY_TICKS = 20
        const val FLOP_MOTION_Y = 0.45
        const val FLOP_MOTION_XZ = 0.1
        const val BODY_ROTATION_SPEED = 10f
        const val MIN_ACTUAL_MOVEMENT_LENGTH_SQR = 1.0E-6

        // SOUNDS
        const val DEFAULT_SOUND_VOLUME = 1.3f
        const val AMBIENT_SOUND_INTERVAL = 550
        const val INK_PARTICLE_COUNT = 500
        const val INK_SOUND_PITCH_BASE = 1.0f
        const val INK_SOUND_PITCH_VAR = 0.2f
        const val BLINDNESS_DURATION_TICKS = 40
        const val BLINDNESS_AMPLIFIER = 1

        // FLEE
        const val FLEE_ANIM_FPS = 20.0
        const val FLEE_RADIUS = 17.0
        const val FLEE_SPEED_K = 3.0f
        const val FLEE_JITTER_OFFSET = 0.15
        const val FLEE_JITTER_SCALE = 0.2
        const val FLEE_STILL_FRICTION = 0.8

        // ATTACK / SWALLOW
        const val SWALLOW_START_FPS = 21.0
        const val SWALLOW_STOP_FPS = 21.0
        const val SWALLOW_FPS = 20.0
        const val ATTACK_START_DIST = 4.5
        const val SWALLOW_DAMAGE = 3f
        const val SPIT_ITEM_DELAY_TICKS = 3
        const val SPIT_OFFSET_FWD = 1.2
        const val SPIT_OFFSET_Y = 0.7
        const val SPIT_MOTION_FWD = 0.3
        const val SPIT_MOTION_Y = 0.2
        const val SPIT_SOUND_VOL = 0.5f
        const val SPIT_SOUND_PITCH = 0.5f

        // GRAB
        const val GRAB_ANIM_FPS = 21.0
        const val GRAB_HOLD_DURATION_TICKS = 7
        const val GRAB_HOLD_FACTOR = 4.5
        const val GRAB_OPEN_SOUND_DELAY_TICKS = 2
        const val GRAB_CLOSE_SOUND_DELAY_TICKS = 2

        // SWIM
        const val SWIM_ANIM_FPS = 22.0
        const val SWIM_SPEED_K = 2.5f

        // IDLE
        const val IDLE_ANIM_FPS = 30.0
        const val IDLE_MIN_TICKS = 60
        const val IDLE_MAX_TICKS = 150
        const val IDLE_MAX_SPEED = 1.0f
        const val IDLE_WATER_CHECK_DIST = 10
        const val IDLE_DIR_CHANCE_1 = 0.33
        const val IDLE_DIR_CHANCE_2 = 0.55

        // ANIMATION
        const val ANIM_FLEE_STILL_S = 0.7083f
        const val ANIM_FLEE_RUSH_S = 1.25f
        const val ANIM_MOVE_STILL_S = 0.7083f
        const val ANIM_MOVE_RUSH_S = 0.875f
        const val ANIM_MOUTH_OPEN_S = 0.3333f
        const val ANIM_MOUTH_CLOSE_S = 0.75f
        const val ANIM_SWALLOW_START_S = 0.2083f
        const val ANIM_SWALLOW_STOP_S = 0.375f
        const val ANIM_SWALLOW_S = 0.375f

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
        val TARGET: EntityDataAccessor<Optional<EntityReference<LivingEntity>>> =
            SynchedEntityData.defineId(
                BarnacleEntity::class.java,
                EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE
            )
        private val DIR_X: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.FLOAT)
        private val DIR_Y: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.FLOAT)
        private val DIR_Z: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(BarnacleEntity::class.java, EntityDataSerializers.FLOAT)

        fun createAttributes(): AttributeSupplier.Builder =
            createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ATTACK_DAMAGE, SWALLOW_DAMAGE.toDouble())
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
                .add(Attributes.FOLLOW_RANGE, DEFAULT_FOLLOW_RANGE_ATTRIBUTE)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CURRENT_GOAL_STATE, BarnacleBehaviorState.FLEE.syncedId)
        builder.define(RUSH_PHASE, false)
        builder.define(IS_IDLE, false)
        builder.define(MOUTH_OPEN, false)
        builder.define(IS_SWALLOWING, false)
        builder.define(
            TARGET,
            Optional.empty<EntityReference<LivingEntity>>()
        )
        builder.define(DIR_X, 0f)
        builder.define(DIR_Y, 0f)
        builder.define(DIR_Z, 1f)
    }

    override fun registerGoals() {
        goalSelector.addGoal(BarnacleGoalPriorities.FLEE, BarnacleFleeBehaviorGoal(this))
        goalSelector.addGoal(BarnacleGoalPriorities.SWALLOW, BarnacleSwallowGoal(this))
        goalSelector.addGoal(BarnacleGoalPriorities.GRAB, BarnacleGrabBehaviorGoal(this))
        goalSelector.addGoal(BarnacleGoalPriorities.PURSUE, BarnaclePursueGoal(this))
        goalSelector.addGoal(BarnacleGoalPriorities.IDLE, BarnacleIdleBehaviorGoal(this))
        targetSelector.addGoal(0, BarnacleTargetGoal(this))
    }

    override fun createNavigation(level: Level): PathNavigation =
        WaterBoundPathNavigation(this, level).apply {
            setRequiredPathLength(detectionRange.toFloat())
        }

    override fun checkSpawnObstruction(level: LevelReader): Boolean {
        return level.noCollision(this) && level.isUnobstructed(this)
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
                return
            }

            val currentGoal = BarnacleBehaviorState.fromSyncedId(entityData.get(CURRENT_GOAL_STATE))
            val currentRush = entityData.get(RUSH_PHASE)
            val currentIsIdle = entityData.get(IS_IDLE)
            val currentMouthOpen = entityData.get(MOUTH_OPEN)
            val currentSwallowing = entityData.get(IS_SWALLOWING)

            fun getCurrentTarget(): LivingEntity? {
                val data: SynchedEntityData = this.getEntityData()
                val optional: Optional<EntityReference<LivingEntity>> = data.get(TARGET)

                if (!optional.isPresent) return null

                val ref: EntityReference<LivingEntity> = optional.get()
                return ref.getEntity(level(), LivingEntity::class.java)
            }


            var needsReset = false
            var keepClosingDuringReset = false
            var keepSwallowStopDuringReset = false

            if (currentGoal != lastGoalState) {
                needsReset = true

                if (lastGoalState == BarnacleBehaviorState.GRAB && currentGoal != BarnacleBehaviorState.SWALLOW) {
                    closeMouthAnimationState.startIfStopped(tickCount)
                    keepClosingDuringReset = true
                }

                if (lastGoalState == BarnacleBehaviorState.SWALLOW) {
                    swallowStopAnimationState.startIfStopped(tickCount)
                    keepSwallowStopDuringReset = true
                }

                lastGoalState = currentGoal
                lastRushPhase = null
                lastIsIdle = null
                lastMouthOpen = null
                lastSwallowing = null
            }

            if (!needsReset) {
                when (currentGoal) {
                    BarnacleBehaviorState.FLEE, BarnacleBehaviorState.PURSUE ->
                        if (currentRush != lastRushPhase) needsReset = true
                    BarnacleBehaviorState.IDLE -> {
                        if (currentRush != lastRushPhase) needsReset = true
                        if (currentIsIdle != lastIsIdle) needsReset = true
                    }

                    BarnacleBehaviorState.GRAB -> if (currentMouthOpen != lastMouthOpen) needsReset = false
                    BarnacleBehaviorState.SWALLOW -> if (currentSwallowing != lastSwallowing) needsReset = false
                }
            }

            lastRushPhase = currentRush
            lastIsIdle = currentIsIdle
            lastMouthOpen = currentMouthOpen
            lastSwallowing = currentSwallowing

            if (needsReset || animationResetScheduled) {
                resetAnimationStates(keepClosingDuringReset, keepSwallowStopDuringReset)
                animationResetScheduled = false
            }

            when (currentGoal) {
                BarnacleBehaviorState.IDLE -> {
                    if (currentIsIdle) {
                        stillMouthCloseAnimationState.startIfStopped(tickCount)
                    } else {
                        if (currentRush) moveRushAnimationState.startIfStopped(tickCount)
                        else moveStillAnimationState.startIfStopped(tickCount)
                    }
                }

                BarnacleBehaviorState.PURSUE -> {
                    if (currentRush) moveRushAnimationState.startIfStopped(tickCount)
                    else moveStillAnimationState.startIfStopped(tickCount)
                }

                BarnacleBehaviorState.GRAB -> {
                    if (getCurrentTarget() != null) {
                        if (!currentMouthOpen) {
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

                BarnacleBehaviorState.SWALLOW -> {
                    val currentTarget = getCurrentTarget()
                    if (currentTarget != null && currentTarget.isAlive) {
                        swallowStopAnimationState.stop()
                        if (!currentSwallowing) {
                            swallowStartAnimationState.startIfStopped(tickCount)
                            swallowAnimationState.stop()
                        } else {
                            swallowStartAnimationState.stop()
                            swallowAnimationState.startIfStopped(tickCount)
                        }
                    } else {
                        swallowStartAnimationState.stop()
                        swallowAnimationState.stop()
                        swallowStopAnimationState.startIfStopped(tickCount)
                    }
                }

                BarnacleBehaviorState.FLEE -> {
                    if (currentRush) {
                        fleeRushAnimationState.startIfStopped(tickCount)
                        fleeStillAnimationState.stop()
                    } else {
                        fleeStillAnimationState.startIfStopped(tickCount)
                        fleeRushAnimationState.stop()
                    }
                }
            }
        } else {
            val newTarget = myTarget
            val currentSyncedEntity = entityData.get(TARGET)
                .orElse(null)
                ?.getEntity(level(), LivingEntity::class.java)

            if (newTarget != currentSyncedEntity) {
                entityData.set(TARGET, Optional.ofNullable(newTarget?.let { EntityReference.of(it) }))
            }
        }
    }

    private fun resetAnimationStates(keepClosing: Boolean = false, keepSwallowStop: Boolean = false) {
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

        if (!keepSwallowStop) {
            swallowStopAnimationState.stop()
        }
    }

    override fun aiStep() {
        super.aiStep()
        if (level().isClientSide) return
        if (isUnderWater) {
            timeExposedInAir = 0
            airSupply = maxAirSupply

            val regenCooldownTicks = ModServerConfig.BARNACLE_REGEN_COOLDOWN.get()
            val notInCombat = lastHurtByMobTimestamp + regenCooldownTicks < tickCount
            if (notInCombat && health < maxHealth) {
                regenCooldown++
                if (regenCooldown >= regenCooldownTicks) {
                    heal(REGEN_HEAL_AMOUNT)
                    regenCooldown = 0
                }
            } else {
                regenCooldown = 0
            }

        } else {
            timeExposedInAir++
            if (timeExposedInAir >= MAX_AIR_TICKS) {
                hurtServer(
                    level() as ServerLevel,
                    damageSources().drown(),
                    DROWN_DAMAGE)
            }

            if (onGround() && tickCount > FLOP_DELAY_TICKS) {
                deltaMovement = deltaMovement.add(0.0, FLOP_MOTION_Y, 0.0)
                val horizontalDir = getRandomDirection().normalize()
                deltaMovement = deltaMovement.add(horizontalDir.x * FLOP_MOTION_XZ, 0.0, horizontalDir.z * FLOP_MOTION_XZ)
                yRot = atan2(horizontalDir.z, horizontalDir.x).toFloat() * (180f / Math.PI.toFloat()) - 90f
                xRot = 0f
                setMovementDirection(horizontalDir)
                setOnGround(false)
                playSound(getFlopSound())
            }
        }
    }


    private fun targetWithin(radius: Double): LivingEntity? = target?.takeIf {
        BarnacleTargeting.isEligibleTarget(this, it, radius)
    }

    fun getMyDir(target: LivingEntity?): Vec3? {
        val tgt = target ?: return null
        val targetPos = tgt.position().add(0.0, 1.0, 0.0)
        return targetPos.subtract(this@BarnacleEntity.position()).normalize()
    }

    override fun getAmbientSound(): SoundEvent? {
        return ModSounds.BARNACLE_AMBIENT.get()
    }

//    override fun playAmbientSound() {
//        val soundEvent = this.ambientSound
//        if (soundEvent != null) {
//            this.playSound(soundEvent, getSoundVolume() * 1.5f, this.voicePitch)
//        }
//    }

    override fun getHurtSound(source: DamageSource): SoundEvent = ModSounds.BARNACLE_HURT.get()


    override fun getDeathSound(): SoundEvent = ModSounds.BARNACLE_DEATH.get()

    fun getFlopSound(): SoundEvent = ModSounds.BARNACLE_FLOP.get()

    override fun getSoundVolume(): Float = DEFAULT_SOUND_VOLUME

    override fun getAmbientSoundInterval(): Int = AMBIENT_SOUND_INTERVAL

    fun getRandomDirection(): Vec3 {
        val x = (this.random.nextDouble() * 2) - 1
        val y = (this.random.nextDouble() * 2) - 1
        val z = (this.random.nextDouble() * 2) - 1
        return Vec3(x, y, z).normalize()
    }

    private fun updateBodyRotation() {
        val actualMovement = Vec3(x - xo, y - yo, z - zo)
        val dir = if (actualMovement.lengthSqr() >= MIN_ACTUAL_MOVEMENT_LENGTH_SQR) {
            actualMovement.normalize()
        } else {
            getMovementDirection()
        }
        if (dir.lengthSqr() < 0.01) return

        val targetYaw = (atan2(dir.z, dir.x) * (180.0 / Math.PI)).toFloat() - 90.0f
        val targetPitch = (atan2(dir.y, sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0 / Math.PI)).toFloat()

        this.yRot = Mth.approachDegrees(this.yRot, targetYaw, BODY_ROTATION_SPEED)
        this.xRot = Mth.approachDegrees(this.xRot, -targetPitch, BODY_ROTATION_SPEED)

        this.yBodyRot = this.yRot
        this.yHeadRot = this.yRot
    }

    fun barnacleSpeed(t: Float, tMax: Float, vMax: Float, k: Float = 2.0f): Float {
        val ratio = (t / tMax).coerceIn(0.0f, 1.0f)
        return vMax * (1.0f - ratio).pow(k)
    }

    fun hasWaterAhead(
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

    fun holdTarget(tgt: LivingEntity, factor: Double) {
        val dir = getMovementDirection()

        val holdPos = this.position().add(dir.scale(factor))

        if (tgt.isPassenger) {
            tgt.stopRiding()
        }

        tgt.fallDistance = 0.0
        tgt.addEffect(MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, BLINDNESS_AMPLIFIER, true, false))

        val pullVec = holdPos.subtract(tgt.position())
        val distance = pullVec.length()

        if (distance > 0.1) {
            val pullSpeed = (distance * 0.4).coerceAtMost(1.5)
            tgt.deltaMovement = pullVec.normalize().scale(pullSpeed)
        } else {
            tgt.deltaMovement = Vec3.ZERO
        }

        tgt.hurtMarked = true
    }

    fun spawnInk() {
        val level = this.level()
        if (level is ServerLevel) {

            this.playSound(
                ModSounds.BARNACLE_SHOOT.get(),
                1.0f,
                (this.random.nextFloat() - this.random.nextFloat()) * INK_SOUND_PITCH_VAR + INK_SOUND_PITCH_BASE
            )

            level.sendParticles(
                ParticleTypes.SQUID_INK,
                this.x, this.eyeY, this.z,
                INK_PARTICLE_COUNT,
                2.0, 2.0, 4.0,
                0.3
            )
        }
    }

    fun addToBeExpelled(itemStack: ItemStack) {
        stomach.add(itemStack)
    }

    fun setMovementDirection(direction: Vec3) {
        entityData.set(DIR_X, direction.x.toFloat())
        entityData.set(DIR_Y, direction.y.toFloat())
        entityData.set(DIR_Z, direction.z.toFloat())
    }

    fun getMovementDirection(): Vec3 {
        val x = entityData.get(DIR_X).toDouble()
        val y = entityData.get(DIR_Y).toDouble()
        val z = entityData.get(DIR_Z).toDouble()
        return Vec3(x, y, z)
    }


    var rushPhase: Boolean
        get() = entityData.get(RUSH_PHASE)
        set(value) = entityData.set(RUSH_PHASE, value)

    var idlePhase: Boolean
        get() = entityData.get(IS_IDLE)
        set(value) = entityData.set(IS_IDLE, value)

    var mouthOpen: Boolean
        get() = entityData.get(MOUTH_OPEN)
        set(value) = entityData.set(MOUTH_OPEN, value)

    var swallowing: Boolean
        get() = entityData.get(IS_SWALLOWING)
        set(value) = entityData.set(IS_SWALLOWING, value)

    fun ensureGoalState(state: BarnacleBehaviorState) {
        if (entityData.get(CURRENT_GOAL_STATE) != state.syncedId) {
            entityData.set(CURRENT_GOAL_STATE, state.syncedId)
        }
    }

    val hasStomachContents: Boolean
        get() = stomach.isNotEmpty()

    fun spitNextStomachItem(): Boolean {
        if (stomach.isEmpty()) return false

        val stack = stomach.removeAt(0)
        val lookDir = getMovementDirection()
        val spawnPos = position().add(lookDir.scale(SPIT_OFFSET_FWD)).add(0.0, SPIT_OFFSET_Y, 0.0)
        val itemEntity = ItemEntity(level(), spawnPos.x, spawnPos.y, spawnPos.z, stack)
        itemEntity.deltaMovement = lookDir.scale(SPIT_MOTION_FWD).add(0.0, SPIT_MOTION_Y, 0.0)
        level().addFreshEntity(itemEntity)
        level().playSound(
            null,
            blockPosition(),
            SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE,
            SoundSource.HOSTILE,
            SPIT_SOUND_VOL,
            SPIT_SOUND_PITCH
        )
        return true
    }

    fun swallowDamageSource(): DamageSource {
        return DamageSource(
            level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(ModDamagesTypes.BARNACLE_SWALLOW),
            this
        )
    }

    override fun dropCustomDeathLoot(level: ServerLevel, source: DamageSource, recentlyHit: Boolean) {
        super.dropCustomDeathLoot(level, source, recentlyHit)

        for (stack in stomach) {
            if (!stack.isEmpty) {
                val itemEntity = ItemEntity(level, x, y, z, stack)
                itemEntity.setDefaultPickUpDelay()
                level.addFreshEntity(itemEntity)
            }
        }
        stomach.clear()
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        val validItems = this.stomach.filter { !it.isEmpty }

        output.store("stomach", ItemStack.CODEC.listOf(), validItems)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)

        val loadedItems = input.read("stomach", ItemStack.CODEC.listOf())

        stomach.clear()
        loadedItems.ifPresent { items ->
            stomach.addAll(items)
        }
    }

}
