package fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.*
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth

class NautilusLampModel(modelPart: ModelPart) : EntityModel<EntityRenderState>(modelPart) {

    private val root = modelPart.getChild("root")
    private val border = root.getChild("border")

    private val right = border.getChild("right")
    private val left = border.getChild("left")
    private val lower = border.getChild("lower")
    private val front = border.getChild("front")

    private val lleft2 = root.getChild("lleft2")
    private val lantern_left3 = lleft2.getChild("lantern_left3")
    private val connexion4 = lantern_left3.getChild("connexion4")
    private val principal4 = lantern_left3.getChild("principal4")
    private val upper4 = principal4.getChild("upper4")
    private val base4 = principal4.getChild("base4")

    private val lantern_left2 = lleft2.getChild("lantern_left2")
    private val connexion5 = lantern_left2.getChild("connexion5")
    private val principal5 = lantern_left2.getChild("principal5")
    private val upper5 = principal5.getChild("upper5")
    private val base5 = principal5.getChild("base5")

    private val lantern_left1 = lleft2.getChild("lantern_left1")
    private val connexion6 = lantern_left1.getChild("connexion6")
    private val principal6 = lantern_left1.getChild("principal6")
    private val upper6 = principal6.getChild("upper6")
    private val base6 = principal6.getChild("base6")

    private val lright = root.getChild("lright")
    private val lantern_right3 = lright.getChild("lantern_right3")
    private val connexion3 = lantern_right3.getChild("connexion3")
    private val principal3 = lantern_right3.getChild("principal3")
    private val upper3 = principal3.getChild("upper3")
    private val base3 = principal3.getChild("base3")

    private val lantern_right2 = lright.getChild("lantern_right2")
    private val connexion2 = lantern_right2.getChild("connexion2")
    private val principal2 = lantern_right2.getChild("principal2")
    private val upper2 = principal2.getChild("upper2")
    private val base2 = principal2.getChild("base2")

    private val lantern_right1 = lright.getChild("lantern_right1")
    private val connexion = lantern_right1.getChild("connexion")
    private val principal1 = lantern_right1.getChild("principal1")
    private val upper = principal1.getChild("upper")
    private val base = principal1.getChild("base")


    companion object {
        val LAYER_LOCATION: ModelLayerLocation =
            ModelLayerLocation(Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "nautilus_lamp"), "main")

        private const val SWAY_AMPLITUDE = 0.0873f // ~5 degrees
        private const val SWAY_SPEED = 0.1f
        private val ARM_PHASE_STEP = (Math.PI / 3.0).toFloat()
        private val PRINCIPAL_PHASE_OFFSET = (Math.PI / 2.0).toFloat()
        private val OPPOSITE_SIDE_PHASE = Math.PI.toFloat()

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition: MeshDefinition = MeshDefinition()
            val partdefinition: PartDefinition = meshdefinition.root

