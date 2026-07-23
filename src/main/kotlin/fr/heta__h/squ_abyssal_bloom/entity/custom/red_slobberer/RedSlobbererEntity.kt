package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer

import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererBottomStrollGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererBreedGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererCollectiveCohesionGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererGoalPriorities
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererGrazeBloodSeagrassGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererReefResidenceGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control.RedSlobbererBodyRotationControl
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control.RedSlobbererMoveControl
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control.RedSlobbererTerrainAlignment
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.RedSlobbererGroupController
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.RedSlobbererFishRefugeStorage
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.LegacyRedSlobbererReefSnapshot
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.RedSlobbererReefManager
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.defense.RedSlobbererDefenseController
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.defense.RedSlobbererDefenseState
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.BodyRotationControl
import net.minecraft.world.entity.ai.goal.FollowParentGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.entity.player.Player
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

class RedSlobbererEntity(type: EntityType<out Animal>, level: Level) : Animal(type, level) {

    companion object {
        const val MAX_AIR_TICKS = 200

        private const val BABY_START_AGE_TICKS = -72_000
        private const val NATURAL_FOLLOWER_BABY_SPAWN_CHANCE = 0.30f
        private const val ADULT_STEP_HEIGHT = 2.0f
        private const val BABY_STEP_HEIGHT = 1.5f
        const val LOCOMOTION_ANIMATION_LOOP_TICKS = 50
        const val LOCOMOTION_CHARGE_TICKS = 35
        const val LOCOMOTION_PROPULSION_SPEED_MULTIPLIER = 50.0 / 15.0
        const val LOCOMOTION_CHARGE_TURN_DEGREES = 8.0f
        const val ANIM_HIDE_S = 1.0f
        const val ANIM_SHOW_S = 0.5833f
        private const val WATER_GRAVITY = 0.4
        private const val MIN_FISH_PUSH_DISTANCE = 0.01
        private const val FISH_PUSH_STRENGTH = 0.05
        private const val HIDDEN_DAMAGE_MULTIPLIER = 0.5f
        private const val BABY_SHELTER_SEARCH_RADIUS = 12.0
        private const val LEGACY_TAG_HAS_REEF_ANCHOR = "red_slobberer_has_reef_anchor"
        private const val LEGACY_TAG_REEF_ANCHOR = "red_slobberer_reef_anchor"
        private const val LEGACY_TAG_RESIDENCE_TICKS = "red_slobberer_residence_tick"
        private const val LEGACY_TAG_PLACED_DECORATIONS = "red_slobberer_placed_decorations"

        private val LOCOMOTION_ACTIVE: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            RedSlobbererEntity::class.java,
            EntityDataSerializers.BOOLEAN
        )
        private val LOCOMOTION_CYCLE_ANCHOR: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            RedSlobbererEntity::class.java,
            EntityDataSerializers.INT
        )
        private val DEFENSE_STATE: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            RedSlobbererEntity::class.java,
            EntityDataSerializers.INT
        )
        private val DEFENSE_PHASE_START_GAME_TIME: EntityDataAccessor<Long> = SynchedEntityData.defineId(
            RedSlobbererEntity::class.java,
            EntityDataSerializers.LONG
        )

        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.TEMPT_RANGE, 10.0)
        }
    }

    val idleAnimationState = AnimationState()
    val moveAnimationState = AnimationState()
    val swingingAnimationState = AnimationState()
    val eyesAnimationState = AnimationState()

    val groupController = RedSlobbererGroupController(this)
    private val terrainAlignment = RedSlobbererTerrainAlignment(this)
    private val fishRefugeStorage = RedSlobbererFishRefugeStorage(this)
    private val defenseController = RedSlobbererDefenseController(this)

    private var activeLocomotionAnimation: LocomotionAnimation? = null
    private var renderedLocomotionCycleAnchor = Int.MIN_VALUE
    private var locomotionCycleInitialized = false
    private var legacyReefSnapshot: LegacyRedSlobbererReefSnapshot? = null

    var timeExposedInAir = 0

    init {
        moveControl = RedSlobbererMoveControl(this)
        setPathfindingMalus(PathType.WATER, 0.0f)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(LOCOMOTION_ACTIVE, false)
        builder.define(LOCOMOTION_CYCLE_ANCHOR, 0)
        builder.define(DEFENSE_STATE, RedSlobbererDefenseState.NORMAL.networkId)
        builder.define(DEFENSE_PHASE_START_GAME_TIME, 0L)
    }

    override fun createNavigation(level: Level): PathNavigation {
        return GroundPathNavigation(this, level).apply {
            setCanFloat(false)
        }
    }

    override fun createBodyControl(): BodyRotationControl {
        return RedSlobbererBodyRotationControl(this)
    }

    override fun getDefaultGravity(): Double {
        return if (isInWater) WATER_GRAVITY else super.getDefaultGravity()
    }

    override fun moveRelative(speed: Float, input: Vec3) {
        val tangentMovement = terrainAlignment.createTangentMovement(input, speed, yRot)

        if (tangentMovement == null) {
            super.moveRelative(speed, input)
        } else {
            deltaMovement = deltaMovement.add(tangentMovement)
        }
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(
            RedSlobbererGoalPriorities.BREED,
            RedSlobbererBreedGoal(this, 1.0)
        )
        goalSelector.addGoal(
            RedSlobbererGoalPriorities.TEMPT,
            TemptGoal(
                this,
                1.25,
                Ingredient.of(ModItems.BLOOD_SEAGRASS.get(), ModItems.TALL_BLOOD_SEAGRASS.get()),
                false
            )
        )
        goalSelector.addGoal(RedSlobbererGoalPriorities.FOLLOW_PARENT, FollowParentGoal(this, 1.1))
        goalSelector.addGoal(
            RedSlobbererGoalPriorities.REEF_RESIDENCE,
            RedSlobbererReefResidenceGoal(this, 1.0)
        )
        goalSelector.addGoal(
            RedSlobbererGoalPriorities.FOLLOW_GROUP,
            RedSlobbererCollectiveCohesionGoal(this, 0.8)
        )
        goalSelector.addGoal(
            RedSlobbererGoalPriorities.GRAZE,
            RedSlobbererGrazeBloodSeagrassGoal(this, 0.7)
        )
        goalSelector.addGoal(
            RedSlobbererGoalPriorities.STROLL,
            RedSlobbererBottomStrollGoal(this, 1.0)
        )
        goalSelector.addGoal(
            RedSlobbererGoalPriorities.LOOK_AT_PLAYER,
            LookAtPlayerGoal(this, Player::class.java, 6.0f)
        )
        goalSelector.addGoal(RedSlobbererGoalPriorities.RANDOM_LOOK, RandomLookAroundGoal(this))
    }

    override fun jumpFromGround() = Unit

    val maximumStepHeight: Float
        get() = if (isBaby) BABY_STEP_HEIGHT else ADULT_STEP_HEIGHT

    override fun maxUpStep(): Float =
        if (isInWater) maximumStepHeight else super.maxUpStep()

    fun getTerrainNormal(partialTick: Float): Vec3 {
        return terrainAlignment.getInterpolatedNormal(partialTick)
    }

    val defenseState: RedSlobbererDefenseState
        get() = RedSlobbererDefenseState.fromNetworkId(entityData.get(DEFENSE_STATE))

    val isDefenseImmobilized: Boolean
        get() = !isBaby && defenseState != RedSlobbererDefenseState.NORMAL

    fun getDefensePhaseElapsedTicks(partialTick: Float): Float {
        val elapsedTicks = level().gameTime - entityData.get(DEFENSE_PHASE_START_GAME_TIME)
        return elapsedTicks.coerceAtLeast(0L).toFloat() + partialTick
    }

    internal fun syncDefenseState(
        state: RedSlobbererDefenseState,
        phaseStartGameTime: Long
    ) {
        entityData.set(DEFENSE_PHASE_START_GAME_TIME, phaseStartGameTime)
        entityData.set(DEFENSE_STATE, state.networkId)
        resetLocomotionCycle()
        if (state != RedSlobbererDefenseState.NORMAL) {
            stopDefenseMovement()
            hurtMarked = true
        }
    }

    internal fun hasActiveRefugeUnsafeCooldown(): Boolean =
        fishRefugeStorage.hasActiveUnsafeCooldown

    private fun tryShelterAfterDamage(level: ServerLevel): Boolean {
        val parent = level.getEntitiesOfClass(
            RedSlobbererEntity::class.java,
            boundingBox.inflate(BABY_SHELTER_SEARCH_RADIUS)
        ) { candidate ->
            candidate !== this &&
                candidate.fishRefugeStorage.canAcceptBaby(this)
        }.minByOrNull(::distanceToSqr) ?: return false

        return parent.acceptShelteredBaby(level, this)
    }

    private fun acceptShelteredBaby(
        level: ServerLevel,
        baby: RedSlobbererEntity
    ): Boolean = fishRefugeStorage.tryStoreBaby(level, baby)

    fun updateLocomotionCycle(hasMovementIntent: Boolean): Boolean {
        if (level().isClientSide) return isInLocomotionPropulsionPhase
        if (isDefenseImmobilized) {
            resetLocomotionCycle()
            return false
        }

        if (!locomotionCycleInitialized) {
            locomotionCycleInitialized = true
            entityData.set(
                LOCOMOTION_CYCLE_ANCHOR,
                currentLocomotionClockTick()
            )
            entityData.set(LOCOMOTION_ACTIVE, hasMovementIntent)
            return isInLocomotionPropulsionPhase
        }

        if (
            entityData.get(LOCOMOTION_ACTIVE) != hasMovementIntent &&
            locomotionCycleTick() == 0
        ) {
            entityData.set(
                LOCOMOTION_CYCLE_ANCHOR,
                currentLocomotionClockTick()
            )
            entityData.set(LOCOMOTION_ACTIVE, hasMovementIntent)
        }
        return isInLocomotionPropulsionPhase
    }

    val isInLocomotionPropulsionPhase: Boolean
        get() {
            if (!entityData.get(LOCOMOTION_ACTIVE)) return false
            return locomotionCycleTick() >= LOCOMOTION_CHARGE_TICKS
        }

    val isInLocomotionChargePhase: Boolean
        get() {
            if (!entityData.get(LOCOMOTION_ACTIVE)) return false
            return locomotionCycleTick() < LOCOMOTION_CHARGE_TICKS
        }

    private fun currentLocomotionClockTick(): Int =
        Math.floorMod(level().gameTime, LOCOMOTION_ANIMATION_LOOP_TICKS.toLong()).toInt()

    private fun locomotionCycleTick(): Int = Math.floorMod(
        currentLocomotionClockTick() -
            entityData.get(LOCOMOTION_CYCLE_ANCHOR),
        LOCOMOTION_ANIMATION_LOOP_TICKS
    )

    private fun resetLocomotionCycle() {
        entityData.set(LOCOMOTION_ACTIVE, false)
        entityData.set(LOCOMOTION_CYCLE_ANCHOR, currentLocomotionClockTick())
        locomotionCycleInitialized = false
    }

    override fun tick() {
        super.tick()

        setXRot(0.0f)
        xRotO = 0.0f

        if (level().isClientSide) {
            setupAnimationStates()
        }
    }

    private fun setupAnimationStates() {
        if (isBaby) {
            setupLocomotionAnimationState()
            swingingAnimationState.stop()
            eyesAnimationState.stop()
            return
        }

        val synchronizedStartTick = tickCount - locomotionCycleTick()
        swingingAnimationState.startIfStopped(synchronizedStartTick)
        if (defenseState != RedSlobbererDefenseState.NORMAL) {
            stopAnimationsForDefense()
            return
        }

        setupLocomotionAnimationState()
        eyesAnimationState.startIfStopped(synchronizedStartTick)
    }

    private fun setupLocomotionAnimationState() {
        val animation = if (entityData.get(LOCOMOTION_ACTIVE)) {
            LocomotionAnimation.MOVE
        } else {
            LocomotionAnimation.IDLE
        }
        val synchronizedAnchor = entityData.get(LOCOMOTION_CYCLE_ANCHOR)
        if (
            activeLocomotionAnimation != animation ||
            renderedLocomotionCycleAnchor != synchronizedAnchor
        ) {
            startLocomotionAnimation(
                animation,
                tickCount - locomotionCycleTick()
            )
            renderedLocomotionCycleAnchor = synchronizedAnchor
        }
    }

    private fun stopAnimationsForDefense() {
        idleAnimationState.stop()
        moveAnimationState.stop()
        eyesAnimationState.stop()
        activeLocomotionAnimation = null
        renderedLocomotionCycleAnchor = Int.MIN_VALUE
    }

    private fun startLocomotionAnimation(animation: LocomotionAnimation, startTick: Int) {
        idleAnimationState.stop()
        moveAnimationState.stop()

        when (animation) {
            LocomotionAnimation.IDLE -> idleAnimationState.start(startTick)
            LocomotionAnimation.MOVE -> moveAnimationState.start(startTick)
        }

        activeLocomotionAnimation = animation
    }

    override fun aiStep() {
        terrainAlignment.tick()
        if (isDefenseImmobilized) {
            stopDefenseMovement()
        }
        super.aiStep()
        val serverLevel = level() as? ServerLevel ?: return
        fishRefugeStorage.tickUnsafeCooldown(serverLevel)
        defenseController.tick(serverLevel)
        if (isDefenseImmobilized) {
            stopDefenseMovement()
        }

        if (isUnderWater) {
            timeExposedInAir = 0
            airSupply = maxAirSupply
        } else {
            timeExposedInAir++
            if (timeExposedInAir >= MAX_AIR_TICKS) {
                hurtServer(serverLevel, damageSources().drown(), 2.0f)
            }
        }

        groupController.tick()
        RedSlobbererReefManager.forLevel(serverLevel).observe(this)
        fishRefugeStorage.tick(serverLevel)
    }

    override fun isImmobile(): Boolean {
        return isDefenseImmobilized || super.isImmobile()
    }

    override fun isPushable(): Boolean {
        return !isDefenseImmobilized && super.isPushable()
    }

    override fun travel(travelVector: Vec3) {
        if (isDefenseImmobilized) {
            deltaMovement = Vec3.ZERO
            return
        }
        super.travel(travelVector)
    }

    private fun stopDefenseMovement() {
        navigation.stop()
        (moveControl as? RedSlobbererMoveControl)?.stopForDefense()
        speed = 0.0f
        xxa = 0.0f
        yya = 0.0f
        zza = 0.0f
        deltaMovement = Vec3.ZERO
    }

    val shelteredFishCount: Int
        get() = fishRefugeStorage.size

    fun canShelterFish(fish: AbstractFish): Boolean = fishRefugeStorage.canAccept(fish)

    fun fishRefugeEntrance(fish: AbstractFish): Vec3 = fishRefugeStorage.entranceFor(fish)

    fun tryShelterFish(
        level: ServerLevel,
        fish: AbstractFish,
        panic: Double
    ): Boolean = fishRefugeStorage.tryStore(level, fish, panic)

    override fun hurtServer(
        level: ServerLevel,
        source: DamageSource,
        amount: Float
    ): Boolean {
        fishRefugeStorage.tickUnsafeCooldown(level)
        defenseController.tick(level)
        val appliesHiddenResistance =
            !isBaby && defenseController.state == RedSlobbererDefenseState.HIDDEN
        val appliedAmount = if (appliesHiddenResistance) {
            amount * HIDDEN_DAMAGE_MULTIPLIER
        } else {
            amount
        }
        if (!super.hurtServer(level, source, appliedAmount)) return false

        if (isBaby && isAlive) {
            tryShelterAfterDamage(level)
        } else if (isAlive) {
            fishRefugeStorage.invalidateAfterDamage(level)
            defenseController.onDamage(level)
            defenseController.tick(level)
        }
        if (isDefenseImmobilized) {
            stopDefenseMovement()
        }
        return true
    }

    override fun getHurtSound(source: DamageSource): SoundEvent? {
        return if (!isBaby && defenseState == RedSlobbererDefenseState.HIDDEN) {
            SoundEvents.SHULKER_HURT_CLOSED
        } else {
            super.getHurtSound(source)
        }
    }

    override fun remove(reason: Entity.RemovalReason) {
        if (reason.shouldDestroy() && !isRemoved) {
            (level() as? ServerLevel)?.let { serverLevel ->
                fishRefugeStorage.releaseOnDestruction(serverLevel)
            }
        }
        super.remove(reason)
    }

    override fun isPushedByFluid(): Boolean = false

    override fun push(entity: Entity) {
        if (entity is AbstractFish) {
            pushFishAwayWithoutRecoil(entity)
        } else {
            super.push(entity)
        }
    }

    override fun doPush(entity: Entity) {
        if (entity is AbstractFish) {
            pushFishAwayWithoutRecoil(entity)
        } else {
            super.doPush(entity)
        }
    }

    private fun pushFishAwayWithoutRecoil(fish: AbstractFish) {
        if (isPassengerOfSameVehicle(fish) || noPhysics || fish.noPhysics) return
        if (fish.isVehicle || !fish.isPushable) return
        val serverLevel = level() as? ServerLevel
        if (
            serverLevel != null &&
            RedSlobbererReefManager.forLevel(serverLevel).isFishUsingRefuge(fish, this)
        ) {
            return
        }

        var pushX = fish.x - x
        var pushZ = fish.z - z
        var maximumAxisDistance = Mth.absMax(pushX, pushZ)
        if (maximumAxisDistance < MIN_FISH_PUSH_DISTANCE) return

        maximumAxisDistance = sqrt(maximumAxisDistance)
        pushX /= maximumAxisDistance
        pushZ /= maximumAxisDistance
        val proximityScale = (1.0 / maximumAxisDistance).coerceAtMost(1.0)
        fish.push(
            pushX * proximityScale * FISH_PUSH_STRENGTH,
            0.0,
            pushZ * proximityScale * FISH_PUSH_STRENGTH
        )
    }

    override fun getWalkTargetValue(pos: BlockPos, level: LevelReader): Float {
        return ModUtilities.preferWaterWalkTarget(pos, level) { super.getWalkTargetValue(pos, level) }
    }

    override fun checkSpawnRules(level: LevelAccessor, spawnReason: EntitySpawnReason): Boolean {
        if (
            spawnReason == EntitySpawnReason.NATURAL ||
            spawnReason == EntitySpawnReason.CHUNK_GENERATION
        ) {
            val spawnPos = ModEntities.findRedSlobbererSpawnPosition(
                level,
                blockPosition(),
                type
            ) ?: return false

            setPos(
                spawnPos.x + 0.5,
                spawnPos.y.toDouble(),
                spawnPos.z + 0.5
            )
        }

        return super.checkSpawnRules(level, spawnReason)
    }

    override fun checkSpawnObstruction(level: LevelReader): Boolean {
        return level.noCollision(this) && level.isUnobstructed(this)
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnReason: EntitySpawnReason,
        groupData: SpawnGroupData?
    ): SpawnGroupData? {
        val redSlobbererGroupData = if (
            groupData == null && (
                spawnReason == EntitySpawnReason.NATURAL ||
                    spawnReason == EntitySpawnReason.CHUNK_GENERATION
                )
        ) {
            AgeableMobGroupData(true, NATURAL_FOLLOWER_BABY_SPAWN_CHANCE)
        } else {
            groupData
        }

        return super.finalizeSpawn(level, difficulty, spawnReason, redSlobbererGroupData)
    }

    override fun getBabyStartAge(): Int = BABY_START_AGE_TICKS

    override fun isFood(food: ItemStack): Boolean {
        return food.typeHolder().`is`(ModTags.Items.RED_SLOBBERER_FOOD) ||
            food.item === ModItems.BLOOD_SEAGRASS.get() ||
            food.item === ModItems.TALL_BLOOD_SEAGRASS.get()
    }

    override fun getBreedOffspring(serverLevel: ServerLevel, otherParent: AgeableMob): AgeableMob? {
        return ModEntities.RED_SLOBBERER.get().create(serverLevel, EntitySpawnReason.BREEDING)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        fishRefugeStorage.load(input)
        defenseController.load(input, level().gameTime)
        if (!input.getBooleanOr(LEGACY_TAG_HAS_REEF_ANCHOR, false)) return

        val residenceTicks = input.getIntOr(LEGACY_TAG_RESIDENCE_TICKS, 0).coerceAtLeast(0)
        if (residenceTicks <= 0) return
        legacyReefSnapshot = LegacyRedSlobbererReefSnapshot(
            anchor = BlockPos.of(input.getLongOr(LEGACY_TAG_REEF_ANCHOR, BlockPos.ZERO.asLong())),
            residenceTicks = residenceTicks,
            placedDecorations = input.getIntOr(LEGACY_TAG_PLACED_DECORATIONS, 0).coerceAtLeast(0)
        )
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        fishRefugeStorage.save(output)
        defenseController.save(output, level().gameTime)
    }

    fun consumeLegacyReefSnapshot(): LegacyRedSlobbererReefSnapshot? {
        val snapshot = legacyReefSnapshot
        legacyReefSnapshot = null
        return snapshot
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions {
        return if (isBaby) {
            EntityDimensions.scalable(0.4f, 0.5f)
        } else {
            super.getDefaultDimensions(pose)
        }
    }

    private enum class LocomotionAnimation {
        IDLE,
        MOVE
    }
}
