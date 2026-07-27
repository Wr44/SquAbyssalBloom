package fr.heta__h.squ_abyssal_bloom.config.subunit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.types.StringListOption
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block

object BlockListManagerScreens {

    fun open(
        parent: Screen,
        listOption: StringListOption,
        title: Component,
        excludeTag: TagKey<Block>? = null,
        additionalFilter: (String) -> Boolean = { true }
    ) {
        val entries = mutableListOf<Pair<String, ItemStack>>()

        BuiltInRegistries.BLOCK.forEach { block ->
            try {
                val idString = BuiltInRegistries.BLOCK.getKey(block).toString()
                if (!additionalFilter(idString)) return@forEach
                if (excludeTag != null && BuiltInRegistries.BLOCK.wrapAsHolder(block).`is`(excludeTag)) return@forEach

                val icon = ItemStack(block.asItem())
                if (icon.isEmpty) return@forEach

                entries.add(idString to icon)
            } catch (e: Exception) {
                SquAbyssalBloom.LOGGER.warn("Skipping block {} in block list manager screen", block, e)
            }
        }

        IconListManagerScreens.open(parent, listOption, title, entries)
    }
}
