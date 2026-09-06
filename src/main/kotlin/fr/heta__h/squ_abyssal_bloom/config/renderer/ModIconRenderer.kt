package fr.heta__h.squ_abyssal_bloom.config.renderer

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import dev.isxander.yacl3.gui.image.ImageRenderer
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.repository.PackSource
import net.neoforged.fml.ModList
import net.neoforged.neoforge.resource.ResourcePackLoader
import java.util.Optional

class ModIconRenderer(private val modId: String) : ImageRenderer {

    private class IconData(val textureId: Identifier, val width: Int, val height: Int)

    companion object {
        private const val HEIGHT = 64
        private val loadedIcons = HashMap<String, IconData?>()

        private fun loadIcon(modId: String): IconData? {
            try {
                val modInfo = ModList.get().getModContainerById(modId).orElse(null)?.modInfo ?: return null
                val logoFile = modInfo.logoFile.orElse(null) ?: return null
                val resourcePack = ResourcePackLoader.getPackFor(modId).orElse(null) ?: return null
                resourcePack.openPrimary(
                    PackLocationInfo("mod/$modId", Component.empty(), PackSource.BUILT_IN, Optional.empty())
                ).use { packResources ->
                    val logoResource = packResources.getRootResource(*logoFile.split(Regex("[/\\\\]")).toTypedArray())
                        ?: return null
                    val logo = NativeImage.read(logoResource.get())
                    val textureId = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "dynamic/mod_icon/$modId")
                    Minecraft.getInstance().textureManager.register(
                        textureId,
                        object : DynamicTexture({ textureId.toString() }, logo) {
                            override fun upload() {
                                val filter = if (modInfo.logoBlur) FilterMode.LINEAR else FilterMode.NEAREST
                                sampler = RenderSystem.getSamplerCache()
                                    .getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filter, filter, false)
                                super.upload()
                            }
                        }
                    )
                    return IconData(textureId, logo.width, logo.height)
                }
            } catch (_: Exception) {
                return null
            }
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
        val icon = loadedIcons.getOrPut(modId) { loadIcon(modId) } ?: return 0

        val width = (HEIGHT.toLong() * icon.width / icon.height).toInt().coerceAtLeast(1).coerceAtMost(renderWidth)
        val drawX = x + (renderWidth - width) / 2
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            icon.textureId,
            drawX, y,
            0f, 0f,
            width, HEIGHT,
            icon.width, icon.height,
            icon.width, icon.height
        )
        return HEIGHT
    }

    override fun close() {}
}
