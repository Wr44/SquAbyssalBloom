package fr.heta__h.squ_abyssal_bloom.mixin.render

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.framegraph.FrameGraphBuilder
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.minecraft.client.Camera
import net.minecraft.world.level.material.FogType
import org.joml.Matrix4f
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import net.minecraft.client.renderer.LevelRenderer

@Mixin(LevelRenderer::class)
abstract class LevelRendererSkyMixin {

    @Inject(
        method = ["addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V"],
        at = [At("HEAD")],
        cancellable = true
    )
    private fun cancelSkyWhenUnderwater(
        frameGraphBuilder: FrameGraphBuilder,
        camera: Camera,
        shaderFog: GpuBufferSlice,
        modelViewMatrix: Matrix4f,
        ci: CallbackInfo
    ) {
        if (!ModConfig.enableAbyssFog) return
        if (camera.fluidInCamera == FogType.WATER) {
            ci.cancel()
        }
    }
}