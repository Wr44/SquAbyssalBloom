package fr.heta__h.squ_abyssal_bloom.entity.client.guardian_spike

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.*
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.resources.Identifier


class GuardianSpikeModel(modelPart: ModelPart) : EntityModel<EntityRenderState>(modelPart) {


    private val root: ModelPart = modelPart.getChild("root")
    private val Spike: ModelPart = root.getChild("Spike")


    companion object {
        val LAYER_LOCATION: ModelLayerLocation =
            ModelLayerLocation(Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "guardian_spike"), "main")

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            val root = partdefinition.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-7.0f, 24.0f, -0.5f, 0.0f, 1.5708f, 0.0f)
            )

            val Spike: PartDefinition? = root.addOrReplaceChild(
                "Spike",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 14.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 32, 32)
        }
    }
}