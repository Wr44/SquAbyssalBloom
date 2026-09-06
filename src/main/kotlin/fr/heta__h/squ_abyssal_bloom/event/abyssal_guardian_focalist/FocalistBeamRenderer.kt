package fr.heta__h.squ_abyssal_bloom.event.abyssal_guardian_focalist

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.abyssal_guardian_focalist.GuardianBeamDynamicLightCompat
import fr.heta__h.squ_abyssal_bloom.item.abyssal_guardian_focalist.AbyssalGuardianFocalistItem
import fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist.ClientBeamData
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.event.level.LevelEvent
import kotlin.math.acos
import kotlin.math.atan2

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object FocalistBeamRenderer {

    private val BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian/guardian_beam.png")

    private val playersShootingLastFrame = mutableSetOf<Int>()
    private val lastBubbleTickByPlayer = mutableMapOf<Int, Long>()

    private fun reset() {
        if (ModCompat.hasDynLights) GuardianBeamDynamicLightCompat.clearBeamLights()
        playersShootingLastFrame.clear()
        lastBubbleTickByPlayer.clear()
        ClientBeamData.clear()
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level is ClientLevel) reset()
    }

    @SubscribeEvent
    fun onRenderLevel(event: RenderLevelStageEvent.AfterOpaqueFeatures) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return

        val currentlyShooting = mutableSetOf<Int>()

        for (player in level.players()) {

            if (!player.isUsingItem || player.useItem.item !is AbyssalGuardianFocalistItem) continue

            val targetId = ClientBeamData.activeBeams[player.id] ?: continue
            val target = level.getEntity(targetId) as? LivingEntity ?: continue

            currentlyShooting.add(player.id)

            val poseStack = event.poseStack
            val partialTick = mc.deltaTracker.gameTimeDeltaTicks
            val cameraPos = mc.gameRenderer.mainCamera.position()

            val startPos = player.getEyePosition(partialTick).subtract(0.0, 0.2, 0.0)
            val endPos = target.getPosition(partialTick).add(0.0, target.bbHeight * 0.5, 0.0)

            var beamVector = endPos.subtract(startPos)
            val distance = beamVector.length().toFloat()
            if (distance == 0f) continue

            val attackTime = player.ticksUsingItem + partialTick
            val scale = attackTime / AbyssalGuardianFocalistItem.MAX_CHARGE_TICKS.toFloat()
            val animationTime = attackTime * 0.5f

            poseStack.pushPose()

            poseStack.translate(startPos.x - cameraPos.x, startPos.y - cameraPos.y, startPos.z - cameraPos.z)

            val f = distance + 1.0f
            beamVector = beamVector.normalize()

            if (lastBubbleTickByPlayer.put(player.id, level.gameTime) != level.gameTime) {
                spawnBeamBubbles(level, startPos, beamVector, distance.toDouble(), scale.toDouble())
            }

            val f1 = acos(beamVector.y).toFloat()
            val f2 = (Math.PI / 2.0).toFloat() - atan2(beamVector.z, beamVector.x).toFloat()

            poseStack.mulPose(Axis.YP.rotationDegrees(f2 * (180f / Math.PI.toFloat())))
            poseStack.mulPose(Axis.XP.rotationDegrees(f1 * (180f / Math.PI.toFloat())))

            val f3 = attackTime * 0.05f * -1.5f
            val f4 = scale * scale

            val i = 64 + (f4 * 191.0f).toInt()
            val j = 32 + (f4 * 191.0f).toInt()
            val k = 128 - (f4 * 64.0f).toInt()

            val f7 = Mth.cos((f3 + 2.3561945f).toDouble()) * 0.282f
            val f8 = Mth.sin((f3 + 2.3561945f).toDouble()) * 0.282f
            val f9 = Mth.cos((f3 + (Math.PI / 4.0))) * 0.282f
            val f10 = Mth.sin(f3 + (Math.PI / 4.0)) * 0.282f
            val f11 = Mth.cos((f3 + 3.926991f).toDouble()) * 0.282f
            val f12 = Mth.sin((f3 + 3.926991f).toDouble()) * 0.282f
            val f13 = Mth.cos((f3 + 5.4977875f).toDouble()) * 0.282f
            val f14 = Mth.sin((f3 + 5.4977875f).toDouble()) * 0.282f

            val f15 = Mth.cos(f3 + Math.PI) * 0.2f
            val f16 = Mth.sin(f3 + Math.PI) * 0.2f
            val f17 = Mth.cos(f3.toDouble()) * 0.2f
            val f18 = Mth.sin(f3.toDouble()) * 0.2f
            val f19 = Mth.cos(f3 + (Math.PI / 2.0)) * 0.2f
            val f20 = Mth.sin(f3 + (Math.PI / 2.0)) * 0.2f
            val f21 = Mth.cos(f3 + (Math.PI * 1.5)) * 0.2f
            val f22 = Mth.sin(f3 + (Math.PI * 1.5)) * 0.2f

            val f25 = -1.0f + animationTime
            val f26 = f25 + f * 2.5f

            val bufferSource = mc.renderBuffers().bufferSource()
            val consumer = bufferSource.getBuffer(RenderTypes.entityCutout(BEAM_LOCATION))
            val pose = poseStack.last()

            vertex(consumer, pose, f15, f, f16, i, j, k, 0.4999f, f26)
            vertex(consumer, pose, f15, 0.0f, f16, i, j, k, 0.4999f, f25)
            vertex(consumer, pose, f17, 0.0f, f18, i, j, k, 0.0f, f25)
            vertex(consumer, pose, f17, f, f18, i, j, k, 0.0f, f26)

            vertex(consumer, pose, f19, f, f20, i, j, k, 0.4999f, f26)
            vertex(consumer, pose, f19, 0.0f, f20, i, j, k, 0.4999f, f25)
            vertex(consumer, pose, f21, 0.0f, f22, i, j, k, 0.0f, f25)
            vertex(consumer, pose, f21, f, f22, i, j, k, 0.0f, f26)

            val f27 = if (Mth.floor(attackTime) % 2 == 0) 0.5f else 0.0f
            vertex(consumer, pose, f7, f, f8, i, j, k, 0.5f, f27 + 0.5f)
            vertex(consumer, pose, f9, f, f10, i, j, k, 1.0f, f27 + 0.5f)
            vertex(consumer, pose, f13, f, f14, i, j, k, 1.0f, f27)
            vertex(consumer, pose, f11, f, f12, i, j, k, 0.5f, f27)


            poseStack.popPose()

            if (ModCompat.hasDynLights) {
                GuardianBeamDynamicLightCompat.updateBeamLight(player, endPos.x, endPos.y, endPos.z, scale)
            }
        }

        if (ModCompat.hasDynLights) {
            val stoppedShooting = playersShootingLastFrame.subtract(currentlyShooting)
            for (playerId in stoppedShooting) {
                GuardianBeamDynamicLightCompat.removeBeamLight(playerId)
            }

            playersShootingLastFrame.clear()
            playersShootingLastFrame.addAll(currentlyShooting)
        }

        lastBubbleTickByPlayer.keys.retainAll(currentlyShooting)
    }

    private fun spawnBeamBubbles(
        level: ClientLevel,
        startPos: Vec3,
        direction: Vec3,
        distance: Double,
        scale: Double
    ) {
        var dist = level.random.nextDouble()
        while (dist < distance) {
            dist += 1.8 - scale + level.random.nextDouble() * (1.7 - scale)
            val x = startPos.x + direction.x * dist
            val y = startPos.y + direction.y * dist
            val z = startPos.z + direction.z * dist

            if (!level.getFluidState(BlockPos.containing(x, y, z)).`is`(FluidTags.WATER)) continue

            level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.0, 0.0)
        }
    }

    private fun vertex(consumer: VertexConsumer, pose: PoseStack.Pose, x: Float, y: Float, z: Float, r: Int, g: Int, b: Int, u: Float, v: Float) {
        consumer.addVertex(pose, x, y, z)
            .setColor(r, g, b, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(ModUtilities.FULL_BRIGHT_LIGHTMAP)
            .setNormal(pose, 0.0f, 1.0f, 0.0f)
    }
}
