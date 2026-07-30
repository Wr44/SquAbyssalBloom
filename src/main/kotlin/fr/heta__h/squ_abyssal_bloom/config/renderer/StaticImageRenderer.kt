package fr.heta__h.squ_abyssal_bloom.config.renderer

import dev.isxander.yacl3.gui.image.ImageRenderer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

class StaticImageRenderer(
    private val texture: Identifier,
    private val textureWidth: Int,
    private val textureHeight: Int
) : ImageRenderer {

    constructor(texturePath: String, textureWidth: Int, textureHeight: Int) : this(
        Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "textures/gui/$texturePath.png"),
        textureWidth,
        textureHeight
    )

    override fun render(
        graphics: GuiGraphicsExtractor?,
        x: Int,
        y: Int,
        renderWidth: Int,
        tickDelta: Float
    ): Int {
        if (graphics == null) return 0

        val height = (renderWidth.toLong() * textureHeight / textureWidth).toInt().coerceAtLeast(1)
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x, y,
            0f, 0f,
            renderWidth, height,
            textureWidth, textureHeight,
            textureWidth, textureHeight
        )
        return height
    }

    override fun close() {}
}
