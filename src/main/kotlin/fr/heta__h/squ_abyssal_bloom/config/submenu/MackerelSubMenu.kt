package fr.heta__h.squ_abyssal_bloom.config.submenu

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import fr.heta__h.squ_abyssal_bloom.config.renderer.EntityConfigRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverBool
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverDouble
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object MackerelSubMenu {

    fun previewRenderer(): EntityConfigRenderer =
        EntityConfigRenderer(entityType = ModEntities.MACKEREL.get())

    fun buildGroups(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("entity.squ_abyssal_bloom.mackerel").copy().withStyle(ChatFormatting.AQUA))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.mackerel.desc"))
                .customImage(previewRenderer())
                .build())
            .option(serverBool(ModServerConfig.MACKEREL_SPAWN_ENABLED))
            .option(serverDouble(ModServerConfig.MACKEREL_MAX_SPAWN_TEMPERATURE, step = 0.01, format = ModUtilities.plainDouble(2)))
            .build())
    }
}
