package fr.heta__h.squ_abyssal_bloom.entity.client.ghost_chimaera

import com.mojang.blaze3d.vertex.PoseStack
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.resources.Identifier

class GhostChimaeraEyesLayer(
    renderer: RenderLayerParent<GhostChimaeraRenderState, GhostChimaeraModel>
) : RenderLayer<GhostChimaeraRenderState, GhostChimaeraModel>(renderer) {

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/ghost_chimaera/ghost_chimaera.png"
        )
    }

    override fun submit(
        poseStack: PoseStack,
        nodeCollector: SubmitNodeCollector,
        packedLight: Int,
        renderState: GhostChimaeraRenderState,
        yRot: Float,
        xRot: Float
    ) {
        val model = parentModel

        model.root().visible = false
        model.eyes.visible = true

        coloredCutoutModelCopyLayerRender(
            model,
            TEXTURE,
            poseStack,
            nodeCollector,
            packedLight,
            renderState,
            -1,
            0
        )

        model.root().visible = true
    }
}
