package fr.heta__h.squ_abyssal_bloom.mixin.render

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.accessor.AddPropertiesToRenderState
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LivingEntityRenderer::class)
abstract class LivingEntityRendererMixin<T : LivingEntity, S : LivingEntityRenderState> {

    @Inject(method = ["extractRenderState"], at = [At("TAIL")])
    private fun guardianRedistributionState(entity: T, state: S?, partialTick: Float, ci: CallbackInfo?) {
        if (state == null) return

        val hasEffect = entity.getData(ModAttachments.HAS_GUARDIAN_SPIKES)
        val item = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)

        if (state is AddPropertiesToRenderState) {
            state.setHasGuardianSpikes(hasEffect)

            val eyeH = entity.getEyeHeight(Pose.STANDING)
            state.setEyeHeight(eyeH)

            if (entity is AbstractNautilus) {
                state.setNautilusExtraItem(item)
            }
        }
    }
}