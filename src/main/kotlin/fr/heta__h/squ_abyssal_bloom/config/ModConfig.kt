package fr.heta__h.squ_abyssal_bloom.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.config.server.EntityConfigRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigData
import fr.heta__h.squ_abyssal_bloom.config.server.types.BoolOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.DoubleOption
import fr.heta__h.squ_abyssal_bloom.config.server.types.IntOption
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import fr.heta__h.squ_abyssal_bloom.entity.client.brine.BrineAnimation
import fr.heta__h.squ_abyssal_bloom.entity.client.red_slobberer.RedSlobbererAnimation
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import fr.heta__h.squ_abyssal_bloom.network.config.C2SServerConfigPacket
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import java.io.File

object ModConfig {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FMLPaths.CONFIGDIR.get().resolve("squ_abyssal_bloom.json").toFile()

    var enableAbyssFog: Boolean = true
    var abyssDepthStart: Double = 30.0
    var abyssMaxDepth: Double = 80.0
    var maxMarinSnowParticles: Int = 75
    var fogDarknessIntensity: Double = 1.0
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
    var enableSurfaceOccluder: Boolean = true
    var surfaceOccluderAlphaMin: Double = 0.05
    var surfaceOccluderAlphaMax: Double = 0.70
    var surfaceOccluderNumLayers: Int = 5
    var surfaceOccluderFadeSpeed: Double = 2.5
    var surfaceOccluderLodBandWidth: Int = 64
    var surfaceOccluderTargetDepth: Double = 0.5

