package fr.heta__h.squ_abyssal_bloom.mixin.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.util.accessor.AddPropertiesToRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private const val FISH_ROTATION_LOG_INTERVAL_TICKS = 40L
private const val MAXIMUM_FISH_ROTATION_LOGS_PER_TICK = 4

@Mixin(LivingEntityRenderer::class)
abstract class LivingEntityRendererMixin<T : LivingEntity, S : LivingEntityRenderState> {

    @Unique
    private var fishRotationExtractLogTick = Long.MIN_VALUE

    @Unique
    private var fishRotationExtractLogsThisTick = 0

    @Unique
    private var fishRotationRenderLogTick = Long.MIN_VALUE

    @Unique
    private var fishRotationRenderLogsThisTick = 0

    @Inject(method = ["extractRenderState"], at = [At("TAIL")])
    private fun guardianRedistributionState(entity: T, state: S?, partialTick: Float, ci: CallbackInfo?) {
        if (state == null) return

        val hasEffect = entity.getData(ModAttachments.HAS_GUARDIAN_SPIKES)
        val item = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)

        if (state is AddPropertiesToRenderState) {
            state.setHasGuardianSpikes(hasEffect)
            state.setFishRotationEntityId(
                if (entity is AbstractFish) entity.id else -1
            )
            state.setApplyFishBodyPitch(
                entity is AbstractFish &&
                    entity.isAlive &&
                    entity.isInWater
            )

            val eyeH = entity.getEyeHeight(Pose.STANDING)
            state.setEyeHeight(eyeH)

            if (entity is AbstractNautilus) {
                state.setNautilusExtraItem(item)
            }
        }

        val fish = entity as? AbstractFish ?: return
        val extendedState = state as? AddPropertiesToRenderState ?: return
        if (shouldLogFishRotationExtract()) {
            val velocity = fish.deltaMovement
            SquAbyssalBloom.LOGGER.info(
                "[Fish school rotation debug] stage=clientExtract fishId={} type={} entityPitch={} statePitch={} syncedPitchOld={} yaw={} bodyYaw={} headYaw={} moveX={} moveY={} moveZ={} inWater={} alive={} applyPitch={}",
                fish.id,
                fish.type.description.string,
                roundedRotationDebug(fish.xRot.toDouble(), 10.0),
                roundedRotationDebug(state.xRot.toDouble(), 10.0),
                roundedRotationDebug(fish.xRotO.toDouble(), 10.0),
                roundedRotationDebug(fish.yRot.toDouble(), 10.0),
                roundedRotationDebug(fish.yBodyRot.toDouble(), 10.0),
                roundedRotationDebug(fish.yHeadRot.toDouble(), 10.0),
                roundedRotationDebug(velocity.x, 1000.0),
                roundedRotationDebug(velocity.y, 1000.0),
                roundedRotationDebug(velocity.z, 1000.0),
                fish.isInWater,
                fish.isAlive,
                extendedState.getApplyFishBodyPitch()
            )
        }
    }

    @Inject(method = ["setupRotations"], at = [At("TAIL")])
    private fun applyFishBodyPitch(
        state: S,
        poseStack: PoseStack,
        bodyRot: Float,
        entityScale: Float,
        ci: CallbackInfo
    ) {
        val extendedState = state as? AddPropertiesToRenderState ?: return
        if (!extendedState.getApplyFishBodyPitch()) return

        val willApplyPitch =
            state.deathTime <= 0.0f &&
                !state.isUpsideDown &&
                !state.isAutoSpinAttack &&
                !state.hasPose(Pose.SLEEPING)
        if (shouldLogFishRotationRender()) {
            SquAbyssalBloom.LOGGER.info(
                "[Fish school rotation debug] stage=clientRender fishId={} type={} statePitch={} appliedPitch={} willApply={} deathTime={} upsideDown={} autoSpin={} pose={} posX={} posY={} posZ={}",
                extendedState.getFishRotationEntityId(),
                state.entityType.description.string,
                roundedRotationDebug(state.xRot.toDouble(), 10.0),
                roundedRotationDebug(
                    if (willApplyPitch) -state.xRot.toDouble() else 0.0,
                    10.0
                ),
                willApplyPitch,
                roundedRotationDebug(state.deathTime.toDouble(), 10.0),
                state.isUpsideDown,
                state.isAutoSpinAttack,
                state.pose,
                roundedRotationDebug(state.x, 10.0),
                roundedRotationDebug(state.y, 10.0),
                roundedRotationDebug(state.z, 10.0)
            )
        }
        if (!willApplyPitch) return
        poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot))
    }

    @Unique
    private fun shouldLogFishRotationExtract(): Boolean {
        if (!ModServerConfig.FISH_SCHOOL_DEBUG.get()) return false
        val gameTime = Minecraft.getInstance().level?.gameTime ?: return false
        if (fishRotationExtractLogTick != gameTime) {
            fishRotationExtractLogTick = gameTime
            fishRotationExtractLogsThisTick = 0
        }
        if (
            Math.floorMod(gameTime, FISH_ROTATION_LOG_INTERVAL_TICKS) != 0L ||
            fishRotationExtractLogsThisTick >= MAXIMUM_FISH_ROTATION_LOGS_PER_TICK
        ) {
            return false
        }
        fishRotationExtractLogsThisTick++
        return true
    }

    @Unique
    private fun shouldLogFishRotationRender(): Boolean {
        if (!ModServerConfig.FISH_SCHOOL_DEBUG.get()) return false
        val gameTime = Minecraft.getInstance().level?.gameTime ?: return false
        if (fishRotationRenderLogTick != gameTime) {
            fishRotationRenderLogTick = gameTime
            fishRotationRenderLogsThisTick = 0
        }
        if (
            Math.floorMod(gameTime, FISH_ROTATION_LOG_INTERVAL_TICKS) != 0L ||
            fishRotationRenderLogsThisTick >= MAXIMUM_FISH_ROTATION_LOGS_PER_TICK
        ) {
            return false
        }
        fishRotationRenderLogsThisTick++
        return true
    }

    @Unique
    private fun roundedRotationDebug(value: Double, scale: Double): Double {
        return kotlin.math.round(value * scale) / scale
    }
}
