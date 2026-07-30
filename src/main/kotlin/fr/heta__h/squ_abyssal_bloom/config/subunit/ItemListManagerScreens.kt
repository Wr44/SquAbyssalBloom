package fr.heta__h.squ_abyssal_bloom.config.subunit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.types.StringListOption
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item

object ItemListManagerScreens {

    fun open(
        parent: Screen,
        listOption: StringListOption,
        title: Component,
        excludeTag: TagKey<Item>? = null,
        additionalFilter: (String) -> Boolean = { true }
    ) {
        val entries = mutableListOf<IconListManagerScreens.IconEntry>()

        BuiltInRegistries.ITEM.forEach { item ->
            try {
                val itemId = BuiltInRegistries.ITEM.getKey(item)
                val idString = itemId.toString()
                if (!additionalFilter(idString)) return@forEach
                if (excludeTag != null && BuiltInRegistries.ITEM.wrapAsHolder(item).`is`(excludeTag)) return@forEach

                val textureId = Identifier.fromNamespaceAndPath(itemId.namespace, "item/${itemId.path}")
                entries.add(
                    IconListManagerScreens.IconEntry(
                        idString,
                        textureId,
                        Component.translatable(item.descriptionId)
                    )
                )
            } catch (e: Exception) {
                SquAbyssalBloom.LOGGER.warn("Skipping item {} in item list manager screen", item, e)
            }
        }

        IconListManagerScreens.open(parent, listOption, title, entries)
    }
}
