package fr.heta__h.squ_abyssal_bloom.entity.client.crystal_jelly

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.CrystalJellyEntity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.FULL_BRIGHT_LIGHTMAP
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.phys.AABB

class CrystalJellyRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<CrystalJellyEntity, CrystalJellyRenderState, CrystalJellyModel>(
        context,
        CrystalJellyModel(context.bakeLayer(CrystalJellyModel.LAYER_LOCATION)),
        0.35f
    ) {

    companion object {
        private const val BELL_PIVOT_HEIGHT =
            -EntityModel.MODEL_Y_OFFSET - CrystalJellyModel.ROOT_PIVOT_Y / 16.0f

        private const val TENTACLE_REACH = 1.1

        private const val DEPLETED_DIMMING = 0.45f
        private const val DEPLETED_DESATURATION = 0.45f

        private val TEXTURE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/crystal_jelly/crystal_jelly.png"
        )
    }

    override fun createRenderState(): CrystalJellyRenderState = CrystalJellyRenderState()

    override fun getTextureLocation(state: CrystalJellyRenderState): Identifier = TEXTURE

    override fun getRenderType(
        renderState: CrystalJellyRenderState,
        isVisible: Boolean,
        renderTranslucent: Boolean,
        appearsGlowing: Boolean
    ): RenderType? = RenderTypes.entityTranslucent(getTextureLocation(renderState))

    override fun getModelTint(renderState: CrystalJellyRenderState): Int {
        val alpha = (renderState.fadeOutAlpha * 255.0f).toInt().coerceIn(0, 255)

        val depletion = renderState.depletion
        if (depletion <= 0.0f) return ARGB.color(alpha, 255, 255, 255)

        val dimmed = 1.0f - DEPLETED_DIMMING * depletion
        val drained = dimmed * (1.0f - DEPLETED_DESATURATION * depletion)
        val warm = (dimmed * 255.0f).toInt().coerceIn(0, 255)
        val cool = (drained * 255.0f).toInt().coerceIn(0, 255)

        return ARGB.color(alpha, warm, cool, cool)
    }

    override fun getBoundingBoxForCulling(entity: CrystalJellyEntity): AABB =
        entity.boundingBox.inflate(TENTACLE_REACH)

    override fun setupRotations(
        state: CrystalJellyRenderState,
        poseStack: PoseStack,
        rotationYaw: Float,
        entityScale: Float
    ) {
        super.setupRotations(state, poseStack, rotationYaw, entityScale)

        poseStack.translate(0.0f, BELL_PIVOT_HEIGHT, 0.0f)
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot))
        poseStack.translate(0.0f, -BELL_PIVOT_HEIGHT, 0.0f)
    }

    override fun extractRenderState(
        entity: CrystalJellyEntity,
        state: CrystalJellyRenderState,
        partialTicks: Float
    ) {
        super.extractRenderState(entity, state, partialTicks)

        state.lightCoords = FULL_BRIGHT_LIGHTMAP
        state.idleAnimationState.copyFrom(entity.idleAnimationState)
        state.swimAnimationState.copyFrom(entity.swimAnimationState)
        state.outOfWaterAnimationState.copyFrom(entity.outOfWaterAnimationState)
        state.strandingAnimationState.copyFrom(entity.strandingAnimationState)
        state.recoveryAnimationState.copyFrom(entity.recoveryAnimationState)
        state.tentacleAnimationState.copyFrom(entity.tentacleAnimationState)
        state.fadeOutAlpha = entity.fadeOutAlpha
        state.depletion = entity.depletion
    }
}
