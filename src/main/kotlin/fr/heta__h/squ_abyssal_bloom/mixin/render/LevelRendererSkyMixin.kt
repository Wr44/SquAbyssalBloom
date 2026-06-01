package fr.heta__h.squ_abyssal_bloom.mixin.render

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.framegraph.FrameGraphBuilder
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.world.level.material.FogType
import org.joml.Matrix4fc
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LevelRenderer::class)
abstract class LevelRendererSkyMixin {

    @Inject(
        method = ["addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4fc;)V"],
        at = [At("HEAD")],
        cancellable = true
    )
    private fun cancelSkyWhenUnderwater(
        frameGraphBuilder: FrameGraphBuilder,
        cameraState: CameraRenderState,
        shaderFog: GpuBufferSlice,
        modelViewMatrix: Matrix4fc,
        ci: CallbackInfo
    ) {
        if (!ModConfig.enableAbyssFog) return
        if (cameraState.fogType == FogType.WATER) {
            ci.cancel()
        }
    }
}