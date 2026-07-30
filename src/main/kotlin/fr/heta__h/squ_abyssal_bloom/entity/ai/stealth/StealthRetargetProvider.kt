package fr.heta__h.squ_abyssal_bloom.entity.ai.stealth

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob

interface StealthRetargetProvider {

    fun appliesTo(mob: Mob): Boolean

    fun findStealthRetarget(mob: Mob, excluded: LivingEntity?, isEligible: (LivingEntity) -> Boolean): LivingEntity?
}
