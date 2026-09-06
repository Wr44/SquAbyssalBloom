package fr.heta__h.squ_abyssal_bloom.mixin.render

import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.BioluminescentWaterRenderer
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.GameRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GameRenderer::class)
abstract class GameRendererBioluminescenceMixin {

    @Inject(
        method = ["renderLevel"],
        at = [At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture" +
                "(Lcom/mojang/blaze3d/textures/GpuTexture;D)V"
        )]
    )
    private fun renderRadianceBeforeWorldDepthReset(deltaTracker: DeltaTracker, ci: CallbackInfo) {
        BioluminescentWaterRenderer.renderPostShaderRadiance()
    }
}
