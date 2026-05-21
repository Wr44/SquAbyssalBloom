package fr.heta__h.squ_abyssal_bloom.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.neoforged.fml.loading.FMLPaths
import java.io.File

object ModConfig {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FMLPaths.CONFIGDIR.get().resolve("squ_abyssal_bloom.json").toFile()

    var enableAbyssFog: Boolean = true
    var abyssDepthStart: Double = 30.0
    var abyssMaxDepth: Double = 80.0
    var maxMarinSnowParticles: Int = 75
    var strictBarnacleSpawning: Boolean = true
    var fogDarknessIntensity: Double = 1.0
    var shaderCompatModeOverride: Boolean = false
    var nautilusLampInfluence: Double = 0.5
    var marineSnowDensity: Double = 0.5
    var marineSnowVisibilityRange: Int = 20
    var marineSnowSwayAmplitude: Double = 1.0
    var marineSnowSpawnHeightAbove: Double = 2.0
    var enableDepthVignette: Boolean = true
    var vignetteIntensity: Double = 1.0
    var lightDimmingStrength: Double = 0.7
    var abyssColorRetention: Double = 0.15
    var lampNearPlaneMultiplier: Double = 12.0
    var lampFarPlaneMultiplier: Double = 60.0
    var conduitBoundaryTriggerDistance: Double = 5.0
    var conduitBoundaryRingRadius: Double = 2.5
    var conduitBoundaryRingPoints: Int = 24
    var conduitBoundaryParticleInterval: Int = 3
    var conduitBoundaryParticlesEnabled: Boolean = true

