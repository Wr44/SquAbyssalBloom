package fr.heta__h.squ_abyssal_bloom.entity.client.crystal_jelly

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.AnimationState

class CrystalJellyRenderState : LivingEntityRenderState() {
    var idleAnimationState = AnimationState()
    var swimAnimationState = AnimationState()
    var outOfWaterAnimationState = AnimationState()
    var strandingAnimationState = AnimationState()
    var recoveryAnimationState = AnimationState()
    var tentacleAnimationState = AnimationState()
    var fadeOutAlpha = 1.0f
    var depletion = 0.0f
}
