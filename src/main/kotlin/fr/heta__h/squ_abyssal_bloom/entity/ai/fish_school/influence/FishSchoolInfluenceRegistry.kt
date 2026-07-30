package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.AbstractFish
import java.util.concurrent.CopyOnWriteArrayList

object FishSchoolInfluenceRegistry {
    private val providers = CopyOnWriteArrayList<FishSchoolInfluenceProvider>()

    fun register(provider: FishSchoolInfluenceProvider): Boolean {
        return providers.addIfAbsent(provider)
    }

    fun maximumSearchRadius(): Double {
        var maximumRadius = 0.0
        for (provider in providers) {
            if (provider.searchRadius > maximumRadius) {
                maximumRadius = provider.searchRadius
            }
        }
        return maximumRadius
    }

    fun collect(
        fish: AbstractFish,
        nearbyEntities: List<LivingEntity>,
        output: MutableList<FishSchoolInfluence>
    ) {
        for (provider in providers) {
            provider.collectInfluences(fish, nearbyEntities, output)
        }
    }
}
