package fr.heta__h.squ_abyssal_bloom.config.submenu.mods

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import fr.heta__h.squ_abyssal_bloom.config.renderer.ModIconRenderer
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

abstract class AbstractModSubMenu(private val modId: String) {

    abstract val displayName: Component
    abstract val displayDescription: Component
    abstract val headerOption: Option<*>

    abstract fun clientOptions(): List<Option<*>>

    open fun serverOptions(): List<Option<*>> = emptyList()

    fun previewRenderer(): ModIconRenderer = ModIconRenderer(modId)

    fun buildGroups(category: ConfigCategory.Builder) {
        val headerGroup = OptionGroup.createBuilder()
            .name(displayName.copy().withStyle(ChatFormatting.LIGHT_PURPLE))
            .description(OptionDescription.createBuilder()
                .text(displayDescription)
                .customImage(previewRenderer())
                .build())
            .option(headerOption)
        category.group(headerGroup.build())

        val clientOpts = clientOptions()
        if (clientOpts.isNotEmpty()) {
            val clientGroup = OptionGroup.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.group.client_settings").withStyle(ChatFormatting.AQUA))
                .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.group.client_settings.desc")))
            clientOpts.forEach { clientGroup.option(it) }
            category.group(clientGroup.build())
        }

        val serverOpts = serverOptions()
        if (serverOpts.isNotEmpty()) {
            val serverGroup = OptionGroup.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.group.server_settings").withStyle(ChatFormatting.GOLD))
                .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.group.server_settings.desc")))
            serverOpts.forEach { serverGroup.option(it) }
            category.group(serverGroup.build())
        }
    }
}
