package fr.heta__h.squ_abyssal_bloom.config.subunit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.types.StringListOption
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block

object BlockListManagerScreens {

    fun open(
        parent: Screen,
        listOption: StringListOption,
        title: Component,
        excludeTag: TagKey<Block>? = null,
        additionalFilter: (String) -> Boolean = { true }
    ) {
        val entries = mutableListOf<IconListManagerScreens.IconEntry>()

        BuiltInRegistries.BLOCK.forEach { block ->
            try {
                val blockId = BuiltInRegistries.BLOCK.getKey(block)
                val idString = blockId.toString()
                if (!additionalFilter(idString)) return@forEach
                if (excludeTag != null && BuiltInRegistries.BLOCK.wrapAsHolder(block).`is`(excludeTag)) return@forEach

                val blockItem = block.asItem()
                if (blockItem === Items.AIR) return@forEach

                val textureId = Identifier.fromNamespaceAndPath(blockId.namespace, "block/${blockId.path}")
                entries.add(
                    IconListManagerScreens.IconEntry(
                        idString,
                        textureId,
                        Component.translatable(blockItem.descriptionId)
                    )
                )
            } catch (e: Exception) {
                SquAbyssalBloom.LOGGER.warn("Skipping block {} in block list manager screen", block, e)
            }
        }

        IconListManagerScreens.open(parent, listOption, title, entries)
    }
}
