package fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.*
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.resources.Identifier

class NautilusChestModel(modelPart: ModelPart) : EntityModel<EntityRenderState>(modelPart) {

    private val chest1 = modelPart.getChild("chest1")
    private val chest2 = modelPart.getChild("chest2")

    companion object {
        val LAYER_LOCATION: ModelLayerLocation =
            ModelLayerLocation(
                Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "nautilus_chest"),
                "main"
            )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            partdefinition.addOrReplaceChild(
                "chest1",
                CubeListBuilder.create().texOffs(5, 18)
                    .addBox(-4.0f, -4.0f, -1.5f, 8.0f, 8.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-7.0f, 15.0f, 0.0f, 0.0f, -1.5708f, 0.0f)
            )

            partdefinition.addOrReplaceChild(
                "chest2",
                CubeListBuilder.create().texOffs(5, 7)
                    .addBox(-4.0f, -4.0f, -1.5f, 8.0f, 8.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(7.0f, 15.0f, 0.0f, 0.0f, -1.5708f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 32, 32)
        }
    }
}