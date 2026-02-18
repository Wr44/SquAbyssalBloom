package fr.heta__h.squ_abyssal_bloom.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.util.EntityConfigRenderer
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.LivingEntity
import java.io.File


object ModConfig {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile = File(Minecraft.getInstance().gameDirectory, "config/squ_abyssal_bloom.json")

    var enableAbyssFog: Boolean = true
    var abyssDepthStart: Double = 15.0
    var abyssMaxDepth: Double = 80.0
    var maxMarinSnowParticles: Int = 100
    var strictBarnacleSpawning: Boolean = true

    private fun getEntityToRender(entityName: String): LivingEntity? {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return null

        return when (entityName) {
            "barnacle" -> {
                ModEntities.BARNACLE.get().create(level,
                    EntitySpawnReason.TRIGGERED)
            }

            "ghost_chimaera" -> {
                ModEntities.GHOST_CHIMAERA.get().create(level,
                    EntitySpawnReason.TRIGGERED)
            }

            else -> {
                null
            }
        }
    }

    fun loadConfig() {
        if (!configFile.exists()) {
            saveConfig()
            return
        }
        try {
            val json = gson.fromJson(configFile.readText(), JsonObject::class.java)
            if (json != null) {
                enableAbyssFog = json.get("enableAbyssFog")?.asBoolean ?: true
                abyssDepthStart = json.get("abyssDepthStart")?.asDouble ?: 15.0
                abyssMaxDepth = json.get("abyssMaxDepth")?.asDouble ?: 80.0
                maxMarinSnowParticles = json.get("maxMarinSnowParticles")?.asInt ?: 100
                strictBarnacleSpawning = json.get("strictBarnacleSpawning")?.asBoolean ?: true
            }
        } catch (e: Exception) {
            println("Erreur config : ${e.message}")
        }
    }

    fun saveConfig() {
        try {
            val json = JsonObject().apply {
                addProperty("enableAbyssFog", enableAbyssFog)
                addProperty("abyssDepthStart", abyssDepthStart)
                addProperty("abyssMaxDepth", abyssMaxDepth)
                addProperty("maxMarinSnowParticles", maxMarinSnowParticles)
                addProperty("strictBarnacleSpawning", strictBarnacleSpawning)
            }
            configFile.parentFile?.mkdirs()
            configFile.writeText(gson.toJson(json))
        } catch (e: Exception) {
            println("Erreur save config : ${e.message}")
        }
    }

    fun createConfigScreen(parent: Screen): Screen {

        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("config.squ_abyssal_bloom.category").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA))
            .save(this::saveConfig)


            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.visual"))
                .tooltip(Component.translatable("config.squ_abyssal_bloom.visual.tooltip"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.fog").withStyle(ChatFormatting.AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.fog.desc"))
                        .build())

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.enableAbyssFog"))
                        .binding(Binding.generic(true, { enableAbyssFog }, { enableAbyssFog = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.abyssDepthStart"))
                        .binding(Binding.generic(15.0, { abyssDepthStart }, { abyssDepthStart = it }))
                        .controller { opt ->
                            DoubleSliderControllerBuilder.create(opt)
                                .range(0.0, 100.0)
                                .step(1.0)
                                .formatValue { v -> Component.literal(String.format("%.0f blocs", v)) }
                        }
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.abyssMaxDepth"))
                        .binding(Binding.generic(80.0, { abyssMaxDepth }, { abyssMaxDepth = it }))
                        .controller { opt ->
                            DoubleSliderControllerBuilder.create(opt)
                                .range(10.0, 200.0)
                                .step(1.0)
                                .formatValue { v -> Component.literal(String.format("%.0f blocs", v)) }
                        }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.particles").withStyle(ChatFormatting.GOLD))
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.maxMarinSnowParticles"))
                        .binding(Binding.generic(100, { maxMarinSnowParticles }, { maxMarinSnowParticles = it }))
                        .controller { opt ->
                            IntegerSliderControllerBuilder.create(opt)
                                .range(0, 500)
                                .step(10)
                                .formatValue { v -> Component.literal("$v part.") }
                        }
                        .build())
                    .build())
                .build())


            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.entity"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("entity.squ_abyssal_bloom.barnacle").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.barnacle.desc"))
                        .customImage(EntityConfigRenderer { getEntityToRender("barnacle") })
                        .build())

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.strictBarnacleSpawning"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.strictBarnacleSpawning.tooltip")))
                        .binding(Binding.generic(true, { strictBarnacleSpawning }, { strictBarnacleSpawning = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("entity.squ_abyssal_bloom.ghost_chimaera").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.ghost_chimaera.desc"))
                        .customImage(EntityConfigRenderer { getEntityToRender("ghost_chimaera") })
                        .build())
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.template"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.template")))
                        .binding(Binding.generic(false, { false }, {  }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .build())

                .build())

            .build()
            .generateScreen(parent)
    }
}