package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

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

class BarnacleModel(modelPart: ModelPart) : EntityModel<BarnacleRenderState>(modelPart) {

    private val stillMouthCloseAnimation: KeyframeAnimation = BarnacleAnimation.still_mouth_close.bake(modelPart)
    private val stillMouthOpenAnimation: KeyframeAnimation = BarnacleAnimation.still_mouth_open.bake(modelPart)
    private val mouthOpenAnimation: KeyframeAnimation = BarnacleAnimation.mouth_open.bake(modelPart)
    private val mouthCloseAnimation: KeyframeAnimation = BarnacleAnimation.mouth_close.bake(modelPart)
    private val moveStillAnimation: KeyframeAnimation = BarnacleAnimation.move_still.bake(modelPart)
    private val moveRushAnimation: KeyframeAnimation = BarnacleAnimation.move_rush.bake(modelPart)
    private val fleeStillAnimation: KeyframeAnimation = BarnacleAnimation.flee_still.bake(modelPart)
    private val fleeRushAnimation: KeyframeAnimation = BarnacleAnimation.flee_rush.bake(modelPart)
    private val swallowAnimation: KeyframeAnimation = BarnacleAnimation.swallow.bake(modelPart)
    private val swallowStartAnimation: KeyframeAnimation = BarnacleAnimation.swallow_start.bake(modelPart)
    private val swallowStopAnimation: KeyframeAnimation = BarnacleAnimation.swallow_stop.bake(modelPart)

    companion object {
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(
            Identifier.fromNamespaceAndPath(
                SquAbyssalBloom.ID, "barnacle"
            ), "main"
        )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            val root = partdefinition.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0f, 17.925f, -0.9f, 0.0f, 3.1416f, 0.0f)
            )

            val tongue = root.addOrReplaceChild(
                "tongue",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -0.025f, -1.6f)
            )

            val tipOfTongue =
                tongue.addOrReplaceChild("tipOfTongue", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f))

            val tentacles =
                root.addOrReplaceChild("tentacles", CubeListBuilder.create(), PartPose.offset(0.0f, -0.025f, -10.9f))

            val fpos_r1 = tentacles.addOrReplaceChild(
                "fpos_r1",
                CubeListBuilder.create().texOffs(26, 25).mirror()
                    .addBox(-5.5f, -0.1f, 0.0f, 11.0f, 13.0f, 0.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -1.5708f, 0.0f, -0.7854f)
            )

            val fneg_r1 = tentacles.addOrReplaceChild(
                "fneg_r1",
                CubeListBuilder.create().texOffs(26, 25)
                    .addBox(-5.5f, -0.1f, 0.0f, 11.0f, 13.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -1.5708f, 0.0f, 0.7854f)
            )

            val head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 30)
                    .addBox(-4.0f, -4.0f, -5.0f, 8.0f, 8.0f, 10.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -0.025f, -5.9f)
            )

            val mouth = root.addOrReplaceChild("mouth", CubeListBuilder.create(), PartPose.offset(0.0f, 0.075f, -1.6f))

            val back = mouth.addOrReplaceChild(
                "back",
                CubeListBuilder.create().texOffs(40, 0)
                    .addBox(-5.0f, -5.0f, -0.5f, 10.0f, 10.0f, 1.0f, CubeDeformation(0.025f)),
                PartPose.offset(0.0f, 0.0f, 0.5f)
            )

            val topLeftMouthPart = mouth.addOrReplaceChild(
                "topLeftMouthPart",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(0.0f, -5.0f, -0.5f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.5f)
            )

            val bottomLeftMouthPart = mouth.addOrReplaceChild(
                "bottomLeftMouthPart",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 0.0f, 0.5f)
            )

            val bottomLeftMouthPart_r1 = bottomLeftMouthPart.addOrReplaceChild(
                "bottomLeftMouthPart_r1",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(0.0f, -5.0f, -0.5f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.5708f)
            )

            val bottomRightMouthPart = mouth.addOrReplaceChild(
                "bottomRightMouthPart",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 0.0f, 0.5f)
            )

            val bottomRightMouthPart_r1 = bottomRightMouthPart.addOrReplaceChild(
                "bottomRightMouthPart_r1",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                    .addBox(-5.0f, -5.0f, -0.5f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.5708f)
            )

            val topRightMouthPart = mouth.addOrReplaceChild(
                "topRightMouthPart",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                    .addBox(-5.0f, -5.0f, -0.5f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offset(0.0f, 0.0f, 0.5f)
            )

            val bone = partdefinition.addOrReplaceChild(
                "bone",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 17.925f, -0.9f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }

    override fun setupAnim(renderState: BarnacleRenderState) {
        super.setupAnim(renderState)


        this.stillMouthOpenAnimation.apply(renderState.stillMouthOpenAnimationState, renderState.ageInTicks)
        this.stillMouthCloseAnimation.apply(renderState.stillMouthCloseAnimationState, renderState.ageInTicks)
        this.mouthOpenAnimation.apply(renderState.openMouthAnimationState, renderState.ageInTicks)
        this.mouthCloseAnimation.apply(renderState.closeMouthAnimationState, renderState.ageInTicks)
        this.moveStillAnimation.apply(renderState.moveStillAnimationState, renderState.ageInTicks)
        this.moveRushAnimation.apply(renderState.moveRushAnimationState, renderState.ageInTicks)
        this.fleeStillAnimation.apply(renderState.fleeStillAnimationState, renderState.ageInTicks)
        this.fleeRushAnimation.apply(renderState.fleeRushAnimationState, renderState.ageInTicks)
        this.swallowAnimation.apply(renderState.swallowAnimationState, renderState.ageInTicks)
        this.swallowStartAnimation.apply(renderState.swallowStartAnimationState, renderState.ageInTicks)
        this.swallowStopAnimation.apply(renderState.swallowStopAnimationState, renderState.ageInTicks)
    }
}
