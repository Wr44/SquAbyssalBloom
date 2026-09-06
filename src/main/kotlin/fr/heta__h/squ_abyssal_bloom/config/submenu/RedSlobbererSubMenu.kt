package fr.heta__h.squ_abyssal_bloom.config.submenu

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import fr.heta__h.squ_abyssal_bloom.config.renderer.EntityConfigRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverBool
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverDouble
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverInt
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object RedSlobbererSubMenu {

    fun previewRenderer(): EntityConfigRenderer =
        EntityConfigRenderer(entityType = ModEntities.RED_SLOBBERER.get()) { entity, tick ->
            if (entity is RedSlobbererEntity) {
                entity.idleAnimationState.startIfStopped(tick)
            }
        }

    fun buildGroups(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("entity.squ_abyssal_bloom.red_slobberer").copy().withStyle(ChatFormatting.DARK_RED))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.red_slobberer.desc"))
                .customImage(previewRenderer())
                .build())
            .option(serverBool(ModServerConfig.RED_SLOBBERER_SPAWN_ENABLED))
            .option(serverBool(ModServerConfig.RED_SLOBBERER_REEF_DEBUG))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_REEF_MATURITY_TICKS, step = 200, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_MAX_REEF_FISH, format = ModUtilities.unitInt("fish")))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_DEPOSIT_MIN_INTERVAL_TICKS, step = 20, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_DEPOSIT_MAX_INTERVAL_TICKS, step = 20, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_MAX_CALCAREOUS_DEPOSITS, format = ModUtilities.unitInt("deposits")))
            .option(serverDouble(ModServerConfig.RED_SLOBBERER_MINIMUM_REEF_STABILITY, step = 0.01, format = ModUtilities.percentFormat()))
            .option(serverDouble(ModServerConfig.RED_SLOBBERER_DEPOSIT_GROWTH_CHANCE, step = 0.005, format = ModUtilities.percentFormat()))
            .option(serverDouble(ModServerConfig.RED_SLOBBERER_NEW_DEPOSIT_CHANCE, step = 0.005, format = ModUtilities.percentFormat()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_DECORATION_MIN_INTERVAL_TICKS, step = 20, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_DECORATION_MAX_INTERVAL_TICKS, step = 20, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_MAX_DECORATIONS, format = ModUtilities.unitInt("decorations")))
            .option(serverDouble(ModServerConfig.RED_SLOBBERER_DECORATION_CHANCE, step = 0.005, format = ModUtilities.percentFormat()))
            .option(serverDouble(ModServerConfig.RED_SLOBBERER_FISH_REFUGE_RADIUS, step = 0.5, format = ModUtilities.blocksFormatDouble()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_FISH_REFUGE_CAPACITY, format = ModUtilities.unitInt("fish")))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_FISH_REFUGE_MINIMUM_STAY_TICKS, step = 20, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_FISH_REFUGE_MAXIMUM_STAY_TICKS, step = 20, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_FISH_REFUGE_QUIET_RELEASE_TICKS, step = 20, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.RED_SLOBBERER_FISH_REFUGE_UNSAFE_COOLDOWN_TICKS, step = 20, format = ModUtilities.ticksFormat()))
            .option(serverDouble(ModServerConfig.RED_SLOBBERER_REEF_RESIDENCE_RADIUS, step = 0.5, format = ModUtilities.blocksFormatDouble()))
            .build())
    }
}
