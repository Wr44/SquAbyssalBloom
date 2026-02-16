package fr.heta__h.squ_abyssal_bloom.mixin.entity

import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.Level
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


@Mixin(Guardian::class, remap = false)
open class GuardianMixin protected constructor(type: EntityType<out Monster>, level: Level) : Monster(type, level) {
    @Inject(method = ["registerGoals"], at = [At("TAIL")])
    private fun addBarnacleTarget(ci: CallbackInfo?) {
        this.targetSelector.addGoal(
            1,
            NearestAttackableTargetGoal(this, BarnacleEntity::class.java, true)
        )
    }
}