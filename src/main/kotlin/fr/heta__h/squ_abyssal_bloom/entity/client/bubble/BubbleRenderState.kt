package fr.heta__h.squ_abyssal_bloom.entity.client.bubble

import net.minecraft.client.renderer.entity.state.EntityRenderState

class BubbleRenderState : EntityRenderState() {
    var bubbleStage: Int = 0
    var heldYaw: Float = 0f
    var isHeld: Boolean = false
    var ticksSinceRelease = 0f
    var releaseYaw = 0f
}