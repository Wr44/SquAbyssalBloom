package fr.heta__h.squ_abyssal_bloom.config.renderer

import dev.isxander.yacl3.gui.image.ImageRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.sprite.SpriteId
import net.minecraft.resources.Identifier

class ItemIconRenderer(
    private val textureId: Identifier
) : ImageRenderer {

    companion object {
        private const val HEIGHT = 24
        private const val ICON_SIZE = 16
        private val BARRIER_TEXTURE = Identifier.withDefaultNamespace("item/barrier")


        fun resolveSprite(graphics: GuiGraphicsExtractor, textureId: Identifier): TextureAtlasSprite =
            try {
                val sprite = graphics.getSprite(SpriteId(TextureAtlas.LOCATION_ITEMS, textureId))
                if (sprite.contents().name() != MissingTextureAtlasSprite.getLocation()) {
                    sprite
                } else {
                    graphics.getSprite(SpriteId(TextureAtlas.LOCATION_ITEMS, BARRIER_TEXTURE))
                }
            } catch (_: Exception) {
                graphics.getSprite(SpriteId(TextureAtlas.LOCATION_ITEMS, BARRIER_TEXTURE))
            }
    }

    override fun render(
        graphics: GuiGraphicsExtractor?,
        x: Int,
        y: Int,
        renderWidth: Int,
        tickDelta: Float
    ): Int {
        if (graphics == null) return 0

        val sprite = resolveSprite(graphics, textureId)
        val iconX = x + (renderWidth - ICON_SIZE) / 2
        val iconY = y + (HEIGHT - ICON_SIZE) / 2
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, iconX, iconY, ICON_SIZE, ICON_SIZE)

        return HEIGHT
    }

    override fun close() {}
}
