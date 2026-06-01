package fr.heta__h.squ_abyssal_bloom.mixin.block.conduit

import fr.heta__h.squ_abyssal_bloom.util.conduit.ConduitMaterialHolder
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.ConduitRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ConduitRenderer::class)
abstract class ConduitRendererMixin {
    @Inject(method = ["<init>"], at = [At("TAIL")])
    private fun captureMaterials(context: BlockEntityRendererProvider.Context, ci: CallbackInfo) {
        ConduitMaterialHolder.materials = context.sprites
    }
}