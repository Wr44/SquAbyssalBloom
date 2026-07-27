package fr.heta__h.squ_abyssal_bloom.event.fish_school

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.FishCollectiveManager
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object FishSchoolLifecycleListener {

    @SubscribeEvent
    fun onFishDeath(event: LivingDeathEvent) {
        forget(event.entity)
    }

    @SubscribeEvent
    fun onFishLeaveLevel(event: EntityLeaveLevelEvent) {
        forget(event.entity)
    }

    private fun forget(entity: Entity) {
        val fish = entity as? AbstractFish ?: return
        val level = fish.level() as? ServerLevel ?: return
        FishCollectiveManager.forLevel(level).forget(fish)
    }
}
