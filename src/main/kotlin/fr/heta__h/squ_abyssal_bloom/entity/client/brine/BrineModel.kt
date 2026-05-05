package fr.heta__h.squ_abyssal_bloom.entity.client.brine

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
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

class BrineModel(modelPart: ModelPart) : EntityModel<BrineRenderState>(modelPart) {

    private val idleAnimation: KeyframeAnimation = BrineAnimation.idle.bake(modelPart)
    private val startAttackAnimation: KeyframeAnimation = BrineAnimation.start_attack.bake(modelPart)
    private val stopAttackAnimation: KeyframeAnimation = BrineAnimation.stop_attack.bake(modelPart)
    private val loopAttackAnimation: KeyframeAnimation = BrineAnimation.loop_attack.bake(modelPart)

    companion object {
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "brine"), "main"
        )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            val root = partdefinition.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 12.1237f, -0.3816f)
            )

            root.addOrReplaceChild(
                "upperBodyParts0",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(8.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -6.7237f, 0.3816f)
            )

            root.addOrReplaceChild(
                "upperBodyParts1",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-10.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -6.7237f, 0.3816f)
            )

            root.addOrReplaceChild(
                "upperBodyParts2",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-1.0f, -1.0f, 8.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -6.7237f, 0.3816f)
            )

            root.addOrReplaceChild(
                "upperBodyParts3",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-1.0f, -1.0f, -10.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -6.7237f, 0.3816f)
            )

            val upperBodyParts15 = root.addOrReplaceChild(
                "upperBodyParts15",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, -1.5737f, 0.3816f)
            )
            upperBodyParts15.addOrReplaceChild(
                "r15",
                CubeListBuilder.create().texOffs(52, 51)
                    .addBox(-1.3f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-7.0f, 0.0f, 6.4722f, 0.0f, -0.7854f, 0.0f)
            )

            root.addOrReplaceChild(
                "upperBodyParts4",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(5.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 1.7763f, 0.3816f)
            )

            root.addOrReplaceChild(
                "upperBodyParts5",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-7.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 1.7763f, 0.3816f)
            )

            val upperBodyParts14 = root.addOrReplaceChild(
                "upperBodyParts14",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, -1.5737f, 0.3816f)
            )
            upperBodyParts14.addOrReplaceChild(
                "r14",
                CubeListBuilder.create().texOffs(52, 51)
                    .addBox(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-6.6757f, 0.0f, -6.7864f, 0.0f, -0.7854f, 0.0f)
            )

            root.addOrReplaceChild(
                "upperBodyParts6",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-1.0f, -1.0f, 5.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 1.7763f, 0.3816f)
            )

            val upperBodyParts12 = root.addOrReplaceChild(
                "upperBodyParts12",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, -1.5737f, 0.3816f)
            )
            upperBodyParts12.addOrReplaceChild(
                "r12",
                CubeListBuilder.create().texOffs(52, 51)
                    .addBox(-1.3f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(6.6464f, 0.0f, -6.75f, 0.0f, -0.7854f, 0.0f)
            )

            root.addOrReplaceChild(
                "upperBodyParts7",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-1.0f, -1.0f, -7.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 1.7763f, 0.3816f)
            )

            val upperBodyParts13 = root.addOrReplaceChild(
                "upperBodyParts13",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, -1.5737f, 0.3816f)
            )
            upperBodyParts13.addOrReplaceChild(
                "r13",
                CubeListBuilder.create().texOffs(52, 51)
                    .addBox(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(6.5757f, 0.0f, 6.7879f, 0.0f, -0.7854f, 0.0f)
            )

            val upperBodyParts8 = root.addOrReplaceChild(
                "upperBodyParts8",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 6.7763f, 0.3816f)
            )
            upperBodyParts8.addOrReplaceChild(
                "r8",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(4.0f, 0.0f, 4.0f, 0.0f, -0.7854f, 0.0f)
            )

            val upperBodyParts9 = root.addOrReplaceChild(
                "upperBodyParts9",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 6.7763f, 0.3816f)
            )
            upperBodyParts9.addOrReplaceChild(
                "r9",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-4.0f, 0.0f, -4.0f, 0.0f, -0.7854f, 0.0f)
            )

            val upperBodyParts10 = root.addOrReplaceChild(
                "upperBodyParts10",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 6.7763f, 0.3816f)
            )
            upperBodyParts10.addOrReplaceChild(
                "r10",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-4.0f, 0.0f, 4.0f, 0.0f, -0.7854f, 0.0f)
            )

            val upperBodyParts11 = root.addOrReplaceChild(
                "upperBodyParts11",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 6.7763f, 0.3816f)
            )
            upperBodyParts11.addOrReplaceChild(
                "r11",
                CubeListBuilder.create().texOffs(56, 60)
                    .addBox(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(4.0f, 0.0f, -4.0f, 0.0f, -0.7854f, 0.0f)
            )

            root.addOrReplaceChild(
                "L2",
                CubeListBuilder.create().texOffs(48, 40)
                    .addBox(-2.0f, -2.0f, -2.0f, 4.0f, 4.0f, 4.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 3.9263f, 0.3816f)
            )

            root.addOrReplaceChild(
                "L1",
                CubeListBuilder.create().texOffs(52, 51)
                    .addBox(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 10.2763f, 0.3816f)
            )

            root.addOrReplaceChild(
                "Head",
                CubeListBuilder.create().texOffs(18, 48)
                    .addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -5.2237f, 0.3816f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }

    override fun setupAnim(renderState: BrineRenderState) {
        super.setupAnim(renderState)

        this.idleAnimation.apply(renderState.idleAnimationState, renderState.ageInTicks)
        this.startAttackAnimation.apply(renderState.startAttackAnimationState, renderState.ageInTicks)
        this.stopAttackAnimation.apply(renderState.stopAttackAnimationState, renderState.ageInTicks)
        this.loopAttackAnimation.apply(renderState.loopAttackAnimationState, renderState.ageInTicks)
    }
}