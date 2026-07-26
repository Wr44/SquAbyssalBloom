package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.control

import fr.heta__h.squ_abyssal_bloom.entity.ai.stealth.StealthRetargetProvider
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob

class BarnacleStealthRetargetProvider : StealthRetargetProvider {
    override fun appliesTo(mob: Mob): Boolean = mob is BarnacleEntity

    override fun findStealthRetarget(mob: Mob, excluded: LivingEntity?, isEligible: (LivingEntity) -> Boolean): LivingEntity? {
        val barnacle = mob as BarnacleEntity
        return BarnacleTargeting.findNearestTarget(barnacle, barnacle.detectionRange, excluded, isEligible)
    }
}
