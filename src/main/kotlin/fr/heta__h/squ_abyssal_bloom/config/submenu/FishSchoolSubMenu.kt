package fr.heta__h.squ_abyssal_bloom.config.submenu

import dev.isxander.yacl3.api.ButtonOption
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.gui.YACLScreen
import fr.heta__h.squ_abyssal_bloom.config.renderer.StaticImageRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.config.subunit.EntityListManagerScreens
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverBool
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverDouble
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverInt
import fr.heta__h.squ_abyssal_bloom.util.cache.AbstractFishTypeCache
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object FishSchoolSubMenu {

    fun buildGroups(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_general").withStyle(ChatFormatting.AQUA))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_general.desc"))
                .customImage(StaticImageRenderer("fish_school", 1920, 991))
                .build())
            .option(serverBool(ModServerConfig.FISH_SCHOOL_ENABLED))
            .option(serverBool(ModServerConfig.FISH_SCHOOL_DEBUG))
            .option(ButtonOption.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.fishSchoolFriendlyEntitiesManager"))
                .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.fishSchoolFriendlyEntitiesManager.desc")))
                .text(Component.translatable("config.squ_abyssal_bloom.entityListManager.button"))
                .action { screen: YACLScreen ->
                    EntityListManagerScreens.open(
                        screen,
                        ModServerConfig.FISH_SCHOOL_FRIENDLY_ENTITIES,
                        Component.translatable("config.squ_abyssal_bloom.fishSchoolFriendlyEntitiesManager"),
                        excludeTag = ModTags.EntityTypes.FISH_SCHOOL_FRIENDLY,
                        additionalFilter = { entityType -> !AbstractFishTypeCache.isAbstractFish(entityType) }
                    )
                }
                .build())
            .option(serverInt(ModServerConfig.FISH_SCHOOL_NEIGHBOR_COUNT) { Component.literal("$it poissons") })
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_NEIGHBOR_SEARCH_RADIUS, step = 0.5, format = ModUtilities.blocksFormatDouble()))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_AGGREGATION_RADIUS, step = 0.5, format = ModUtilities.blocksFormatDouble()))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_CROSS_SPECIES_AFFINITY, step = 0.01, format = ModUtilities.percentFormat()))
            .option(serverInt(ModServerConfig.FISH_SCHOOL_NEIGHBOR_REFRESH_INTERVAL, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.FISH_SCHOOL_THREAT_REFRESH_INTERVAL, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.FISH_SCHOOL_LONG_RANGE_REFRESH_INTERVAL, format = ModUtilities.ticksFormat()))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_NEIGHBOR_FOV_HALF_ANGLE, step = 5.0) { Component.literal(String.format("%.0f°", it)) })
            .build())

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_activation").withStyle(ChatFormatting.BLUE))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_activation.desc"))
                .build())
            .option(serverInt(ModServerConfig.FISH_SCHOOL_ACTIVATION_THRESHOLD) { Component.literal("$it poissons") })
            .option(serverInt(ModServerConfig.FISH_SCHOOL_DEACTIVATION_THRESHOLD) { Component.literal("$it poissons") })
            .option(serverInt(ModServerConfig.FISH_SCHOOL_ACTIVATION_DELAY, step = 20, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.FISH_SCHOOL_DEACTIVATION_DELAY, step = 20, format = ModUtilities.ticksFormat()))
            .build())

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_cohesion").withStyle(ChatFormatting.DARK_AQUA))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_cohesion.desc"))
                .build())
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_SEPARATION_WEIGHT, step = 0.05))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_ALIGNMENT_WEIGHT, step = 0.05))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_COHESION_WEIGHT, step = 0.05))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_OBSTACLE_AVOIDANCE_WEIGHT, step = 0.05))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_AGGREGATION_COHESION_SCALE, step = 0.05))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_COHESION_SPEED_RESPONSE_SCALE, step = 0.05))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_SEPARATION_RADIUS, step = 0.25, format = ModUtilities.blocksFormatDouble()))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_MAXIMUM_SEPARATION_FORCE, step = 0.1))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_COHESION_FULL_STRENGTH_DISTANCE, step = 0.25, format = ModUtilities.blocksFormatDouble()))
            .build())

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_threat").withStyle(ChatFormatting.DARK_RED))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_threat.desc"))
                .build())
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_THREAT_AVOIDANCE_WEIGHT, step = 0.05))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_THREAT_DETECTION_RADIUS, step = 0.5, format = ModUtilities.blocksFormatDouble()))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_THREAT_PROPAGATION_SPEED, step = 0.01, format = ModUtilities.percentFormat()))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_THREAT_SIGNAL_DECAY, step = 0.001))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_HERD_COMPRESSION, step = 0.05))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_PANIC_SPEED, step = 0.01))
            .build())

        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_speed").withStyle(ChatFormatting.GOLD))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.group.fish_schools_speed.desc"))
                .build())
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_MAXIMUM_SPEED, step = 0.01))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_MAXIMUM_TURN_RATE, step = 0.5) { Component.literal(String.format("%.1f°/tick", it)) })
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_VERTICAL_MOVEMENT_WEIGHT, step = 0.01))
            .option(serverDouble(ModServerConfig.FISH_SCHOOL_VERTICAL_DRIFT_SPEED, step = 0.001))
            .build())
    }
}
