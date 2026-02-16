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
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth.wrapDegrees
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.decoration.Mannequin
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
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
    private inline val healthRatio: Float
        get() = this.health / this.maxHealth
    private inline val isHealthCritical: Boolean
        get() = healthRatio <= 0.25f
    private var stomach: MutableList<ItemStack> = mutableListOf()


    private var lastGoalState = -1
    private var lastRushPhase: Boolean? = null
    private var lastIsIdle: Boolean? = null
    private var lastMouthOpen: Boolean? = null
    private var lastSwallowing: Boolean? = null
    private var animationResetScheduled = false

    init {
        this.xpReward = 15
    }

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
        builder.define(
            TARGET,
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
                return
            }

            val currentGoal = entityData.get(CURRENT_GOAL_STATE)
            val currentRush = entityData.get(RUSH_PHASE)
            val currentIsIdle = entityData.get(IS_IDLE)
            val currentMouthOpen = entityData.get(MOUTH_OPEN)
            val currentSwallowing = entityData.get(IS_SWALLOWING)

            val currentTarget: LivingEntity? by lazy {
                entityData.get(TARGET)?.orElse(null)
                    ?.getEntity(level(), LivingEntity::class.java as Class<LivingEntity?>) ?: null
            }

            var needsReset = false
            var keepClosingDuringReset = false
            var keepSwallowStopDuringReset = false

            if (currentGoal != lastGoalState) {
                needsReset = true

                if (lastGoalState == 2 && currentGoal != 1) {
                    closeMouthAnimationState.startIfStopped(tickCount)
                    keepClosingDuringReset = true
                }

                if (lastGoalState == 1) {
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
                    0, 3 -> if (currentRush != lastRushPhase) needsReset = true
                    4 -> {
                        if (currentRush != lastRushPhase) needsReset = true
                        if (currentIsIdle != lastIsIdle) needsReset = true
                    }

                    2 -> if (currentMouthOpen != lastMouthOpen) needsReset = false
                    1 -> if (currentSwallowing != lastSwallowing) needsReset = false
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
                    if (currentTarget != null) {
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

                1 -> {
                    if (currentTarget != null && currentTarget!!.isAlive) {
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

                0 -> {
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
                    Optional.ofNullable(newTargetRef) as Optional<EntityReference<LivingEntity?>?>
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
        ).filter {
            (it.gameMode() == GameType.SURVIVAL || it.gameMode() == GameType.ADVENTURE) && !it.hasEffect(MobEffects.INVISIBILITY) && it.isAlive
        }

        val guardians = this.level().getEntitiesOfClass(
            Guardian::class.java,
            this.boundingBox.inflate(radius)
        ).filter { it.isAlive }

        val mannequins = this.level().getEntitiesOfClass(
            Mannequin::class.java,
            this.boundingBox.inflate(radius)
        ).filter { it.isAlive }

        val validPlayers = players.filter { isClosestBarnacle(it, radius) && ModUtilities.hasClearPath(level(), this@BarnacleEntity, it) }
            .sortedBy { it.distanceToSqr(this) }

        val validGuardians = guardians.filter { isClosestBarnacle(it, radius) && ModUtilities.hasClearPath(level(), this@BarnacleEntity, it) }
            .sortedBy { it.distanceToSqr(this) }

        val validMannequins = mannequins.filter { isClosestBarnacle(it, radius) && ModUtilities.hasClearPath(level(), this@BarnacleEntity, it) }
            .sortedBy { it.distanceToSqr(this) }

        return when {
            validPlayers.isNotEmpty() -> validPlayers.first()
            validGuardians.isNotEmpty() -> validGuardians.first()
            validMannequins.isNotEmpty() -> validMannequins.first()
            else -> null
        }
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
        tgt.addEffect(MobEffectInstance(MobEffects.BLINDNESS, 40, 1, true, false))


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
                (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f
            )

            level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                this.x, this.eyeY, this.z,
                500,
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


    inner class BarnacleFleeGoal : Goal() {

        private val moveStillDuration =
            ceil(BarnacleAnimation.flee_still.lengthInSeconds * 20).toInt()
        private val moveRushDuration =
            ceil(BarnacleAnimation.flee_rush.lengthInSeconds * 20).toInt()

        private val fleeRadiusSqr = 17.0 * 17.0

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

            ensureGoalState(0)
            entityData.set(RUSH_PHASE, false)
            animationStartTick = tickCount

            getAnyThreat()?.let { setFleeDirection(it) }
        }

        override fun tick() {
            if (level().isClientSide) return

            ensureGoalState(0)

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

            val playerThreat = level().getNearestPlayer(this@BarnacleEntity, 17.0)?.takeIf {
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
                random.nextDouble() - 0.15,
                random.nextDouble() - 0.15,
                random.nextDouble() - 0.15
            ).scale(0.2)

            setDirectionInData(baseDir.add(jitter).normalize())
        }

        private fun handleFleeStill(threat: LivingEntity) {
            val t = tickCount - animationStartTick

            deltaMovement = deltaMovement.scale(0.8)
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
                t.toDouble(),
                moveRushDuration.toDouble(),
                3.0,
                3.0
            )

            deltaMovement = dir.scale(speed)
        }
    }

    inner class BarnacleAttackGoal : Goal() {

        private val startSwallowDuration =
            ceil(BarnacleAnimation.swallow_start.lengthInSeconds * 21).toInt()
        private val stopSwallowDuration =
            ceil(BarnacleAnimation.swallow_stop.lengthInSeconds * 21).toInt()
        private val swallowDuration =
            ceil(BarnacleAnimation.swallow.lengthInSeconds * 20).toInt()

        private val closeSoundDelay = 2
        private val startDist = 4.5
        private val holdDist = 1.5

        private var drop: MutableList<ItemStack> = mutableListOf()
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

            ensureGoalState(1)
            animationStartTick = tickCount

            myTarget?.let {
                setDirectionInData(getMyDir(it) ?: getRandomDirection().normalize())
            }
        }

        override fun tick() {
            if (level().isClientSide) return

            val t = tickCount - animationStartTick
            val target = myTarget
            val isSwallowing = entityData.get(IS_SWALLOWING)

            if (target == null || isHealthCritical) {
                handleFinish(t)
                return
            }

            isFinishingAnimation = false

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

                if (!target.isAlive) {
                    handleFinish(t)
                    return
                }

                deltaMovement = Vec3.ZERO
                maintenirCible(target, holdDist)

                if (t % ceil(swallowDuration / 2.0).toInt() == 0) {
                    target.hurt(swallowDamageSource(), 3f)
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
                if (t >= 3) {
                    val stack = stomach.removeAt(0)
                    val lookDir = getDirectionFromData()
                    val spawnPos = position().add(lookDir.scale(1.2)).add(0.0, 0.7, 0.0)
                    val itemEntity = ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, stack)

                    itemEntity.deltaMovement = lookDir.scale(0.3).add(0.0, 0.2, 0.0)
                    world.addFreshEntity(itemEntity)

                    world.playSound(null, blockPosition(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.HOSTILE, 0.5f, 0.5f)

                    animationStartTick = tickCount
                }
            }
            else {
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

            if (isHealthCritical) {
                entityData.set(MOUTH_OPEN, false)
                isFinishingAnimation = false
            }
        }
    }


    inner class BarnacleGrabGoal : Goal() {

        private val openMouthDuration =
            ceil(BarnacleAnimation.mouth_open.lengthInSeconds * 21).toInt()
        private val closeMouthDuration =
            ceil(BarnacleAnimation.mouth_close.lengthInSeconds * 21).toInt()

        private val holdOpenDuration = 7
        private val factor = 4.5

        private val openSoundDelay = 2
        private val closeSoundDelay = 2

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

            ensureGoalState(2)
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

        private val moveStillDuration =
            ceil(BarnacleAnimation.move_still.lengthInSeconds * 22).toInt()
        private val moveRushDuration =
            ceil(BarnacleAnimation.move_rush.lengthInSeconds * 22).toInt()

        private val swimTarget: LivingEntity?
            get() = getMyTarget(55.0)

        init {
            flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean =
            swimTarget != null && isUnderWater && !isHealthCritical

        override fun requiresUpdateEveryTick() = true
        override fun isInterruptable() = true

        override fun start() {
            if (level().isClientSide) {
                resetAnimationStates()
                return
            }

            ensureGoalState(3)
            entityData.set(RUSH_PHASE, false)
            entityData.set(IS_IDLE, false)
            animationStartTick = tickCount

            setDirectionInData(
                getMyDir(swimTarget) ?: getRandomDirection().normalize()
            )

            deltaMovement = Vec3.ZERO
        }

        override fun tick() {
            if (level().isClientSide) return

            ensureGoalState(3)

            if (entityData.get(RUSH_PHASE)) {
                handleRush()
            } else {
                handleIdle()
            }
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

            val dir =
                getMyDir(swimTarget) ?: getRandomDirection().normalize()

            setDirectionInData(dir)

            val speed = barnacleSpeed(
                t.toDouble(),
                moveRushDuration.toDouble(),
                2.0,
                2.5
            )

            deltaMovement = dir.scale(speed)
        }
    }


    inner class BarnacleIdleGoal : Goal() {

        private val moveStillDuration =
            ceil(BarnacleAnimation.move_still.lengthInSeconds * 30).toInt()
        private val moveRushDuration =
            ceil(BarnacleAnimation.move_rush.lengthInSeconds * 30).toInt()

        private var stillDuration = 0

        init {
            flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean = isUnderWater
        override fun requiresUpdateEveryTick() = true
        override fun isInterruptable() = true

        override fun start() {
            if (level().isClientSide) return

            ensureGoalState(4)
            entityData.set(RUSH_PHASE, false)
            entityData.set(IS_IDLE, false)
            animationStartTick = tickCount

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
            }
        }

        private fun handleRush() {
            val t = tickCount - animationStartTick

            if (t >= moveRushDuration) {
                entityData.set(RUSH_PHASE, false)
                animationStartTick = tickCount
                deltaMovement = Vec3.ZERO

                when (random.nextDouble()) {
                    in 0.0..0.33 ->
                        setDirectionInData(getRandomDirection().normalize())

                    in 0.33..0.55 -> {
                        entityData.set(IS_IDLE, true)
                        stillDuration = random.nextInt(60, 150)
                    }
                }
                return
            }

            val dir = adjustDirectionForObstacles(getDirectionFromData())
            val speed = barnacleSpeed(
                t.toDouble(),
                moveRushDuration.toDouble(),
                1.0
            )

            deltaMovement = dir.scale(speed)
        }

        private fun adjustDirectionForObstacles(dirIn: Vec3): Vec3 {
            val world = level()
            val maxDistance = 10
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