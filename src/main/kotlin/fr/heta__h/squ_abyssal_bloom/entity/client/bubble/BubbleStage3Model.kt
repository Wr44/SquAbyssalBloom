package fr.heta__h.squ_abyssal_bloom.entity.client.bubble

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.Identifier
import net.minecraft.client.renderer.entity.state.EntityRenderState

class BubbleStage3Model(rootPart: ModelPart) : EntityModel<EntityRenderState>(rootPart) {

    private val bubble: ModelPart = rootPart.getChild("bubble")

    companion object {
        val LAYER_LOCATION = ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "bubble_stage3"),
            "main"
        )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.getRoot()

            val bubble = partdefinition.addOrReplaceChild(
                "bubble",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-5.0f, -10f, -5.0f, 10.0f, 10.0f, 10.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }
}