package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.goal

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.FishCollectiveManager
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.fish.AbstractFish

class FishCollectiveObservationGoal(
    private val fish: AbstractFish
) : Goal() {
    override fun canUse(): Boolean = canObserve()

    override fun canContinueToUse(): Boolean = canObserve()

    override fun tick() {
        val serverLevel = fish.level() as? ServerLevel ?: return
        FishCollectiveManager.forLevel(serverLevel).observe(fish)
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    private fun canObserve(): Boolean {
        return fish.level() is ServerLevel &&
            fish.isAlive &&
            fish.isInWater
    }
}
