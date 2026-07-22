package fr.heta__h.squ_abyssal_bloom.mixin.entity.fish_school

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.FishCollectiveManager
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.FollowFlockLeaderGoal
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(FollowFlockLeaderGoal::class)
abstract class FollowFlockLeaderGoalMixin {
    @Shadow
    @Final
    private lateinit var mob: AbstractSchoolingFish

    @Inject(
        method = ["canUse"],
        at = [At("HEAD")],
        cancellable = true
    )
    private fun suspendVanillaSchoolStart(
        callbackInfo: CallbackInfoReturnable<Boolean>
    ) {
        if (usesCollectiveMovement()) callbackInfo.returnValue = false
    }

    @Inject(
        method = ["canContinueToUse"],
        at = [At("HEAD")],
        cancellable = true
    )
    private fun suspendRunningVanillaSchool(
        callbackInfo: CallbackInfoReturnable<Boolean>
    ) {
        if (usesCollectiveMovement()) callbackInfo.returnValue = false
    }

    private fun usesCollectiveMovement(): Boolean {
        val serverLevel = mob.level() as? ServerLevel ?: return false
        return FishCollectiveManager.forLevel(serverLevel)
            .shouldUseCollectiveMovement(mob)
    }
}
