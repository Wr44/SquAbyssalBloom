package fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus


import com.mojang.blaze3d.vertex.PoseStack
import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.mixin.`interface`.AddPropertiesToRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
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

        val NAUTILUS_LAMP: Item = ModItems.NAUTILUS_LAMP.get()
        val SHIELD: Item = ModItems.BARBED_NAUTILUS_SCALE.get()
    }

    override fun submit(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        packedLight: Int,
        state: S,
        p4: Float,
        p5: Float
    ) {
        val extraItem = (state as? AddPropertiesToRenderState)?.getNautilusExtraItem() ?: ItemStack.EMPTY

        if (extraItem.isEmpty) return

        when (extraItem.item) {

            NAUTILUS_LAMP -> {
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

            SHIELD -> { poseStack.pushPose()
                this.parentModel.setupAnim(state)

                val dummyRenderStack = ItemStack(ModItems.BARBED_NAUTILUS_SCALE_DISPLAY.get())
                if (extraItem.hasFoil()) {
                    dummyRenderStack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                }
                val itemRenderState = ItemStackRenderState()
                val localPlayer = Minecraft.getInstance().player ?: return

                Minecraft.getInstance().itemModelResolver.updateForNonLiving(
                    itemRenderState,
                    dummyRenderStack,
                    ItemDisplayContext.FIXED,
                    localPlayer
                )

                itemRenderState.submit(
                    poseStack,
                    collector,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    0
                )

                poseStack.popPose()
            }
        }
    }
}