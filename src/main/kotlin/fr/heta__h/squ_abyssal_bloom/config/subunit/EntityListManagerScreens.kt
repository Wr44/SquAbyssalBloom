package fr.heta__h.squ_abyssal_bloom.config.subunit

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.renderer.EntityConfigRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.config.server.types.StringListOption
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.persistServerConfigChanges
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.DefaultAttributes

object EntityListManagerScreens {

    fun open(
        parent: Screen,
        listOption: StringListOption,
        title: Component,
        excludeTag: TagKey<EntityType<*>>? = null,
        additionalFilter: (String) -> Boolean = { true }
    ) {
        val initialServerConfig = ServerConfigCache.toData()

        val categoryBuilder = ConfigCategory.createBuilder().name(title)

        BuiltInRegistries.ENTITY_TYPE.forEach { entityType ->
            try {
                val idString = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString()
                if (!additionalFilter(idString)) return@forEach

                if (!DefaultAttributes.hasSupplier(entityType)) return@forEach
                if (excludeTag != null && entityType.builtInRegistryHolder().`is`(excludeTag)) return@forEach

                @Suppress("UNCHECKED_CAST")
                val livingType = entityType as EntityType<out LivingEntity>

                categoryBuilder.group(
                    OptionGroup.createBuilder()
                        .name(Component.translatable(entityType.descriptionId))
                        .description(
                            OptionDescription.createBuilder()
                                .customImage(EntityConfigRenderer(livingType))
                                .build()
                        )
                        .option(
                            Option.createBuilder<Boolean>()
                                .name(Component.translatable("config.squ_abyssal_bloom.entityListManager.include"))
                                .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.entityListManager.include.desc")))
                                .binding(
                                    Binding.generic(
                                        listOption.default.contains(idString),
                                        { ServerConfigCache.current(listOption).contains(idString) },
                                        { included ->
                                            val current = ServerConfigCache.current(listOption).toMutableList()
                                            if (included) {
                                                if (idString !in current) current.add(idString)
                                            } else {
                                                current.remove(idString)
                                            }
                                            ServerConfigCache.set(listOption, current)
                                        }
                                    )
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )
                        .build()
                )
            } catch (e: Exception) {
                SquAbyssalBloom.LOGGER.warn("Skipping entity type {} in entity list manager screen", entityType, e)
            }
        }

        try {
            val screen = YetAnotherConfigLib.createBuilder()
                .title(title)
                .save { persistServerConfigChanges(initialServerConfig) }
                .category(categoryBuilder.build())
                .build()
                .generateScreen(parent)

            Minecraft.getInstance().setScreen(screen)
        } catch (e: Exception) {
            SquAbyssalBloom.LOGGER.error("Failed to open entity list manager screen", e)
        }
    }
}
