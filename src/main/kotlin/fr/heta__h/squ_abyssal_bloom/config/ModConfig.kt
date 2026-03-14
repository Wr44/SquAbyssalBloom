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
    var fogDarknessIntensity: Double = 1.0
    var shaderCompatModeOverride: Boolean = false
    var nautilusLampInfluence: Double = 0.5
    var marineSnowDensity: Double = 1.0
    var marineSnowVisibilityRange: Int = 20
    var marineSnowSwayAmplitude: Double = 1.0
    var marineSnowSpawnHeightAbove: Double = 2.0
    var enableDepthVignette: Boolean = true
    var vignetteIntensity: Double = 1.0
    var lightDimmingStrength: Double = 0.7
    var enableNautilusLampFog: Boolean = true
    var nautilusLampFogStart: Double = 3.0
    var nautilusLampFogEnd: Double = 22.0
    var nautilusLampGivesWaterBreathing: Boolean = true
    var enableExteriorMurkiness: Boolean = true
    var exteriorMurkinessIntensity: Double = 1.0
    var exteriorMurkinessMaxHeight: Double = 24.0

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
                fogDarknessIntensity = json.get("fogDarknessIntensity")?.asDouble ?: 1.0
                shaderCompatModeOverride = json.get("shaderCompatModeOverride")?.asBoolean ?: false
                nautilusLampInfluence = json.get("nautilusLampInfluence")?.asDouble ?: 0.5
                marineSnowDensity = json.get("marineSnowDensity")?.asDouble ?: 1.0
                marineSnowVisibilityRange = json.get("marineSnowVisibilityRange")?.asInt ?: 20
                marineSnowSwayAmplitude = json.get("marineSnowSwayAmplitude")?.asDouble ?: 1.0
                marineSnowSpawnHeightAbove = json.get("marineSnowSpawnHeightAbove")?.asDouble ?: 2.0
                enableDepthVignette = json.get("enableDepthVignette")?.asBoolean ?: true
                vignetteIntensity = json.get("vignetteIntensity")?.asDouble ?: 1.0
                lightDimmingStrength = json.get("lightDimmingStrength")?.asDouble ?: 0.7
                enableNautilusLampFog = json.get("enableNautilusLampFog")?.asBoolean ?: true
                nautilusLampFogStart = json.get("nautilusLampFogStart")?.asDouble ?: 3.0
                nautilusLampFogEnd = json.get("nautilusLampFogEnd")?.asDouble ?: 22.0
                nautilusLampGivesWaterBreathing = json.get("nautilusLampGivesWaterBreathing")?.asBoolean ?: true
                enableExteriorMurkiness = json.get("enableExteriorMurkiness")?.asBoolean ?: true
                exteriorMurkinessIntensity = json.get("exteriorMurkinessIntensity")?.asDouble ?: 1.0
                exteriorMurkinessMaxHeight = json.get("exteriorMurkinessMaxHeight")?.asDouble ?: 24.0
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
                addProperty("fogDarknessIntensity", fogDarknessIntensity)
                addProperty("shaderCompatModeOverride", shaderCompatModeOverride)
                addProperty("nautilusLampInfluence", nautilusLampInfluence)
                addProperty("marineSnowDensity", marineSnowDensity)
                addProperty("marineSnowVisibilityRange", marineSnowVisibilityRange)
                addProperty("marineSnowSwayAmplitude", marineSnowSwayAmplitude)
                addProperty("marineSnowSpawnHeightAbove", marineSnowSpawnHeightAbove)
                addProperty("enableDepthVignette", enableDepthVignette)
                addProperty("vignetteIntensity", vignetteIntensity)
                addProperty("lightDimmingStrength", lightDimmingStrength)
                addProperty("enableNautilusLampFog", enableNautilusLampFog)
                addProperty("nautilusLampFogStart", nautilusLampFogStart)
                addProperty("nautilusLampFogEnd", nautilusLampFogEnd)
                addProperty("nautilusLampGivesWaterBreathing", nautilusLampGivesWaterBreathing)
                addProperty("enableExteriorMurkiness", enableExteriorMurkiness)
                addProperty("exteriorMurkinessIntensity", exteriorMurkinessIntensity)
                addProperty("exteriorMurkinessMaxHeight", exteriorMurkinessMaxHeight)
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

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.fogDarknessIntensity"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.fogDarknessIntensity.tooltip")))
                        .binding(Binding.generic(1.0, { fogDarknessIntensity }, { fogDarknessIntensity = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.shaderCompatModeOverride"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shaderCompatModeOverride.tooltip")))
                        .binding(Binding.generic(false, { shaderCompatModeOverride }, { shaderCompatModeOverride = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.nautilusLampInfluence"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.nautilusLampInfluence.tooltip")))
                        .binding(Binding.generic(0.5, { nautilusLampInfluence }, { nautilusLampInfluence = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05) }
                        .build())

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.enableDepthVignette"))
                        .binding(Binding.generic(true, { enableDepthVignette }, { enableDepthVignette = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.vignetteIntensity"))
                        .binding(Binding.generic(1.0, { vignetteIntensity }, { vignetteIntensity = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.lightDimmingStrength"))
                        .binding(Binding.generic(0.7, { lightDimmingStrength }, { lightDimmingStrength = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.nautilusLamp").withStyle(ChatFormatting.YELLOW))

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.enableNautilusLampFog"))
                        .binding(Binding.generic(true, { enableNautilusLampFog }, { enableNautilusLampFog = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.nautilusLampFogStart"))
                        .binding(Binding.generic(3.0, { nautilusLampFogStart }, { nautilusLampFogStart = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 20.0).step(0.5) }
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.nautilusLampFogEnd"))
                        .binding(Binding.generic(22.0, { nautilusLampFogEnd }, { nautilusLampFogEnd = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(5.0, 50.0).step(1.0) }
                        .build())

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.nautilusLampGivesWaterBreathing"))
                        .binding(Binding.generic(true, { nautilusLampGivesWaterBreathing }, { nautilusLampGivesWaterBreathing = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.exteriorWater").withStyle(ChatFormatting.BLUE))

                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.enableExteriorMurkiness"))
                        .binding(Binding.generic(true, { enableExteriorMurkiness }, { enableExteriorMurkiness = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.exteriorMurkinessIntensity"))
                        .binding(Binding.generic(1.0, { exteriorMurkinessIntensity }, { exteriorMurkinessIntensity = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.exteriorMurkinessMaxHeight"))
                        .binding(Binding.generic(24.0, { exteriorMurkinessMaxHeight }, { exteriorMurkinessMaxHeight = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 64.0).step(1.0) }
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

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.marineSnowDensity"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.marineSnowDensity.tooltip")))
                        .binding(Binding.generic(1.0, { marineSnowDensity }, { marineSnowDensity = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 3.0).step(0.1) }
                        .build())

                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.marineSnowVisibilityRange"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.marineSnowVisibilityRange.tooltip")))
                        .binding(Binding.generic(20, { marineSnowVisibilityRange }, { marineSnowVisibilityRange = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(5, 50).step(5) }
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.marineSnowSwayAmplitude"))
                        .binding(Binding.generic(1.0, { marineSnowSwayAmplitude }, { marineSnowSwayAmplitude = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())

                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.marineSnowSpawnHeightAbove"))
                        .binding(Binding.generic(2.0, { marineSnowSpawnHeightAbove }, { marineSnowSpawnHeightAbove = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 20.0).step(0.5) }
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