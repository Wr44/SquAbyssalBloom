package fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer

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

class RedSlobbererModel(modelPart: ModelPart) : EntityModel<RedSlobbererRenderState>(modelPart) {

    private val idleAnimation = RedSlobbererAnimation.idle.bake(modelPart)
    private val moveAnimation = RedSlobbererAnimation.move.bake(modelPart)

    companion object {
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "red_slobberer"), "main"
        )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            val root = partdefinition.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 12.7111f, -18.6971f)
            )

            val yeux = root.addOrReplaceChild(
                "yeux",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.3491f, 0.0f, 0.0f)
            )

            val oeild = yeux.addOrReplaceChild(
                "oeild",
                CubeListBuilder.create().texOffs(120, 119)
                    .addBox(-0.7559f, -4.6009f, -0.5896f, 2.0f, 7.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-4.5f, 1.1276f, -0.4104f, 0.0f, 0.0f, -0.2182f)
            )

            oeild.addOrReplaceChild(
                "irisd",
                CubeListBuilder.create().texOffs(116, 104)
                    .addBox(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.2441f, -6.1009f, 0.4104f)
            )

            val oeilg = yeux.addOrReplaceChild(
                "oeilg",
                CubeListBuilder.create().texOffs(120, 119)
                    .addBox(-1.2885f, -4.8012f, -1.1534f, 2.0f, 7.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(4.5f, 1.3328f, 0.1534f, 0.0f, 0.0f, 0.2182f)
            )

            oeilg.addOrReplaceChild(
                "irisg",
                CubeListBuilder.create().texOffs(116, 111)
                    .addBox(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.3885f, -6.3012f, -0.1534f)
            )

            root.addOrReplaceChild(
                "corps",
                CubeListBuilder.create().texOffs(9, 0)
                    .addBox(-8.0f, -5.0f, -20.0f, 16.0f, 10.0f, 40.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 6.2889f, 19.6971f)
            )

            val coquille = root.addOrReplaceChild(
                "coquille",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 1.4863f, 4.1108f)
            )

            coquille.addOrReplaceChild(
                "membrane1",
                CubeListBuilder.create().texOffs(62, 107)
                    .addBox(-7.0f, -5.3f, -5.0f, 14.0f, 11.0f, 10.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, -1.2105f, 5.6548f, 1.2654f, 0.0f, 0.0f)
            )

            val membrane2 = coquille.addOrReplaceChild(
                "membrane2",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(8.0f, -2.1974f, 8.5863f, 0.1745f, 0.0f, 0.0f)
            )

            val vegetation = membrane2.addOrReplaceChild(
                "vegetation",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )

            val grass = vegetation.addOrReplaceChild(
                "grass",
                CubeListBuilder.create(),
                PartPose.offset(-3.0f, -5.6f, 6.4f)
            )

            val grass_h = grass.addOrReplaceChild(
                "grass_h",
                CubeListBuilder.create(),
                PartPose.offset(-4.0f, -1.5f, 0.0f)
            )

            grass_h.addOrReplaceChild(
                "grass_h4",
                CubeListBuilder.create().texOffs(89, 101)
                    .addBox(-9.0f, 0.0f, 0.5f, 18.0f, 0.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offset(-1.0f, 0.3f, 3.5f)
            )

            grass_h.addOrReplaceChild(
                "grass_g",
                CubeListBuilder.create().texOffs(124, 97)
                    .addBox(-0.5f, -0.3f, 0.0f, 1.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(7.5f, 13.0f, 4.0f)
            )

            grass_h.addOrReplaceChild(
                "grass_d",
                CubeListBuilder.create().texOffs(124, 97)
                    .addBox(-0.5f, -0.3f, 0.0f, 1.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-9.5f, 13.0f, 4.0f)
            )

            val grass_v = grass.addOrReplaceChild(
                "grass_v",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(4.0f, 1.5f, 0.0f, 0.0f, 0.0f, -1.5708f)
            )

            grass_v.addOrReplaceChild(
                "grass_g4",
                CubeListBuilder.create().texOffs(100, 93)
                    .addBox(-6.5f, 0.0f, 0.0f, 13.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-3.5f, -0.3f, 4.0f)
            )

            grass_v.addOrReplaceChild(
                "grass1gout",
                CubeListBuilder.create().texOffs(108, 87)
                    .addBox(-4.5f, 0.0f, 0.0f, 9.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-3.5f, 0.6f, 1.0f)
            )

            grass_v.addOrReplaceChild(
                "grass2out",
                CubeListBuilder.create().texOffs(108, 87)
                    .addBox(-4.5f, 0.0f, 0.0f, 9.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-3.5f, -18.65f, 1.0f)
            )

            grass_v.addOrReplaceChild(
                "grass_d2",
                CubeListBuilder.create().texOffs(100, 93)
                    .addBox(-6.5f, 0.3f, 0.0f, 13.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-3.5f, -18.0f, 4.0f)
            )

            val vegetation_tree = vegetation.addOrReplaceChild(
                "vegetation_tree",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-17.4f, -7.3015f, -5.4174f, 0.258f, 0.045f, -0.5614f)
            )

            vegetation_tree.addOrReplaceChild(
                "vegetation_tree2",
                CubeListBuilder.create().texOffs(108, 76)
                    .addBox(-5.0f, -6.0f, 0.0f, 10.0f, 7.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.7854f, 0.0f)
            )

            vegetation_tree.addOrReplaceChild(
                "vegetation_tree1",
                CubeListBuilder.create().texOffs(108, 76)
                    .addBox(-5.0f, -6.0f, 0.0f, 10.0f, 7.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.7854f, 0.0f)
            )

            val vegetation_little_tree = vegetation.addOrReplaceChild(
                "vegetation_little_tree",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.7f, 6.1f, -5.0f, 0.3491f, 0.0f, 2.0508f)
            )

            vegetation_little_tree.addOrReplaceChild(
                "vegetation_tree4",
                CubeListBuilder.create().texOffs(105, 110)
                    .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 3.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.7854f, 0.0f)
            )

            vegetation_little_tree.addOrReplaceChild(
                "vegetation_tree5",
                CubeListBuilder.create().texOffs(105, 110)
                    .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 3.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.7854f, 0.0f)
            )

            val vegetation_little_tree2 = vegetation.addOrReplaceChild(
                "vegetation_little_tree2",
                CubeListBuilder.create(),
                PartPose.offset(-2.3f, -6.9f, 7.0f)
            )

            vegetation_little_tree2.addOrReplaceChild(
                "vegetation_tree3",
                CubeListBuilder.create().texOffs(105, 110)
                    .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 3.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.7854f, 0.0f)
            )

            vegetation_little_tree2.addOrReplaceChild(
                "vegetation_tree6",
                CubeListBuilder.create().texOffs(105, 110)
                    .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 3.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.7854f, 0.0f)
            )

            val vegetation_little_tree3 = vegetation.addOrReplaceChild(
                "vegetation_little_tree3",
                CubeListBuilder.create(),
                PartPose.offset(-12.3f, -6.9f, 2.0f)
            )

            vegetation_little_tree3.addOrReplaceChild(
                "vegetation_tree7",
                CubeListBuilder.create().texOffs(105, 110)
                    .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 3.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.7854f, 0.0f)
            )

            vegetation_little_tree3.addOrReplaceChild(
                "vegetation_tree8",
                CubeListBuilder.create().texOffs(105, 110)
                    .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 3.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.7854f, 0.0f)
            )

            membrane2.addOrReplaceChild(
                "inner",
                CubeListBuilder.create().texOffs(0, 108)
                    .addBox(-18.0f, -5.1131f, -2.5315f, 20.0f, 9.0f, 10.0f, CubeDeformation(0.0f))
                    .texOffs(18, 77).addBox(-17.0f, -7.1131f, -5.5315f, 18.0f, 13.0f, 16.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )

            val membrane3 = coquille.addOrReplaceChild(
                "membrane3",
                CubeListBuilder.create().texOffs(1, 53)
                    .addBox(-15.0f, -6.1131f, 0.4685f, 14.0f, 11.0f, 10.0f, CubeDeformation(0.0f))
                    .texOffs(54, 57).addBox(-16.0f, -4.1131f, 3.4685f, 16.0f, 6.0f, 5.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(8.0f, -2.1974f, 16.5863f, 0.1309f, 0.0f, 0.0f)
            )

            val grass2 = membrane3.addOrReplaceChild(
                "grass2",
                CubeListBuilder.create(),
                PartPose.offset(1.0f, -4.1f, -1.6f)
            )

            grass2.addOrReplaceChild(
                "grass_h2",
                CubeListBuilder.create().texOffs(98, 71)
                    .addBox(-7.0f, 0.3f, 0.0f, 14.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-9.0f, -2.0f, 12.0f)
            )

            val grass_v2 = grass2.addOrReplaceChild(
                "grass_v2",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0f, 1.0442f, 7.9316f, 0.0f, 0.0f, -1.5708f)
            )

            grass_v2.addOrReplaceChild(
                "grass_g2",
                CubeListBuilder.create().texOffs(106, 66)
                    .addBox(-5.0f, -0.3f, 0.0f, 10.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-2.0f, -2.0442f, 4.0684f)
            )

            grass_v2.addOrReplaceChild(
                "grass_d3",
                CubeListBuilder.create().texOffs(106, 66)
                    .addBox(-5.0f, 0.2f, 0.0f, 10.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-2.0f, -16.0442f, 4.0684f)
            )

            val vegetation_little_tree4 = grass2.addOrReplaceChild(
                "vegetation_little_tree4",
                CubeListBuilder.create(),
                PartPose.offset(-8.7f, -1.3f, 8.6f)
            )

            vegetation_little_tree4.addOrReplaceChild(
                "vegetation_tree9",
                CubeListBuilder.create().texOffs(105, 110)
                    .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 3.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.7854f, 0.0f)
            )

            vegetation_little_tree4.addOrReplaceChild(
                "vegetation_tree10",
                CubeListBuilder.create().texOffs(105, 110)
                    .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 3.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.7854f, 0.0f)
            )

            val membrane4 = coquille.addOrReplaceChild(
                "membrane4",
                CubeListBuilder.create().texOffs(72, 72)
                    .addBox(-13.0f, -5.1131f, 8.4685f, 10.0f, 8.0f, 6.0f, CubeDeformation(0.0f))
                    .texOffs(0, 84).addBox(-13.5f, -3.1131f, 11.0685f, 11.0f, 4.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(8.0f, -2.1974f, 16.5863f, 0.0873f, 0.0f, 0.0f)
            )

            val grass3 = membrane4.addOrReplaceChild(
                "grass3",
                CubeListBuilder.create(),
                PartPose.offset(1.0f, -4.1f, -1.6f)
            )

            val grass_h3 = grass3.addOrReplaceChild(
                "grass_h3",
                CubeListBuilder.create(),
                PartPose.offset(-8.0f, -3.0f, 0.0f)
            )

            grass_h3.addOrReplaceChild(
                "grass_h5",
                CubeListBuilder.create().texOffs(106, 61)
                    .addBox(-5.0f, 0.0f, 0.0f, 10.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-1.0f, 2.3f, 16.0f)
            )

            grass_h3.addOrReplaceChild(
                "grass_b",
                CubeListBuilder.create().texOffs(106, 61)
                    .addBox(-5.0f, 0.0f, 0.0f, 10.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-1.0f, 9.7f, 16.0f)
            )

            val grass_v3 = grass3.addOrReplaceChild(
                "grass_v3",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.5708f)
            )

            grass_v3.addOrReplaceChild(
                "grass_g3",
                CubeListBuilder.create().texOffs(110, 57)
                    .addBox(-4.0f, -0.3f, 0.0f, 8.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-3.0f, -4.0f, 16.0f)
            )

            grass_v3.addOrReplaceChild(
                "grass_d4",
                CubeListBuilder.create().texOffs(110, 57)
                    .addBox(-4.0f, 0.3f, 0.0f, 8.0f, 0.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-3.0f, -14.0f, 16.0f)
            )

            coquille.addOrReplaceChild(
                "membrane5",
                CubeListBuilder.create().texOffs(95, 21)
                    .addBox(-11.0f, -4.1131f, 7.4685f, 6.0f, 5.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(8.0f, -2.1974f, 20.5863f, 0.0436f, 0.0f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 128, 128)
        }
    }

    override fun setupAnim(renderState: RedSlobbererRenderState) {
        super.setupAnim(renderState)

        this.idleAnimation.apply(renderState.idleAnimationState, renderState.ageInTicks)
        this.moveAnimation.apply(renderState.moveAnimationState, renderState.ageInTicks)
    }
}