package fr.heta__h.squ_abyssal_bloom.config.renderer

import dev.isxander.yacl3.gui.image.ImageRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack

class ItemIconRenderer(
    private val itemStack: ItemStack
) : ImageRenderer {

    private companion object {
        const val HEIGHT = 24
        const val ICON_SIZE = 16
    }

    override fun render(
        graphics: GuiGraphicsExtractor?,
        x: Int,
        y: Int,
        renderWidth: Int,
        tickDelta: Float
    ): Int {
        if (graphics == null) return 0

        val iconX = x + (renderWidth - ICON_SIZE) / 2
        val iconY = y + (HEIGHT - ICON_SIZE) / 2
        graphics.item(itemStack, iconX, iconY)

        return HEIGHT
    }

    override fun close() {}
}
