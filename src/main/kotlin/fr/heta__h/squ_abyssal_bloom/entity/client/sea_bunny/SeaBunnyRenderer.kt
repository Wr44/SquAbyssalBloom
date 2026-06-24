package fr.heta__h.squ_abyssal_bloom.entity.client.sea_bunny

import com.mojang.blaze3d.vertex.PoseStack
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.sea_bunny.SeaBunnyEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier
import org.joml.Vector3f

class SeaBunnyRenderer(context: EntityRendererProvider.Context) : MobRenderer<SeaBunnyEntity, SeaBunnyRenderState, SeaBunnyModel>(
        context,
        SeaBunnyModel(context.bakeLayer(SeaBunnyModel.LAYER_LOCATION)),
        SHADOW
) {

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/sea_bunny/sea_bunny_white_variant.png"
        )

        private val size: Vector3f = Vector3f(0.25f, 0.25f, 0.25f)
        private val SHADOW = 1 * size.y
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
        poseStack.pushPose()
        poseStack.scale( size.x, size.y, size.z )
        super.submit(state, poseStack, submitNodeCollector, cameraState)
        poseStack.popPose()
    }

    override fun getTextureLocation(p0: SeaBunnyRenderState): Identifier = TEXTURE
}