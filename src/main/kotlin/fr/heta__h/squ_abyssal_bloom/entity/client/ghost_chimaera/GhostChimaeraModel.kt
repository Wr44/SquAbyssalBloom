package fr.heta__h.squ_abyssal_bloom.entity.client.ghost_chimaera

import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.Identifier

class GhostChimaeraModel(rootPart: ModelPart) : EntityModel<GhostChimaeraRenderState>(rootPart) {

    private val root = rootPart.getChild("root")
    private val body = root.getChild("Body")
    private val lower = body.getChild("Lower")
    private val startTail = body.getChild("StartTail")
    private val tail = startTail.getChild("Tail")
    private val lowerLower = startTail.getChild("LowerLower")
    private val headToros = root.getChild("HeadToros")
    private val nose = headToros.getChild("Nose")
    val eyes: ModelPart = root.getChild("Eyes")
    private val flipper = root.getChild("Flipper")

    companion object {
        val LAYER_LOCATION = ModelLayerLocation(Identifier.fromNamespaceAndPath("modid", "ghostchimaera"), "main")

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.getRoot()

            val root =
                partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0f, 19.0f, -11.0f))

            val Body = root.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.offset(-0.35f, 0.0f, 8.5f))

            val Lower = Body.addOrReplaceChild("Lower", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f))

            val LowerTexture = Lower.addOrReplaceChild(
                "LowerTexture",
                CubeListBuilder.create().texOffs(32, 110)
                    .addBox(0.0f, -1.0f, -4.0f, 0.0f, 2.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.35f, -3.05f, 4.5f)
            )

            val TailFinRight = Lower.addOrReplaceChild(
                "TailFinRight",
                CubeListBuilder.create().texOffs(119, 80).mirror()
                    .addBox(-1.5f, 0.0f, -1.5f, 3.0f, 0.0f, 3.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offsetAndRotation(-3.7916f, 3.6901f, 7.0f, 0.0f, 0.0f, -0.3054f)
            )

            val TailFinLeft = Lower.addOrReplaceChild(
                "TailFinLeft",
                CubeListBuilder.create().texOffs(119, 80)
                    .addBox(-1.5f, 0.0f, -1.5f, 3.0f, 0.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(4.48f, 3.7653f, 7.0f, 0.0f, 0.0f, 0.3054f)
            )

            val LowerFin = Lower.addOrReplaceChild(
                "LowerFin",
                CubeListBuilder.create().texOffs(20, 114)
                    .addBox(-0.5f, -0.5f, -4.0f, 1.0f, 1.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.35f, -2.5f, 4.5f)
            )

            val LowerSegement = Lower.addOrReplaceChild(
                "LowerSegement",
                CubeListBuilder.create().texOffs(96, 92)
                    .addBox(-3.5f, -3.0f, -4.5f, 7.0f, 6.0f, 9.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.35f, 0.5f, 4.0f)
            )

            val StartTail =
                Body.addOrReplaceChild("StartTail", CubeListBuilder.create(), PartPose.offset(0.35f, 0.6f, 8.35f))

            val Tail = StartTail.addOrReplaceChild("Tail", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 8.25f))

            val TailTexture = Tail.addOrReplaceChild(
                "TailTexture",
                CubeListBuilder.create().texOffs(40, 112)
                    .addBox(0.0f, -1.0f, -5.0f, 0.0f, 2.0f, 10.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, -1.4816f, 7.4408f, -0.192f, 0.0f, 0.0f)
            )

            val RisingFin = Tail.addOrReplaceChild(
                "RisingFin",
                CubeListBuilder.create().texOffs(121, 94)
                    .addBox(-0.5f, -0.5f, -1.0f, 1.0f, 1.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, -1.8251f, 1.618f, 0.7854f, 0.0f, 0.0f)
            )

            val DownFin = Tail.addOrReplaceChild(
                "DownFin",
                CubeListBuilder.create().texOffs(120, 110)
                    .addBox(-0.5f, -0.5f, -1.0f, 1.0f, 1.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, -1.882f, 0.2251f, -0.7854f, 0.0f, 0.0f)
            )

            val BackTailFin = Tail.addOrReplaceChild(
                "BackTailFin",
                CubeListBuilder.create().texOffs(37, 108)
                    .addBox(0.0f, -1.0f, -9.0f, 0.0f, 2.0f, 18.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.7137f, 8.6545f, 0.0436f, 0.0f, 0.0f)
            )

            val TailFin = Tail.addOrReplaceChild(
                "TailFin",
                CubeListBuilder.create().texOffs(0, 110)
                    .addBox(-0.5f, -1.0f, -8.0f, 1.0f, 2.0f, 16.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, -0.7999f, 9.8049f, -0.1309f, 0.0f, 0.0f)
            )

            val TailSegement = Tail.addOrReplaceChild(
                "TailSegement",
                CubeListBuilder.create().texOffs(98, 61)
                    .addBox(-1.5f, -1.5f, -6.0f, 3.0f, 3.0f, 12.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -0.1f, 0.9f)
            )

            val LowerLower =
                StartTail.addOrReplaceChild("LowerLower", CubeListBuilder.create(), PartPose.offset(0.1f, 0.1f, -0.05f))

            val LowerLowerTexture = LowerLower.addOrReplaceChild(
                "LowerLowerTexture",
                CubeListBuilder.create().texOffs(51, 110)
                    .addBox(0.0f, -1.0f, -4.0f, 0.0f, 2.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-0.1f, -3.0224f, 3.7592f, -0.192f, 0.0f, 0.0f)
            )

            val LowerLowerFin = LowerLower.addOrReplaceChild(
                "LowerLowerFin",
                CubeListBuilder.create().texOffs(64, 114)
                    .addBox(-0.5f, -0.5f, -4.0f, 1.0f, 1.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-0.1f, -2.8852f, 4.1474f, -0.0873f, 0.0f, 0.0f)
            )

            val LowerLowerSegement = LowerLower.addOrReplaceChild(
                "LowerLowerSegement",
                CubeListBuilder.create().texOffs(100, 77)
                    .addBox(-2.5f, -2.5f, -4.5f, 5.0f, 5.0f, 9.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.1f, -0.2f, 3.7f)
            )

            val HeadToros =
                root.addOrReplaceChild("HeadToros", CubeListBuilder.create(), PartPose.offset(-1.5f, 3.85f, -4.5f))

            val TorsoSegement = HeadToros.addOrReplaceChild(
                "TorsoSegement",
                CubeListBuilder.create().texOffs(86, 108)
                    .addBox(-7.0f, -6.0f, -1.0f, 8.0f, 7.0f, 13.0f, CubeDeformation(0.0f)),
                PartPose.offset(4.5f, -0.85f, 1.5f)
            )

            val LightFinsTextureBody = HeadToros.addOrReplaceChild(
                "LightFinsTextureBody",
                CubeListBuilder.create().texOffs(83, 90)
                    .addBox(-3.0f, -7.05f, 7.0f, 0.0f, 2.0f, 5.0f, CubeDeformation(0.0f)),
                PartPose.offset(4.5f, -0.85f, 1.5f)
            )

            val Nose =
                HeadToros.addOrReplaceChild("Nose", CubeListBuilder.create(), PartPose.offset(1.5f, -3.25f, -2.3f))

            val Nose2 = Nose.addOrReplaceChild(
                "Nose2",
                CubeListBuilder.create().texOffs(30, 96)
                    .addBox(-3.0f, -2.0f, -3.0f, 6.0f, 4.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.011f, -1.7329f, 0.1309f, 0.0f, 0.0f)
            )

            val RightTopTeeth = Nose.addOrReplaceChild(
                "RightTopTeeth",
                CubeListBuilder.create().texOffs(83, 87)
                    .addBox(0.0f, -1.0f, -2.0f, 0.0f, 2.0f, 4.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-2.55f, 2.4391f, -2.122f, 0.1309f, 0.0f, 0.0f)
            )

            val LeftTopTeeth = Nose.addOrReplaceChild(
                "LeftTopTeeth",
                CubeListBuilder.create().texOffs(83, 75)
                    .addBox(0.0f, -1.0f, -2.0f, 0.0f, 2.0f, 4.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(2.55f, 2.4391f, -2.122f, 0.1309f, 0.0f, 0.0f)
            )

            val FrontTopTeeth = Nose.addOrReplaceChild(
                "FrontTopTeeth",
                CubeListBuilder.create().texOffs(81, 81)
                    .addBox(0.0f, -1.0f, -2.5f, 0.0f, 2.0f, 5.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 2.578f, -4.1391f, 1.5708f, 1.4399f, 1.5708f)
            )

            val Jaw = HeadToros.addOrReplaceChild(
                "Jaw",
                CubeListBuilder.create().texOffs(1, 98)
                    .addBox(-2.5f, -2.4f, -5.3f, 5.0f, 3.0f, 7.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(1.5f, 0.0f, -0.3f, 0.1745f, 0.0f, 0.0f)
            )

            val Forehead = HeadToros.addOrReplaceChild(
                "Forehead",
                CubeListBuilder.create().texOffs(57, 94)
                    .addBox(-3.5f, -2.0f, -2.0f, 7.0f, 4.0f, 4.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(1.5f, -4.25f, -0.5f, 0.384f, 0.0f, 0.0f)
            )

            val BodyFIn = HeadToros.addOrReplaceChild(
                "BodyFIn",
                CubeListBuilder.create().texOffs(81, 101)
                    .addBox(-0.5f, -3.5f, -1.5f, 1.0f, 7.0f, 3.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(1.5f, -8.0205f, 5.0671f, -0.829f, 0.0f, 0.0f)
            )

            val Eyes = root.addOrReplaceChild(
                "Eyes",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.75f, -0.4f, -5.0f, 0.384f, 0.0f, 0.0f)
            )

            val RightEye = Eyes.addOrReplaceChild(
                "RightEye",
                CubeListBuilder.create().texOffs(97, 96)
                    .addBox(-0.5f, -0.5f, -1.0f, 1.0f, 1.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-6.0f, -0.5f, 0.0f)
            )

            val LeftEye = Eyes.addOrReplaceChild(
                "LeftEye",
                CubeListBuilder.create().texOffs(97, 96)
                    .addBox(-0.5f, -0.5f, -1.0f, 1.0f, 1.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.5f, -0.5f, 0.0f)
            )

            val Flipper =
                root.addOrReplaceChild("Flipper", CubeListBuilder.create(), PartPose.offset(4.0f, 3.0f, -1.0f))

            val ConnexionLeft = Flipper.addOrReplaceChild(
                "ConnexionLeft",
                CubeListBuilder.create().texOffs(120, 116)
                    .addBox(-1.0f, -0.5f, -1.0f, 2.0f, 1.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -0.5f, 0.0f)
            )

            val FlipperLeft = Flipper.addOrReplaceChild(
                "FlipperLeft",
                CubeListBuilder.create().texOffs(110, 48)
                    .addBox(0.4f, -2.9f, -1.05f, 1.0f, 3.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.3054f, 0.0f)
            )

            val ConnexionRight = Flipper.addOrReplaceChild(
                "ConnexionRight",
                CubeListBuilder.create().texOffs(120, 116).mirror()
                    .addBox(-1.0f, -0.5f, -1.0f, 2.0f, 1.0f, 2.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offset(-8.0f, -0.5f, 0.0f)
            )

            val FlipperRight = Flipper.addOrReplaceChild(
                "FlipperRight",
                CubeListBuilder.create().texOffs(110, 48).mirror()
                    .addBox(-0.65f, -2.9f, -1.05f, 1.0f, 3.0f, 8.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offsetAndRotation(-8.75f, 0.0f, 0.0f, 0.0f, -0.3054f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 128, 128)
        }
    }
}