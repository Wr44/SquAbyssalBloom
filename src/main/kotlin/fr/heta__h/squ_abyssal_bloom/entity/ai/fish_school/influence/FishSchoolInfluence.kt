package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.phys.Vec3

interface FishSchoolInfluence {
    val source: Entity

    fun isActive(fish: AbstractFish): Boolean =
        source.isAlive && !source.isRemoved

    fun computeInfluence(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Vec3


    fun threatEscapeSuppression(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Double = 0.0


    fun sourceEntityAvoidanceSuppression(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Double = 0.0

    fun separationSuppression(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Double = 0.0

    fun additionalVerticalSpeed(
        fish: AbstractFish,
        context: FishSchoolInfluenceContext
    ): Double = 0.0
}