    fun loadConfig() {
        if (!configFile.exists()) { saveConfig(); return }
        try {
            val json = gson.fromJson(configFile.readText(), JsonObject::class.java) ?: return
            enableAbyssFog = json.get("enableAbyssFog")?.asBoolean ?: true
            abyssDepthStart = json.get("abyssDepthStart")?.asDouble ?: 30.0
            abyssMaxDepth = json.get("abyssMaxDepth")?.asDouble ?: 80.0
            maxMarinSnowParticles = json.get("maxMarinSnowParticles")?.asInt ?: 75
            fogDarknessIntensity = json.get("fogDarknessIntensity")?.asDouble ?: 1.0
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
            enableSurfaceOccluder = json.get("enableSurfaceOccluder")?.asBoolean ?: true
            surfaceOccluderAlphaMin = json.get("surfaceOccluderAlphaMin")?.asDouble ?: 0.05
            surfaceOccluderAlphaMax = json.get("surfaceOccluderAlphaMax")?.asDouble ?: 0.70
            surfaceOccluderNumLayers = json.get("surfaceOccluderNumLayers")?.asInt ?: 5
            surfaceOccluderFadeSpeed = json.get("surfaceOccluderFadeSpeed")?.asDouble ?: 2.5
            surfaceOccluderLodBandWidth = json.get("surfaceOccluderLodBandWidth")?.asInt ?: 64
            surfaceOccluderTargetDepth = json.get("surfaceOccluderTargetDepth")?.asDouble ?: 0.5
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
                addProperty("fogDarknessIntensity", fogDarknessIntensity)
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
                addProperty("enableSurfaceOccluder", enableSurfaceOccluder)
                addProperty("surfaceOccluderAlphaMin", surfaceOccluderAlphaMin)
                addProperty("surfaceOccluderAlphaMax", surfaceOccluderAlphaMax)
                addProperty("surfaceOccluderNumLayers", surfaceOccluderNumLayers)
                addProperty("surfaceOccluderFadeSpeed", surfaceOccluderFadeSpeed)
                addProperty("surfaceOccluderLodBandWidth", surfaceOccluderLodBandWidth)
                addProperty("surfaceOccluderTargetDepth", surfaceOccluderTargetDepth)
            }
            configFile.parentFile?.mkdirs()
            configFile.writeText(gson.toJson(json))
        } catch (e: Exception) {
            println("Erreur save config : ${e.message}")
        }
    }

    fun createConfigScreen(parent: Screen): Screen {
        if (ServerConfigCache.isSingleplayer()) {
            ServerConfigCache.update(ServerConfigData.fromSpec())
        }

        val initialServerConfig = ServerConfigCache.toData()

        val depthStartOpt = Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.abyssDepthStart"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.abyssDepthStart.tooltip")))
            .binding(Binding.generic(30.0, { abyssDepthStart }, { abyssDepthStart = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 100.0).step(1.0).formatValue { v -> Component.literal(String.format("%.0f blocs", v)) } }
            .build()

        val depthMaxOpt = Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.abyssMaxDepth"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.abyssMaxDepth.tooltip")))
            .binding(Binding.generic(80.0, { abyssMaxDepth }, { abyssMaxDepth = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(10.0, 200.0).step(1.0).formatValue { v -> Component.literal(String.format("%.0f blocs", v)) } }
            .build()

        depthStartOpt.addListener { _, newStart ->
            if (depthMaxOpt.pendingValue() <= newStart) depthMaxOpt.requestSet(newStart + 1.0)
        }
        depthMaxOpt.addListener { _, newMax ->
            if (depthStartOpt.pendingValue() >= newMax) depthStartOpt.requestSet(newMax - 1.0)
        }

        val alphaMinOpt = Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderAlphaMin"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderAlphaMin.desc")))
            .binding(Binding.generic(0.05, { surfaceOccluderAlphaMin }, { surfaceOccluderAlphaMin = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 0.5).step(0.01) }
            .build()

        val alphaMaxOpt = Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderAlphaMax"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderAlphaMax.desc")))
            .binding(Binding.generic(0.70, { surfaceOccluderAlphaMax }, { surfaceOccluderAlphaMax = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.1, 1.0).step(0.01) }
            .build()

        alphaMinOpt.addListener { _, newMin ->
            if (alphaMaxOpt.pendingValue() <= newMin) alphaMaxOpt.requestSet(newMin + 0.01)
        }
        alphaMaxOpt.addListener { _, newMax ->
            if (alphaMinOpt.pendingValue() >= newMax) alphaMinOpt.requestSet(newMax - 0.01)
        }

        val shallowDeepOpt = serverDouble(ModServerConfig.SHALLOW_DEEP_BOUNDARY, -0.95..-0.21, 0.001) { Component.literal(String.format("%.3f", it)) }
        val deepAbyssalOpt = serverDouble(ModServerConfig.DEEP_ABYSSAL_BOUNDARY, -1.049..-0.30, 0.001) { Component.literal(String.format("%.3f", it)) }

        shallowDeepOpt.addListener { _, newShallow ->
            if (deepAbyssalOpt.pendingValue() >= newShallow) deepAbyssalOpt.requestSet(newShallow - 0.001)
        }
        deepAbyssalOpt.addListener { _, newDeep ->
            if (shallowDeepOpt.pendingValue() <= newDeep) shallowDeepOpt.requestSet(newDeep + 0.001)
        }

        val shallowFloorOpt = serverInt(ModServerConfig.SHALLOW_FLOOR_Y, -60..60) { Component.literal("Y=$it") }
        val deepFloorOpt = serverInt(ModServerConfig.DEEP_FLOOR_TARGET, -60..60) { Component.literal("Y=$it") }
        val abyssalFloorOpt = serverInt(ModServerConfig.TARGET_FLOOR_Y, -64..20) { Component.literal("Y=$it") }

        shallowFloorOpt.addListener { _, newShallow ->
            if (deepFloorOpt.pendingValue() >= newShallow) deepFloorOpt.requestSet(newShallow - 1)
        }
        deepFloorOpt.addListener { _, newDeep ->
            if (shallowFloorOpt.pendingValue() <= newDeep) shallowFloorOpt.requestSet(newDeep + 1)
            if (abyssalFloorOpt.pendingValue() >= newDeep) abyssalFloorOpt.requestSet(newDeep - 1)
        }
        abyssalFloorOpt.addListener { _, newAbyssal ->
            if (deepFloorOpt.pendingValue() <= newAbyssal) deepFloorOpt.requestSet(newAbyssal + 1)
        }

        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("config.squ_abyssal_bloom.category").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA))
            .save {
                saveConfig()
                if (ServerConfigCache.isSingleplayer()) {
                    ServerConfigCache.toData().applyToSpec()
                } else {
                    val currentServerConfig = ServerConfigCache.toData()
                    if (currentServerConfig != initialServerConfig && Minecraft.getInstance().connection != null) {
                        ClientPacketDistributor.sendToServer(C2SServerConfigPacket(currentServerConfig))
                    }
                }
            }

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
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.enableAbyssFog.tooltip")))
                        .binding(Binding.generic(true, { enableAbyssFog }, { enableAbyssFog = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(depthStartOpt)
                    .option(depthMaxOpt)
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.fogDarknessIntensity"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.fogDarknessIntensity.tooltip")))
                        .binding(Binding.generic(1.0, { fogDarknessIntensity }, { fogDarknessIntensity = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.abyssColorRetention"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.abyssColorRetention.desc")))
                        .binding(Binding.generic(0.15, { abyssColorRetention }, { abyssColorRetention = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.nautilusLampInfluence"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.nautilusLampInfluence.tooltip")))
                        .binding(Binding.generic(0.5, { nautilusLampInfluence }, { nautilusLampInfluence = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.lampNearPlaneMultiplier"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.lampNearPlaneMultiplier.desc")))
                        .binding(Binding.generic(12.0, { lampNearPlaneMultiplier }, { lampNearPlaneMultiplier = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 50.0).step(1.0) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.lampFarPlaneMultiplier"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.lampFarPlaneMultiplier.desc")))
                        .binding(Binding.generic(60.0, { lampFarPlaneMultiplier }, { lampFarPlaneMultiplier = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 200.0).step(5.0) }
                        .build())
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.enableDepthVignette"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.enableDepthVignette.desc")))
                        .binding(Binding.generic(true, { enableDepthVignette }, { enableDepthVignette = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.vignetteIntensity"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.vignetteIntensity.desc")))
                        .binding(Binding.generic(1.0, { vignetteIntensity }, { vignetteIntensity = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.lightDimmingStrength"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.lightDimmingStrength.desc")))
                        .binding(Binding.generic(0.7, { lightDimmingStrength }, { lightDimmingStrength = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.marine_snow").withStyle(ChatFormatting.GOLD))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.marine_snow.desc"))
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.maxMarinSnowParticles"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.maxMarinSnowParticles.tooltip")))
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
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.marineSnowSwayAmplitude.desc")))
                        .binding(Binding.generic(1.0, { marineSnowSwayAmplitude }, { marineSnowSwayAmplitude = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.marineSnowSpawnHeightAbove"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.marineSnowSpawnHeightAbove.desc")))
                        .binding(Binding.generic(2.0, { marineSnowSpawnHeightAbove }, { marineSnowSpawnHeightAbove = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 20.0).step(0.5) }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.surface_occluder").withStyle(ChatFormatting.DARK_BLUE))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.surface_occluder.desc"))
                        .build())
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.enableSurfaceOccluder"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.enableSurfaceOccluder.desc")))
                        .binding(Binding.generic(true, { enableSurfaceOccluder }, { enableSurfaceOccluder = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(alphaMinOpt)
                    .option(alphaMaxOpt)
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderNumLayers"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderNumLayers.desc")))
                        .binding(Binding.generic(5, { surfaceOccluderNumLayers }, { surfaceOccluderNumLayers = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(1, 10).step(1) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderFadeSpeed"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderFadeSpeed.desc")))
                        .binding(Binding.generic(2.5, { surfaceOccluderFadeSpeed }, { surfaceOccluderFadeSpeed = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.5, 10.0).step(0.5) }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderLodBandWidth"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderLodBandWidth.desc")))
                        .binding(Binding.generic(64, { surfaceOccluderLodBandWidth }, { surfaceOccluderLodBandWidth = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(16, 256).step(16) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderTargetDepth"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderTargetDepth.desc")))
                        .binding(Binding.generic(0.5, { surfaceOccluderTargetDepth }, { surfaceOccluderTargetDepth = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05).formatValue { v -> Component.literal(String.format("%.0f%%", v * 100)) } }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.conduit").withStyle(ChatFormatting.DARK_AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.conduit.desc"))
                        .build())
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryParticlesEnabled"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryParticlesEnabled.desc")))
                        .binding(Binding.generic(true, { conduitBoundaryParticlesEnabled }, { conduitBoundaryParticlesEnabled = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryTriggerDistance"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryTriggerDistance.desc")))
                        .binding(Binding.generic(5.0, { conduitBoundaryTriggerDistance }, { conduitBoundaryTriggerDistance = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(1.0, 20.0).step(0.5) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingRadius"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingRadius.desc")))
                        .binding(Binding.generic(2.5, { conduitBoundaryRingRadius }, { conduitBoundaryRingRadius = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.5, 10.0).step(0.5) }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingPoints"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingPoints.desc")))
                        .binding(Binding.generic(24, { conduitBoundaryRingPoints }, { conduitBoundaryRingPoints = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(4, 64).step(4) }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryParticleInterval"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryParticleInterval.desc")))
                        .binding(Binding.generic(3, { conduitBoundaryParticleInterval }, { conduitBoundaryParticleInterval = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(1, 20).step(1) }
                        .build())
                    .build())

                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.server")
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD))
                .tooltip(Component.translatable("config.squ_abyssal_bloom.server.tooltip"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.ocean_zones").withStyle(ChatFormatting.DARK_AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.ocean_zones.desc"))
                        .build())
                    .option(shallowDeepOpt)
                    .option(deepAbyssalOpt)
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.ocean_territories").withStyle(ChatFormatting.BLUE))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.ocean_territories.desc"))
                        .build())
                    .option(serverInt(ModServerConfig.OCEAN_TERRITORY_EXTRA_ZOOMS, step = 1))
                    .option(serverInt(ModServerConfig.OCEAN_TERRITORY_DEFAULT_WEIGHT, step = 5))
                    .option(serverInt(ModServerConfig.OCEAN_TERRITORY_OWN_WEIGHT, step = 5))
                    .option(serverBool(ModServerConfig.OCEAN_TERRITORY_INCLUDE_VANILLA))
                    .build())

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

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("entity.squ_abyssal_bloom.brine").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.brine.desc"))
                        .customImage(EntityConfigRenderer(entityType = ModEntities.BRINE.get()) { entity, tick ->
                            if (entity is BrineEntity) {
                                val durationTicks = (BrineAnimation.idle.lengthInSeconds * 20).toInt()
                                val loopTick = tick % durationTicks
                                if (loopTick == 0 || !entity.idleAnimationState.isStarted)
                                    entity.idleAnimationState.start(tick)
                            }
                        })
                        .build())
                    .option(serverBool(ModServerConfig.BRINE_NATURAL_SPAWNING))
                    .option(serverDouble(ModServerConfig.BRINE_DETECTION_RANGE, step = 1.0))
                    .option(serverDouble(ModServerConfig.BRINE_FOLLOW_SPEED, step = 0.01))
                    .option(serverDouble(ModServerConfig.BRINE_ATTACK_SPEED, step = 0.01))
                    .option(serverDouble(ModServerConfig.BRINE_ATTACK_ENTER_RADIUS, step = 0.25))
                    .option(serverDouble(ModServerConfig.BRINE_ATTACK_EXIT_RADIUS, step = 0.25))
                    .option(serverInt(ModServerConfig.BRINE_COLUMN_ATTACK_COOLDOWN, step = 5))
                    .option(serverInt(ModServerConfig.BRINE_DIRECT_ATTACK_COOLDOWN, step = 5))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("entity.squ_abyssal_bloom.red_slobberer").withStyle(ChatFormatting.RED))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.red_slobberer.desc"))
                        .customImage(EntityConfigRenderer(entityType = ModEntities.RED_SLOBBERER.get()) { entity, tick ->
                            if (entity is RedSlobbererEntity) {
                                val durationTicks = (RedSlobbererAnimation.idle.lengthInSeconds * 20).toInt()
                                    .coerceAtLeast(1)
                                val loopTick = tick % durationTicks
                                if (loopTick == 0 || !entity.idleAnimationState.isStarted)
                                    entity.idleAnimationState.start(tick)
                            }
                        })
                        .build())
                    .option(serverInt(ModServerConfig.RED_SLOBBERER_REEF_MATURITY_TICKS, step = 200))
                    .option(serverInt(ModServerConfig.RED_SLOBBERER_MAX_REEF_FISH))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.fish_schools").withStyle(ChatFormatting.AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.fish_schools.desc"))
                        .build())
                    .option(serverBool(ModServerConfig.FISH_SCHOOL_DEBUG))
                    .option(serverInt(ModServerConfig.FISH_SCHOOL_NEIGHBOR_COUNT))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_NEIGHBOR_SEARCH_RADIUS, step = 0.5))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_AGGREGATION_RADIUS, step = 0.5))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_CROSS_SPECIES_AFFINITY, step = 0.01))
                    .option(serverInt(ModServerConfig.FISH_SCHOOL_ACTIVATION_THRESHOLD))
                    .option(serverInt(ModServerConfig.FISH_SCHOOL_DEACTIVATION_THRESHOLD))
                    .option(serverInt(ModServerConfig.FISH_SCHOOL_ACTIVATION_DELAY, step = 20))
                    .option(serverInt(ModServerConfig.FISH_SCHOOL_DEACTIVATION_DELAY, step = 20))
                    .option(serverInt(ModServerConfig.FISH_SCHOOL_NEIGHBOR_REFRESH_INTERVAL))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_SEPARATION_WEIGHT, step = 0.05))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_ALIGNMENT_WEIGHT, step = 0.05))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_COHESION_WEIGHT, step = 0.05))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_OBSTACLE_AVOIDANCE_WEIGHT, step = 0.05))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_THREAT_AVOIDANCE_WEIGHT, step = 0.05))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_THREAT_DETECTION_RADIUS, step = 0.5))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_THREAT_PROPAGATION_SPEED, step = 0.01))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_THREAT_SIGNAL_DECAY, step = 0.001))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_MAXIMUM_SPEED, step = 0.01))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_PANIC_SPEED, step = 0.01))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_MAXIMUM_TURN_RATE, step = 0.5))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_VERTICAL_MOVEMENT_WEIGHT, step = 0.01))
                    .option(serverDouble(ModServerConfig.FISH_SCHOOL_VERTICAL_DRIFT_SPEED, step = 0.001))
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

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.worldgen")
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GREEN))
                .tooltip(Component.translatable("config.squ_abyssal_bloom.worldgen.tooltip"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.terrain_zones").withStyle(ChatFormatting.GREEN))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.terrain_zones.desc"))
                        .build())
                    .option(shallowFloorOpt)
                    .option(deepFloorOpt)
                    .option(abyssalFloorOpt)
                    .option(serverInt(ModServerConfig.SHALLOW_CLEARANCE))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.topo").withStyle(ChatFormatting.AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.topo.desc"))
                        .build())
                    .option(serverDouble(ModServerConfig.WARP_AMP, step = 5.0))
                    .option(serverDouble(ModServerConfig.WARP2_AMP, step = 2.0))
                    .option(serverDouble(ModServerConfig.TOPO_LARGE_AMP, step = 5.0))
                    .option(serverDouble(ModServerConfig.TOPO_AMP, step = 2.0))
                    .option(serverDouble(ModServerConfig.TOPO_MID_AMP, step = 1.0))
                    .option(serverDouble(ModServerConfig.WALL_AMP, step = 2.0))
                    .option(serverDouble(ModServerConfig.DETAIL_AMP, step = 1.0))
                    .option(serverDouble(ModServerConfig.MICRO_AMP, step = 0.5))
                    .option(serverDouble(ModServerConfig.WEIRDNESS_AMP, step = 1.0))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.faults").withStyle(ChatFormatting.GOLD))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.faults.desc"))
                        .build())
                    .option(serverDouble(ModServerConfig.FAULT_THRESHOLD, 0.001..0.15, 0.001) { Component.literal(String.format("%.3f", it)) })
                    .option(serverDouble(ModServerConfig.FAULT_BLEND, step = 0.001) { Component.literal(String.format("%.3f", it)) })
                    .option(serverDouble(ModServerConfig.FAULT_OFFSET_AMP, step = 1.0))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.trenches").withStyle(ChatFormatting.DARK_RED))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.trenches.desc"))
                        .build())
                    .option(serverDouble(ModServerConfig.TRENCH_THRESHOLD, step = 0.01) { Component.literal(String.format("%.2f", it)) })
                    .option(serverDouble(ModServerConfig.TRENCH_DEPTH_AMP, step = 1.0))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.terraces").withStyle(ChatFormatting.YELLOW))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.terraces.desc"))
                        .build())
                    .option(serverDouble(ModServerConfig.TERRACE_MASK_THRESHOLD, step = 0.01) { Component.literal(String.format("%.2f", it)) })
                    .option(serverDouble(ModServerConfig.TERRACE_STEP, step = 1.0))
                    .build())

                .build())

            .build()
            .generateScreen(parent)
    }

    private fun serverDouble(
        opt: DoubleOption,
        range: ClosedFloatingPointRange<Double> = opt.min..opt.max,
        step: Double = 0.1,
        format: ((Double) -> Component)? = null
    ): Option<Double> =
        Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.${opt.key}"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.${opt.key}.desc")))
            .binding(Binding.generic(opt.default, { ServerConfigCache.current(opt) }, { ServerConfigCache.set(opt, it) }))
            .controller { o ->
                var c = DoubleSliderControllerBuilder.create(o).range(range.start, range.endInclusive).step(step)
                if (format != null) c = c.formatValue(format)
                c
            }
            .build()

    private fun serverInt(
        opt: IntOption,
        range: IntRange = opt.min..opt.max,
        step: Int = 1,
        format: ((Int) -> Component)? = null
    ): Option<Int> =
        Option.createBuilder<Int>()
            .name(Component.translatable("config.squ_abyssal_bloom.${opt.key}"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.${opt.key}.desc")))
            .binding(Binding.generic(opt.default, { ServerConfigCache.current(opt) }, { ServerConfigCache.set(opt, it) }))
            .controller { o ->
                var c = IntegerSliderControllerBuilder.create(o).range(range.first, range.last).step(step)
                if (format != null) c = c.formatValue(format)
                c
            }
            .build()

    private fun serverBool(opt: BoolOption): Option<Boolean> =
        Option.createBuilder<Boolean>()
            .name(Component.translatable("config.squ_abyssal_bloom.${opt.key}"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.${opt.key}.desc")))
            .binding(Binding.generic(opt.default, { ServerConfigCache.current(opt) }, { ServerConfigCache.set(opt, it) }))
            .controller(TickBoxControllerBuilder::create)
            .build()
}
