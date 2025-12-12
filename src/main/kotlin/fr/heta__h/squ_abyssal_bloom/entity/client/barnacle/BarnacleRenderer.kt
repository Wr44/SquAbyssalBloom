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
import kotlin.math.atan2
import kotlin.math.sqrt

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
        nodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        poseStack.pushPose()

        val dir = renderState.direction
        if (dir.lengthSqr() > 0.0001) {
            val yaw = (atan2(dir.z, dir.x) - Math.PI / 2).toFloat()
            val pitch = -atan2(dir.y, sqrt(dir.x * dir.x + dir.z * dir.z)).toFloat()

            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(yaw))
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(pitch))
        }
        poseStack.scale(2f, 2f, 2f) 
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState)
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
        state.direction = entity.directionMob
    }

}

