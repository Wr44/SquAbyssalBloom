package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import net.minecraft.world.entity.animal.fish.AbstractFish

object FishSchoolCompatibility {
    fun socialAffinity(
        observer: AbstractFish,
        neighbor: AbstractFish,
        settings: FishCollectiveSettings
    ): Double {
        return if (observer.type === neighbor.type) {
            1.0
        } else {
            settings.crossSpeciesAffinity
        }
    }
}
