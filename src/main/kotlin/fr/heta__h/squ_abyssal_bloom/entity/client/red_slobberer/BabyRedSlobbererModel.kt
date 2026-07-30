package fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.animation.KeyframeAnimation
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.Identifier

class BabyRedSlobbererModel(modelPart: ModelPart) : EntityModel<RedSlobbererRenderState>(modelPart) {

    private val moveAnimation: KeyframeAnimation = BabyRedSlobbererAnimation.move.bake(modelPart)

    companion object {
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "baby_red_slobberer"), "main"
        )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            val root = partdefinition.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offset(-0.6f, 20.0f, -2.6f)
            )

            val carapace = root.addOrReplaceChild(
                "carapace",
                CubeListBuilder.create(),
                PartPose.offset(0.5f, 0.9f, -2.4f)
            )

            carapace.addOrReplaceChild(
                "segement1",
                CubeListBuilder.create().texOffs(52, 19)
                    .addBox(-1.0f, -2.0f, -2.0f, 3.0f, 2.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-0.5f, -0.9f, 2.4f, 1.2654f, 0.0f, 0.0f)
            )

            val segement3 = carapace.addOrReplaceChild(
                "segement3",
                CubeListBuilder.create(),
                PartPose.offset(0.1f, 0.6f, 9.0f)
            )

            segement3.addOrReplaceChild(
                "segement3_r1",
                CubeListBuilder.create().texOffs(52, 13)
                    .addBox(-2.0f, -4.0f, -1.0f, 4.0f, 3.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0873f, 0.0f, 0.0f)
            )

            carapace.addOrReplaceChild(
                "segement2",
                CubeListBuilder.create().texOffs(0, 18)
                    .addBox(-3.0f, -2.0f, -4.0f, 6.0f, 4.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.1f, -1.4f, 5.0f, 0.1309f, 0.0f, 0.0f)
            )

            root.addOrReplaceChild(
                "corps",
                CubeListBuilder.create().texOffs(30, 16)
                    .addBox(-2.0f, -3.0f, -7.0f, 4.0f, 3.0f, 13.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.6f, 4.0f, 2.6f)
            )

            val oeild = root.addOrReplaceChild(
                "oeild",
                CubeListBuilder.create().texOffs(34, 21)
                    .addBox(-0.5f, -1.7422f, 0.0414f, 1.0f, 2.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-0.3479f, 1.2854f, -4.3235f, 0.258f, 0.045f, -0.1687f)
            )

            oeild.addOrReplaceChild(
                "irisd",
                CubeListBuilder.create().texOffs(37, 21)
                    .addBox(-1.0f, -1.0f, -2.0f, 1.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.5f, -1.3422f, 1.5414f)
            )

            val oeilg = root.addOrReplaceChild(
                "oeilg",
                CubeListBuilder.create().texOffs(34, 21)
                    .addBox(-0.4f, -1.8924f, 0.0166f, 1.0f, 2.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(1.5223f, 1.2799f, -4.325f, 0.2533f, -0.067f, 0.2533f)
            )

            oeilg.addOrReplaceChild(
                "irisg",
                CubeListBuilder.create().texOffs(37, 19)
                    .addBox(-0.9f, -1.0f, -2.3f, 1.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.5f, -1.3889f, 1.8029f)
            )

            return LayerDefinition.create(meshdefinition, 64, 32)
        }
    }

    override fun setupAnim(renderState: RedSlobbererRenderState) {
        super.setupAnim(renderState)

        this.moveAnimation.apply(renderState.moveAnimationState, renderState.ageInTicks)
    }
}