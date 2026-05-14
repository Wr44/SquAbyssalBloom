package fr.heta__h.squ_abyssal_bloom.entity.client.ghost_chimaera

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.ghost_chimaera.GhostChimaeraEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

class GhostChimaeraRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<GhostChimaeraEntity, GhostChimaeraRenderState, GhostChimaeraModel>(
        context,
        GhostChimaeraModel(context.bakeLayer(GhostChimaeraModel.LAYER_LOCATION)),
        0f
    ) {

    init {
        addLayer(GhostChimaeraEyesLayer(this))
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/ghost_chimaera/ghost_chimaera.png"
        )
    }

    override fun getRenderType(
        renderState: GhostChimaeraRenderState,
        isVisible: Boolean,
        renderTranslucent: Boolean,
        appearsGlowing: Boolean
    ): RenderType? {
        val texture = getTextureLocation(renderState)
        return RenderTypes.entityTranslucent(texture)
    }

    
    override fun getModelTint(renderState: GhostChimaeraRenderState): Int {
        val alpha = (renderState.bodyAlpha * 255).toInt().coerceIn(0, 255)
        return ARGB.color(alpha, 255, 255, 255)
    }

    override fun submit(
        renderState: GhostChimaeraRenderState,
        poseStack: PoseStack,
        nodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        poseStack.pushPose()
        poseStack.scale(5f,5f,5f)

        model.eyes.visible = false
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState)
        model.eyes.visible = true

        poseStack.popPose()
    }

    override fun createRenderState(): GhostChimaeraRenderState = GhostChimaeraRenderState()

    override fun getTextureLocation(state: GhostChimaeraRenderState): Identifier = TEXTURE

    override fun setupRotations(
        state: GhostChimaeraRenderState,
        poseStack: PoseStack,
        rotationYaw: Float,
        partialTicks: Float
    ) {
        super.setupRotations(state, poseStack, rotationYaw, partialTicks)

        poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot))
    }

    override fun extractRenderState(
        entity: GhostChimaeraEntity,
        state: GhostChimaeraRenderState,
        partialTicks: Float
    ) {
        super.extractRenderState(entity, state, partialTicks)

        state.xRot = entity.getViewXRot(partialTicks)
        state.yRot = entity.getViewYRot(partialTicks)
        state.bodyAlpha = entity.bodyAlpha
    }

}