package fr.heta__h.squ_abyssal_bloom.entity.client.brine

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.AnimationState

class BrineRenderState : LivingEntityRenderState() {
    val idleAnimationState = AnimationState()
    val startAttackAnimationState = AnimationState()
    val stopAttackAnimationState = AnimationState()
    val loopAttackAnimationState = AnimationState()
}