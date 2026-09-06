package fr.heta__h.squ_abyssal_bloom.config.submenu

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import fr.heta__h.squ_abyssal_bloom.config.renderer.EntityConfigRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverBool
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverDouble
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverInt
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object BrineSubMenu {

    fun previewRenderer(): EntityConfigRenderer =
        EntityConfigRenderer(entityType = ModEntities.BRINE.get()) { entity, tick ->
            if (entity is BrineEntity) {
                entity.idleAnimationState.startIfStopped(tick)
            }
        }

    fun buildGroups(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("entity.squ_abyssal_bloom.brine").copy().withStyle(ChatFormatting.BLUE))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.brine.desc"))
                .customImage(previewRenderer())
                .build())
            .option(serverBool(ModServerConfig.BRINE_NATURAL_SPAWNING))
            .option(serverDouble(ModServerConfig.BRINE_DETECTION_RANGE, step = 1.0, format = ModUtilities.blocksFormatDouble()))
            .option(serverDouble(ModServerConfig.BRINE_FOLLOW_SPEED, step = 0.01))
            .option(serverDouble(ModServerConfig.BRINE_ATTACK_SPEED, step = 0.01))
            .option(serverDouble(ModServerConfig.BRINE_ATTACK_ENTER_RADIUS, step = 0.25, format = ModUtilities.blocksFormatDouble()))
            .option(serverDouble(ModServerConfig.BRINE_ATTACK_EXIT_RADIUS, step = 0.25, format = ModUtilities.blocksFormatDouble()))
            .option(serverInt(ModServerConfig.BRINE_COLUMN_ATTACK_COOLDOWN, step = 5, format = ModUtilities.ticksFormat()))
            .option(serverInt(ModServerConfig.BRINE_DIRECT_ATTACK_COOLDOWN, step = 5, format = ModUtilities.ticksFormat()))
            .build())
    }
}
