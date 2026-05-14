package fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus


import com.mojang.blaze3d.vertex.PoseStack
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.accessor.AddPropertiesToRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class NautilusLayer<S : LivingEntityRenderState, M : EntityModel<S>>(
    renderer: RenderLayerParent<S, M>,
    private val lampModel: NautilusLampModel,
    private val bubbleModel: NautilusBubbleSpitterModel,
    private val chestModel: NautilusChestModel
) : RenderLayer<S, M>(renderer) {

    companion object {
        private val TEXTURE_NAUTILUS_LAMP = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/nautilus_lamp/nautilus_lamp.png"
        )

        private val TEXTURE_BUBBLE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/nautilus_bubble_spitter/nautilus_bubble_spitter.png"
        )

        private val TEXTURE_CHEST = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/nautilus_chest/nautilus_chest.png"
        )

        val LAMP: Item = ModItems.NAUTILUS_LAMP.get()
        val SHIELD: Item = ModItems.BARBED_NAUTILUS_SCALE.get()
        val BUBBLE: Item = ModItems.BUBBLE_SPITTER.get()
        val CHEST: Item = Items.CHEST
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

            LAMP -> {
                poseStack.pushPose()
                this.parentModel.setupAnim(state)

                collector.submitModel(
                    lampModel,
                    state,
                    poseStack,
                    RenderTypes.entityCutout(TEXTURE_NAUTILUS_LAMP),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    0,
                    null
                )
                poseStack.popPose()
            }

            SHIELD -> {
                poseStack.pushPose()
                this.parentModel.setupAnim(state)

                val dummyRenderStack = ItemStack(ModItems.BARBED_NAUTILUS_SCALE_DISPLAY.get())
                if (extraItem.hasFoil()) {
                    dummyRenderStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
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

            BUBBLE -> {
                poseStack.pushPose()
                this.parentModel.setupAnim(state)

                collector.submitModel(
                    bubbleModel,
                    state,
                    poseStack,
                    RenderTypes.entityTranslucent(TEXTURE_BUBBLE),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    0,
                    null
                )

                if (extraItem.hasFoil()) {
                    collector.submitModel(
                        bubbleModel,
                        state,
                        poseStack,
                        RenderTypes.entityGlint(),
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        0,
                        null
                    )
                }

                poseStack.popPose()
            }

            CHEST -> {
                poseStack.pushPose()
                this.parentModel.setupAnim(state)

                collector.submitModel(
                    chestModel,
                    state,
                    poseStack,
                    RenderTypes.entityCutout(TEXTURE_CHEST),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    0,
                    null
                )
                poseStack.popPose()
            }
        }
    }
}