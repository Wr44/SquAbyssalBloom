package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.damage_type.ModDamagesTypes
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.findWaterSurface
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth.wrapDegrees
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
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
    private var regenCooldown = 0
    private var cachedTarget: LivingEntity? = null
    private var lastTargetScanTick: Int = -TARGET_SCAN_INTERVAL_TICKS

    val myTarget: LivingEntity?
        get() = getMyTarget(DEFAULT_TARGET_RADIUS)
    private val healthRatio: Float
        get() = this.health / this.maxHealth
    private val isHealthCritical: Boolean
        get() = healthRatio <= CRITICAL_HEALTH_RATIO
    private var stomach: MutableList<ItemStack> = mutableListOf()


    private var lastGoalState = -1
    private var lastRushPhase: Boolean? = null
    private var lastIsIdle: Boolean? = null
    private var lastMouthOpen: Boolean? = null
    private var lastSwallowing: Boolean? = null
    private var animationResetScheduled = false

    init {
        this.xpReward = XP_REWARD
    }

    companion object {
        
        const val XP_REWARD = 15
        const val CRITICAL_HEALTH_RATIO = 0.25f
        const val REGEN_COOLDOWN_TICKS = 100
        const val REGEN_HEAL_AMOUNT = 1.0f
        const val MAX_AIR_TICKS = 200
        const val DROWN_DAMAGE = 2.0f
        const val TARGET_SCAN_INTERVAL_TICKS = 5
        const val DEFAULT_TARGET_RADIUS = 5.5

        
        const val FLOP_DELAY_TICKS = 20
        const val FLOP_MOTION_Y = 0.45
        const val FLOP_MOTION_XZ = 0.1
        const val BODY_ROTATION_SPEED = 10f

        
        const val DEFAULT_SOUND_VOLUME = 1.3f
        const val AMBIENT_SOUND_INTERVAL = 550
        const val INK_PARTICLE_COUNT = 500
        const val INK_SOUND_PITCH_BASE = 1.0f
        const val INK_SOUND_PITCH_VAR = 0.2f
        const val BLINDNESS_DURATION_TICKS = 40
        const val BLINDNESS_AMPLIFIER = 1

        
        const val GOAL_FLEE = 0
        const val GOAL_ATTACK = 1
        const val GOAL_GRAB = 2
        const val GOAL_SWIM = 3
        const val GOAL_IDLE = 4

        
        const val FLEE_ANIM_FPS = 20.0
        const val FLEE_RADIUS = 17.0
        const val FLEE_MAX_SPEED = 3.0f
        const val FLEE_SPEED_K = 3.0f
        const val FLEE_JITTER_OFFSET = 0.15
        const val FLEE_JITTER_SCALE = 0.2
        const val FLEE_STILL_FRICTION = 0.8

        
        const val SWALLOW_START_FPS = 21.0
        const val SWALLOW_STOP_FPS = 21.0
        const val SWALLOW_FPS = 20.0
        const val ATTACK_START_DIST = 4.5
        const val ATTACK_HOLD_DIST = 1.5
        const val SWALLOW_DAMAGE = 3f
        const val SPIT_ITEM_DELAY_TICKS = 3
        const val SPIT_OFFSET_FWD = 1.2
        const val SPIT_OFFSET_Y = 0.7
        const val SPIT_MOTION_FWD = 0.3
        const val SPIT_MOTION_Y = 0.2
        const val SPIT_SOUND_VOL = 0.5f
        const val SPIT_SOUND_PITCH = 0.5f

        
        const val GRAB_ANIM_FPS = 21.0
        const val GRAB_HOLD_DURATION_TICKS = 7
        const val GRAB_HOLD_FACTOR = 4.5
        const val GRAB_OPEN_SOUND_DELAY_TICKS = 2
        const val GRAB_CLOSE_SOUND_DELAY_TICKS = 2

        
        const val SWIM_ANIM_FPS = 22.0
        const val SWIM_TARGET_RADIUS = 55.0
        const val SWIM_MAX_SPEED = 2.0f
        const val SWIM_SPEED_K = 2.5f

        
        const val IDLE_ANIM_FPS = 30.0
        const val IDLE_MIN_TICKS = 60
        const val IDLE_MAX_TICKS = 150
        const val IDLE_MAX_SPEED = 1.0f
        const val IDLE_WATER_CHECK_DIST = 10
        const val IDLE_DIR_CHANCE_1 = 0.33
        const val IDLE_DIR_CHANCE_2 = 0.55

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
                .add(Attributes.FOLLOW_RANGE, SWIM_TARGET_RADIUS)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CURRENT_GOAL_STATE, GOAL_FLEE)
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
        goalSelector.addGoal(GOAL_FLEE, BarnacleFleeGoal())
        goalSelector.addGoal(GOAL_ATTACK, BarnacleAttackGoal())
        goalSelector.addGoal(GOAL_GRAB, BarnacleGrabGoal())
        goalSelector.addGoal(GOAL_SWIM, BarnacleSwimTowardsGoal())
        goalSelector.addGoal(GOAL_IDLE, BarnacleIdleGoal())
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

            val currentGoal = entityData.get(CURRENT_GOAL_STATE)
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

                if (lastGoalState == GOAL_GRAB && currentGoal != GOAL_ATTACK) {
                    closeMouthAnimationState.startIfStopped(tickCount)
                    keepClosingDuringReset = true
                }

                if (lastGoalState == GOAL_ATTACK) {
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
                    GOAL_FLEE, GOAL_SWIM -> if (currentRush != lastRushPhase) needsReset = true
                    GOAL_IDLE -> {
                        if (currentRush != lastRushPhase) needsReset = true
                        if (currentIsIdle != lastIsIdle) needsReset = true
                    }

                    GOAL_GRAB -> if (currentMouthOpen != lastMouthOpen) needsReset = false
                    GOAL_ATTACK -> if (currentSwallowing != lastSwallowing) needsReset = false
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
                GOAL_IDLE -> {
                    if (currentIsIdle) {
                        stillMouthCloseAnimationState.startIfStopped(tickCount)
                    } else {
                        if (currentRush) moveRushAnimationState.startIfStopped(tickCount)
                        else moveStillAnimationState.startIfStopped(tickCount)
                    }
                }

                GOAL_SWIM -> {
                    if (currentRush) moveRushAnimationState.startIfStopped(tickCount)
                    else moveStillAnimationState.startIfStopped(tickCount)
                }

                GOAL_GRAB -> {
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

                GOAL_ATTACK -> {
                    if (getCurrentTarget() != null && getCurrentTarget()!!.isAlive) {
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

                GOAL_FLEE -> {
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
            val newTargetRef = myTarget?.let { EntityReference.of(it) }
            val currentSyncedTarget = entityData.get(TARGET)?.orElse(null) ?: null

            if (newTargetRef != currentSyncedTarget) {
                entityData.set(
                    TARGET,
                    Optional.ofNullable(newTargetRef)
                )
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

            val notInCombat = lastHurtByMobTimestamp + REGEN_COOLDOWN_TICKS < tickCount
            if (notInCombat && health < maxHealth) {
                regenCooldown++
                if (regenCooldown >= REGEN_COOLDOWN_TICKS) {
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
                setDirectionInData(horizontalDir)
                setOnGround(false)
                playSound(getFlopSound())
            }
        }
    }


    fun getMyTarget(radius: Double): LivingEntity? {
        if (this.tickCount < lastTargetScanTick + TARGET_SCAN_INTERVAL_TICKS) {
            val target = cachedTarget
            if (target != null && target.isAlive && this.distanceToSqr(target) <= radius * radius && this.target == target) {
                return target
            }
        }

        val searchBox = boundingBox.inflate(radius)

        val nearbyBarnacles = level().getEntitiesOfClass(BarnacleEntity::class.java, searchBox)

        val potentialTarget = level().getEntitiesOfClass(
            LivingEntity::class.java,
            searchBox
        ) { entity ->
            when (entity) {
                is Player ->
                    (entity.gameMode() == GameType.SURVIVAL ||
                            entity.gameMode() == GameType.ADVENTURE) &&
                            !entity.hasEffect(MobEffects.INVISIBILITY)

                is Guardian -> true
                else -> false
            } &&
                    entity.isAlive &&
                    isClosestBarnacleOptimized(entity, nearbyBarnacles) &&
                    ModUtilities.hasClearPath(level(), this, entity)
        }.minByOrNull { it.distanceToSqr(this) }

        this.target = potentialTarget
        cachedTarget = this.target
        lastTargetScanTick = this.tickCount

        return cachedTarget
    }

    private fun isClosestBarnacleOptimized(victim: LivingEntity, nearbyBarnacles: List<BarnacleEntity>): Boolean {
        val myDistanceSqr = this.distanceToSqr(victim)

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

    fun getMyDir(target: LivingEntity?): Vec3? {
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
            this.playSound(soundEvent, getSoundVolume() * 1.5f, this.voicePitch)
        }
    }

    override fun getHurtSound(source: DamageSource): SoundEvent = ModSounds.BARNACLE_HURT.get()


    override fun getDeathSound(): SoundEvent = ModSounds.BARNACLE_DEATH.get()

    fun getFlopSound(): SoundEvent = SoundEvents.GUARDIAN_FLOP

    override fun getSoundVolume(): Float = DEFAULT_SOUND_VOLUME

    override fun getAmbientSoundInterval(): Int = AMBIENT_SOUND_INTERVAL

    private fun getRandomDirection(): Vec3 {
        val x = (this.random.nextDouble() * 2) - 1
        val y = (this.random.nextDouble() * 2) - 1
        val z = (this.random.nextDouble() * 2) - 1
        return Vec3(x, y, z).normalize()
    }

    private fun updateBodyRotation() {
        val dir = getDirectionFromData()
        if (dir.lengthSqr() < 0.01) return

        val targetYaw = (atan2(dir.z, dir.x) * (180.0 / Math.PI)).toFloat() - 90.0f
        val targetPitch = (atan2(dir.y, sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0 / Math.PI)).toFloat()

        this.yRot = rotlerp(this.yRot, targetYaw, BODY_ROTATION_SPEED)
        this.xRot = rotlerp(this.xRot, -targetPitch, BODY_ROTATION_SPEED)

        this.yBodyRot = this.yRot
        this.yHeadRot = this.yRot
    }

    private fun rotlerp(current: Float, target: Float, maxChange: Float): Float {
        var f = wrapDegrees(target - current)
        if (f > maxChange) f = maxChange
        if (f < -maxChange) f = -maxChange
        return current + f
    }

    private fun barnacleSpeed(t: Float, tMax: Float, vMax: Float, k: Float = 2.0f): Float {
        val ratio = (t / tMax).coerceIn(0.0f, 1.0f)
        return vMax * (1.0f - ratio).pow(k)
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

        if (tgt.isPassenger) {
            tgt.stopRiding()
        }

        tgt.deltaMovement = Vec3.ZERO
        tgt.fallDistance = 0.0
        tgt.addEffect(MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, BLINDNESS_AMPLIFIER, true, false))

        if (tgt is ServerPlayer) {
            tgt.connection.teleport(holdPos.x, holdPos.y, holdPos.z, tgt.yRot, tgt.xRot)
            tgt.connection.send(ClientboundSetEntityMotionPacket(tgt))
        } else {
            tgt.setPos(holdPos.x, holdPos.y, holdPos.z)
        }
    }

    fun spawnInk() {
        val level = this.level()
        if (level is ServerLevel) {

            this.playSound(
                SoundEvents.SQUID_SQUIRT,
                1.0f,
                (this.random.nextFloat() - this.random.nextFloat()) * INK_SOUND_PITCH_VAR + INK_SOUND_PITCH_BASE
            )

            level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SQUID_INK,
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


    private fun ensureGoalState(state: Int) {
        if (entityData.get(CURRENT_GOAL_STATE) != state) {
            entityData.set(CURRENT_GOAL_STATE, state)
        }
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


    inner class BarnacleFleeGoal : Goal() {

        private val moveStillDuration =
            ceil(BarnacleAnimation.flee_still.lengthInSeconds * FLEE_ANIM_FPS).toInt()
        private val moveRushDuration =
            ceil(BarnacleAnimation.flee_rush.lengthInSeconds * FLEE_ANIM_FPS).toInt()

        private val fleeRadiusSqr = FLEE_RADIUS * FLEE_RADIUS

        private var isPlaying = false
        private var previousThreat: LivingEntity = this@BarnacleEntity

        init {
            flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean =
            isPlaying || (
                    isUnderWater &&
                            !entityData.get(MOUTH_OPEN) &&
                            isHealthCritical &&
                            getAnyThreat() != null
                    )

        override fun start() {
            if (level().isClientSide) return

            ensureGoalState(GOAL_FLEE)
            entityData.set(RUSH_PHASE, false)
            animationStartTick = tickCount

            getAnyThreat()?.let { setFleeDirection(it) }
        }

        override fun tick() {
            if (level().isClientSide) return

            ensureGoalState(GOAL_FLEE)

            val threat = getAnyThreat() ?: previousThreat
            previousThreat = threat

            if (entityData.get(RUSH_PHASE)) {
                handleFleeRush(threat)
            } else {
                handleFleeStill(threat)
            }
        }

        override fun stop() {
            if (level().isClientSide) return

            entityData.set(RUSH_PHASE, false)
            entityData.set(IS_IDLE, false)
            deltaMovement = Vec3.ZERO
            isPlaying = false
        }

        private fun getAnyThreat(): LivingEntity? {
            val mobThreat = lastHurtByMob?.takeIf {
                it.isAlive &&
                        it !is Player &&
                        distanceToSqr(it) < fleeRadiusSqr
            }

            val playerThreat = level().getNearestPlayer(this@BarnacleEntity, FLEE_RADIUS)?.takeIf {
                !it.isCreative &&
                        !it.isSpectator &&
                        !it.hasEffect(MobEffects.INVISIBILITY) &&
                        it.isAlive
            }


            return when {
                mobThreat != null && playerThreat != null -> {
                    if (distanceToSqr(mobThreat) < distanceToSqr(playerThreat)) mobThreat else playerThreat
                }

                mobThreat != null -> mobThreat
                playerThreat != null -> playerThreat
                else -> null
            }
        }

        private fun setFleeDirection(threat: LivingEntity) {
            val baseDir =
                position().subtract(threat.position()).normalize()

            val jitter = Vec3(
                random.nextDouble() - FLEE_JITTER_OFFSET,
                random.nextDouble() - FLEE_JITTER_OFFSET,
                random.nextDouble() - FLEE_JITTER_OFFSET
            ).scale(FLEE_JITTER_SCALE)

            setDirectionInData(baseDir.add(jitter).normalize())
        }

        private fun handleFleeStill(threat: LivingEntity) {
            val t = tickCount - animationStartTick

            deltaMovement = deltaMovement.scale(FLEE_STILL_FRICTION)
            setFleeDirection(threat)

            if (t >= moveStillDuration) {
                isPlaying = true
                spawnInk()
                entityData.set(RUSH_PHASE, true)
                animationStartTick = tickCount
            }
        }

        private fun handleFleeRush(threat: LivingEntity) {
            val t = tickCount - animationStartTick

            if (t >= moveRushDuration) {
                isPlaying = false
                entityData.set(RUSH_PHASE, false)
                deltaMovement = Vec3.ZERO
                animationStartTick = tickCount
                return
            }

            setFleeDirection(threat)

            val dir = getDirectionFromData()
            val speed = barnacleSpeed(
                t.toFloat(),
                moveRushDuration.toFloat(),
                FLEE_MAX_SPEED,
                FLEE_SPEED_K
            )

            deltaMovement = dir.scale(speed.toDouble())
        }
    }

    inner class BarnacleAttackGoal : Goal() {

        private val startSwallowDuration =
            ceil(BarnacleAnimation.swallow_start.lengthInSeconds * SWALLOW_START_FPS).toInt()
        private val stopSwallowDuration =
            ceil(BarnacleAnimation.swallow_stop.lengthInSeconds * SWALLOW_STOP_FPS).toInt()
        private val swallowDuration =
            ceil(BarnacleAnimation.swallow.lengthInSeconds * SWALLOW_FPS).toInt()

        private val startDist = ATTACK_START_DIST
        private val holdDist = ATTACK_HOLD_DIST

        private var isFinishingAnimation = false
        private var isCapturingDrops = false

        init {
            flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean =
            isUnderWater && (
                    entityData.get(IS_SWALLOWING) ||
                            isFinishingAnimation ||
                            (myTarget != null && entityData.get(MOUTH_OPEN) && !isHealthCritical)
                    )

        override fun canContinueToUse(): Boolean = canUse()

        override fun start() {
            if (level().isClientSide) return

            ensureGoalState(GOAL_ATTACK)
            animationStartTick = tickCount
            isFinishingAnimation = false

            myTarget?.let {
                setDirectionInData(getMyDir(it) ?: getRandomDirection().normalize())
            }
        }

        override fun tick() {
            if (level().isClientSide) return

            val t = tickCount - animationStartTick
            val target = myTarget
            val isSwallowing = entityData.get(IS_SWALLOWING)

            if (target == null || isHealthCritical || (!target.isAlive && isSwallowing)) {
                handleFinish(t)
                return
            }

            if (!isSwallowing) {
                if (t >= startSwallowDuration) {
                    animationStartTick = tickCount
                    entityData.set(IS_SWALLOWING, true)

                    if (!isCapturingDrops) {
                        target.captureDrops(mutableListOf())
                        isCapturingDrops = true
                    }

                    maintenirCible(target, holdDist)
                } else {
                    val progress = t.toDouble() / startSwallowDuration
                    val dist = startDist + (holdDist - startDist) * progress
                    maintenirCible(target, dist)
                }
            } else {
                deltaMovement = Vec3.ZERO
                maintenirCible(target, holdDist)

                if (t % ceil(swallowDuration / 2.0).toInt() == 0) {
                    target.hurtServer(
                        level() as ServerLevel,
                        swallowDamageSource(),
                        SWALLOW_DAMAGE)
                }
            }
        }

        private fun handleFinish(t: Int) {
            val world = level()
            if (world.isClientSide) return

            if (!isFinishingAnimation) {
                playSound(ModSounds.BARNACLE_CLOSE_MOUTH.get())

                isFinishingAnimation = true
                animationStartTick = tickCount
                entityData.set(IS_SWALLOWING, false)

                if (isHealthCritical) {
                    entityData.set(MOUTH_OPEN, false)
                    isFinishingAnimation = false
                }
                return
            }

            deltaMovement = Vec3.ZERO

            if (stomach.isNotEmpty()) {

                if (t >= SPIT_ITEM_DELAY_TICKS) {
                    val stack = stomach.removeAt(0)
                    val lookDir = getDirectionFromData()
                    val spawnPos = position().add(lookDir.scale(SPIT_OFFSET_FWD)).add(0.0, SPIT_OFFSET_Y, 0.0)
                    val itemEntity = ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, stack)

                    itemEntity.deltaMovement = lookDir.scale(SPIT_MOTION_FWD).add(0.0, SPIT_MOTION_Y, 0.0)
                    world.addFreshEntity(itemEntity)

                    world.playSound(
                        null,
                        blockPosition(),
                        SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE,
                        SoundSource.HOSTILE,
                        SPIT_SOUND_VOL,
                        SPIT_SOUND_PITCH
                    )

                    animationStartTick = tickCount
                }
            } else {
                val fastStop = stopSwallowDuration / 2

                if (t >= fastStop) {
                    isFinishingAnimation = false
                    entityData.set(MOUTH_OPEN, false)
                }
            }
        }

        override fun stop() {
            if (level().isClientSide) return
            entityData.set(IS_SWALLOWING, false)
            isCapturingDrops = false
            isFinishingAnimation = false

            if (isHealthCritical) {
                entityData.set(MOUTH_OPEN, false)
            }
        }
    }


    inner class BarnacleGrabGoal : Goal() {

        private val openMouthDuration =
            ceil(BarnacleAnimation.mouth_open.lengthInSeconds * GRAB_ANIM_FPS).toInt()
        private val closeMouthDuration =
            ceil(BarnacleAnimation.mouth_close.lengthInSeconds * GRAB_ANIM_FPS).toInt()

        private val holdOpenDuration = GRAB_HOLD_DURATION_TICKS
        private val factor = GRAB_HOLD_FACTOR

        private val openSoundDelay = GRAB_OPEN_SOUND_DELAY_TICKS
        private val closeSoundDelay = GRAB_CLOSE_SOUND_DELAY_TICKS

        private var initialVelocity = Vec3.ZERO
        private var isClosing = false

        init {
            flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean =
            isUnderWater && (
                    (myTarget != null && !isHealthCritical) || isClosing
                    )

        override fun start() {
            if (level().isClientSide) return

            ensureGoalState(GOAL_GRAB)
            animationStartTick = tickCount
            initialVelocity = deltaMovement
            isClosing = false

            myTarget?.let {
                setDirectionInData(getMyDir(it) ?: getRandomDirection().normalize())
            }
        }

        override fun tick() {
            if (level().isClientSide) return

            val t = tickCount - animationStartTick
            val target = myTarget
            val mouthOpen = entityData.get(MOUTH_OPEN)

            if ((target == null || isHealthCritical) && !isClosing) {
                isClosing = true
                animationStartTick = tickCount
                entityData.set(MOUTH_OPEN, false)
                deltaMovement = Vec3.ZERO
                return
            }

            if (target != null && target.isAlive) {
                isClosing = false
                maintenirCible(target, factor)

                if (!mouthOpen) {
                    when {
                        t == openSoundDelay ->
                            playSound(ModSounds.BARNACLE_OPEN_MOUTH.get())

                        t >= openMouthDuration + holdOpenDuration -> {
                            animationStartTick = tickCount
                            entityData.set(MOUTH_OPEN, true)
                            deltaMovement = Vec3.ZERO
                        }

                        t >= openMouthDuration ->
                            deltaMovement = Vec3.ZERO

                        else -> {
                            val slowdown =
                                (1.0 - t.toDouble() / openMouthDuration)
                                    .coerceAtLeast(0.0)
                            deltaMovement = initialVelocity.scale(slowdown)
                        }
                    }
                } else {
                    deltaMovement = Vec3.ZERO
                }
            } else if (isClosing) {
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
            if (level().isClientSide) return

            isClosing = false
            if (!(myTarget != null && entityData.get(MOUTH_OPEN))) {
                entityData.set(MOUTH_OPEN, false)
            }
        }
    }

    inner class BarnacleSwimTowardsGoal : Goal() {

        private val moveStillDuration = ceil(BarnacleAnimation.move_still.lengthInSeconds * SWIM_ANIM_FPS).toInt()
        private val moveRushDuration = ceil(BarnacleAnimation.move_rush.lengthInSeconds * SWIM_ANIM_FPS).toInt()

        private var lockedTarget: LivingEntity? = null

        init {
            flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }


        override fun canUse(): Boolean {
            val target = getMyTarget(SWIM_TARGET_RADIUS)

            if (target != null && isUnderWater && !isHealthCritical && this@BarnacleEntity.hasLineOfSight(target)) {
                lockedTarget = target
                return true
            }
            return false
        }

        override fun canContinueToUse(): Boolean {
            if (lockedTarget is Player) {
                val player = lockedTarget as Player
                if (player.gameMode() == GameType.CREATIVE || player.gameMode() == GameType.SPECTATOR || player.hasEffect(
                        MobEffects.INVISIBILITY
                    )
                ) {
                    return false
                }
            }

            return lockedTarget?.isAlive == true &&
                    isUnderWater &&
                    !isHealthCritical &&
                    this@BarnacleEntity.hasLineOfSight(lockedTarget!!)
        }

        override fun requiresUpdateEveryTick() = true

        override fun isInterruptable() = true

        override fun start() {
            if (level().isClientSide) {
                resetAnimationStates()
                return
            }

            ensureGoalState(GOAL_SWIM)
            entityData.set(RUSH_PHASE, false)
            entityData.set(IS_IDLE, false)
            animationStartTick = tickCount

            val dir = getMyDir(lockedTarget) ?: getRandomDirection().normalize()
            setDirectionInData(dir)
            deltaMovement = Vec3.ZERO
        }

        override fun tick() {
            if (level().isClientSide) return
            ensureGoalState(GOAL_SWIM)
            if (entityData.get(RUSH_PHASE)) {
                handleRush()
            } else {
                handleIdle()
            }
        }

        override fun stop() {
            lockedTarget = null
            deltaMovement = Vec3.ZERO
        }

        private fun handleIdle() {
            if (tickCount - animationStartTick >= moveStillDuration) {
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
            val dir = getMyDir(lockedTarget) ?: getRandomDirection().normalize()

            setDirectionInData(dir)

            val speed = barnacleSpeed(
                t.toFloat(),
                moveRushDuration.toFloat(),
                SWIM_MAX_SPEED,
                SWIM_SPEED_K
            )

            deltaMovement = dir.scale(speed.toDouble())
        }
    }

    inner class BarnacleIdleGoal : Goal() {

        private val moveStillDuration =
            ceil(BarnacleAnimation.move_still.lengthInSeconds * IDLE_ANIM_FPS).toInt()
        private val moveRushDuration =
            ceil(BarnacleAnimation.move_rush.lengthInSeconds * IDLE_ANIM_FPS).toInt()

        private var stillDuration = 0

        private var ticksSinceLastObstacleCheck = 0

        init {
            flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean = isUnderWater
        override fun requiresUpdateEveryTick() = true
        override fun isInterruptable() = true

        override fun start() {
            if (level().isClientSide) return

            ensureGoalState(GOAL_IDLE)
            entityData.set(RUSH_PHASE, false)
            entityData.set(IS_IDLE, false)
            animationStartTick = tickCount

            ticksSinceLastObstacleCheck = 10

            setDirectionInData(getRandomDirection().normalize())
            deltaMovement = Vec3.ZERO
        }

        override fun tick() {
            if (level().isClientSide) return

            if (entityData.get(IS_IDLE)) {
                handleStill()
            } else if (entityData.get(RUSH_PHASE)) {
                handleRush()
            } else {
                handleIdle()
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

                ticksSinceLastObstacleCheck = 10
            }
        }

        private fun handleRush() {
            val t = tickCount - animationStartTick

            if (t >= moveRushDuration) {
                entityData.set(RUSH_PHASE, false)
                animationStartTick = tickCount
                deltaMovement = Vec3.ZERO

                when (random.nextDouble()) {
                    in 0.0..IDLE_DIR_CHANCE_1 -> setDirectionInData(getRandomDirection().normalize())

                    in IDLE_DIR_CHANCE_1..IDLE_DIR_CHANCE_2 -> {
                        entityData.set(IS_IDLE, true)
                        stillDuration = random.nextInt(IDLE_MIN_TICKS, IDLE_MAX_TICKS)
                    }
                }
                return
            }

            ticksSinceLastObstacleCheck++
            var currentDir = getDirectionFromData()

            if (ticksSinceLastObstacleCheck >= 10) {
                currentDir = adjustDirectionForObstacles(currentDir)
                ticksSinceLastObstacleCheck = 0
            }

            val speed = barnacleSpeed(
                t.toFloat(),
                moveRushDuration.toFloat(),
                IDLE_MAX_SPEED
            )

            deltaMovement = currentDir.scale(speed.toDouble())
        }

        private fun adjustDirectionForObstacles(dirIn: Vec3): Vec3 {
            val world = level()
            val maxDistance = IDLE_WATER_CHECK_DIST
            val blocksUp = findWaterSurface(world, this@BarnacleEntity.blockPosition(), 1)

            var dir = dirIn

            if (blocksUp < maxDistance) {
                val adjust = (maxDistance - blocksUp).toDouble() / maxDistance
                dir = Vec3(dir.x, dir.y - adjust * 0.5, dir.z).normalize()
                setDirectionInData(dir)
            } else if (!hasWaterAhead(dir)) {
                dir = Vec3(
                    random.nextDouble() - 0.5,
                    random.nextDouble().coerceIn(-0.4, 0.4),
                    random.nextDouble() - 0.5
                ).normalize()
                setDirectionInData(dir)
            }

            return dir
        }
    }
}