package fr.heta__h.squ_abyssal_bloom.entity.render_layer.guardian_spike

import com.mojang.blaze3d.vertex.PoseStack
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.accessor.AddPropertiesToRenderState
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import org.joml.Quaternionf
import kotlin.math.cos
import kotlin.math.sin

class GuardianSpikesLayer<S : LivingEntityRenderState, M : EntityModel<S>>(
    renderer: RenderLayerParent<S, M>,
    private val spikeModel: GuardianSpikeModel
) : RenderLayer<S, M>(renderer) {

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/guardian_spike/guardian_spike.png")
        const val NUMBER_OF_SPIKES = 10
    }

    override fun submit(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        packedLight: Int,
        state: S,
        p4: Float,
        p5: Float
    ) {
        val hasPotion = (state as? AddPropertiesToRenderState)?.getHasGuardianSpikes() ?: false
        if (!hasPotion) return

        val renderType = RenderTypes.entityCutout(TEXTURE)

        val width = state.boundingBoxWidth
        val height = state.boundingBoxHeight
        val tick = state.ageInTicks
        val eyeHeight = (state as? AddPropertiesToRenderState)?.getEyeHeight() ?: (height * 0.85f)

        val radius = width * 0.8f + 0.75f
        val rotationSpeed = tick * 0.05f

        poseStack.pushPose()

        val compensationRad = -state.bodyRot * (Math.PI.toFloat() / 180f)
        poseStack.mulPose(Quaternionf().rotationY(compensationRad))

        for (i in 0 until NUMBER_OF_SPIKES) {
            poseStack.pushPose()

            val normalizedRandom = cos(i.toDouble() * 654.321).toFloat()
            val randomOffsetV = -(height * 0.65f + normalizedRandom * (height * 0.35f))

            val baseAngle = i * (Math.PI.toFloat() * 2.0f / NUMBER_OF_SPIKES)
            val finalAngle = baseAngle + rotationSpeed
            val floatY = sin((tick * 0.1f + i).toDouble()).toFloat() * 0.15f

            val posX = cos(finalAngle.toDouble()).toFloat() * radius
            val posZ = sin(finalAngle.toDouble()).toFloat() * radius

            poseStack.translate(posX.toDouble(), (floatY + randomOffsetV).toDouble(), posZ.toDouble())
            poseStack.mulPose(Quaternionf().rotationY(-finalAngle))

            val scaleFactor = (width / 0.6f).coerceIn(0.4f, 2.5f)
            poseStack.scale(scaleFactor, scaleFactor, scaleFactor)

            collector.submitModel(
                spikeModel,
                state,
                poseStack,
                renderType,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0,
                null
            )

            poseStack.popPose()
        }

        poseStack.popPose()
    }
}