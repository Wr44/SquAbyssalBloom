package fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus


import com.mojang.blaze3d.vertex.PoseStack
import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.mixin.`interface`.AddPropertiesToRenderState
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

class NautilusLayer<S : LivingEntityRenderState, M : EntityModel<S>>(
    renderer: RenderLayerParent<S, M>,
    private val lampModel: NautilusLampModel
) : RenderLayer<S, M>(renderer) {

    companion object {
        private val TEXTURE_NAUTILUS_LAMP = Identifier.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID,
            "textures/entity/nautilus_lamp/nautilus_lamp.png"
        )

        val NAUTILUS_LAMP: Item = Items.CONDUIT
    }

    override fun submit(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        packedLight: Int,
        state: S,
        p4: Float,
        p5: Float
    ) {
        val extraItem = (state as? AddPropertiesToRenderState)?.getNautilusExtraItem() ?: net.minecraft.world.item.ItemStack.EMPTY

        if (extraItem.isEmpty || !extraItem.`is`(NAUTILUS_LAMP)) return
        val renderType = RenderTypes.entityCutout(TEXTURE_NAUTILUS_LAMP)

        poseStack.pushPose()

        this.parentModel.setupAnim(state)

        collector.submitModel(
            lampModel,
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
}