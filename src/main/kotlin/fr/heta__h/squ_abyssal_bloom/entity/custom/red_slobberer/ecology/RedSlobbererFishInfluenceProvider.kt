package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluence
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluenceProvider
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.server.level.ServerLevel

class RedSlobbererFishInfluenceProvider : FishSchoolInfluenceProvider {

    private companion object {
        const val NORMAL_INFLUENCE_DISTANCE = 16.0
        const val NORMAL_INFLUENCE_DISTANCE_SQR = NORMAL_INFLUENCE_DISTANCE * NORMAL_INFLUENCE_DISTANCE
    }

    override val searchRadius: Double
        get() = ModServerConfig.RED_SLOBBERER_FISH_REFUGE_RADIUS.get().coerceAtLeast(1.0)

    override fun collectInfluences(
        fish: AbstractFish,
        nearbyEntities: List<LivingEntity>,
        output: MutableList<FishSchoolInfluence>
    ) {
        val refugeSearchDistance = searchRadius
        val refugeSearchDistanceSqr = refugeSearchDistance * refugeSearchDistance
        val redSlobberers = nearbyEntities.asSequence()
            .filterIsInstance<RedSlobbererEntity>()
            .filter { redSlobberer ->
                    redSlobberer.isAlive &&
                    redSlobberer.isUnderWater &&
                    fish.distanceToSqr(redSlobberer) <= refugeSearchDistanceSqr
            }
            .toList()
        val serverLevel = fish.level() as? ServerLevel
        val reefManager = serverLevel?.let(RedSlobbererReefManager::forLevel)
        reefManager?.recordRefugeScan(redSlobberers.isNotEmpty())
        if (redSlobberers.isEmpty()) return

        val reefShelter = reefManager?.findNearestShelter(fish, redSlobberers)
        if (reefShelter != null) {
            output.add(
                RedSlobbererFishInfluence(
                    reefShelter,
                    isReefRefuge = true,
                    refugeInfluenceDistance = refugeSearchDistance
                )
            )
            return
        }

        for (redSlobberer in redSlobberers) {
            if (fish.distanceToSqr(redSlobberer) > NORMAL_INFLUENCE_DISTANCE_SQR) continue
            output.add(
                RedSlobbererFishInfluence(
                    redSlobberer,
                    isReefRefuge = false,
                    refugeInfluenceDistance = refugeSearchDistance
                )
            )
        }
    }

}
