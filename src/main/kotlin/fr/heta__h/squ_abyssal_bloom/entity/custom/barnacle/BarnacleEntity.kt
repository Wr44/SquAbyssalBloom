package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.Level

class BarnacleEntity(type: EntityType<out Monster>, level: Level) : Monster(type, level) {
    var stillMouthCloseAnimationState = AnimationState()
    var stillMouthOpenAnimationState = AnimationState()
    var openMouthAnimationState = AnimationState()
    var closeMouthAnimationState = AnimationState()
    var moveStillAnimationState = AnimationState()
    var moveRushAnimationState = AnimationState()
    var fleeStillAnimationState = AnimationState()
    var fleeRushAnimationState = AnimationState()
    var swallowAnimationState = AnimationState()
    var swallowStartAnimationState = AnimationState()
    var swallowStopAnimationState = AnimationState()


    private var timeExposedInAir = 0


    override fun registerGoals() {
        
    }

    override fun tick() {
        super.tick()

        if (this.level().isClientSide) {
            this.setupAnimationStates()
            
        }
    }

    private fun setupAnimationStates() {
        if (!this.stillMouthCloseAnimationState.isStarted) {
        this.stillMouthCloseAnimationState.start(this.tickCount)
        }
    }

    

    
    override fun aiStep() {
        super.aiStep()

        

        if (this.isUnderWater) {
            this.timeExposedInAir = 0
            this.airSupply = this.maxAirSupply
        } else {
            this.timeExposedInAir++
            if (this.timeExposedInAir >= 200) {
                this.hurt(this.damageSources().drown(), 1.0f)
            }
        }
    }

    companion object {
        fun createAttributes(): AttributeSupplier.Builder =
            createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 1.5)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
    }
}
