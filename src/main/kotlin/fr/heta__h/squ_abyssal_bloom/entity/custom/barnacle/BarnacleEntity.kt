package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimationState
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.Level

class BarnacleEntity(type: EntityType<out Monster>, level: Level) : Monster(type, level) {
    var animationState: BarnacleAnimationState = BarnacleAnimationState.STILL_MOUTH_CLOSE
    var animationUsage = AnimationState()
    private var timeExposedInAir = 0
    var animationTime: Float = 0f

    override fun registerGoals() {
        super.registerGoals()
        
    }

    override fun tick() {
        super.tick()

        if (this.level().isClientSide) {
            
            val targetAnim = if (this.isUnderWater) {
                BarnacleAnimationState.STILL_MOUTH_CLOSE
            } else {
                BarnacleAnimationState.MOUTH_OPEN
            }

            
            playAnimation(targetAnim)

            

        }
    }

    fun playAnimation(anim: BarnacleAnimationState) {
        if (animationState != anim) {
            animationState = anim
            anim.anim.let { animDef ->
                animationUsage.stop()
                animationUsage.start(this.tickCount)
                
            }
        }
    }

    
    override fun aiStep() {
        super.aiStep()

        if (!animationUsage.isStarted) {
            animationUsage.start(this.tickCount)
        }

        animationTime += 0.05f 

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
