package fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.defense.RedSlobbererDefenseState
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Pose
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class RedSlobbererRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<RedSlobbererEntity, RedSlobbererRenderState, EntityModel<RedSlobbererRenderState>>(
        context,
        RedSlobbererModel(context.bakeLayer(RedSlobbererModel.LAYER_LOCATION)),
        0.5f
    ) {

    private val adultModel = this.model
    private val babyModel = BabyRedSlobbererModel(context.bakeLayer(BabyRedSlobbererModel.LAYER_LOCATION))

    companion object {
        private val TEXTURE_ADULT = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/red_slobberer/red_slobberer.png"
        )
        private val TEXTURE_BABY = Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "textures/entity/red_slobberer/red_slobberer_baby.png"
        )
    }

    override fun submit(
        renderState: RedSlobbererRenderState,
        poseStack: PoseStack,
        nodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        this.model = if (renderState.isBaby) babyModel else adultModel
        this.shadowRadius = if (renderState.isBaby) 0.25f else 1f
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState)
    }

    override fun scale(state: RedSlobbererRenderState, poseStack: PoseStack) {
        if (!state.isBaby) poseStack.scale(2f, 2f, 2f)
    }

    override fun createRenderState(): RedSlobbererRenderState = RedSlobbererRenderState()

    override fun getTextureLocation(state: RedSlobbererRenderState): Identifier {
        return if (state.isBaby) TEXTURE_BABY else TEXTURE_ADULT
    }

    override fun setupRotations(
        state: RedSlobbererRenderState,
        poseStack: PoseStack,
        rotationYaw: Float,
        partialTicks: Float
    ) {
        super.setupRotations(state, poseStack, rotationYaw, partialTicks)

        if (
            state.deathTime > 0.0f ||
            state.isUpsideDown ||
            state.isAutoSpinAttack ||
            state.hasPose(Pose.SLEEPING)
        ) {
            return
        }
        poseStack.translate(0.0, -state.stepRenderOffset, 0.0)
        poseStack.mulPose(Axis.XP.rotationDegrees(state.terrainPitch))
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.terrainRoll))
    }

    override fun extractRenderState(
        entity: RedSlobbererEntity,
        state: RedSlobbererRenderState,
        partialTicks: Float
    ) {
        super.extractRenderState(entity, state, partialTicks)

        state.isBaby = entity.isBaby
        state.stepRenderOffset = entity.getStepRenderOffset(partialTicks)

        val terrainNormal = entity.getTerrainNormal(partialTicks)
        val renderYawRadians = (180.0 - state.bodyRot) * PI / 180.0
        val yawCos = cos(renderYawRadians)
        val yawSin = sin(renderYawRadians)
        val localNormalX = yawCos * terrainNormal.x - yawSin * terrainNormal.z
        val localNormalZ = yawSin * terrainNormal.x + yawCos * terrainNormal.z
        state.terrainPitch = (atan2(localNormalZ, terrainNormal.y) * 180.0 / PI).toFloat()
        state.terrainRoll = (-asin(localNormalX.coerceIn(-1.0, 1.0)) * 180.0 / PI).toFloat()

        state.defenseState = if (entity.isBaby) {
            RedSlobbererDefenseState.NORMAL
        } else {
            entity.defenseState
        }
        state.defensePhaseElapsedTicks = if (entity.isBaby) {
            0.0f
        } else {
            entity.getDefensePhaseElapsedTicks(partialTicks)
        }

        state.idleAnimationState.copyFrom(entity.idleAnimationState)
        state.moveAnimationState.copyFrom(entity.moveAnimationState)
        state.swingingAnimationState.copyFrom(entity.swingingAnimationState)
        state.eyesAnimationState.copyFrom(entity.eyesAnimationState)
    }
}
