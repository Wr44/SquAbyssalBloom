package fr.heta__h.squ_abyssal_bloom.config.submenu.mods

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.minecraft.network.chat.Component

object DynamicLightSubMenu : AbstractModSubMenu(ModCompat.dynLightsModId ?: "lambdynlights") {

    override val displayName: Component = Component.translatable("config.squ_abyssal_bloom.group.dynamic_light")
    override val displayDescription: Component = Component.translatable("config.squ_abyssal_bloom.group.dynamic_light.desc")

    override val headerOption: Option<*> = Option.createBuilder<Boolean>()
        .name(Component.translatable("config.squ_abyssal_bloom.enableDynamicLights"))
        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.enableDynamicLights.desc")))
        .binding(Binding.generic(true, { ModConfig.enableDynamicLights }, { ModConfig.enableDynamicLights = it }))
        .controller(TickBoxControllerBuilder::create)
        .build()

    override fun clientOptions(): List<Option<*>> = listOf(
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.dynamicLightsNautilusIntensity"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.dynamicLightsNautilusIntensity.desc")))
            .binding(Binding.generic(1.0, { ModConfig.dynamicLightsNautilusIntensity }, { ModConfig.dynamicLightsNautilusIntensity = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.dynamicLightsBioluminescenceActiveIntensity"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.dynamicLightsBioluminescenceActiveIntensity.desc")))
            .binding(Binding.generic(1.0, { ModConfig.dynamicLightsBioluminescenceActiveIntensity }, { ModConfig.dynamicLightsBioluminescenceActiveIntensity = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
            .build(),
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.dynamicLightsBioluminescenceInactiveIntensity"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.dynamicLightsBioluminescenceInactiveIntensity.desc")))
            .binding(Binding.generic(1.0, { ModConfig.dynamicLightsBioluminescenceInactiveIntensity }, { ModConfig.dynamicLightsBioluminescenceInactiveIntensity = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
            .build()
    )
}
