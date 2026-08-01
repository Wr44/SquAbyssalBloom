package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.client.renderer.texture.DynamicTexture

internal class BioluminescentDynamicTexture(
    label: String,
    width: Int,
    height: Int,
    zero: Boolean
) : DynamicTexture(label, width, height, zero) {
    init {
        sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
    }
}
