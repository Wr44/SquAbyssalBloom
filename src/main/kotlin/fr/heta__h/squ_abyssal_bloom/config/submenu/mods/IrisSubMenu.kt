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
        .name(Component.translatable("config.squ_abyssal_bloom.irisCompatibilityEnabled"))
        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.irisCompatibilityEnabled.desc")))
        .binding(Binding.generic(true, { ModConfig.irisCompatibilityEnabled }, { ModConfig.irisCompatibilityEnabled = it }))
        .controller(TickBoxControllerBuilder::create)
        .build()

    override fun clientOptions(): List<Option<*>> = listOf(
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescencePrimaryAlphaMultiplier"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescencePrimaryAlphaMultiplier.desc")))
            .binding(Binding.generic(1.6, { ModConfig.shaderBioluminescencePrimaryAlphaMultiplier }, { ModConfig.shaderBioluminescencePrimaryAlphaMultiplier = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(1.0, 3.0).step(0.1) }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceVisibilityCompensation"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceVisibilityCompensation.desc")))
            .binding(Binding.generic(0.12, { ModConfig.shaderBioluminescenceVisibilityCompensation }, { ModConfig.shaderBioluminescenceVisibilityCompensation = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 0.4).step(0.01) }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceDeepWaterBoost"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceDeepWaterBoost.desc")))
            .binding(Binding.generic(1.45, { ModConfig.shaderBioluminescenceDeepWaterBoost }, { ModConfig.shaderBioluminescenceDeepWaterBoost = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(1.0, 3.0).step(0.05) }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceGrazingStrength"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceGrazingStrength.desc")))
            .binding(Binding.generic(0.20, { ModConfig.shaderBioluminescenceGrazingStrength }, { ModConfig.shaderBioluminescenceGrazingStrength = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05) }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceUnderwaterCompensation"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderBioluminescenceUnderwaterCompensation.desc")))
            .binding(Binding.generic(0.45, { ModConfig.shaderBioluminescenceUnderwaterCompensation }, { ModConfig.shaderBioluminescenceUnderwaterCompensation = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.5).step(0.05) }
            .build()
    )
}
