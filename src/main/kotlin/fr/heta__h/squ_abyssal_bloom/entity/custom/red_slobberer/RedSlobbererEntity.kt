package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer

import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererBottomStrollGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererBreedGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererFollowGroupGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererGoalPriorities
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererGrazeBloodSeagrassGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control.RedSlobbererBodyRotationControl
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control.RedSlobbererMoveControl
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.RedSlobbererGroupController
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.RedSlobbererReefController
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Pose
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
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import kotlin.math.abs

class RedSlobbererEntity(type: EntityType<out Animal>, level: Level) : Animal(type, level) {

    companion object {
        const val MAX_AIR_TICKS = 200

        private const val ADULT_MAXIMUM_CLIMB_HEIGHT = 2.0
        private const val BABY_MAXIMUM_CLIMB_HEIGHT = 1.5
        private const val MINIMUM_VISIBLE_CLIMB_PITCH = 0.1f
        private const val MODEL_PITCH_CHANGE_PER_TICK = 0.65f
        private const val LOCOMOTION_ANIMATION_LOOP_TICKS = 50
        private const val WATER_GRAVITY = 0.4

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
    private val reefController = RedSlobbererReefController(this, groupController)

    private var climbVisualPitch = 0.0f
    private var previousClimbVisualPitch = 0.0f
    private var activeLocomotionAnimation: LocomotionAnimation? = null
    private var nextLocomotionAnimationBoundaryTick = 0

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
            RedSlobbererGoalPriorities.FOLLOW_GROUP,
            RedSlobbererFollowGroupGoal(this, 0.8)
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

    override fun maxUpStep(): Float {
        val movementController = moveControl as? RedSlobbererMoveControl
        return if (movementController?.preventsNativeStep == true) {
            0.0f
        } else {
            maximumClimbHeight.toFloat()
        }
    }

    fun setClimbVisualPitchTarget(pitch: Float) {
        if (!level().isClientSide && entityData.get(CLIMB_VISUAL_PITCH_TARGET) != pitch) {
            entityData.set(CLIMB_VISUAL_PITCH_TARGET, pitch)
        }
    }

    fun getClimbVisualPitch(partialTick: Float): Float {
        return Mth.lerp(partialTick, previousClimbVisualPitch, climbVisualPitch)
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
        reefController.tick(serverLevel)
    }

    @Deprecated("Minecraft still calls this hook for fluid-push immunity")
    override fun isPushedByFluid(): Boolean = false

    override fun isPushable(): Boolean = false

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

    override fun isFood(food: ItemStack): Boolean {
        return food.typeHolder().`is`(ModTags.Items.RED_SLOBBERER_FOOD) ||
            food.item === ModItems.BLOOD_SEAGRASS.get() ||
            food.item === ModItems.TALL_BLOOD_SEAGRASS.get()
    }

    override fun getBreedOffspring(serverLevel: ServerLevel, otherParent: AgeableMob): AgeableMob? {
        return ModEntities.RED_SLOBBERER.get().create(serverLevel, EntitySpawnReason.BREEDING)
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        reefController.addAdditionalSaveData(output)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        reefController.readAdditionalSaveData(input)
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions {
        return if (isBaby) {
            EntityDimensions.scalable(0.75f, 0.5f)
        } else {
            super.getDefaultDimensions(pose)
        }
    }

    private enum class LocomotionAnimation {
        IDLE,
        MOVE
    }
}
