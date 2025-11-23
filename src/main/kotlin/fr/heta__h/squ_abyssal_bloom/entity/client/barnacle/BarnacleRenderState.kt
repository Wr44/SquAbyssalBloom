package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.AnimationState


class BarnacleRenderState : LivingEntityRenderState() {
    var animationUsage = AnimationState()
    var animationState: BarnacleAnimationState = BarnacleAnimationState.STILL_MOUTH_CLOSE
    var animationTime: Float = 0f
}