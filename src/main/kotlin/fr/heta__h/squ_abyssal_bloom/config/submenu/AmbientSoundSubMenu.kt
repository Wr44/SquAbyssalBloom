package fr.heta__h.squ_abyssal_bloom.config.submenu

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.config.renderer.StaticImageRenderer
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object AmbientSoundSubMenu {
    val displayName: Component = Component.translatable("config.squ_abyssal_bloom.group.ambient_sounds")
    val displayDescription: Component = Component.translatable("config.squ_abyssal_bloom.group.ambient_sounds.desc")

    fun buildGroups(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(displayName.copy().withStyle(ChatFormatting.AQUA))
            .description(OptionDescription.createBuilder()
                .text(displayDescription)
                .customImage(StaticImageRenderer("wave", 1920, 991))
                .build())
            .option(Option.createBuilder<Boolean>()
                .name(Component.translatable("config.squ_abyssal_bloom.enableBeachWaveSound"))
                .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.enableBeachWaveSound.desc")))
                .binding(Binding.generic(true, { ModConfig.enableBeachWaveSound }, { ModConfig.enableBeachWaveSound = it }))
                .controller(TickBoxControllerBuilder::create)
                .build())
            .build())
    }
}
