package fr.heta__h.squ_abyssal_bloom.entity.client.sea_bunny

import com.mojang.blaze3d.vertex.PoseStack
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.sea_bunny.SeaBunnyEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier

class SeaBunnyRenderer(context: EntityRendererProvider.Context) : LivingEntityRenderer<SeaBunnyEntity, SeaBunnyRenderState, SeaBunnyModel>(
        context,
        SeaBunnyModel(context.bakeLayer(SeaBunnyModel.LAYER_LOCATION)),
        0.5f
) {

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/sea_bunny/sea_bunny.png"
        )
    }

    override fun createRenderState(): SeaBunnyRenderState {
        return SeaBunnyRenderState()
    }

    override fun extractRenderState(entity: SeaBunnyEntity, state: SeaBunnyRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
    }

    override fun submit(
        state: SeaBunnyRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        cameraState: CameraRenderState
    ) {
        super.submit(state, poseStack, submitNodeCollector, cameraState)
    }

    override fun getTextureLocation(p0: SeaBunnyRenderState): Identifier = TEXTURE
}