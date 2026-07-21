package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.phys.Vec3

interface FishSchoolInfluence {
    val source: Entity

    fun computeInfluence(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Vec3
}
