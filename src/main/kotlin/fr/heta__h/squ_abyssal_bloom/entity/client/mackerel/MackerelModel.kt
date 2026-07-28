package fr.heta__h.squ_abyssal_bloom.entity.client.mackerel

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth

class MackerelModel(modelPart: ModelPart) : EntityModel<MackerelRenderState>(modelPart) {

    private val tailfin: ModelPart = modelPart.getChild("body").getChild("tailfin")

    companion object {
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "mackerel"), "main"
        )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            val body = partdefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 24.0f, -5.0f)
            )

            val mass = body.addOrReplaceChild(
                "mass",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, -1.0f, 0.0f)
            )

            mass.addOrReplaceChild(
                "inner_body",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-1.0f, -4.0f, 1.0f, 2.0f, 3.0f, 6.0f, CubeDeformation(0.0f))
                    .texOffs(16, 4).addBox(0.0f, -1.0f, 3.0f, 0.0f, 1.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(0, 17).addBox(0.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, CubeDeformation(0.0f))
                    .texOffs(16, 15).addBox(0.0f, -5.5f, 0.9f, 0.0f, 2.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(16, 7).addBox(0.0f, -5.0f, 5.0f, 0.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 1.0f, 0.0f)
            )

            mass.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(10, 15)
                    .addBox(-0.9992f, -1.5008f, -3.0f, 2.0f, 2.0f, 1.0f, CubeDeformation(0.0f))
                    .texOffs(10, 9).addBox(-1.0f, -2.0f, -2.0f, 2.0f, 3.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -1.0f, 0.0f)
            )

            mass.addOrReplaceChild(
                "leftFin",
                CubeListBuilder.create().texOffs(16, 0)
                    .addBox(0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.6109f)
            )

            mass.addOrReplaceChild(
                "rightFin",
                CubeListBuilder.create().texOffs(16, 2)
                    .addBox(-2.0f, 0.0f, 0.0f, 2.0f, 0.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-1.0f, -1.0f, 0.0f, 0.0f, 0.0f, -0.6109f)
            )

            body.addOrReplaceChild(
                "tailfin",
                CubeListBuilder.create().texOffs(0, 9)
                    .addBox(0.0f, -1.5f, 0.0f, 0.0f, 3.0f, 5.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -2.5f, 7.0f)
            )

            return LayerDefinition.create(meshdefinition, 32, 32)
        }
    }

    override fun setupAnim(renderState: MackerelRenderState) {
        super.setupAnim(renderState)
        val amplitudeMultiplier = if (renderState.isInWater) 1.0f else 1.5f
        tailfin.yRot = -amplitudeMultiplier * 0.45f * Mth.sin((0.6f * renderState.ageInTicks).toDouble())
    }
}
