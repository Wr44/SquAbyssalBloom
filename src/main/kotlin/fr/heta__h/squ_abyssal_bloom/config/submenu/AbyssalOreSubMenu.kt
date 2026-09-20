package fr.heta__h.squ_abyssal_bloom.config.submenu

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import fr.heta__h.squ_abyssal_bloom.config.renderer.StaticImageRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverBool
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverInt
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object AbyssalOreSubMenu {
    val displayName: Component = Component.translatable("config.squ_abyssal_bloom.group.abyssal_ores")
    val displayDescription: Component = Component.translatable("config.squ_abyssal_bloom.group.abyssal_ores.desc")

    fun previewRenderer(): StaticImageRenderer = StaticImageRenderer("wave", 1920, 991)

    fun buildGroups(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(displayName.copy().withStyle(ChatFormatting.GOLD))
            .description(OptionDescription.createBuilder()
                .text(displayDescription)
                .customImage(previewRenderer())
                .build())
            .option(serverBool(ModServerConfig.ABYSSAL_ORE_ENRICHMENT_ENABLED))
            .option(serverInt(ModServerConfig.ABYSSAL_ORE_BONUS_PASSES, step = 1))
            .option(serverBool(ModServerConfig.ABYSSAL_ORE_CATALOG_DEBUG))
            .build())
    }
}
