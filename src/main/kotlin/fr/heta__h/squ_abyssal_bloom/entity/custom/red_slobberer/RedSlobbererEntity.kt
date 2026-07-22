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
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.util.Mth.lerp
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
import kotlin.math.abs
import kotlin.math.sqrt

class RedSlobbererEntity(type: EntityType<out Animal>, level: Level) : Animal(type, level) {

    companion object {
        const val MAX_AIR_TICKS = 200

        private const val BABY_START_AGE_TICKS = -72_000
        private const val NATURAL_FOLLOWER_BABY_SPAWN_CHANCE = 0.30f
        private const val ADULT_MAXIMUM_CLIMB_HEIGHT = 2.0
        private const val BABY_MAXIMUM_CLIMB_HEIGHT = 1.5
        private const val MINIMUM_VISIBLE_CLIMB_PITCH = 0.1f
        private const val MODEL_PITCH_CHANGE_PER_TICK = 0.65f
        private const val LOCOMOTION_ANIMATION_LOOP_TICKS = 50
        private const val WATER_GRAVITY = 0.4
        private const val MIN_FISH_PUSH_DISTANCE = 0.01
        private const val FISH_PUSH_STRENGTH = 0.05
        private const val LEGACY_TAG_HAS_REEF_ANCHOR = "RedSlobbererHasReefAnchor"
        private const val LEGACY_TAG_REEF_ANCHOR = "RedSlobbererReefAnchor"
        private const val LEGACY_TAG_RESIDENCE_TICKS = "RedSlobbererReefResidenceTicks"
        private const val LEGACY_TAG_PLACED_DECORATIONS = "RedSlobbererReefDecorations"

        private val CLIMB_VISUAL_PITCH_TARGET: EntityDataAccessor<Float> = SynchedEntityData.defineId(
            RedSlobbererEntity::class.java,
            EntityDataSerializers.FLOAT
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

    val groupController = RedSlobbererGroupController(this)
    private val terrainAlignment = RedSlobbererTerrainAlignment(this)
    private val fishRefugeStorage = RedSlobbererFishRefugeStorage(this)

    private var climbVisualPitch = 0.0f
    private var previousClimbVisualPitch = 0.0f
    private var activeLocomotionAnimation: LocomotionAnimation? = null
    private var nextLocomotionAnimationBoundaryTick = 0
    private var isPathfinding = false
    private var legacyReefSnapshot: LegacyRedSlobbererReefSnapshot? = null

    var timeExposedInAir = 0

    init {
        moveControl = RedSlobbererMoveControl(this)
        setPathfindingMalus(PathType.WATER, 0.0f)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(CLIMB_VISUAL_PITCH_TARGET, 0.0f)
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
        val movementController = moveControl as? RedSlobbererMoveControl
        return if (isInWater && movementController?.preventsNativeStep != true) {
            WATER_GRAVITY
        } else {
            super.getDefaultGravity()
        }
    }

    override fun moveRelative(speed: Float, input: Vec3) {
        val movementController = moveControl as? RedSlobbererMoveControl
        val tangentMovement = if (movementController?.followsTerrainTangent == true) {
            terrainAlignment.createTangentMovement(input, speed, yRot)
        } else {
            null
        }

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

    val maximumClimbHeight: Double
        get() = if (isBaby) BABY_MAXIMUM_CLIMB_HEIGHT else ADULT_MAXIMUM_CLIMB_HEIGHT

    override fun onPathfindingStart() {
        super.onPathfindingStart()
        isPathfinding = true
    }

    override fun onPathfindingDone() {
        isPathfinding = false
        super.onPathfindingDone()
    }

    override fun maxUpStep(): Float {
        // The pathfinder needs the full climb height to create elevated nodes. Exposing it while
        // moving would also let Entity's generic collision code step onto side blocks and entities.
        return if (isPathfinding) {
            maximumClimbHeight.toFloat()
        } else {
            0.0f
        }
    }

    fun setClimbVisualPitchTarget(pitch: Float) {
        if (!level().isClientSide && entityData.get(CLIMB_VISUAL_PITCH_TARGET) != pitch) {
            entityData.set(CLIMB_VISUAL_PITCH_TARGET, pitch)
        }
    }

    fun getClimbVisualPitch(partialTick: Float): Float {
        return lerp(partialTick, previousClimbVisualPitch, climbVisualPitch)
    }

    fun getTerrainNormal(partialTick: Float): Vec3 {
        return terrainAlignment.getInterpolatedNormal(partialTick)
    }

    override fun tick() {
        super.tick()

        previousClimbVisualPitch = climbVisualPitch
        climbVisualPitch = Mth.approachDegrees(
            climbVisualPitch,
            entityData.get(CLIMB_VISUAL_PITCH_TARGET),
            MODEL_PITCH_CHANGE_PER_TICK
        )

        setXRot(0.0f)
        xRotO = 0.0f

        if (level().isClientSide) {
            setupAnimationStates()
        }
    }

    private fun setupAnimationStates() {
        val isClimbAnimating = abs(climbVisualPitch) > MINIMUM_VISIBLE_CLIMB_PITCH ||
            entityData.get(CLIMB_VISUAL_PITCH_TARGET) != 0.0f
        val isMoving = isClimbAnimating || walkAnimation.speed() > 0.01f
        val requestedAnimation = if (isMoving) {
            LocomotionAnimation.MOVE
        } else {
            LocomotionAnimation.IDLE
        }

        val activeAnimation = activeLocomotionAnimation
        if (activeAnimation == null) {
            startLocomotionAnimation(requestedAnimation)
            return
        }

        if (tickCount < nextLocomotionAnimationBoundaryTick) return

        if (requestedAnimation != activeAnimation) {
            startLocomotionAnimation(requestedAnimation)
            return
        }

        do {
            nextLocomotionAnimationBoundaryTick += LOCOMOTION_ANIMATION_LOOP_TICKS
        } while (tickCount >= nextLocomotionAnimationBoundaryTick)
    }

    private fun startLocomotionAnimation(animation: LocomotionAnimation) {
        idleAnimationState.stop()
        moveAnimationState.stop()

        when (animation) {
            LocomotionAnimation.IDLE -> idleAnimationState.start(tickCount)
            LocomotionAnimation.MOVE -> moveAnimationState.start(tickCount)
        }

        activeLocomotionAnimation = animation
        nextLocomotionAnimationBoundaryTick = tickCount + LOCOMOTION_ANIMATION_LOOP_TICKS
    }

    override fun aiStep() {
        terrainAlignment.tick()
        super.aiStep()
        val serverLevel = level() as? ServerLevel ?: return

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
        val wasHurt = super.hurtServer(level, source, amount)
        if (wasHurt && source.entity != null) {
            fishRefugeStorage.invalidateAfterAttack(level)
        }
        return wasHurt
    }

    override fun remove(reason: Entity.RemovalReason) {
        if (reason.shouldDestroy() && !isRemoved) {
            (level() as? ServerLevel)?.let(fishRefugeStorage::releaseOnDestruction)
        }
        super.remove(reason)
    }

    @Deprecated("Minecraft still calls this hook for fluid-push immunity")
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
        return if (level.getFluidState(pos).`is`(FluidTags.WATER)) {
            10.0f
        } else {
            super.getWalkTargetValue(pos, level)
        }
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
