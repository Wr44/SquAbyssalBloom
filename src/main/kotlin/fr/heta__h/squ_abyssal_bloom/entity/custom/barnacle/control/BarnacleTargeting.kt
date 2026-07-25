package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import kotlin.math.abs

object BarnacleTargeting {
    fun isEligibleTarget(barnacle: BarnacleEntity, candidate: LivingEntity, radius: Double): Boolean {
        if (!candidate.isAlive || candidate.isRemoved || !candidate.isInWater || candidate.level() !== barnacle.level()) return false
        if (barnacle.distanceToSqr(candidate) > radius * radius) return false

        return when (candidate) {
            is Player ->
                (candidate.gameMode() == GameType.SURVIVAL || candidate.gameMode() == GameType.ADVENTURE) &&
                    !candidate.hasEffect(MobEffects.INVISIBILITY)

            is Guardian -> true
            else -> false
        }
    }

    fun findNearestTarget(
        barnacle: BarnacleEntity,
        radius: Double,
        excluded: LivingEntity? = null,
        additionalFilter: (LivingEntity) -> Boolean = { true }
    ): LivingEntity? {
        val searchBox = barnacle.boundingBox.inflate(radius)
        val nearbyBarnacles = barnacle.level().getEntitiesOfClass(BarnacleEntity::class.java, searchBox) {
            it.isAlive && !it.isRemoved
        }

        return barnacle.level().getEntitiesOfClass(LivingEntity::class.java, searchBox) { candidate ->
            candidate !== excluded &&
                isEligibleTarget(barnacle, candidate, radius) &&
                additionalFilter(candidate) &&
                isClosestBarnacle(barnacle, candidate, nearbyBarnacles) &&
                ModUtilities.hasClearPath(barnacle.level(), barnacle, candidate)
        }.minByOrNull(barnacle::distanceToSqr)
    }

    fun isPreferredBarnacle(barnacle: BarnacleEntity, victim: LivingEntity, radius: Double): Boolean {
        val nearbyBarnacles = barnacle.level().getEntitiesOfClass(
            BarnacleEntity::class.java,
            barnacle.boundingBox.inflate(radius)
        ) { it.isAlive && !it.isRemoved }
        return isClosestBarnacle(barnacle, victim, nearbyBarnacles)
    }

    private fun isClosestBarnacle(
        barnacle: BarnacleEntity,
        victim: LivingEntity,
        nearbyBarnacles: List<BarnacleEntity>
    ): Boolean {
        val myDistanceSqr = barnacle.distanceToSqr(victim)

        for (other in nearbyBarnacles) {
            if (other === barnacle) continue

            val otherDistanceSqr = other.distanceToSqr(victim)
            if (otherDistanceSqr < myDistanceSqr) return false
            if (abs(otherDistanceSqr - myDistanceSqr) < 0.0001 && other.id < barnacle.id) return false
        }

        return true
    }
}
