package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.AnimationState

class BarnacleRenderState : LivingEntityRenderState() {
    var stillMouthCloseAnimationState = AnimationState()
    var stillMouthOpenAnimationState = AnimationState()
    var moveStillAnimationState = AnimationState()
    var moveRushAnimationState = AnimationState()
    var openMouthAnimationState = AnimationState()
    var closeMouthAnimationState = AnimationState()
    var fleeStillAnimationState = AnimationState()
    var fleeRushAnimationState = AnimationState()
    var swallowAnimationState = AnimationState()
    var swallowStartAnimationState = AnimationState()
    var swallowStopAnimationState = AnimationState()
}