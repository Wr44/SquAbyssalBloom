package fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.defense.RedSlobbererDefenseState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.AnimationState

class RedSlobbererRenderState : LivingEntityRenderState() {
    var isBaby: Boolean = false
    var climbPitch: Float = 0.0f
    var terrainPitch: Float = 0.0f
    var terrainRoll: Float = 0.0f
    var defenseState: RedSlobbererDefenseState = RedSlobbererDefenseState.NORMAL
    var defensePhaseElapsedTicks: Float = 0.0f
    val idleAnimationState = AnimationState()
    val moveAnimationState = AnimationState()
    val swingingAnimationState = AnimationState()
    val eyesAnimationState = AnimationState()
}
