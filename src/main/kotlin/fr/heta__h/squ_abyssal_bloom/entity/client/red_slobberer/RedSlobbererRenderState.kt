package fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.AnimationState

class RedSlobbererRenderState : LivingEntityRenderState() {
    var isBaby: Boolean = false
    val idleAnimationState = AnimationState()
    val moveAnimationState = AnimationState()
}