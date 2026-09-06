package fr.heta__h.squ_abyssal_bloom.config.submenu

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import fr.heta__h.squ_abyssal_bloom.config.renderer.EntityConfigRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.CrystalJellyEntity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.bindOrderedPair
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverBool
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverDouble
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverInt
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object CrystalJellySubMenu {

    private val countFormat: (Int) -> Component = ModUtilities.unitInt("jellies")
    private val pulseFormat: (Int) -> Component = ModUtilities.unitInt("pulses")

    fun previewRenderer(): EntityConfigRenderer =
        EntityConfigRenderer(entityType = ModEntities.CRYSTAL_JELLY.get()) { entity, tick ->
            if (entity is CrystalJellyEntity) {
                entity.idleAnimationState.startIfStopped(tick)
            }
        }

    fun buildGroups(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("entity.squ_abyssal_bloom.crystal_jelly").copy().withStyle(ChatFormatting.AQUA))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.crystal_jelly.desc"))
                .customImage(previewRenderer())
                .build())
            .option(serverBool(ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_ENABLED))
            .build())

        buildPopulationGroup(category)
        buildPropulsionGroup(category)
        buildDriftGroup(category)
        buildGelGroup(category)
        buildDiverPropulsionGroup(category)
    }

    private fun buildPopulationGroup(category: ConfigCategory.Builder) {
        val smallMinimum = serverInt(ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_SMALL_MIN_COUNT, format = countFormat)
        val smallMaximum = serverInt(ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_SMALL_MAX_COUNT, format = countFormat)
        val largeMinimum = serverInt(ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_LARGE_MIN_COUNT, format = countFormat)
        val largeMaximum = serverInt(ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_LARGE_MAX_COUNT, format = countFormat)

        bindOrderedPair(smallMinimum, smallMaximum)
        bindOrderedPair(largeMinimum, largeMaximum)

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.crystal_jelly_population").withStyle(ChatFormatting.DARK_AQUA))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.crystal_jelly_population.desc")
            ))
            .option(smallMinimum)
            .option(smallMaximum)
            .option(largeMinimum)
            .option(largeMaximum)
            .option(serverInt(
                ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_CELLS_PER_JELLY,
                step = 10,
                format = ModUtilities.unitInt("cells")
            ))
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_INACTIVE_MULTIPLIER,
                step = 0.05,
                format = ModUtilities.percentFormat()
            ))
            .option(serverDouble(
                ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_TOTAL_NIGHT_MULTIPLIER,
                step = 0.05,
                format = ModUtilities.percentFormat()
            ))
            .build())
    }

    private fun buildPropulsionGroup(category: ConfigCategory.Builder) {
        val minimumPulses = serverInt(ModServerConfig.CRYSTAL_JELLY_MIN_PULSES, format = pulseFormat)
        val maximumPulses = serverInt(ModServerConfig.CRYSTAL_JELLY_MAX_PULSES, format = pulseFormat)
        val minimumDistance = serverDouble(
            ModServerConfig.CRYSTAL_JELLY_MIN_PULSE_DISTANCE,
            step = 0.05,
            format = ModUtilities.blocksFormatDouble()
        )
        val maximumDistance = serverDouble(
            ModServerConfig.CRYSTAL_JELLY_MAX_PULSE_DISTANCE,
            step = 0.05,
            format = ModUtilities.blocksFormatDouble()
        )

        bindOrderedPair(minimumPulses, maximumPulses)
        bindOrderedPair(minimumDistance, maximumDistance)

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.crystal_jelly_propulsion").withStyle(ChatFormatting.BLUE))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.crystal_jelly_propulsion.desc")
            ))
            .option(minimumPulses)
            .option(maximumPulses)
            .option(minimumDistance)
            .option(maximumDistance)
            .option(serverDouble(
                ModServerConfig.CRYSTAL_JELLY_MAX_TILT_DEGREES,
                step = 1.0,
                format = ModUtilities.unitDouble("degrees", 0)
            ))
            .build())
    }

    private fun buildDriftGroup(category: ConfigCategory.Builder) {
        val minimumRetreat = serverDouble(
            ModServerConfig.CRYSTAL_JELLY_MIN_SURFACE_RETREAT,
            step = 0.5,
            format = ModUtilities.blocksFormatDouble()
        )
        val maximumRetreat = serverDouble(
            ModServerConfig.CRYSTAL_JELLY_MAX_SURFACE_RETREAT,
            step = 0.5,
            format = ModUtilities.blocksFormatDouble()
        )

        bindOrderedPair(minimumRetreat, maximumRetreat)

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.crystal_jelly_drift").withStyle(ChatFormatting.GRAY))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.crystal_jelly_drift.desc")
            ))
            .option(serverInt(
                ModServerConfig.CRYSTAL_JELLY_DRIFT_MEAN_TICKS,
                step = 10,
                format = ModUtilities.ticksFormat()
            ))
            .option(serverInt(
                ModServerConfig.CRYSTAL_JELLY_SURFACE_PROXIMITY,
                format = ModUtilities.blocksFormatInt()
            ))
            .option(minimumRetreat)
            .option(maximumRetreat)
            .build())
    }

    private fun buildGelGroup(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.crystal_jelly_bottle").withStyle(ChatFormatting.BLUE))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.crystal_jelly_bottle.desc")
            ))
            .option(serverBool(ModServerConfig.CRYSTAL_JELLY_BOTTLE_HARVEST_ENABLED))
            .option(serverInt(
                ModServerConfig.CRYSTAL_JELLY_BOTTLE_HARVEST_COOLDOWN,
                step = 100,
                format = ModUtilities.ticksFormat()
            ))
            .option(serverInt(ModServerConfig.CRYSTAL_JELLY_BOTTLE_FILLS_REQUIRED))
            .build())
    }

    private fun buildDiverPropulsionGroup(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.propulsion_effect").withStyle(ChatFormatting.AQUA))
            .description(OptionDescription.of(
                Component.translatable("config.squ_abyssal_bloom.group.propulsion_effect.desc")
            ))
            .option(serverInt(ModServerConfig.PROPULSION_CYCLE_TICKS, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.PROPULSION_CHARGE_TICKS, format = ModUtilities.ticksFormat()))
            .option(serverDouble(
                ModServerConfig.PROPULSION_BURST_SPEED,
                step = 0.01,
                format = ModUtilities.plainDouble(2)
            ))
            .option(serverDouble(
                ModServerConfig.PROPULSION_SPEED_DECAY,
                step = 0.1,
                format = ModUtilities.plainDouble(1)
            ))
            .option(serverDouble(
                ModServerConfig.PROPULSION_AMPLIFIER_SCALE,
                step = 0.1,
                format = ModUtilities.plainDouble(1)
            ))
            .build())
    }
}
