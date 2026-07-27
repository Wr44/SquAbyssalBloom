package fr.heta__h.squ_abyssal_bloom.config.subunit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.types.StringListOption
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

object ItemListManagerScreens {

    fun open(
        parent: Screen,
        listOption: StringListOption,
        title: Component,
        excludeTag: TagKey<Item>? = null,
        additionalFilter: (String) -> Boolean = { true }
    ) {
        val entries = mutableListOf<Pair<String, ItemStack>>()

        BuiltInRegistries.ITEM.forEach { item ->
            try {
                val idString = BuiltInRegistries.ITEM.getKey(item).toString()
                if (!additionalFilter(idString)) return@forEach
                if (excludeTag != null && BuiltInRegistries.ITEM.wrapAsHolder(item).`is`(excludeTag)) return@forEach

                val icon = ItemStack(item)
                if (icon.isEmpty) return@forEach

                entries.add(idString to icon)
            } catch (e: Exception) {
                SquAbyssalBloom.LOGGER.warn("Skipping item {} in item list manager screen", item, e)
            }
        }

        IconListManagerScreens.open(parent, listOption, title, entries)
    }
}
