package fr.heta__h.squ_abyssal_bloom.config.submenu

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionEventListener
import dev.isxander.yacl3.api.OptionGroup
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverBool
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverDouble
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverInt
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object BioluminescenceServerSubMenu {
    val displayName: Component = Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_server")
    val displayDescription: Component = Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_server.desc")

    private val durationFormat: (Int) -> Component = { Component.literal(ModUtilities.formatTicksAsDuration(it)) }

    fun buildGroups(category: ConfigCategory.Builder) {
        val opportunityMinimum = serverInt(
            ModServerConfig.BIOLUMINESCENCE_OPPORTUNITY_MIN_NIGHT_TICKS,
            1..2000000,
            200,
            durationFormat
        )
        val opportunityMaximum = serverInt(
            ModServerConfig.BIOLUMINESCENCE_OPPORTUNITY_MAX_NIGHT_TICKS,
            1..2000000,
            1200,
            durationFormat
        )
        opportunityMinimum.addEventListener { option, event ->
            if (event == OptionEventListener.Event.STATE_CHANGE &&
                opportunityMaximum.pendingValue() < option.pendingValue()
            ) opportunityMaximum.requestSet(option.pendingValue())
        }
        opportunityMaximum.addEventListener { option, event ->
            if (event == OptionEventListener.Event.STATE_CHANGE &&
                opportunityMinimum.pendingValue() > option.pendingValue()
            ) opportunityMinimum.requestSet(option.pendingValue())
        }

        val durationMinimum = serverInt(ModServerConfig.BIOLUMINESCENCE_DURATION_MIN_TICKS, step = 100, format = durationFormat)
        val durationMaximum = serverInt(ModServerConfig.BIOLUMINESCENCE_DURATION_MAX_TICKS, step = 100, format = durationFormat)

        durationMinimum.addEventListener { option, event ->
            if (event == OptionEventListener.Event.STATE_CHANGE &&
                durationMaximum.pendingValue() < option.pendingValue()
            ) durationMaximum.requestSet(option.pendingValue())
        }
        durationMaximum.addEventListener { option, event ->
            if (event == OptionEventListener.Event.STATE_CHANGE &&
                durationMinimum.pendingValue() > option.pendingValue()
            ) durationMinimum.requestSet(option.pendingValue())
        }

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_opportunities").withStyle(ChatFormatting.AQUA))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_opportunities.desc")
            ))
            .option(serverBool(ModServerConfig.BIOLUMINESCENCE_ENABLED))
            .option(serverInt(
                ModServerConfig.BIOLUMINESCENCE_OPPORTUNITY_MEAN_NIGHT_TICKS,
                1..2000000,
                1200,
                durationFormat
            ))
            .option(opportunityMinimum)
            .option(opportunityMaximum)
            .build())

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_waves").withStyle(ChatFormatting.BLUE))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_waves.desc")
            ))
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_LARGE_CHANCE,
                step = 0.01
            ) { value -> Component.literal(String.format("%.0f%%", value * 100.0)) })
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_ACTIVE_CHANCE,
                step = 0.01
            ) { value -> Component.literal(String.format("%.0f%%", value * 100.0)) })
            .option(serverInt(ModServerConfig.BIOLUMINESCENCE_DURATION_MEAN_TICKS, step = 100, format = durationFormat))
            .option(serverInt(
                ModServerConfig.BIOLUMINESCENCE_DURATION_STANDARD_DEVIATION_TICKS,
                step = 100,
                format = durationFormat
            ))
            .option(durationMinimum)
            .option(durationMaximum)
            .option(serverInt(ModServerConfig.BIOLUMINESCENCE_PREPARATION_DELAY_TICKS, step = 5, format = durationFormat))
            .option(serverDouble(ModServerConfig.BIOLUMINESCENCE_VISIBILITY_RADIUS, step = 8.0, format = ModUtilities.blocksFormatDouble()))
            .option(serverDouble(ModServerConfig.BIOLUMINESCENCE_OVERLAP_MARGIN, step = 2.0, format = ModUtilities.blocksFormatDouble()))
            .build())

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_detection").withStyle(ChatFormatting.DARK_AQUA))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_detection.desc")
            ))
            .option(serverInt(ModServerConfig.BIOLUMINESCENCE_WATER_SEARCH_RADIUS, format = ModUtilities.blocksFormatInt()))
            .option(serverInt(ModServerConfig.BIOLUMINESCENCE_MINIMUM_NEARBY_WATER_CELLS) { Component.literal("$it cellules") })
            .build())

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_total_night").withStyle(ChatFormatting.LIGHT_PURPLE))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_total_night.desc")
            ))
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_TOTAL_NIGHT_CHANCE,
                step = 0.01
            ) { value -> Component.literal(String.format("%.0f%%", value * 100.0)) })
            .option(serverInt(ModServerConfig.BIOLUMINESCENCE_NIGHT_START_TICK, step = 100, format = durationFormat))
            .option(serverInt(ModServerConfig.BIOLUMINESCENCE_NIGHT_END_TICK, step = 100, format = durationFormat))
            .build())
    }
}
