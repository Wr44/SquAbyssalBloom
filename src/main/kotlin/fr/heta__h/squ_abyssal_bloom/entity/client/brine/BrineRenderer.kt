package fr.heta__h.squ_abyssal_bloom.entity.client.brine

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.resources.Identifier

class BrineRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<BrineEntity, BrineRenderState, BrineModel>(
        context,
        BrineModel(context.bakeLayer(BrineModel.LAYER_LOCATION)),
        0.5f 
    ) {

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "textures/entity/brine/brine.png"
        )
    }

    override fun submit(
        renderState: BrineRenderState,
        poseStack: PoseStack,
        nodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        poseStack.pushPose()
        poseStack.scale(1.4f, 1.4f, 1.4f)
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState)
        poseStack.popPose()
    }

    override fun createRenderState(): BrineRenderState = BrineRenderState()

    override fun getTextureLocation(state: BrineRenderState): Identifier = TEXTURE

    override fun setupRotations(
        state: BrineRenderState,
        poseStack: PoseStack,
        rotationYaw: Float,
        partialTicks: Float
    ) {
        super.setupRotations(state, poseStack, rotationYaw, partialTicks)

        poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot))
    }

    override fun extractRenderState(
        entity: BrineEntity,
        state: BrineRenderState,
        partialTicks: Float
    ) {
        super.extractRenderState(entity, state, partialTicks)

        state.xRot = entity.getViewXRot(partialTicks)
        state.yRot = entity.getViewYRot(partialTicks)

        state.idleAnimationState.copyFrom(entity.idleAnimationState)
        state.startAttackAnimationState.copyFrom(entity.startAttackAnimationState)
        state.stopAttackAnimationState.copyFrom(entity.stopAttackAnimationState)
        state.loopAttackAnimationState.copyFrom(entity.loopAttackAnimationState)
    }
}