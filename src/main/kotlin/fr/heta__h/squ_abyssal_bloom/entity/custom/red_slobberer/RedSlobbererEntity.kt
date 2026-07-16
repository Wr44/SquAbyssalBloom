package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer

import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.AgeableWaterCreature
import net.minecraft.world.level.Level

class RedSlobbererEntity(type: EntityType<out AgeableWaterCreature>, level: Level) : AgeableWaterCreature(type, level) {

    val idleAnimationState = AnimationState()
    val moveAnimationState = AnimationState()

    var timeExposedInAir = 0

    companion object {
        const val MAX_AIR_TICKS = 200

        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
        }    }

    override fun aiStep() {
        super.aiStep()
        if (level() is ServerLevel) {
            if (isUnderWater) {
                timeExposedInAir = 0
                airSupply = maxAirSupply
            } else {
                timeExposedInAir++
                if (timeExposedInAir >= MAX_AIR_TICKS) {
                    hurtServer(level() as ServerLevel, damageSources().drown(), 2.0f)
                }
            }
        }
    }

    override fun getBreedOffspring(
        serverLevel: ServerLevel,
        otherParent: AgeableMob
    ): AgeableMob? {
        return ModEntities.RED_SLOBBERER.get().create(serverLevel, EntitySpawnReason.BREEDING)
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions {
        return if (this.isBaby) {
            EntityDimensions.scalable(0.75f, 0.5f)
        } else {
            super.getDefaultDimensions(pose)
        }
    }

}