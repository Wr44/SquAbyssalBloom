package fr.heta__h.squ_abyssal_bloom.entity.client.barnacle

import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.ResourceLocation

class BarnacleModel(
    private val root: ModelPart,
    private val Langue: ModelPart,
    private val Loca: ModelPart?,
    private val Tentacules: ModelPart?,
    private val Tte: ModelPart?,
    private val Bouche: ModelPart,
    private val Arrire: ModelPart?,
    private val HG: ModelPart?,
    private val BG: ModelPart?,
    private val BD: ModelPart?,
    private val HD: ModelPart?
) {
    fun barnacle(root: ModelPart) {
        this.root = root.getChild("root")
        this.Langue = this.root.getChild("Langue")
        this.Loca = this.Langue.getChild("Loca")
        this.Tentacules = this.root.getChild("Tentacules")
        this.Tte = this.root.getChild("Tte")
        this.Bouche = this.root.getChild("Bouche")
        this.Arrire = this.Bouche.getChild("Arrire")
        this.HG = this.Bouche.getChild("HG")
        this.BG = this.Bouche.getChild("BG")
        this.BD = this.Bouche.getChild("BD")
        this.HD = this.Bouche.getChild("HD")
    }

    companion object {
        
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(ResourceLocation("modid", "barnacle"), "main")
        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.getRoot()

            val root = partdefinition.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.25f, 21.536f, 5.0715f, 0.0f, 3.1416f, 0.0f)
            )

            val Langue = root.addOrReplaceChild(
                "Langue",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-1.0f, -2.0f, -2.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(1.75f, -1.536f, 7.2285f)
            )

            val Loca = Langue.addOrReplaceChild("Loca", CubeListBuilder.create(), PartPose.offset(0.0f, -1.0f, -0.3f))

            val Tentacules = root.addOrReplaceChild(
                "Tentacules",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.75f, -2.292f, -10.3856f, 0.0f, 0.0f, 3.1416f)
            )

            val cube_r1 = Tentacules.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create().texOffs(26, 25)
                    .addBox(-10.0f, -13.0f, -1.0f, 11.0f, 13.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(4.0f, -2.7561f, -6.3859f, -1.6017f, -0.0308f, -0.7849f)
            )

            val cube_r2 = Tentacules.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create().texOffs(26, 25).mirror()
                    .addBox(-1.0f, -13.0f, -1.0f, 11.0f, 13.0f, 0.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offsetAndRotation(-4.0f, -2.7561f, -6.3859f, -1.6017f, 0.0308f, 0.7849f)
            )

            val Tte = root.addOrReplaceChild(
                "Tte",
                CubeListBuilder.create().texOffs(0, 30)
                    .addBox(-1.0f, -8.0f, -1.0f, 8.0f, 8.0f, 10.0f, CubeDeformation(0.0f)),
                PartPose.offset(-1.25f, 1.364f, -3.0715f)
            )

            val Bouche =
                root.addOrReplaceChild("Bouche", CubeListBuilder.create(), PartPose.offset(-2.25f, 2.464f, 6.2285f))

            val Arrire = Bouche.addOrReplaceChild(
                "Arrire",
                CubeListBuilder.create().texOffs(40, 0)
                    .addBox(-4.6364f, -5.4546f, -0.5f, 10.0f, 10.0f, 1.0f, CubeDeformation(0.0f)),
                PartPose.offset(3.6364f, -4.5454f, -0.5f)
            )

            val HG = Bouche.addOrReplaceChild(
                "HG",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(4.0f, -10.0f, -1.0f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )

            val BG = Bouche.addOrReplaceChild("BG", CubeListBuilder.create(), PartPose.offset(-1.0f, -1.0f, 0.0f))

            val BG_r1 = BG.addOrReplaceChild(
                "BG_r1",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-4.0f, 0.0f, -1.0f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(10.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.5708f)
            )

            val BD = Bouche.addOrReplaceChild("BD", CubeListBuilder.create(), PartPose.offset(9.0f, -9.0f, 0.0f))

            val BD_r1 = BD.addOrReplaceChild(
                "BD_r1",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                    .addBox(-9.0f, 0.0f, -1.0f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offsetAndRotation(-10.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.5708f)
            )

            val HD = Bouche.addOrReplaceChild(
                "HD",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                    .addBox(-10.0f, -1.0f, -1.0f, 5.0f, 5.0f, 20.0f, CubeDeformation(0.0f)).mirror(false),
                PartPose.offset(9.0f, -9.0f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }
}
