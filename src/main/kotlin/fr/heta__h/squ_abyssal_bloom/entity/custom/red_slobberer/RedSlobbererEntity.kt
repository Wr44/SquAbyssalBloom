package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer

import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererBottomStrollGoal
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal.RedSlobbererGoalPriorities
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.BreedGoal
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
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.pathfinder.PathType

class RedSlobbererEntity(type: EntityType<out Animal>, level: Level) : Animal(type, level) {

    val idleAnimationState = AnimationState()
    val moveAnimationState = AnimationState()

    var timeExposedInAir = 0

    init {
        this.moveControl = RedSlobbererMoveControl(this)
        this.setPathfindingMalus(PathType.WATER, 0.0f)
    }

    companion object {
        const val MAX_AIR_TICKS = 200

        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.TEMPT_RANGE, 10.0)
        }
    }

    override fun createNavigation(level: Level): PathNavigation {
        return GroundPathNavigation(this, level).apply {
            setCanFloat(false)
        }
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(RedSlobbererGoalPriorities.BREED, BreedGoal(this, 1.0))
        goalSelector.addGoal(
            RedSlobbererGoalPriorities.TEMPT,
            TemptGoal(this, 1.25, Ingredient.of(ModItems.BLOOD_SEAGRASS.get()), false)
        )
        goalSelector.addGoal(RedSlobbererGoalPriorities.FOLLOW_PARENT, FollowParentGoal(this, 1.1))
        goalSelector.addGoal(RedSlobbererGoalPriorities.STROLL, RedSlobbererBottomStrollGoal(this, 1.0))
        goalSelector.addGoal(
            RedSlobbererGoalPriorities.LOOK_AT_PLAYER,
            LookAtPlayerGoal(this, Player::class.java, 6.0f)
        )
        goalSelector.addGoal(RedSlobbererGoalPriorities.RANDOM_LOOK, RandomLookAroundGoal(this))
    }

    /** Red Slobberers step over seabed relief; they never perform a ballistic ground jump. */
    override fun jumpFromGround() = Unit

    override fun tick() {
        super.tick()

        this.setXRot(0f)
        this.xRotO = 0f

        if (this.level().isClientSide) {
            setupAnimationStates()
        }
    }

    private fun setupAnimationStates() {
        val isMoving = this.walkAnimation.speed() > 0.01f

        if (isMoving) {
            this.idleAnimationState.stop()
            if (!this.moveAnimationState.isStarted) {
                this.moveAnimationState.start(this.tickCount)
            }
        } else {
            this.moveAnimationState.stop()
            if (!this.idleAnimationState.isStarted) {
                this.idleAnimationState.start(this.tickCount)
            }
        }
    }

    override fun aiStep() {
        super.aiStep()
        if (level() is ServerLevel) {
            if (isUnderWater) {
                timeExposedInAir = 0
                airSupply = maxAirSupply
            } else {
                timeExposedInAir++
                if (timeExposedInAir >= MAX_AIR_TICKS) {
                    hurtServer(level() as ServerLevel, damageSources().drown(), 2.0f)
                }
            }
        }
    }

    override fun isPushedByFluid(): Boolean = false

    override fun checkSpawnObstruction(level: LevelReader): Boolean {
        return level.isUnobstructed(this)
    }

    override fun isFood(food: ItemStack): Boolean {
        return food.`is`(ModItems.BLOOD_SEAGRASS.get())
    }

    override fun getBreedOffspring(serverLevel: ServerLevel, otherParent: AgeableMob): AgeableMob? {
        return ModEntities.RED_SLOBBERER.get().create(serverLevel, EntitySpawnReason.BREEDING)
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions {
        return if (this.isBaby) {
            EntityDimensions.scalable(0.75f, 0.5f)
        } else {
            super.getDefaultDimensions(pose)
        }
    }
}
