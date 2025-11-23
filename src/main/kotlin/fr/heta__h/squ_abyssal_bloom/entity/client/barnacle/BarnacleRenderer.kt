package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

import com.mojang.blaze3d.vertex.PoseStack
import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import net.minecraft.client.animation.KeyframeAnimation
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.resources.ResourceLocation

class BarnacleRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<BarnacleEntity, BarnacleRenderState, BarnacleModel>(
        context,
        BarnacleModel(context.bakeLayer(BarnacleModel.LAYER_LOCATION)),
        0f
    ) {

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "textures/entity/barnacle/barnacle.png"
        )
    }

    private val barnacleModel: BarnacleModel = this.model
    private var currentAnimation: KeyframeAnimation? = null

    override fun submit(
        renderState: BarnacleRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        poseStack.pushPose()
        poseStack.scale(2f, 2f, 2f) 
        super.submit(renderState, poseStack, collector, camera)
        poseStack.popPose()
    }

    override fun createRenderState(): BarnacleRenderState = BarnacleRenderState()

    override fun getTextureLocation(state: BarnacleRenderState): ResourceLocation = TEXTURE

    override fun extractRenderState(
        entity: BarnacleEntity,
        state: BarnacleRenderState,
        partialTicks: Float
    ) {
        super.extractRenderState(entity, state, partialTicks)

        
        if (state.animationState != entity.animationState) {
            
            state.animationUsage.stop()
            state.animationUsage.start(entity.tickCount)
        }

        state.animationState = entity.animationState
        state.animationUsage = entity.animationUsage
        state.animationTime = entity.animationTime
    }

}

