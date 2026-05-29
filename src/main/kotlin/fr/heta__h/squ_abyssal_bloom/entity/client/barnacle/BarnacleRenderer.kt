package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier

class BarnacleRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<BarnacleEntity, BarnacleRenderState, BarnacleModel>(
        context,
        BarnacleModel(context.bakeLayer(BarnacleModel.LAYER_LOCATION)),
        0f
    ) {

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/barnacle/barnacle.png"
        )
    }

    override fun submit(
        renderState: BarnacleRenderState,
        poseStack: PoseStack,
        nodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        poseStack.pushPose()
        poseStack.scale(2f, 2f, 2f)
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState)
        poseStack.popPose()
    }

    override fun createRenderState(): BarnacleRenderState = BarnacleRenderState()

    override fun getTextureLocation(state: BarnacleRenderState): Identifier = TEXTURE

    override fun setupRotations(
        state: BarnacleRenderState,
        poseStack: PoseStack,
        rotationYaw: Float,
        partialTicks: Float
    ) {
        super.setupRotations(state, poseStack, rotationYaw, partialTicks)

        poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot))
    }

    override fun extractRenderState(
        entity: BarnacleEntity,
        state: BarnacleRenderState,
        partialTicks: Float
    ) {
        super.extractRenderState(entity, state, partialTicks)

        state.xRot = entity.getViewXRot(partialTicks)
        state.yRot = entity.getViewYRot(partialTicks)

        state.stillMouthCloseAnimationState.copyFrom(entity.stillMouthCloseAnimationState)
        state.stillMouthOpenAnimationState.copyFrom(entity.stillMouthOpenAnimationState)
        state.moveStillAnimationState.copyFrom(entity.moveStillAnimationState)
        state.moveRushAnimationState.copyFrom(entity.moveRushAnimationState)
        state.openMouthAnimationState.copyFrom(entity.openMouthAnimationState)
        state.closeMouthAnimationState.copyFrom(entity.closeMouthAnimationState)
        state.fleeStillAnimationState.copyFrom(entity.fleeStillAnimationState)
        state.fleeRushAnimationState.copyFrom(entity.fleeRushAnimationState)
        state.swallowAnimationState.copyFrom(entity.swallowAnimationState)
        state.swallowStartAnimationState.copyFrom(entity.swallowStartAnimationState)
        state.swallowStopAnimationState.copyFrom(entity.swallowStopAnimationState)
    }

}

