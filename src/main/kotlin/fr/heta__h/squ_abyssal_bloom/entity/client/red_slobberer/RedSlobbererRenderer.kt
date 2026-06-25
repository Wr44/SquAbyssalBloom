package fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier

class RedSlobbererRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<RedSlobbererEntity, RedSlobbererRenderState, EntityModel<RedSlobbererRenderState>>(
        context,
        RedSlobbererModel(context.bakeLayer(RedSlobbererModel.LAYER_LOCATION)),
        0.5f
    ) {

    private val adultModel = this.model
    private val babyModel = BabyRedSlobbererModel(context.bakeLayer(BabyRedSlobbererModel.LAYER_LOCATION))

    companion object {
        private val TEXTURE_ADULT = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/red_slobberer/red_slobberer.png"
        )
        private val TEXTURE_BABY = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/red_slobberer/red_slobberer_baby.png"
        )
    }

    override fun submit(
        renderState: RedSlobbererRenderState,
        poseStack: PoseStack,
        nodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        this.model = if (renderState.isBaby) babyModel else adultModel
        this.shadowRadius = if (renderState.isBaby) 0.25f else 1f
        poseStack.pushPose()
        if (!renderState.isBaby) poseStack.scale(2f, 2f, 2f)
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState)
        poseStack.popPose()
    }

    override fun createRenderState(): RedSlobbererRenderState = RedSlobbererRenderState()

    override fun getTextureLocation(state: RedSlobbererRenderState): Identifier {
        return if (state.isBaby) TEXTURE_BABY else TEXTURE_ADULT
    }

    override fun setupRotations(
        state: RedSlobbererRenderState,
        poseStack: PoseStack,
        rotationYaw: Float,
        partialTicks: Float
    ) {
        super.setupRotations(state, poseStack, rotationYaw, partialTicks)
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot))
    }

    override fun extractRenderState(
        entity: RedSlobbererEntity,
        state: RedSlobbererRenderState,
        partialTicks: Float
    ) {
        super.extractRenderState(entity, state, partialTicks)

        state.isBaby = entity.isBaby

        state.xRot = entity.getViewXRot(partialTicks)
        state.yRot = entity.getViewYRot(partialTicks)

        state.idleAnimationState.copyFrom(entity.idleAnimationState)
        state.moveAnimationState.copyFrom(entity.moveAnimationState)
    }
}