package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.AbstractFish

interface FishSchoolInfluenceProvider {
    val searchRadius: Double

    fun collectInfluences(
        fish: AbstractFish,
        nearbyEntities: List<LivingEntity>,
        output: MutableList<FishSchoolInfluence>
    )
}
