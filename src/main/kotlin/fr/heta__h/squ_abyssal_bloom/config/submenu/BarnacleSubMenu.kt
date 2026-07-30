package fr.heta__h.squ_abyssal_bloom.config.submenu

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import fr.heta__h.squ_abyssal_bloom.config.renderer.EntityConfigRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverBool
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverDouble
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverInt
import net.minecraft.network.chat.Component

object BarnacleSubMenu {

    fun previewRenderer(): EntityConfigRenderer =
        EntityConfigRenderer(entityType = ModEntities.BARNACLE.get()) { entity, tick ->
            if (entity is BarnacleEntity) {
                entity.stillMouthOpenAnimationState.startIfStopped(tick)
            }
        }

    fun buildGroups(category: ConfigCategory.Builder) {
        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("entity.squ_abyssal_bloom.barnacle"))
            .description(OptionDescription.createBuilder()
                .text(Component.translatable("config.squ_abyssal_bloom.barnacle.desc"))
                .customImage(previewRenderer())
                .build())
            .option(serverBool(ModServerConfig.BARNACLE_SPAWN_ENABLED))
            .option(serverBool(ModServerConfig.STRICT_BARNACLE_SPAWNING))
            .option(serverInt(ModServerConfig.BARNACLE_SPAWN_MAX_Y))
            .option(serverDouble(ModServerConfig.BARNACLE_DETECTION_RANGE, step = 1.0))
            .option(serverDouble(ModServerConfig.BARNACLE_MOVEMENT_SPEED, step = 0.1))
            .option(serverDouble(ModServerConfig.BARNACLE_OBSTACLE_AVOIDANCE_SPEED, step = 0.05))
            .option(serverDouble(ModServerConfig.BARNACLE_FLEE_SPEED, step = 0.1))
            .option(serverInt(ModServerConfig.BARNACLE_REGEN_COOLDOWN, step = 20))
            .option(serverDouble(ModServerConfig.BARNACLE_DIRECT_CORRIDOR_DISTANCE, step = 0.5))
            .option(serverDouble(ModServerConfig.BARNACLE_CAPTURE_DISTANCE, step = 0.25))
            .option(serverDouble(ModServerConfig.BARNACLE_HOLD_DISTANCE, step = 0.25))
            .build())
    }
}
