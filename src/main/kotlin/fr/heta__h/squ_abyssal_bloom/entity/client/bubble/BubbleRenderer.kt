package fr.heta__h.squ_abyssal_bloom.entity.client.bubble

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleRenderState
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth.rotLerp
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import kotlin.math.sin

class BubbleRenderer(context: EntityRendererProvider.Context) :
    EntityRenderer<BubbleProjectile, BubbleRenderState>(context) {

    private val modelStage1 = BubbleStage1Model(context.bakeLayer(BubbleStage1Model.LAYER_LOCATION))
    private val modelStage2 = BubbleStage2Model(context.bakeLayer(BubbleStage2Model.LAYER_LOCATION))
    private val modelStage3 = BubbleStage3Model(context.bakeLayer(BubbleStage3Model.LAYER_LOCATION))

    private val textureStage1 = Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "textures/entity/projectiles/bubble_stage1.png")
    private val textureStage2 = Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "textures/entity/projectiles/bubble_stage2.png")
    private val textureStage3 = Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "textures/entity/projectiles/bubble_stage3.png")

    override fun createRenderState(): BubbleRenderState = BubbleRenderState()

    override fun extractRenderState(
        entity: BubbleProjectile,
        state: BubbleRenderState,
        partialTicks: Float
    ) {
        super.extractRenderState(entity, state, partialTicks)
        state.bubbleStage = entity.bubbleStage
        state.ageInTicks = entity.tickCount + partialTicks
        if (entity.isHeld) {
            state.isHeld = true
            if (entity.isHeld) {
                val controller = (entity.owner as? AbstractNautilus)?.controllingPassenger
                    ?: entity.owner
                if (controller != null) {
                    state.heldYaw = rotLerp(partialTicks, controller.yRotO, controller.yRot)
                }
            }
        } else {
            state.isHeld = false
        }
        state.releaseYaw = -entity.releaseYaw
        state.ticksSinceRelease = if (entity.releaseTick >= 0)
            (entity.tickCount - entity.releaseTick) + partialTicks
        else -1f
    }

    override fun submit(
        renderState: BubbleRenderState,
        poseStack: PoseStack,
        nodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        poseStack.pushPose()

        if (renderState.isHeld) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.heldYaw))
        } else {
            val spinSpeed = when (renderState.bubbleStage) { 2 -> 1.2f; 1 -> 2.5f; else -> 4.5f }
            val spinAngle = (renderState.ageInTicks * spinSpeed) % 360f
            val wobble = sin((renderState.ageInTicks * 0.08f).toDouble()).toFloat() * 8f

            val finalYaw = if (renderState.ticksSinceRelease in 0f..15f) {
                val t = renderState.ticksSinceRelease / 15f
                val smooth = t * t * (3f - 2f * t) 
                rotLerp(smooth, renderState.releaseYaw, spinAngle)
            } else {
                spinAngle
            }

            poseStack.mulPose(Axis.YP.rotationDegrees(finalYaw))
            poseStack.mulPose(Axis.XP.rotationDegrees(wobble))
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f))

        poseStack.scale( when ( renderState.bubbleStage ) {
            2 -> 3f
            1 -> 2f
            else -> 1.5f
        }, when ( renderState.bubbleStage ) {
            2 -> 3f
            1 -> 2f
            else -> 1.5f
        }, when ( renderState.bubbleStage ) {
            2 -> 3f
            1 -> 2f
            else -> 1.5f
        })

        val activeModel = when (renderState.bubbleStage) {
            2 -> modelStage3
            1 -> modelStage2
            else -> modelStage1
        }

        val activeTexture = when (renderState.bubbleStage) {
            2 -> textureStage3
            1 -> textureStage2
            else -> textureStage1
        }

        val renderType = RenderTypes.entityCutoutNoCull(activeTexture)

        nodeCollector.submitCustomGeometry(poseStack, renderType) { pose, vertexConsumer ->
            val tempStack = PoseStack()

            tempStack.last().pose().set(pose.pose())
            tempStack.last().normal().set(pose.normal())

            activeModel.renderToBuffer(
                tempStack,
                vertexConsumer,
                15728880,
                OverlayTexture.NO_OVERLAY,
                -1
            )
        }

        super.submit(renderState, poseStack, nodeCollector, cameraRenderState)

        poseStack.popPose()
    }
}