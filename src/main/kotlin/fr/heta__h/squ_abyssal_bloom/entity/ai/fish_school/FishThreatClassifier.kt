package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.entity.animal.fish.WaterAnimal
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.player.Player

object FishThreatClassifier {
    fun isThreat(
        observer: AbstractFish,
        entity: LivingEntity
    ): Boolean {
        if (entity === observer || !entity.isAlive) return false
        if (entity is AbstractFish) return false
        if (observer.isAlliedTo(entity) || entity.isAlliedTo(observer)) return false
        if (entity.typeHolder().`is`(ModTags.EntityTypes.FISH_SCHOOL_FRIENDLY)) return false

        if (entity is Player) {
            return !entity.isCreative && !entity.isSpectator
        }
        if (entity is Enemy) return true
        if (entity.type.category.isFriendly) return false
        if (entity is Animal || entity is WaterAnimal) return false

        return true
    }
}
