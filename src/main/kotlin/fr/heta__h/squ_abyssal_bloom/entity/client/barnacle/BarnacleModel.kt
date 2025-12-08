package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

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
import net.minecraft.client.model.geom.builders.PartDefinition
import net.minecraft.resources.ResourceLocation

class BarnacleModel(modelPart: ModelPart) : EntityModel<BarnacleRenderState>(modelPart) {

    private val root: ModelPart = modelPart.getChild("root")
    private val langue: ModelPart = root.getChild("langue")
    private val loca: ModelPart = langue.getChild("loca")
    private val tentacules: ModelPart = root.getChild("tenatcules")
    private val tete: ModelPart = root.getChild("tete")
    private val bouche: ModelPart = root.getChild("bouche")
    private val arriere: ModelPart = bouche.getChild("arriere")
    private val hg: ModelPart = bouche.getChild("hd")
    private val bg: ModelPart = bouche.getChild("bd")
    private val bd: ModelPart = bouche.getChild("bd")
    private val hd: ModelPart = bouche.getChild("hd")

    private val stillMouthCloseAnimation : KeyframeAnimation = BarnacleAnimation.still_mouth_close.get().bake(modelPart)
    private val stillMouthOpenAnimation : KeyframeAnimation = BarnacleAnimation.still_mouth_open.get().bake(modelPart)
    private val mouthOpenAnimation : KeyframeAnimation = BarnacleAnimation.mouth_open.get().bake(modelPart)
    private val mouthCloseAnimation : KeyframeAnimation = BarnacleAnimation.mouth_close.get().bake(modelPart)
    private val moveStillAnimation : KeyframeAnimation = BarnacleAnimation.move_still.get().bake(modelPart)
    private val moveRushAnimation : KeyframeAnimation = BarnacleAnimation.move_rush.get().bake(modelPart)
    private val fleeStillAnimation : KeyframeAnimation = BarnacleAnimation.flee_still.get().bake(modelPart)
    private val fleeRushAnimation : KeyframeAnimation = BarnacleAnimation.flee_rush.get().bake(modelPart)
    private val swallowAnimation : KeyframeAnimation = BarnacleAnimation.swallow.get().bake(modelPart)
    private val swallowStartAnimation : KeyframeAnimation = BarnacleAnimation.swallow_start.get().bake(modelPart)
    private val swallowStopAnimation : KeyframeAnimation = BarnacleAnimation.swallow_stop.get().bake(modelPart)

    companion object {
        
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            Squ_abyssal_bloom.ID, "barnacle"), "main")

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition: MeshDefinition = MeshDefinition()
            val partdefinition: PartDefinition = meshdefinition.root

            val root: PartDefinition = partdefinition.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.75f, 21.536f, 5.0715f, 0.0f, 3.1416f, 0.0f)
            )

            val langue: PartDefinition = root.addOrReplaceChild(
                "langue",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-1.0f, -2.0f, -2.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(1.75f, -1.536f, 7.2285f)
            )

            val loca: PartDefinition? =
                langue.addOrReplaceChild("loca", CubeListBuilder.create(), PartPose.offset(0.0f, -1.0f, -0.3f))

            val tenatcules: PartDefinition = root.addOrReplaceChild(
                "tenatcules",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.75f, -2.292f, -10.3856f, 0.0f, 0.0f, 3.1416f)
            )

            val fpos_r1: PartDefinition? = tenatcules.addOrReplaceChild(
                "fpos_r1",
                CubeListBuilder.create().texOffs(26, 25)
                    .addBox(-10.0f, -13.0f, -1.0f, 11.0f, 13.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(4.0f, -2.7561f, -6.3859f, -1.6017f, -0.0308f, -0.7849f)
            )

            val fneg_r1: PartDefinition? = tenatcules.addOrReplaceChild(
                "fneg_r1",
                CubeListBuilder.create().texOffs(26, 25).mirror()
                    .addBox(-1.0f, -13.0f, -1.0f, 11.0f, 13.0f, 0.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offsetAndRotation(-4.0f, -2.7561f, -6.3859f, -1.6017f, 0.0308f, 0.7849f)
            )

            val tete: PartDefinition? = root.addOrReplaceChild(
                "tete",
                CubeListBuilder.create().texOffs(0, 30)
                    .addBox(-1.0f, -8.0f, -1.0f, 8.0f, 8.0f, 10.0f, CubeDeformation(0.0f)),
                PartPose.offset(-1.25f, 1.364f, -3.0715f)
            )

            val bouche: PartDefinition =
                root.addOrReplaceChild("bouche", CubeListBuilder.create(), PartPose.offset(-2.25f, 2.464f, 6.2285f))

            val arriere: PartDefinition? = bouche.addOrReplaceChild(
                "arriere",
                CubeListBuilder.create().texOffs(40, 0)
                    .addBox(-4.6364f, -5.4546f, -0.5f, 10.0f, 10.0f, 1.0f, CubeDeformation(0.0f))
                    .texOffs(40, 0).addBox(-4.6364f, -5.4546f, -0.5f, 10.0f, 10.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(3.6364f, -4.5454f, -0.5f)
            )

            val hg: PartDefinition? = bouche.addOrReplaceChild(
                "hg",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(4.0f, -10.0f, -1.0f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )

            val bg: PartDefinition =
                bouche.addOrReplaceChild("bg", CubeListBuilder.create(), PartPose.offset(-1.0f, -1.0f, 0.0f))

            val bg_r1: PartDefinition? = bg.addOrReplaceChild(
                "bg_r1",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-4.0f, 0.0f, -1.0f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(10.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.5708f)
            )

            val bd: PartDefinition =
                bouche.addOrReplaceChild("bd", CubeListBuilder.create(), PartPose.offset(9.0f, -9.0f, 0.0f))

            val bd_r1: PartDefinition? = bd.addOrReplaceChild(
                "bd_r1",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                    .addBox(-9.0f, 0.0f, -1.0f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offsetAndRotation(-10.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.5708f)
            )

            val hd: PartDefinition? = bouche.addOrReplaceChild(
                "hd",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                    .addBox(-10.0f, -1.0f, -1.0f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offset(9.0f, -9.0f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }

    override fun setupAnim(renderState: BarnacleRenderState) {
        super.setupAnim(renderState)

        stillMouthCloseAnimation.apply(renderState.stillMouthCloseAnimationState, renderState.ageInTicks)

        
    }
}
