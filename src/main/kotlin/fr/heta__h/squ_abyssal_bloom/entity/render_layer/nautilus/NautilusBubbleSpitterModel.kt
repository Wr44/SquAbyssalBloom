package fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.*
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.AnimationState

class NautilusBubbleSpitterModel(modelPart: ModelPart) : EntityModel<EntityRenderState>(modelPart) {

    private val bubble = modelPart.getChild("bubble")
    private val border = modelPart.getChild("border")
    private val droit  = modelPart.getChild("droit")
    private val gauche = modelPart.getChild("gauche")

    companion object {
        val LAYER_LOCATION: ModelLayerLocation =
            ModelLayerLocation(
                Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "nautilus_bubble_spitter"),
                "main"
            )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            partdefinition.addOrReplaceChild(
                "bubble",
                CubeListBuilder.create().texOffs(3, 54)
                    .addBox(-2.0f, -9.0f, 0.0f, 7.0f, 8.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(-1.25f, 20.0f, -10.0f)
            )

            val border = partdefinition.addOrReplaceChild(
                "border",
                CubeListBuilder.create()
                    .texOffs(55, 42).addBox(-5.25f, -5.0f, -0.75f, 2.0f, 8.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(55, 42).addBox( 3.75f, -5.0f, -0.75f, 2.0f, 8.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(23, 48).addBox(-5.0f,  -8.0f,  0.2f, 11.0f, 14.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 16.0f, -10.25f)
            )

            border.addOrReplaceChild(
                "l_r1",
                CubeListBuilder.create().texOffs(55, 31)
                    .addBox(-1.0f, -2.0f, -1.0f, 2.0f, 7.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-1.25f, 4.0f, 0.25f, 0.0f, 0.0f, -1.5708f)
            )

            border.addOrReplaceChild(
                "u_r1",
                CubeListBuilder.create().texOffs(55, 31)
                    .addBox(-1.0f, -2.0f, -1.0f, 2.0f, 7.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-1.25f, -6.0f, 0.25f, 0.0f, 0.0f, -1.5708f)
            )

            val droit = partdefinition.addOrReplaceChild(
                "droit",
                CubeListBuilder.create()
                    .texOffs(53, 27).addBox(-2.6667f, -0.5f, -10.0f, 5.0f, 1.0f, 0.0f, CubeDeformation(0.0f))
                    .texOffs(53, 27).addBox(-2.6667f, -0.5f,   0.0f, 5.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(5.8667f, 15.0f, -10.0f, 0.0f, 3.1416f, 0.0f)
            )

            droit.addOrReplaceChild(
                "middled_r1",
                CubeListBuilder.create().texOffs(43, 25)
                    .addBox(-9.0f, -1.0f, 1.0f, 10.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-3.6667f, 0.5f, -9.0f, 0.0f, 1.5708f, 0.0f)
            )

            val gauche = partdefinition.addOrReplaceChild(
                "gauche",
                CubeListBuilder.create()
                    .texOffs(53, 27).addBox(-2.6667f, -0.5f,  0.0f, 5.0f, 1.0f, 0.0f, CubeDeformation(0.0f))
                    .texOffs(53, 27).addBox(-2.6667f, -0.5f, 10.0f, 5.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(-5.3333f, 15.0f, -10.0f)
            )

            gauche.addOrReplaceChild(
                "finishg_r1",
                CubeListBuilder.create().texOffs(43, 25)
                    .addBox(-9.0f, -1.0f, 1.0f, 10.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-3.6667f, 0.5f, 1.0f, 0.0f, 1.5708f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }

}