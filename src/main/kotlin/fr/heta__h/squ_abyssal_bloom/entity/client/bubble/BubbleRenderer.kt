package fr.heta__h.squ_abyssal_bloom.entity.client.bubble

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.FULL_BRIGHT_LIGHTMAP
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth.lerp
import net.minecraft.util.Mth.rotLerp
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class BubbleRenderer(context: EntityRendererProvider.Context) :
    EntityRenderer<BubbleProjectile, BubbleRenderState>(context) {

    private val modelStage1 = BubbleStage1Model(context.bakeLayer(BubbleStage1Model.LAYER_LOCATION))
    private val modelStage2 = BubbleStage2Model(context.bakeLayer(BubbleStage2Model.LAYER_LOCATION))
    private val modelStage3 = BubbleStage3Model(context.bakeLayer(BubbleStage3Model.LAYER_LOCATION))

    private val textureStage1 = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/entity/projectiles/bubble_stage1.png")
    private val textureStage2 = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/entity/projectiles/bubble_stage2.png")
    private val textureStage3 = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/entity/projectiles/bubble_stage3.png")

    private val textureStage1Desat = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/entity/projectiles/bubble_stage1_desaturated.png")
    private val textureStage2Desat = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/entity/projectiles/bubble_stage2_desaturated.png")
    private val textureStage3Desat = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/entity/projectiles/bubble_stage3_desaturated.png")

    override fun createRenderState(): BubbleRenderState = BubbleRenderState()

    override fun extractRenderState(
        entity: BubbleProjectile,
        state: BubbleRenderState,
        partialTicks: Float
    ) {
        super.extractRenderState(entity, state, partialTicks)
        state.packedLight = if (entity.isLuminescent) {
            FULL_BRIGHT_LIGHTMAP
        } else {
            getPackedLightCoords(entity, partialTicks)
        }
        state.bubbleStage = entity.bubbleStage
        state.ageInTicks = entity.tickCount + partialTicks
        state.effectColor = entity.effectColor
        if (entity.isHeld) {
            state.isHeld = true
            val controller = (entity.owner as? AbstractNautilus)?.controllingPassenger ?: entity.owner
            if (controller != null) state.heldYaw = rotLerp(partialTicks, controller.yRotO, controller.yRot)
        } else {
            state.isHeld = false
        }
        state.releaseYaw = -entity.releaseYaw
        state.ticksSinceRelease = if (entity.releaseTick >= 0) (entity.tickCount - entity.releaseTick) + partialTicks else -1f

        val playerId = entity.attachedPlayerId

        val player = if (playerId != -1) entity.level().getEntity(playerId) as? LivingEntity else null

        if (player != null) {
            val bx = lerp(partialTicks.toDouble(), entity.xOld, entity.x)
            val by = lerp(partialTicks.toDouble(), entity.yOld, entity.y)
            val bz = lerp(partialTicks.toDouble(), entity.zOld, entity.z)
            val px = lerp(partialTicks.toDouble(), player.xOld, player.x)
            val py = lerp(partialTicks.toDouble(), player.yOld, player.y) + player.bbHeight * 0.8
            val pz = lerp(partialTicks.toDouble(), player.zOld, player.z)

            val mc = Minecraft.getInstance()
            val camPos = mc.gameRenderer.mainCamera.position()

            val horizontalDist = sqrt((px - bx) * (px - bx) + (pz - bz) * (pz - bz))
            val verticalDist = abs(py - by)
            val epsilonX = if (horizontalDist < 0.05) (verticalDist * 0.03).coerceAtLeast(0.02) else 0.0

            val ls = EntityRenderState.LeashState().apply {
                offset = Vec3(0.0, entity.bbHeight * 0.5, 0.0)
                start = Vec3(bx - camPos.x, by + entity.bbHeight * 0.5 - camPos.y, bz - camPos.z)
                end = Vec3(px - camPos.x + epsilonX, py - camPos.y, pz - camPos.z)
                slack = false
                startSkyLight = 15
                endSkyLight = 15
                startBlockLight = 0
                endBlockLight = 0
            }

            state.attachedLeash = ls
            state.hasAttachedPlayer = true
        } else {
            state.attachedLeash = null
            state.hasAttachedPlayer = false
        }
    }

    override fun submit(
        renderState: BubbleRenderState,
        poseStack: PoseStack,
        nodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {

        renderState.attachedLeash?.let { ls ->
            nodeCollector.submitLeash(poseStack, ls)
        }

        poseStack.pushPose()

        if (renderState.isHeld) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.heldYaw))
        } else {
            val spinSpeed = when (renderState.bubbleStage) { 2 -> 1.2f; 1 -> 2.5f; else -> 4.5f }
            val spinAngle = (renderState.ageInTicks * spinSpeed) % 360f
            val wobble = sin((renderState.ageInTicks * 0.08f).toDouble()).toFloat() * 8f

            val finalYaw = if (renderState.ticksSinceRelease in 0f..15f) {
                val t = renderState.ticksSinceRelease / 15f
                val smooth = t * t * (3f - 2f * t)
                rotLerp(smooth, renderState.releaseYaw, spinAngle)
            } else {
                spinAngle
            }

            poseStack.mulPose(Axis.YP.rotationDegrees(finalYaw))
            poseStack.mulPose(Axis.XP.rotationDegrees(wobble))
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f))

        val scaleFactor = when (renderState.bubbleStage) { 2 -> 3f; 1 -> 2f; else -> 1.5f }
        poseStack.scale(scaleFactor, scaleFactor, scaleFactor)

        val activeModel = when (renderState.bubbleStage) {
            2 -> modelStage3
            1 -> modelStage2
            else -> modelStage1
        }

        val hasEffect = renderState.effectColor != 0

        val activeTexture = if (hasEffect) {
            when (renderState.bubbleStage) {
                2 -> textureStage3Desat
                1 -> textureStage2Desat
                else -> textureStage1Desat
            }
        } else {
            when (renderState.bubbleStage) {
                2 -> textureStage3
                1 -> textureStage2
                else -> textureStage1
            }
        }

        val tintColor = if (hasEffect) {
            val col = renderState.effectColor
            (0xFF shl 24) or (col and 0x00FFFFFF)
        } else {
            -1
        }

        val renderType = RenderTypes.entityCutout(activeTexture)

        nodeCollector.submitCustomGeometry(poseStack, renderType) { pose, vertexConsumer ->
            val tempStack = PoseStack()
            tempStack.last().pose().set(pose.pose())
            tempStack.last().normal().set(pose.normal())

            val parts = activeModel.root().allParts.toList()
            parts.forEach { part ->
                part.render(tempStack, vertexConsumer, renderState.packedLight, OverlayTexture.NO_OVERLAY, tintColor)
            }
        }

        super.submit(renderState, poseStack, nodeCollector, cameraRenderState)

        poseStack.popPose()
    }

}