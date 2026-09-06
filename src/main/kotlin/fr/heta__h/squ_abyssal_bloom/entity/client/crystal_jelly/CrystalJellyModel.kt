package fr.heta__h.squ_abyssal_bloom.entity.client.crystal_jelly

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

class CrystalJellyModel(modelPart: ModelPart) : EntityModel<CrystalJellyRenderState>(modelPart) {

    private val idleAnimation: KeyframeAnimation = CrystalJellyAnimation.idle.bake(modelPart)
    private val swimAnimation: KeyframeAnimation = CrystalJellyAnimation.swimm.bake(modelPart)
    private val outOfWaterAnimation: KeyframeAnimation = CrystalJellyAnimation.out_of_water.bake(modelPart)
    private val strandingAnimation: KeyframeAnimation = CrystalJellyAnimation.idle_to_out_of_water.bake(modelPart)
    private val recoveryAnimation: KeyframeAnimation = CrystalJellyAnimation.out_of_water_to_idle.bake(modelPart)
    private val tentacleDriftAnimation: KeyframeAnimation = CrystalJellyAnimation.tentacle_drift.bake(modelPart)

    companion object {
        const val ROOT_PIVOT_Y = 24.0f

        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "crystal_jelly"), "main"
        )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            val root = partdefinition.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, ROOT_PIVOT_Y, 0.0f)
            )

            root.addOrReplaceChild(
                "up",
                CubeListBuilder.create().texOffs(0, 13)
                    .addBox(-4.0f, -3.0f, -4.0f, 8.0f, 2.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -2.0f, 0.0f)
            )

            root.addOrReplaceChild(
                "intern",
                CubeListBuilder.create().texOffs(0, 23)
                    .addBox(-3.0f, -4.0f, -3.0f, 6.0f, 5.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -0.5f, 0.0f)
            )

            val exterior = root.addOrReplaceChild(
                "exterior",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, -2.0f, 0.0f)
            )

            exterior.addOrReplaceChild(
                "down",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-5.0f, -3.0f, -5.0f, 10.0f, 3.0f, 10.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 2.0f, 0.0f)
            )

            val tentacle = exterior.addOrReplaceChild(
                "tentacle",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 2.0f, -5.0f)
            )

            tentacle.addOrReplaceChild(
                "tentacle1",
                CubeListBuilder.create().texOffs(24, 23)
                    .addBox(-1.0f, 0.0f, 0.0f, 2.0f, 12.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )

            tentacle.addOrReplaceChild(
                "tentacle2",
                CubeListBuilder.create().texOffs(32, 25)
                    .addBox(-1.0f, 0.0f, 0.0f, 2.0f, 12.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 10.0f)
            )

            tentacle.addOrReplaceChild(
                "tentacle3",
                CubeListBuilder.create().texOffs(0, 34)
                    .addBox(-1.0f, 0.0f, 0.0f, 2.0f, 12.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(5.0f, 0.0f, 5.0f, 0.0f, 1.5708f, 0.0f)
            )

            tentacle.addOrReplaceChild(
                "tentacle4",
                CubeListBuilder.create().texOffs(12, 34)
                    .addBox(-1.0f, 0.0f, 0.0f, 2.0f, 12.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-5.0f, 0.0f, 5.0f, 0.0f, 1.5708f, 0.0f)
            )

            tentacle.addOrReplaceChild(
                "tentacle5",
                CubeListBuilder.create().texOffs(4, 34)
                    .addBox(-1.0f, 0.0f, 0.0f, 2.0f, 12.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(4.0151f, 0.0f, 9.0707f, 0.0f, 0.7854f, 0.0f)
            )

            tentacle.addOrReplaceChild(
                "tentacle6",
                CubeListBuilder.create().texOffs(8, 34)
                    .addBox(-1.0f, 0.0f, 0.0f, 2.0f, 12.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-4.0459f, 0.0f, 1.0097f, 0.0f, 0.7854f, 0.0f)
            )

            tentacle.addOrReplaceChild(
                "tentacle7",
                CubeListBuilder.create().texOffs(32, 13)
                    .addBox(-1.0f, 0.0f, 0.0f, 2.0f, 12.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-3.9903f, 0.0f, 8.9903f, 0.0f, -0.7854f, 0.0f)
            )

            tentacle.addOrReplaceChild(
                "tentacle8",
                CubeListBuilder.create().texOffs(28, 23)
                    .addBox(-1.0f, 0.0f, 0.0f, 2.0f, 12.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(4.0f, 0.0f, 1.0f, 0.0f, -0.7854f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }

    override fun setupAnim(renderState: CrystalJellyRenderState) {
        super.setupAnim(renderState)

        this.idleAnimation.apply(renderState.idleAnimationState, renderState.ageInTicks)
        this.swimAnimation.apply(renderState.swimAnimationState, renderState.ageInTicks)
        this.outOfWaterAnimation.apply(renderState.outOfWaterAnimationState, renderState.ageInTicks)
        this.strandingAnimation.apply(renderState.strandingAnimationState, renderState.ageInTicks)
        this.recoveryAnimation.apply(renderState.recoveryAnimationState, renderState.ageInTicks)

        this.tentacleDriftAnimation.apply(renderState.tentacleAnimationState, renderState.ageInTicks)
    }
}
