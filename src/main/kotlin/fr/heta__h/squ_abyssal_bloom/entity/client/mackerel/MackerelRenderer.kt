package fr.heta__h.squ_abyssal_bloom.entity.client.mackerel

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.mackerel.MackerelEntity
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import kotlin.math.sin

class MackerelRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<MackerelEntity, MackerelRenderState, EntityModel<MackerelRenderState>>(
        context,
        MackerelModel(context.bakeLayer(MackerelModel.LAYER_LOCATION)),
        0.15f
    ) {

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/mackerel/mackerel.png"
        )
    }

    override fun createRenderState(): MackerelRenderState = MackerelRenderState()

    override fun getTextureLocation(state: MackerelRenderState): Identifier = TEXTURE

    override fun setupRotations(
        state: MackerelRenderState,
        poseStack: PoseStack,
        rotationYaw: Float,
        partialTicks: Float
    ) {
        super.setupRotations(state, poseStack, rotationYaw, partialTicks)
        val bodyZRot = 4.3f * sin((0.6f * state.ageInTicks))
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyZRot))
        if (!state.isInWater) {
            poseStack.translate(0.1f, 0.1f, -0.1f)
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f))
        }
    }
}