            val root: PartDefinition = partdefinition.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offset(0.0667f, 14.9f, 0.3333f)
            )

            val border: PartDefinition =
                root.addOrReplaceChild("border", CubeListBuilder.create(), PartPose.offset(-0.0667f, 9.1f, -0.3333f))

            val right: PartDefinition? = border.addOrReplaceChild(
                "right",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-15.0f, -2.0f, -3.0f, 2.0f, 2.0f, 18.0f, CubeDeformation(0.0f)),
                PartPose.offset(7.0f, -8.0f, -6.0f)
            )

            val left: PartDefinition? = border.addOrReplaceChild(
                "left",
                CubeListBuilder.create().texOffs(0, 20)
                    .addBox(13.0f, -2.0f, 14.0f, 2.0f, 2.0f, 18.0f, CubeDeformation(0.0f)),
                PartPose.offset(-7.0f, -8.0f, -23.0f)
            )

            val lower: PartDefinition? = border.addOrReplaceChild(
                "lower",
                CubeListBuilder.create().texOffs(0, 40)
                    .addBox(1.0f, -2.0f, 14.0f, 12.0f, 2.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(-7.0f, -8.0f, -6.0f)
            )

            val front: PartDefinition? = border.addOrReplaceChild(
                "front",
                CubeListBuilder.create().texOffs(38, 0)
                    .addBox(1.0f, -2.0f, 14.0f, 12.0f, 2.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(-7.0f, -8.0f, -23.0f)
            )

            val lleft2: PartDefinition = root.addOrReplaceChild(
                "lleft2",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(9.8333f, 1.6f, -1.3333f, 0.0f, 3.1416f, 0.0f)
            )

            val lantern_left3: PartDefinition =
                lleft2.addOrReplaceChild("lantern_left3", CubeListBuilder.create(), PartPose.offset(1.9f, -1.5f, 4.0f))

            val connexion4: PartDefinition? = lantern_left3.addOrReplaceChild(
                "connexion4",
                CubeListBuilder.create().texOffs(0, 43)
                    .addBox(-1.0f, -2.0f, -1.0f, 2.0f, 2.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(-1.0f, 1.0f, 1.0f)
            )

            val principal4: PartDefinition = lantern_left3.addOrReplaceChild(
                "principal4",
                CubeListBuilder.create(),
                PartPose.offset(-1.5f, 1.0f, 0.0f)
            )

            val upper4: PartDefinition? = principal4.addOrReplaceChild(
                "upper4",
                CubeListBuilder.create().texOffs(42, 40)
                    .addBox(0.0f, -2.0f, 0.0f, 1.0f, 3.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.5f, 2.0f, -0.5f)
            )

            val base4: PartDefinition? = principal4.addOrReplaceChild(
                "base4",
                CubeListBuilder.create().texOffs(40, 3)
                    .addBox(2.0f, 0.0f, 0.0f, 2.0f, 3.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-3.0f, 0.5f, -1.0f)
            )

            val lantern_left2: PartDefinition =
                lleft2.addOrReplaceChild("lantern_left2", CubeListBuilder.create(), PartPose.offset(2.05f, -1.5f, 0.0f))

            val connexion5: PartDefinition? = lantern_left2.addOrReplaceChild(
                "connexion5",
                CubeListBuilder.create().texOffs(0, 43)
                    .addBox(-1.0f, -2.0f, -1.0f, 2.0f, 2.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(-1.15f, 1.0f, 1.0f)
            )

            val principal5: PartDefinition = lantern_left2.addOrReplaceChild(
                "principal5",
                CubeListBuilder.create(),
                PartPose.offset(-1.65f, 1.0f, 0.0f)
            )

            val upper5: PartDefinition? = principal5.addOrReplaceChild(
                "upper5",
                CubeListBuilder.create().texOffs(42, 40)
                    .addBox(-17.5f, 1.0f, -0.5f, 1.0f, 3.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(17.0f, -1.0f, 0.0f)
            )

            val base5: PartDefinition? = principal5.addOrReplaceChild(
                "base5",
                CubeListBuilder.create().texOffs(40, 3)
                    .addBox(-18.0f, 1.5f, -1.0f, 2.0f, 3.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(17.0f, -1.0f, 0.0f)
            )

            val lantern_left1: PartDefinition = lleft2.addOrReplaceChild(
                "lantern_left1",
                CubeListBuilder.create(),
                PartPose.offset(1.875f, -1.375f, -4.025f)
            )

            val connexion6: PartDefinition? = lantern_left1.addOrReplaceChild(
                "connexion6",
                CubeListBuilder.create().texOffs(0, 43)
                    .addBox(-1.0f, -2.0f, -1.0f, 2.0f, 2.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.975f, 0.875f, 1.025f)
            )

            val principal6: PartDefinition = lantern_left1.addOrReplaceChild(
                "principal6",
                CubeListBuilder.create(),
                PartPose.offset(-1.525f, 0.925f, -0.025f)
            )

            val upper6: PartDefinition? = principal6.addOrReplaceChild(
                "upper6",
                CubeListBuilder.create().texOffs(42, 40)
                    .addBox(0.0f, -2.0f, 0.0f, 1.0f, 3.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.45f, 1.95f, -0.45f)
            )

            val base6: PartDefinition? = principal6.addOrReplaceChild(
                "base6",
                CubeListBuilder.create().texOffs(40, 3)
                    .addBox(2.0f, 0.0f, 0.0f, 2.0f, 3.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-2.95f, 0.45f, -0.95f)
            )

            val lright: PartDefinition =
                root.addOrReplaceChild("lright", CubeListBuilder.create(), PartPose.offset(-9.7667f, 1.6f, -1.3333f))

            val lantern_right3: PartDefinition = lright.addOrReplaceChild(
                "lantern_right3",
                CubeListBuilder.create(),
                PartPose.offset(1.9f, -1.6667f, 4.0833f)
            )

            val connexion3: PartDefinition? = lantern_right3.addOrReplaceChild(
                "connexion3",
                CubeListBuilder.create().texOffs(0, 43)
                    .addBox(-18.6f, -2.0f, -1.0f, 2.0f, 2.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(16.6f, 1.1667f, 0.9167f)
            )

            val principal3: PartDefinition = lantern_right3.addOrReplaceChild(
                "principal3",
                CubeListBuilder.create(),
                PartPose.offset(-1.4f, 1.1667f, -0.0833f)
            )

            val upper3: PartDefinition? = principal3.addOrReplaceChild(
                "upper3",
                CubeListBuilder.create().texOffs(42, 40)
                    .addBox(-19.6f, -2.0f, 0.0f, 1.0f, 3.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(19.0f, 2.0f, -0.5f)
            )

            val base3: PartDefinition? = principal3.addOrReplaceChild(
                "base3",
                CubeListBuilder.create().texOffs(40, 3)
                    .addBox(-22.6f, 0.0f, 0.0f, 2.0f, 3.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(21.5f, 0.5f, -1.0f)
            )

            val lantern_right2: PartDefinition =
                lright.addOrReplaceChild("lantern_right2", CubeListBuilder.create(), PartPose.offset(1.9f, -1.5f, 0.0f))

            val connexion2: PartDefinition? = lantern_right2.addOrReplaceChild(
                "connexion2",
                CubeListBuilder.create().texOffs(0, 43)
                    .addBox(-18.6f, -2.0f, -1.0f, 2.0f, 2.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(16.6f, 1.0f, 1.0f)
            )

            val principal2: PartDefinition = lantern_right2.addOrReplaceChild(
                "principal2",
                CubeListBuilder.create(),
                PartPose.offset(-1.7f, 1.0f, 0.0f)
            )

            val upper2: PartDefinition? = principal2.addOrReplaceChild(
                "upper2",
                CubeListBuilder.create().texOffs(42, 40)
                    .addBox(-2.1f, 1.0f, -0.5f, 1.0f, 3.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(1.8f, -1.0f, 0.0f)
            )

            val base2: PartDefinition? = principal2.addOrReplaceChild(
                "base2",
                CubeListBuilder.create().texOffs(40, 3)
                    .addBox(-2.6f, 1.5f, -1.0f, 2.0f, 3.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(1.8f, -1.0f, 0.0f)
            )

            val lantern_right1: PartDefinition = lright.addOrReplaceChild(
                "lantern_right1",
                CubeListBuilder.create(),
                PartPose.offset(1.9f, -1.5f, -4.0f)
            )

            val connexion: PartDefinition? = lantern_right1.addOrReplaceChild(
                "connexion",
                CubeListBuilder.create().texOffs(0, 43)
                    .addBox(-18.6f, -2.0f, -1.0f, 2.0f, 2.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(16.6f, 1.0f, 1.0f)
            )

            val principal1: PartDefinition = lantern_right1.addOrReplaceChild(
                "principal1",
                CubeListBuilder.create(),
                PartPose.offset(-1.55f, 1.05f, -0.05f)
            )

            val upper: PartDefinition? = principal1.addOrReplaceChild(
                "upper",
                CubeListBuilder.create().texOffs(42, 40)
                    .addBox(-19.6f, -2.0f, 0.0f, 1.0f, 3.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(19.15f, 1.95f, -0.45f)
            )

            val base: PartDefinition? = principal1.addOrReplaceChild(
                "base",
                CubeListBuilder.create().texOffs(40, 3)
                    .addBox(-22.6f, 0.0f, 0.0f, 2.0f, 3.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(21.65f, 0.45f, -0.95f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }

    }

    private fun sway(time: Float, phase: Float): Float =
        SWAY_AMPLITUDE * Mth.sin((SWAY_SPEED * time + phase).toDouble())

    override fun setupAnim(state: EntityRenderState) {
        super.setupAnim(state)

        val time = state.ageInTicks

        lantern_right1.zRot = sway(time, 0f)
        lantern_right2.zRot = sway(time, ARM_PHASE_STEP)
        lantern_right3.zRot = sway(time, 2f * ARM_PHASE_STEP)

        lantern_left1.zRot = sway(time, OPPOSITE_SIDE_PHASE)
        lantern_left2.zRot = sway(time, OPPOSITE_SIDE_PHASE + ARM_PHASE_STEP)
        lantern_left3.zRot = sway(time, OPPOSITE_SIDE_PHASE + 2f * ARM_PHASE_STEP)

        principal1.zRot = sway(time, PRINCIPAL_PHASE_OFFSET)
        principal2.zRot = sway(time, ARM_PHASE_STEP + PRINCIPAL_PHASE_OFFSET)
        principal3.zRot = sway(time, 2f * ARM_PHASE_STEP + PRINCIPAL_PHASE_OFFSET)

        principal4.zRot = sway(time, OPPOSITE_SIDE_PHASE + PRINCIPAL_PHASE_OFFSET)
        principal5.zRot = sway(time, OPPOSITE_SIDE_PHASE + ARM_PHASE_STEP + PRINCIPAL_PHASE_OFFSET)
        principal6.zRot = sway(time, OPPOSITE_SIDE_PHASE + 2f * ARM_PHASE_STEP + PRINCIPAL_PHASE_OFFSET)
    }
}