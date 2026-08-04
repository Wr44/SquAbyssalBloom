package fr.heta__h.squ_abyssal_bloom.config.submenu.mods

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.minecraft.network.chat.Component

object IrisSubMenu : AbstractModSubMenu(ModCompat.irisModId ?: "iris") {

    override val displayName: Component = Component.translatable("config.squ_abyssal_bloom.group.iris")
    override val displayDescription: Component = Component.translatable("config.squ_abyssal_bloom.group.iris.desc")

    override val headerOption: Option<*> = Option.createBuilder<Boolean>()
        .name(Component.translatable("config.squ_abyssal_bloom.enableIrisCompatibility"))
        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.enableIrisCompatibility.desc")))
        .binding(Binding.generic(true, { ModConfig.enableIrisCompatibility }, { ModConfig.enableIrisCompatibility = it }))
        .controller(TickBoxControllerBuilder::create)
        .build()

    override fun clientOptions(): List<Option<*>> = listOf(
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescencePrimaryAlphaMultiplier"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescencePrimaryAlphaMultiplier.desc")))
            .binding(Binding.generic(2.2, { ModConfig.shaderBioluminescencePrimaryAlphaMultiplier }, { ModConfig.shaderBioluminescencePrimaryAlphaMultiplier = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(1.0, 3.0).step(0.1).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceVisibilityCompensation"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceVisibilityCompensation.desc")))
            .binding(Binding.generic(0.22, { ModConfig.shaderBioluminescenceVisibilityCompensation }, { ModConfig.shaderBioluminescenceVisibilityCompensation = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 0.4).step(0.01).formatValue { v -> Component.literal(String.format("%.0f%%", v * 100.0)) } }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceDeepWaterBoost"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceDeepWaterBoost.desc")))
            .binding(Binding.generic(2.2, { ModConfig.shaderBioluminescenceDeepWaterBoost }, { ModConfig.shaderBioluminescenceDeepWaterBoost = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(1.0, 3.0).step(0.05).formatValue { v -> Component.literal(String.format("%.2fx", v)) } }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceGrazingStrength"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceGrazingStrength.desc")))
            .binding(Binding.generic(0.45, { ModConfig.shaderBioluminescenceGrazingStrength }, { ModConfig.shaderBioluminescenceGrazingStrength = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05).formatValue { v -> Component.literal(String.format("%.0f%%", v * 100.0)) } }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceUnderwaterCompensation"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceUnderwaterCompensation.desc")))
            .binding(Binding.generic(0.85, { ModConfig.shaderBioluminescenceUnderwaterCompensation }, { ModConfig.shaderBioluminescenceUnderwaterCompensation = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.5).step(0.05).formatValue { v -> Component.literal(String.format("%.2fx", v)) } }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceSubsurfaceOffset"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceSubsurfaceOffset.desc")))
            .binding(Binding.generic(0.25, { ModConfig.shaderBioluminescenceSubsurfaceOffset }, { ModConfig.shaderBioluminescenceSubsurfaceOffset = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.05, 0.50).step(0.01).formatValue { v -> Component.literal(String.format("%.2f", v)) } }
            .build()
    )
}
