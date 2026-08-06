package fr.heta__h.squ_abyssal_bloom.config.submenu

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
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
    private val countFormat: (Int) -> Component = ModUtilities.unitInt("blooms")
    private val harvestFormat: (Int) -> Component = ModUtilities.unitInt("harvests")

    private fun bindOrderedPair(minimum: Option<Int>, maximum: Option<Int>) {
        minimum.addEventListener { option, event ->
            if (event == OptionEventListener.Event.STATE_CHANGE &&
                maximum.pendingValue() < option.pendingValue()
            ) maximum.requestSet(option.pendingValue())
        }
        maximum.addEventListener { option, event ->
            if (event == OptionEventListener.Event.STATE_CHANGE &&
                minimum.pendingValue() > option.pendingValue()
            ) minimum.requestSet(option.pendingValue())
        }
    }

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
        bindOrderedPair(opportunityMinimum, opportunityMaximum)

        val durationMinimum = serverInt(ModServerConfig.BIOLUMINESCENCE_DURATION_MIN_TICKS, step = 100, format = durationFormat)
        val durationMaximum = serverInt(ModServerConfig.BIOLUMINESCENCE_DURATION_MAX_TICKS, step = 100, format = durationFormat)

        bindOrderedPair(durationMinimum, durationMaximum)

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
            , format = ModUtilities.percentFormat()))
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_ACTIVE_CHANCE,
                step = 0.01
            , format = ModUtilities.percentFormat()))
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
            .option(serverInt(ModServerConfig.BIOLUMINESCENCE_MINIMUM_NEARBY_WATER_CELLS, format = ModUtilities.unitInt("cells")))
            .build())

        val smallMinimum = serverInt(ModServerConfig.BIOLUMINESCENCE_BLOOM_SMALL_MIN_COUNT, format = countFormat)
        val smallMaximum = serverInt(ModServerConfig.BIOLUMINESCENCE_BLOOM_SMALL_MAX_COUNT, format = countFormat)
        val largeMinimum = serverInt(ModServerConfig.BIOLUMINESCENCE_BLOOM_LARGE_MIN_COUNT, format = countFormat)
        val largeMaximum = serverInt(ModServerConfig.BIOLUMINESCENCE_BLOOM_LARGE_MAX_COUNT, format = countFormat)
        val harvestMinimum = serverInt(ModServerConfig.BIOLUMINESCENCE_BLOOM_MIN_HARVESTS, format = harvestFormat)
        val harvestMaximum = serverInt(ModServerConfig.BIOLUMINESCENCE_BLOOM_MAX_HARVESTS, format = harvestFormat)

        bindOrderedPair(smallMinimum, smallMaximum)
        bindOrderedPair(largeMinimum, largeMaximum)
        bindOrderedPair(harvestMinimum, harvestMaximum)

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_blooms").withStyle(ChatFormatting.GREEN))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_blooms.desc")
            ))
            .option(serverBool(ModServerConfig.BIOLUMINESCENCE_BLOOM_ENABLED))
            .option(smallMinimum)
            .option(smallMaximum)
            .option(largeMinimum)
            .option(largeMaximum)
            .option(harvestMinimum)
            .option(harvestMaximum)
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_BLOOM_HARVEST_RADIUS,
                step = 0.5,
                format = ModUtilities.blocksFormatDouble()
            ))
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_BLOOM_ACTIVATION_RADIUS,
                step = 1.0,
                format = ModUtilities.blocksFormatDouble()
            ))
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_BLOOM_ACTIVATION_MIN_DISPLACEMENT,
                step = 0.01,
                format = ModUtilities.blocksFormatDouble()
            ))
            .build())

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_total_night").withStyle(ChatFormatting.LIGHT_PURPLE))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_total_night.desc")
            ))
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_TOTAL_NIGHT_CHANCE,
                step = 0.01
            , format = ModUtilities.percentFormat()))
            .option(serverInt(ModServerConfig.BIOLUMINESCENCE_NIGHT_START_TICK, step = 100, format = durationFormat))
            .option(serverInt(ModServerConfig.BIOLUMINESCENCE_NIGHT_END_TICK, step = 100, format = durationFormat))
            .build())
    }
}
