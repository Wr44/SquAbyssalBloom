package fr.heta__h.squ_abyssal_bloom.mixin.entity.fish_school

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.goal.FishSchoolGoalPriorities
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.goal.FishCollectiveObservationGoal
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.goal.FishSchoolMovementGoal
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.entity.animal.fish.WaterAnimal
import net.minecraft.world.level.Level
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(AbstractFish::class)
abstract class AbstractFishSchoolMixin(
    type: EntityType<out WaterAnimal>,
    level: Level
) : WaterAnimal(type, level) {

    @Inject(method = ["registerGoals"], at = [At("TAIL")])
    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun addCollectiveSchoolGoal(callbackInfo: CallbackInfo) {
        val fish = this as AbstractFish
        goalSelector.addGoal(
            FishSchoolGoalPriorities.OBSERVATION,
            FishCollectiveObservationGoal(fish)
        )
        goalSelector.addGoal(
            FishSchoolGoalPriorities.SCHOOL_MOVEMENT,
            FishSchoolMovementGoal(fish)
        )
    }
}