    fun loadConfig() {
        if (!configFile.exists()) { saveConfig(); return }
        try {
            val json = gson.fromJson(configFile.readText(), JsonObject::class.java) ?: return
            enableAbyssFog = json.get("enableAbyssFog")?.asBoolean ?: true
            abyssDepthStart = json.get("abyssDepthStart")?.asDouble ?: 30.0
            abyssMaxDepth = json.get("abyssMaxDepth")?.asDouble ?: 80.0
            maxMarinSnowParticles = json.get("maxMarinSnowParticles")?.asInt ?: 75
            strictBarnacleSpawning = json.get("strictBarnacleSpawning")?.asBoolean ?: true
            fogDarknessIntensity = json.get("fogDarknessIntensity")?.asDouble ?: 1.0
            shaderCompatModeOverride = json.get("shaderCompatModeOverride")?.asBoolean ?: false
            nautilusLampInfluence = json.get("nautilusLampInfluence")?.asDouble ?: 0.5
            marineSnowDensity = json.get("marineSnowDensity")?.asDouble ?: 0.5
            marineSnowVisibilityRange = json.get("marineSnowVisibilityRange")?.asInt ?: 20
            marineSnowSwayAmplitude = json.get("marineSnowSwayAmplitude")?.asDouble ?: 1.0
            marineSnowSpawnHeightAbove = json.get("marineSnowSpawnHeightAbove")?.asDouble ?: 2.0
            enableDepthVignette = json.get("enableDepthVignette")?.asBoolean ?: true
            vignetteIntensity = json.get("vignetteIntensity")?.asDouble ?: 1.0
            lightDimmingStrength = json.get("lightDimmingStrength")?.asDouble ?: 0.7
            abyssColorRetention = json.get("abyssColorRetention")?.asDouble ?: 0.15
            lampNearPlaneMultiplier = json.get("lampNearPlaneMultiplier")?.asDouble ?: 12.0
            lampFarPlaneMultiplier = json.get("lampFarPlaneMultiplier")?.asDouble ?: 60.0
            conduitBoundaryTriggerDistance = json.get("conduitBoundaryTriggerDistance")?.asDouble ?: 5.0
            conduitBoundaryRingRadius = json.get("conduitBoundaryRingRadius")?.asDouble ?: 2.5
            conduitBoundaryRingPoints = json.get("conduitBoundaryRingPoints")?.asInt ?: 24
            conduitBoundaryParticleInterval = json.get("conduitBoundaryParticleInterval")?.asInt ?: 3
            conduitBoundaryParticlesEnabled = json.get("conduitBoundaryParticlesEnabled")?.asBoolean ?: true
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
                addProperty("abyssColorRetention", abyssColorRetention)
                addProperty("lampNearPlaneMultiplier", lampNearPlaneMultiplier)
                addProperty("lampFarPlaneMultiplier", lampFarPlaneMultiplier)
                addProperty("conduitBoundaryTriggerDistance", conduitBoundaryTriggerDistance)
                addProperty("conduitBoundaryRingRadius", conduitBoundaryRingRadius)
                addProperty("conduitBoundaryRingPoints", conduitBoundaryRingPoints)
                addProperty("conduitBoundaryParticleInterval", conduitBoundaryParticleInterval)
                addProperty("conduitBoundaryParticlesEnabled", conduitBoundaryParticlesEnabled)
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
                        .binding(Binding.generic(30.0, { abyssDepthStart }, { abyssDepthStart = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 100.0).step(1.0).formatValue { v -> Component.literal(String.format("%.0f blocs", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.abyssMaxDepth"))
                        .binding(Binding.generic(80.0, { abyssMaxDepth }, { abyssMaxDepth = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(10.0, 200.0).step(1.0).formatValue { v -> Component.literal(String.format("%.0f blocs", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.fogDarknessIntensity"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.fogDarknessIntensity.tooltip")))
                        .binding(Binding.generic(1.0, { fogDarknessIntensity }, { fogDarknessIntensity = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.abyssColorRetention"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.abyssColorRetention.tooltip")))
                        .binding(Binding.generic(0.15, { abyssColorRetention }, { abyssColorRetention = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05) }
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
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.lampNearPlaneMultiplier"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.lampNearPlaneMultiplier.tooltip")))
                        .binding(Binding.generic(12.0, { lampNearPlaneMultiplier }, { lampNearPlaneMultiplier = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 50.0).step(1.0) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.lampFarPlaneMultiplier"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.lampFarPlaneMultiplier.tooltip")))
                        .binding(Binding.generic(60.0, { lampFarPlaneMultiplier }, { lampFarPlaneMultiplier = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 200.0).step(5.0) }
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
                    .name(Component.translatable("config.squ_abyssal_bloom.group.particles").withStyle(ChatFormatting.GOLD))
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.maxMarinSnowParticles"))
                        .binding(Binding.generic(100, { maxMarinSnowParticles }, { maxMarinSnowParticles = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(0, 500).step(10).formatValue { v -> Component.literal("$v part.") } }
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

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.conduit").withStyle(ChatFormatting.DARK_AQUA))
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryParticlesEnabled"))
                        .binding(Binding.generic(true, { conduitBoundaryParticlesEnabled }, { conduitBoundaryParticlesEnabled = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryTriggerDistance"))
                        .binding(Binding.generic(5.0, { conduitBoundaryTriggerDistance }, { conduitBoundaryTriggerDistance = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(1.0, 20.0).step(0.5) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingRadius"))
                        .binding(Binding.generic(2.5, { conduitBoundaryRingRadius }, { conduitBoundaryRingRadius = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.5, 10.0).step(0.5) }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingPoints"))
                        .binding(Binding.generic(24, { conduitBoundaryRingPoints }, { conduitBoundaryRingPoints = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(4, 64).step(4) }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryParticleInterval"))
                        .binding(Binding.generic(3, { conduitBoundaryParticleInterval }, { conduitBoundaryParticleInterval = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(1, 20).step(1) }
                        .build())
                    .build())

                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.entity"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("entity.squ_abyssal_bloom.barnacle").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.barnacle.desc"))
                        .customImage(EntityConfigRenderer(entityType = ModEntities.BARNACLE.get()) { entity, tick ->
                            if (entity is BarnacleEntity) {
                                val durationTicks = (BarnacleAnimation.still_mouth_open.lengthInSeconds * 20).toInt()
                                val loopTick = tick % durationTicks
                                if (loopTick == 0 || !entity.stillMouthOpenAnimationState.isStarted)
                                    entity.stillMouthOpenAnimationState.start(tick)
                            }
                        })
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
                        .customImage(EntityConfigRenderer(entityType = ModEntities.GHOST_CHIMAERA.get()))
                        .build())
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.template"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.template")))
                        .binding(Binding.generic(false, { false }, {}))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())

                .build())

            .build()
            .generateScreen(parent)
    }
}