package fr.heta__h.squ_abyssal_bloom.config.subunit

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.renderer.ItemIconRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.config.server.types.StringListOption
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.persistServerConfigChanges
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

object IconListManagerScreens {

    data class IconEntry(val id: String, val textureId: Identifier, val name: Component)

    fun open(
        parent: Screen,
        listOption: StringListOption,
        title: Component,
        entries: List<IconEntry>
    ) {
        val initialServerConfig = ServerConfigCache.toData()
        val categoryBuilder = ConfigCategory.createBuilder().name(title)

        for ((idString, textureId, name) in entries) {
            try {
                categoryBuilder.group(
                    OptionGroup.createBuilder()
                        .name(name)
                        .description(
                            OptionDescription.createBuilder()
                                .customImage(ItemIconRenderer(textureId))
                                .build()
                        )
                        .option(
                            Option.createBuilder<Boolean>()
                                .name(Component.translatable("config.squ_abyssal_bloom.entityListManager.include"))
                                .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.entityListManager.include.desc")))
                                .binding(
                                    Binding.generic(
                                        ServerConfigCache.current(listOption).contains(idString),
                                        { ServerConfigCache.current(listOption).contains(idString) },
                                        { included ->
                                            val current = ServerConfigCache.current(listOption).toMutableList()
                                            if (included) {
                                                if (idString !in current) current.add(idString)
                                            } else {
                                                current.remove(idString)
                                            }
                                            ServerConfigCache.set(listOption, current)
                                        }
                                    )
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )
                        .build()
                )
            } catch (e: Exception) {
                SquAbyssalBloom.LOGGER.warn("Skipping entry {} in icon list manager screen", idString, e)
            }
        }

        try {
            val screen = YetAnotherConfigLib.createBuilder()
                .title(title)
                .save { persistServerConfigChanges(initialServerConfig) }
                .category(categoryBuilder.build())
                .build()
                .generateScreen(parent)

            Minecraft.getInstance().setScreen(screen)
        } catch (e: Exception) {
            SquAbyssalBloom.LOGGER.error("Failed to open icon list manager screen", e)
        }
    }
}
