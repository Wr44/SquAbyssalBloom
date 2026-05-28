package fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus

import com.mojang.blaze3d.vertex.PoseStack
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.util.accessor.AddPropertiesToRenderState
import fr.heta__h.squ_abyssal_bloom.util.conduit.ConduitMaterialHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.ConduitRenderer
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
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

class NautilusLayer<S : LivingEntityRenderState, M : EntityModel<S>>(
    renderer: RenderLayerParent<S, M>,
    private val lampModel: NautilusLampModel,
    private val bubbleModel: NautilusBubbleSpitterModel,
    private val chestModel: NautilusChestModel,
    private val conduitCage: ModelPart,
    private val conduitWind: ModelPart,
    private val conduitEye: ModelPart
) : RenderLayer<S, M>(renderer) {

    companion object {
        private val TEXTURE_NAUTILUS_LAMP = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID, "textures/entity/nautilus_lamp/nautilus_lamp.png"
        )
        private val TEXTURE_BUBBLE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID, "textures/entity/nautilus_bubble_spitter/nautilus_bubble_spitter.png"
        )
        private val TEXTURE_CHEST = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID, "textures/entity/nautilus_chest/nautilus_chest.png"
        )
        private val MOBILE_CAGE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID, "textures/entity/mobile_conduit/mobile_cage.png"
        )

        val LAMP: Item = ModItems.NAUTILUS_LAMP.get()
        val SHIELD: Item = ModItems.BARBED_NAUTILUS_SCALE.get()
        val BUBBLE: Item = ModItems.BUBBLE_SPITTER.get()
        val CHEST: Item = Items.CHEST
        val CONDUIT: Item = ModItems.MOBILE_CONDUIT.get()

        const val BASE_HEIGHT = 1f
        const val ORBIT_RADIUS = 1.8f
        const val ORBIT_SPEED = 0.04f
        const val VERTICAL_AMPLITUDE = 0.12f
        const val VERTICAL_PERIODS = 3.5f
        const val SELF_ROTATION_SPEED = 0.06f
        const val CONDUIT_SCALE = 0.90f
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

                if (extraItem.hasFoil()) dummyRenderStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)

                val itemRenderState = ItemStackRenderState()

                val localPlayer = Minecraft.getInstance().player ?: return

                Minecraft.getInstance().itemModelResolver.updateForNonLiving(itemRenderState, dummyRenderStack, ItemDisplayContext.FIXED, localPlayer)

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
                    collector.submitModel(bubbleModel, state, poseStack, RenderTypes.entityGlint(), packedLight, OverlayTexture.NO_OVERLAY, 0, null)
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

            CONDUIT -> {
                poseStack.pushPose()
                this.parentModel.setupAnim(state)
                renderPortableConduit(poseStack, collector, packedLight, state)
                poseStack.popPose()
            }
        }
    }

    private fun renderPortableConduit(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        packedLight: Int,
        state: S
    ) {
        val materials = ConduitMaterialHolder.materials ?: return

        val animTime = state.ageInTicks

        val orbitAngle = animTime * ORBIT_SPEED
        val verticalOffset = sin((orbitAngle * VERTICAL_PERIODS).toDouble()).toFloat() * VERTICAL_AMPLITUDE

        var f1 = sin((animTime * 0.1f).toDouble()).toFloat() / 2f + 0.5f
        f1 = f1 * f1 + f1

        poseStack.pushPose()

        val compensationRad = -state.bodyRot * (Math.PI.toFloat() / 180f)

        poseStack.mulPose(Quaternionf().rotationY(compensationRad))

        poseStack.translate(
            cos(orbitAngle.toDouble()).toFloat() * ORBIT_RADIUS,
            BASE_HEIGHT + verticalOffset,
            sin(orbitAngle.toDouble()).toFloat() * ORBIT_RADIUS
        )

        val scale = CONDUIT_SCALE
        poseStack.scale(scale, scale, scale)
        poseStack.translate(-0.5f, -0.5f, -0.5f)

        poseStack.pushPose()
        poseStack.translate(0.5f, 0.3f + f1 * 0.2f, 0.5f)
        val axis = Vector3f(0.5f, 1.0f, 0.5f).normalize()

        poseStack.mulPose(Quaternionf().rotationAxis(animTime * SELF_ROTATION_SPEED, axis))

        collector.submitModelPart(
            conduitCage,
            poseStack,
            RenderTypes.entityCutoutNoCull(MOBILE_CAGE),
            packedLight,
            OverlayTexture.NO_OVERLAY,
            null
        )

        poseStack.popPose()
        val windSprite = materials.get(ConduitRenderer.WIND_TEXTURE)
        val windType = ConduitRenderer.WIND_TEXTURE.renderType(RenderTypes::entityCutoutNoCull)

        poseStack.pushPose()
        poseStack.translate(0.5f, 0.5f, 0.5f)

        poseStack.mulPose(Quaternionf().rotationXYZ(animTime * 0.02f, animTime * 0.05f, animTime * 0.03f))

        collector.submitModelPart(conduitWind, poseStack, windType, packedLight, OverlayTexture.NO_OVERLAY, windSprite)
        poseStack.popPose()

        poseStack.pushPose()
        poseStack.translate(0.5f, 0.5f, 0.5f)
        poseStack.scale(0.875f, 0.875f, 0.875f)

        poseStack.mulPose(Quaternionf().rotationXYZ(-animTime * 0.03f, -animTime * 0.04f, -animTime * 0.02f))

        collector.submitModelPart(conduitWind, poseStack, windType, packedLight, OverlayTexture.NO_OVERLAY, windSprite)
        poseStack.popPose()

        poseStack.pushPose()
        poseStack.translate(0.5f, 0.3f + f1 * 0.2f, 0.5f)
        poseStack.scale(0.5f, 0.5f, 0.5f)

        poseStack.mulPose(Quaternionf().rotationY(-orbitAngle - (Math.PI.toFloat() / 2f)))

        poseStack.scale(4/3f, 4/3f, 4/3f)

        collector.submitModelPart(
            conduitEye, poseStack,
            ConduitRenderer.CLOSED_EYE_TEXTURE.renderType(RenderTypes::entityCutoutNoCull),
            packedLight, OverlayTexture.NO_OVERLAY,
            materials.get(ConduitRenderer.CLOSED_EYE_TEXTURE)
        )
        poseStack.popPose()

        poseStack.popPose()
    }
}