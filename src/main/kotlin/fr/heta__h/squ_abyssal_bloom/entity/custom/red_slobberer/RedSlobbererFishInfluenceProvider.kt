package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer

import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluence
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluenceProvider
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.AbstractFish

class RedSlobbererFishInfluenceProvider : FishSchoolInfluenceProvider {
    override val searchRadius: Double = MAXIMUM_INFLUENCE_DISTANCE

    override fun collectInfluences(
        fish: AbstractFish,
        nearbyEntities: List<LivingEntity>,
        output: MutableList<FishSchoolInfluence>
    ) {
        for (entity in nearbyEntities) {
            val redSlobberer = entity as? RedSlobbererEntity ?: continue
            if (
                !redSlobberer.isAlive ||
                !redSlobberer.isUnderWater ||
                fish.distanceToSqr(redSlobberer) > MAXIMUM_INFLUENCE_DISTANCE_SQR
            ) {
                continue
            }
            output.add(RedSlobbererFishInfluence(redSlobberer))
        }
    }

    private companion object {
        const val MAXIMUM_INFLUENCE_DISTANCE = 16.0
        const val MAXIMUM_INFLUENCE_DISTANCE_SQR =
            MAXIMUM_INFLUENCE_DISTANCE * MAXIMUM_INFLUENCE_DISTANCE
    }
}
