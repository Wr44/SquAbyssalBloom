package fr.heta__h.squ_abyssal_bloom.entity.client.sea_bunny

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.model.geom.builders.PartDefinition
import net.minecraft.resources.Identifier

class SeaBunnyModel(model: ModelPart) : EntityModel<SeaBunnyRenderState>(model) {
    companion object{
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "sea_bunny"), "main")

        fun createBodyLayer(): LayerDefinition {

            val meshDefinition: MeshDefinition = MeshDefinition()
            val partDefinition: PartDefinition = meshDefinition.root

            val root: PartDefinition =
                partDefinition.addOrReplaceChild("Root", CubeListBuilder.create(), PartPose.offset(0.0f, 24.0f, 0.0f))

            val head = root.addOrReplaceChild(
                "Head",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-6.0f, -6.0f, -10.0f, 12.0f, 10.0f, 10.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -4.0f, -8.0f)
            )

            val headfur = head.addOrReplaceChild("HeadFur", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f))

            headfur.addOrReplaceChild(
                "HeadHair1",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(6.0f, -4.0f, -3.0f, 0.1039f, 0.4245f, 0.2256f)
            )

            headfur.addOrReplaceChild(
                "HeadHair2",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(6.0f, 1.0f, -8.0f, 0.3447f, -0.0257f, 0.3932f)
            )

            headfur.addOrReplaceChild(
                "HeadHair3",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-6.0f, 1.0f, -5.0f, 0.1409f, -0.1277f, 2.6366f)
            )

            headfur.addOrReplaceChild(
                "HeadHair4",
                CubeListBuilder.create().texOffs(0, 2)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-3.0f, 2.5f, -10.0f, -1.9464f, 1.3022f, 1.403f)
            )

            headfur.addOrReplaceChild(
                "HeadHair5",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(5.0f, -1.5f, -10.0f, 2.3749f, 1.1482f, -0.9717f)
            )

            headfur.addOrReplaceChild(
                "HeadHair6",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-3.0f, -2.5f, -10.0f, 0.7224f, 1.1647f, -2.3397f)
            )

            headfur.addOrReplaceChild(
                "HeadHair7",
                CubeListBuilder.create().texOffs(0, 4)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-5.0f, -6.0f, -7.0f, -0.1564f, 0.2635f, -2.1152f)
            )

            headfur.addOrReplaceChild(
                "HeadHair8",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(2.0f, -6.0f, -8.0f, 0.0917f, 0.5555f, -1.2721f)
            )

            val rightear = head.addOrReplaceChild(
                "RightEar",
                CubeListBuilder.create().texOffs(60, 0)
                    .addBox(-1.0f, -12.0f, -1.0f, 1.0f, 12.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-3.2066f, -5.9088f, -3.0f, 0.2618f, 0.0f, -0.6545f)
            )

            rightear.addOrReplaceChild(
                "RBranchPivot",
                CubeListBuilder.create().texOffs(56, 7)
                    .addBox(-2.0f, 0.0f, 0.0f, 2.0f, 6.0f, 0.0f, CubeDeformation(0.0f))
                    .texOffs(56, 3).addBox(-1.0f, -4.0f, 0.0f, 1.0f, 4.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(-1.0f, -7.4f, -0.5f)
            )

            rightear.addOrReplaceChild(
                "RBranchPivot2",
                CubeListBuilder.create().texOffs(56, 7).mirror()
                    .addBox(0.0f, 0.0f, 0.0f, 2.0f, 6.0f, 0.0f, CubeDeformation(0.0f)).mirror(false)
                    .texOffs(56, 3).mirror().addBox(0.0f, -4.0f, 0.0f, 1.0f, 4.0f, 0.0f, CubeDeformation(0.0f))
                    .mirror(false),
                PartPose.offset(0.0f, -7.4f, -0.5f)
            )

            val leftear = head.addOrReplaceChild(
                "LeftEar",
                CubeListBuilder.create().texOffs(60, 0).mirror()
                    .addBox(0.0f, -12.0f, -1.0f, 1.0f, 12.0f, 1.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offsetAndRotation(3.2066f, -5.9088f, -3.0f, 0.2618f, 0.0f, 0.6545f)
            )

            leftear.addOrReplaceChild(
                "LranchPivot",
                CubeListBuilder.create().texOffs(56, 7).mirror()
                    .addBox(0.0f, 0.0f, 0.0f, 2.0f, 6.0f, 0.0f, CubeDeformation(0.0f)).mirror(false)
                    .texOffs(56, 3).mirror().addBox(0.0f, -4.0f, 0.0f, 1.0f, 4.0f, 0.0f, CubeDeformation(0.0f))
                    .mirror(false),
                PartPose.offset(1.0f, -7.4f, -0.5f)
            )

            leftear.addOrReplaceChild(
                "LBranchPivot",
                CubeListBuilder.create().texOffs(56, 7)
                    .addBox(-2.0f, 0.0f, 0.0f, 2.0f, 6.0f, 0.0f, CubeDeformation(0.0f))
                    .texOffs(56, 3).addBox(-1.0f, -4.0f, 0.0f, 1.0f, 4.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -7.4f, -0.5f)
            )

            val upper = root.addOrReplaceChild(
                "Upper",
                CubeListBuilder.create().texOffs(0, 25)
                    .addBox(-8.0f, -7.0f, 0.0f, 16.0f, 11.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -4.0f, -8.0f)
            )

            val upperfur =
                upper.addOrReplaceChild("UpperFur", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f))

            upperfur.addOrReplaceChild(
                "UpperHair1",
                CubeListBuilder.create().texOffs(0, 4)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(8.0f, -6.0f, 3.0f, -0.1069f, 0.1382f, -0.5746f)
            )

            upperfur.addOrReplaceChild(
                "UpperHair2",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(3.0f, -7.0f, 5.0f, 0.1723f, 0.3463f, -1.2159f)
            )

            upperfur.addOrReplaceChild(
                "UpperHair3",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-4.0f, -7.0f, 2.0f, -0.4921f, 0.6476f, -2.1105f)
            )

            upperfur.addOrReplaceChild(
                "UpperHair4",
                CubeListBuilder.create().texOffs(0, 4)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-8.0f, 1.0f, 2.0f, -0.0924f, 0.4355f, 3.1087f)
            )

            val middle = root.addOrReplaceChild(
                "Middle",
                CubeListBuilder.create().texOffs(0, 44)
                    .addBox(-9.0f, -8.0f, 0.0f, 18.0f, 12.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -4.0f, 0.0f)
            )

            val middlefur =
                middle.addOrReplaceChild("MiddleFur", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f))

            middlefur.addOrReplaceChild(
                "MiddleHair1",
                CubeListBuilder.create().texOffs(0, 2)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(9.0f, -3.0f, 5.0f, 0.0f, 0.0f, -0.3491f)
            )

            middlefur.addOrReplaceChild(
                "MiddleHair2",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-9.0f, -7.0f, 1.0f, 0.3054f, 0.6545f, -2.7925f)
            )

            middlefur.addOrReplaceChild(
                "MiddleHair3",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, -8.0f, 3.0f, 0.0855f, 0.0629f, -1.2091f)
            )

            middlefur.addOrReplaceChild(
                "MiddleHair4",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-9.0f, -2.0f, 4.0f, -3.0999f, -0.2331f, 2.8161f)
            )

            val lower = root.addOrReplaceChild(
                "Lower",
                CubeListBuilder.create().texOffs(0, 25)
                    .addBox(-8.0f, -7.0f, 0.0f, 16.0f, 11.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -4.0f, 8.0f)
            )

            val lowerfur =
                lower.addOrReplaceChild("LowerFur", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f))

            lowerfur.addOrReplaceChild(
                "LowerHair1",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(3.0f, 0.0f, 8.0f, -1.879f, -1.3192f, 0.5451f)
            )

            lowerfur.addOrReplaceChild(
                "LowerHair2",
                CubeListBuilder.create().texOffs(0, 4)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(5.0f, -7.0f, 5.0f, -0.2575f, -0.3718f, -1.2479f)
            )

            lowerfur.addOrReplaceChild(
                "LowerHair3",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(8.0f, -6.0f, 3.0f, 0.0f, 0.3054f, -0.3491f)
            )

            lowerfur.addOrReplaceChild(
                "LowerHair4",
                CubeListBuilder.create().texOffs(0, 2)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(8.0f, 2.0f, 6.0f, -0.1582f, -0.4084f, 0.3819f)
            )

            lowerfur.addOrReplaceChild(
                "LowerHair5",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-5.0f, -7.0f, 3.0f, 0.0373f, -0.5089f, -2.0364f)
            )

            lowerfur.addOrReplaceChild(
                "LowerHair6",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-8.0f, 1.0f, 3.0f, -0.1584f, -0.4114f, 2.8867f)
            )

            return LayerDefinition.create(meshDefinition, 64, 64)
        }
    }

    override fun setupAnim(state: SeaBunnyRenderState) {
        super.setupAnim(state)
    }
}