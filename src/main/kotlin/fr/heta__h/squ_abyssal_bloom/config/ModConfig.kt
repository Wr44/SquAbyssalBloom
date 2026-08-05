package fr.heta__h.squ_abyssal_bloom.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.config.renderer.StaticImageRenderer
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigData
import fr.heta__h.squ_abyssal_bloom.config.submenu.AmbientSoundSubMenu
import fr.heta__h.squ_abyssal_bloom.config.submenu.BarnacleSubMenu
import fr.heta__h.squ_abyssal_bloom.config.submenu.BioluminescenceServerSubMenu
import fr.heta__h.squ_abyssal_bloom.config.submenu.BrineSubMenu
import fr.heta__h.squ_abyssal_bloom.config.submenu.FishSchoolSubMenu
import fr.heta__h.squ_abyssal_bloom.config.submenu.MackerelSubMenu
import fr.heta__h.squ_abyssal_bloom.config.submenu.mods.DynamicLightSubMenu
import fr.heta__h.squ_abyssal_bloom.config.submenu.mods.IrisSubMenu
import fr.heta__h.squ_abyssal_bloom.config.submenu.RedSlobbererSubMenu
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.persistServerConfigChanges
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverBool
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverDouble
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.serverInt
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.subScreenButton
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.AbyssalTerrainSettings
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
    var enableMarineSnow: Boolean = true
    var maxMarinSnowParticles: Int = 75
    var fogDarknessIntensity: Double = 1.0
    var fogRepellerInfluence: Double = 0.5
    var marineSnowDensity: Double = 0.5
    var marineSnowVisibilityRange: Int = 20
    var marineSnowSwayAmplitude: Double = 1.0
    var marineSnowSpawnHeightAbove: Double = 2.0
    var enableDepthVignette: Boolean = true
    var vignetteIntensity: Double = 1.0
    var lightDimmingStrength: Double = 0.7
    var abyssColorRetention: Double = 0.15
    var fogRepellerNearPlaneMultiplier: Double = 12.0
    var fogRepellerFarPlaneMultiplier: Double = 60.0
    var conduitBoundaryTriggerDistance: Double = 5.0
    var conduitBoundaryRingRadius: Double = 2.5
    var conduitBoundaryRingPoints: Int = 24
    var conduitBoundaryParticleInterval: Int = 3
    var enableConduitBoundaryParticles: Boolean = true
    var enableSurfaceOccluder: Boolean = true
    var surfaceOccluderAlphaMin: Double = 0.05
    var surfaceOccluderAlphaMax: Double = 0.70
    var surfaceOccluderNumLayers: Int = 5
    var surfaceOccluderFadeSpeed: Double = 2.5
    var surfaceOccluderLodBandWidth: Int = 64
    var surfaceOccluderTargetDepth: Double = 0.5
    var enableIrisCompatibility: Boolean = true
    var shaderBioluminescencePrimaryAlphaMultiplier: Double = 2.2
    var shaderBioluminescenceVisibilityCompensation: Double = 0.22
    var shaderBioluminescenceDeepWaterBoost: Double = 2.2
    var shaderBioluminescenceGrazingStrength: Double = 0.45
    var shaderBioluminescenceUnderwaterCompensation: Double = 0.85
    var shaderBioluminescenceSubsurfaceOffset: Double = 0.25
    var enableDynamicLights: Boolean = true
    var dynamicLightsNautilusIntensity: Double = 1.0
    var dynamicLightsBioluminescenceActiveIntensity: Double = 1.0
    var dynamicLightsBioluminescenceInactiveIntensity: Double = 1.0
    var enableBioluminescenceRendering: Boolean = true
    var bioluminescenceTileFadeInTicks: Int = 20
    var bioluminescenceRenderDistance: Double = 128.0
    var enableBeachWaveSound: Boolean = true

    fun loadConfig() {
        if (!configFile.exists()) { saveConfig(); return }
        try {
            val json = gson.fromJson(configFile.readText(), JsonObject::class.java) ?: return
            enableAbyssFog = json.get("enableAbyssFog")?.asBoolean ?: true
            abyssDepthStart = json.get("abyssDepthStart")?.asDouble ?: 30.0
            abyssMaxDepth = json.get("abyssMaxDepth")?.asDouble ?: 80.0
            enableMarineSnow = json.get("enableMarineSnow")?.asBoolean ?: true
            maxMarinSnowParticles = json.get("maxMarinSnowParticles")?.asInt ?: 75
            fogDarknessIntensity = json.get("fogDarknessIntensity")?.asDouble ?: 1.0
            fogRepellerInfluence = json.get("fogRepellerInfluence")?.asDouble ?: 0.5
            marineSnowDensity = json.get("marineSnowDensity")?.asDouble ?: 0.5
            marineSnowVisibilityRange = json.get("marineSnowVisibilityRange")?.asInt ?: 20
            marineSnowSwayAmplitude = json.get("marineSnowSwayAmplitude")?.asDouble ?: 1.0
            marineSnowSpawnHeightAbove = json.get("marineSnowSpawnHeightAbove")?.asDouble ?: 2.0
            enableDepthVignette = json.get("enableDepthVignette")?.asBoolean ?: true
            vignetteIntensity = json.get("vignetteIntensity")?.asDouble ?: 1.0
            lightDimmingStrength = json.get("lightDimmingStrength")?.asDouble ?: 0.7
            abyssColorRetention = json.get("abyssColorRetention")?.asDouble ?: 0.15
            fogRepellerNearPlaneMultiplier = json.get("fogRepellerNearPlaneMultiplier")?.asDouble ?: 12.0
            fogRepellerFarPlaneMultiplier = json.get("fogRepellerFarPlaneMultiplier")?.asDouble ?: 60.0
            conduitBoundaryTriggerDistance = json.get("conduitBoundaryTriggerDistance")?.asDouble ?: 5.0
            conduitBoundaryRingRadius = json.get("conduitBoundaryRingRadius")?.asDouble ?: 2.5
            conduitBoundaryRingPoints = json.get("conduitBoundaryRingPoints")?.asInt ?: 24
            conduitBoundaryParticleInterval = json.get("conduitBoundaryParticleInterval")?.asInt ?: 3
            enableConduitBoundaryParticles = json.get("enableConduitBoundaryParticles")?.asBoolean ?: true
            enableSurfaceOccluder = json.get("enableSurfaceOccluder")?.asBoolean ?: true
            surfaceOccluderAlphaMin = json.get("surfaceOccluderAlphaMin")?.asDouble ?: 0.05
            surfaceOccluderAlphaMax = json.get("surfaceOccluderAlphaMax")?.asDouble ?: 0.70
            surfaceOccluderNumLayers = json.get("surfaceOccluderNumLayers")?.asInt ?: 5
            surfaceOccluderFadeSpeed = json.get("surfaceOccluderFadeSpeed")?.asDouble ?: 2.5
            surfaceOccluderLodBandWidth = json.get("surfaceOccluderLodBandWidth")?.asInt ?: 64
            surfaceOccluderTargetDepth = json.get("surfaceOccluderTargetDepth")?.asDouble ?: 0.5
            enableIrisCompatibility = json.get("enableIrisCompatibility")?.asBoolean ?: true
            shaderBioluminescencePrimaryAlphaMultiplier =
                json.get("shaderBioluminescencePrimaryAlphaMultiplier")?.asDouble ?: 2.2
            shaderBioluminescenceVisibilityCompensation =
                json.get("shaderBioluminescenceVisibilityCompensation")?.asDouble ?: 0.22
            shaderBioluminescenceDeepWaterBoost =
                json.get("shaderBioluminescenceDeepWaterBoost")?.asDouble ?: 2.2
            shaderBioluminescenceGrazingStrength =
                json.get("shaderBioluminescenceGrazingStrength")?.asDouble ?: 0.45
            shaderBioluminescenceUnderwaterCompensation =
                json.get("shaderBioluminescenceUnderwaterCompensation")?.asDouble ?: 0.85
            shaderBioluminescenceSubsurfaceOffset =
                json.get("shaderBioluminescenceSubsurfaceOffset")?.asDouble ?: 0.25
            enableDynamicLights = json.get("enableDynamicLights")?.asBoolean ?: true
            dynamicLightsNautilusIntensity =
                json.get("dynamicLightsNautilusIntensity")?.asDouble ?: 1.0
            dynamicLightsBioluminescenceActiveIntensity =
                json.get("dynamicLightsBioluminescenceActiveIntensity")?.asDouble ?: 1.0
            dynamicLightsBioluminescenceInactiveIntensity =
                json.get("dynamicLightsBioluminescenceInactiveIntensity")?.asDouble ?: 1.0
            enableBioluminescenceRendering = json.get("enableBioluminescenceRendering")?.asBoolean ?: true
            bioluminescenceTileFadeInTicks = json.get("bioluminescenceTileFadeInTicks")?.asInt ?: 20
            bioluminescenceRenderDistance = json.get("bioluminescenceRenderDistance")?.asDouble ?: 128.0
            enableBeachWaveSound = json.get("enableBeachWaveSound")?.asBoolean ?: true
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
                addProperty("enableMarineSnow", enableMarineSnow)
                addProperty("maxMarinSnowParticles", maxMarinSnowParticles)
                addProperty("fogDarknessIntensity", fogDarknessIntensity)
                addProperty("fogRepellerInfluence", fogRepellerInfluence)
                addProperty("marineSnowDensity", marineSnowDensity)
                addProperty("marineSnowVisibilityRange", marineSnowVisibilityRange)
                addProperty("marineSnowSwayAmplitude", marineSnowSwayAmplitude)
                addProperty("marineSnowSpawnHeightAbove", marineSnowSpawnHeightAbove)
                addProperty("enableDepthVignette", enableDepthVignette)
                addProperty("vignetteIntensity", vignetteIntensity)
                addProperty("lightDimmingStrength", lightDimmingStrength)
                addProperty("abyssColorRetention", abyssColorRetention)
                addProperty("fogRepellerNearPlaneMultiplier", fogRepellerNearPlaneMultiplier)
                addProperty("fogRepellerFarPlaneMultiplier", fogRepellerFarPlaneMultiplier)
                addProperty("conduitBoundaryTriggerDistance", conduitBoundaryTriggerDistance)
                addProperty("conduitBoundaryRingRadius", conduitBoundaryRingRadius)
                addProperty("conduitBoundaryRingPoints", conduitBoundaryRingPoints)
                addProperty("conduitBoundaryParticleInterval", conduitBoundaryParticleInterval)
                addProperty("enableConduitBoundaryParticles", enableConduitBoundaryParticles)
                addProperty("enableSurfaceOccluder", enableSurfaceOccluder)
                addProperty("surfaceOccluderAlphaMin", surfaceOccluderAlphaMin)
                addProperty("surfaceOccluderAlphaMax", surfaceOccluderAlphaMax)
                addProperty("surfaceOccluderNumLayers", surfaceOccluderNumLayers)
                addProperty("surfaceOccluderFadeSpeed", surfaceOccluderFadeSpeed)
                addProperty("surfaceOccluderLodBandWidth", surfaceOccluderLodBandWidth)
                addProperty("surfaceOccluderTargetDepth", surfaceOccluderTargetDepth)
                addProperty("enableIrisCompatibility", enableIrisCompatibility)
                addProperty("shaderBioluminescencePrimaryAlphaMultiplier", shaderBioluminescencePrimaryAlphaMultiplier)
                addProperty("shaderBioluminescenceVisibilityCompensation", shaderBioluminescenceVisibilityCompensation)
                addProperty("shaderBioluminescenceDeepWaterBoost", shaderBioluminescenceDeepWaterBoost)
                addProperty("shaderBioluminescenceGrazingStrength", shaderBioluminescenceGrazingStrength)
                addProperty("shaderBioluminescenceUnderwaterCompensation", shaderBioluminescenceUnderwaterCompensation)
                addProperty("shaderBioluminescenceSubsurfaceOffset", shaderBioluminescenceSubsurfaceOffset)
                addProperty("enableDynamicLights", enableDynamicLights)
                addProperty("dynamicLightsNautilusIntensity", dynamicLightsNautilusIntensity)
                addProperty("dynamicLightsBioluminescenceActiveIntensity", dynamicLightsBioluminescenceActiveIntensity)
                addProperty("dynamicLightsBioluminescenceInactiveIntensity", dynamicLightsBioluminescenceInactiveIntensity)
                addProperty("enableBioluminescenceRendering", enableBioluminescenceRendering)
                addProperty("bioluminescenceTileFadeInTicks", bioluminescenceTileFadeInTicks)
                addProperty("bioluminescenceRenderDistance", bioluminescenceRenderDistance)
                addProperty("enableBeachWaveSound", enableBeachWaveSound)
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
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 0.5).step(0.01).formatValue { v -> Component.literal(String.format("%.0f%%", v * 100.0)) } }
            .build()

        val alphaMaxOpt = Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderAlphaMax"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderAlphaMax.desc")))
            .binding(Binding.generic(0.70, { surfaceOccluderAlphaMax }, { surfaceOccluderAlphaMax = it }))
            .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.1, 1.0).step(0.01).formatValue { v -> Component.literal(String.format("%.0f%%", v * 100.0)) } }
            .build()

        alphaMinOpt.addListener { _, newMin ->
            if (alphaMaxOpt.pendingValue() <= newMin) alphaMaxOpt.requestSet(newMin + 0.01)
        }
        alphaMaxOpt.addListener { _, newMax ->
            if (alphaMinOpt.pendingValue() >= newMax) alphaMinOpt.requestSet(newMax - 0.01)
        }

        val shallowDeepOpt = serverDouble(ModServerConfig.SHALLOW_DEEP_BOUNDARY, -0.915..-0.21, 0.001) { Component.literal(String.format("%.3f", it)) }
        val deepAbyssalOpt = serverDouble(ModServerConfig.DEEP_ABYSSAL_BOUNDARY, -0.965..-0.24, 0.001) { Component.literal(String.format("%.3f", it)) }

        shallowDeepOpt.addListener { _, newShallow ->
            val maximumDeep = newShallow - AbyssalTerrainSettings.MIN_BOUNDARY_GAP
            if (deepAbyssalOpt.pendingValue() > maximumDeep) deepAbyssalOpt.requestSet(maximumDeep)
        }
        deepAbyssalOpt.addListener { _, newDeep ->
            val minimumShallow = newDeep + AbyssalTerrainSettings.MIN_BOUNDARY_GAP
            if (shallowDeepOpt.pendingValue() < minimumShallow) shallowDeepOpt.requestSet(minimumShallow)
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

        val hasAnyCompatMod = ModCompat.hasIris || ModCompat.hasDynLights
        val compatCategory = if (hasAnyCompatMod) {
            val compatModsGroup = OptionGroup.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.group.compat_mods").withStyle(ChatFormatting.LIGHT_PURPLE))
                .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.group.compat_mods.desc")))

            if (ModCompat.hasIris) {
                compatModsGroup.option(subScreenButton(
                    name = IrisSubMenu.displayName,
                    description = OptionDescription.createBuilder()
                        .text(IrisSubMenu.displayDescription)
                        .customImage(IrisSubMenu.previewRenderer())
                        .build(),
                    screenTitle = IrisSubMenu.displayName,
                    buildGroups = IrisSubMenu::buildGroups,
                    onSave = { saveConfig() }
                ))
            }
            if (ModCompat.hasDynLights) {
                compatModsGroup.option(subScreenButton(
                    name = DynamicLightSubMenu.displayName,
                    description = OptionDescription.createBuilder()
                        .text(DynamicLightSubMenu.displayDescription)
                        .customImage(DynamicLightSubMenu.previewRenderer())
                        .build(),
                    screenTitle = DynamicLightSubMenu.displayName,
                    buildGroups = DynamicLightSubMenu::buildGroups,
                    onSave = { saveConfig() }
                ))
            }

            ConfigCategory.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.compat").withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE))
                .tooltip(Component.translatable("config.squ_abyssal_bloom.compat.tooltip"))
                .group(compatModsGroup.build())
                .build()
        } else {
            null
        }

        val libraryBuilder = YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("config.squ_abyssal_bloom.category").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA))
            .save {
                saveConfig()
                persistServerConfigChanges(initialServerConfig)
            }

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.client"))
                .tooltip(Component.translatable("config.squ_abyssal_bloom.client.tooltip"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.fog").withStyle(ChatFormatting.AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.fog.desc"))
                        .customImage(StaticImageRenderer("marine_fog", 1920, 991))
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
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.abyssColorRetention"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.abyssColorRetention.desc")))
                        .binding(Binding.generic(0.15, { abyssColorRetention }, { abyssColorRetention = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05).formatValue { v -> Component.literal(String.format("%.0f%%", v * 100.0)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.fogRepellerInfluence"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.fogRepellerInfluence.tooltip")))
                        .binding(Binding.generic(0.5, { fogRepellerInfluence }, { fogRepellerInfluence = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05).formatValue { v -> Component.literal(String.format("%.0f%%", v * 100.0)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.fogRepellerNearPlaneMultiplier"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.fogRepellerNearPlaneMultiplier.desc")))
                        .binding(Binding.generic(12.0, { fogRepellerNearPlaneMultiplier }, { fogRepellerNearPlaneMultiplier = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 50.0).step(1.0).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.fogRepellerFarPlaneMultiplier"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.fogRepellerFarPlaneMultiplier.desc")))
                        .binding(Binding.generic(60.0, { fogRepellerFarPlaneMultiplier }, { fogRepellerFarPlaneMultiplier = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 200.0).step(5.0).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
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
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.lightDimmingStrength"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.lightDimmingStrength.desc")))
                        .binding(Binding.generic(0.7, { lightDimmingStrength }, { lightDimmingStrength = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.marine_snow").withStyle(ChatFormatting.GOLD))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.marine_snow.desc"))
                        .customImage(StaticImageRenderer("marine_snow", 1920, 991))
                        .build())
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.enableMarineSnow"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.enableMarineSnow.desc")))
                        .binding(Binding.generic(true, { enableMarineSnow }, { enableMarineSnow = it }))
                        .controller(TickBoxControllerBuilder::create)
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
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 3.0).step(0.1).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.marineSnowVisibilityRange"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.marineSnowVisibilityRange.tooltip")))
                        .binding(Binding.generic(20, { marineSnowVisibilityRange }, { marineSnowVisibilityRange = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(5, 50).step(5).formatValue { v -> Component.literal("$v blocs") } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.marineSnowSwayAmplitude"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.marineSnowSwayAmplitude.desc")))
                        .binding(Binding.generic(1.0, { marineSnowSwayAmplitude }, { marineSnowSwayAmplitude = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 2.0).step(0.1).formatValue { v -> Component.literal(String.format("%.1f blocs", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.marineSnowSpawnHeightAbove"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.marineSnowSpawnHeightAbove.desc")))
                        .binding(Binding.generic(2.0, { marineSnowSpawnHeightAbove }, { marineSnowSpawnHeightAbove = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 20.0).step(0.5).formatValue { v -> Component.literal(String.format("%.1f blocs", v)) } }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.surface_occluder").withStyle(ChatFormatting.DARK_BLUE))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.surface_occluder.desc"))
                        .customImage(StaticImageRenderer("surface_occluder", 1920, 991))
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
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(1, 10).step(1).formatValue { v -> Component.literal("$v couches") } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderFadeSpeed"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderFadeSpeed.desc")))
                        .binding(Binding.generic(2.5, { surfaceOccluderFadeSpeed }, { surfaceOccluderFadeSpeed = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.5, 10.0).step(0.5).formatValue { v -> Component.literal(String.format("%.1fx", v)) } }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderLodBandWidth"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderLodBandWidth.desc")))
                        .binding(Binding.generic(64, { surfaceOccluderLodBandWidth }, { surfaceOccluderLodBandWidth = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(16, 256).step(16).formatValue { v -> Component.literal("$v blocs") } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderTargetDepth"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.surfaceOccluderTargetDepth.desc")))
                        .binding(Binding.generic(0.5, { surfaceOccluderTargetDepth }, { surfaceOccluderTargetDepth = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.05).formatValue { v -> Component.literal(String.format("%.0f%%", v * 100)) } }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_client").withStyle(ChatFormatting.AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.bioluminescence_client.desc"))
                        .customImage(StaticImageRenderer("wave", 1920, 991))
                        .build()
                    )
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.enableBioluminescenceRendering"))
                        .description(OptionDescription.of(
                            Component.translatable("config.squ_abyssal_bloom.enableBioluminescenceRendering.desc")
                        ))
                        .binding(Binding.generic(
                            true,
                            { enableBioluminescenceRendering },
                            { enableBioluminescenceRendering = it }
                        ))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.bioluminescenceRenderDistance"))
                        .description(OptionDescription.of(
                            Component.translatable("config.squ_abyssal_bloom.bioluminescenceRenderDistance.desc")
                        ))
                        .binding(Binding.generic(
                            128.0,
                            { bioluminescenceRenderDistance },
                            { bioluminescenceRenderDistance = it }
                        ))
                        .controller { option -> DoubleSliderControllerBuilder.create(option).range(32.0, 256.0).step(8.0).formatValue { v -> Component.literal(String.format("%.0f blocs", v)) } }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.bioluminescenceTileFadeInTicks"))
                        .description(OptionDescription.of(
                            Component.translatable("config.squ_abyssal_bloom.bioluminescenceTileFadeInTicks.desc")
                        ))
                        .binding(Binding.generic(
                            20,
                            { bioluminescenceTileFadeInTicks },
                            { bioluminescenceTileFadeInTicks = it }
                        ))
                        .controller { option ->
                            IntegerSliderControllerBuilder.create(option).range(0, 100).step(5)
                                .formatValue { value -> Component.literal(ModUtilities.formatTicksAsDuration(value)) }
                        }
                        .build())
                    .build())


                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.conduit").withStyle(ChatFormatting.DARK_AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.conduit.desc"))
                        .customImage(StaticImageRenderer("conduit_boundary", 1920, 991))
                        .build())
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.enableConduitBoundaryParticles"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.enableConduitBoundaryParticles.desc")))
                        .binding(Binding.generic(true, { enableConduitBoundaryParticles }, { enableConduitBoundaryParticles = it }))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryTriggerDistance"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryTriggerDistance.desc")))
                        .binding(Binding.generic(5.0, { conduitBoundaryTriggerDistance }, { conduitBoundaryTriggerDistance = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(1.0, 20.0).step(0.5).formatValue { v -> Component.literal(String.format("%.1f blocs", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingRadius"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingRadius.desc")))
                        .binding(Binding.generic(2.5, { conduitBoundaryRingRadius }, { conduitBoundaryRingRadius = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.5, 10.0).step(0.5).formatValue { v -> Component.literal(String.format("%.1f blocs", v)) } }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingPoints"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryRingPoints.desc")))
                        .binding(Binding.generic(24, { conduitBoundaryRingPoints }, { conduitBoundaryRingPoints = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(4, 64).step(4).formatValue { v -> Component.literal("$v points") } }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryParticleInterval"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.conduitBoundaryParticleInterval.desc")))
                        .binding(Binding.generic(3, { conduitBoundaryParticleInterval }, { conduitBoundaryParticleInterval = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(1, 20).step(1).formatValue { v -> Component.literal("$v ticks") } }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(AmbientSoundSubMenu.displayName.copy().withStyle(ChatFormatting.AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(AmbientSoundSubMenu.displayDescription)
                        .customImage(StaticImageRenderer("wave", 1920, 991))
                        .build())
                    .option(subScreenButton(
                        name = AmbientSoundSubMenu.displayName,
                        description = OptionDescription.createBuilder()
                            .text(AmbientSoundSubMenu.displayDescription)
                            .customImage(StaticImageRenderer("wave", 1920, 991))
                            .build(),
                        screenTitle = AmbientSoundSubMenu.displayName,
                        buildGroups = AmbientSoundSubMenu::buildGroups
                    ))
                    .build())

                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.server")
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD))
                .tooltip(Component.translatable("config.squ_abyssal_bloom.server.tooltip"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.ocean_territories").withStyle(ChatFormatting.BLUE))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.ocean_territories.desc"))
                        .build())
                    .option(serverInt(ModServerConfig.OCEAN_TERRITORY_EXTRA_ZOOMS, step = 1) { Component.literal("$it zooms") })
                    .option(serverInt(ModServerConfig.OCEAN_TERRITORY_DEFAULT_WEIGHT, step = 5))
                    .option(serverInt(ModServerConfig.OCEAN_TERRITORY_OWN_WEIGHT, step = 5))
                    .option(serverBool(ModServerConfig.OCEAN_TERRITORY_INCLUDE_VANILLA))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(BioluminescenceServerSubMenu.displayName.copy().withStyle(ChatFormatting.AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(BioluminescenceServerSubMenu.displayDescription)
                        .customImage(StaticImageRenderer("wave", 1920, 991))
                        .build())
                    .option(subScreenButton(
                        name = BioluminescenceServerSubMenu.displayName,
                        description = OptionDescription.createBuilder()
                            .text(BioluminescenceServerSubMenu.displayDescription)
                            .customImage(StaticImageRenderer("wave", 1920, 991))
                            .build(),
                        screenTitle = BioluminescenceServerSubMenu.displayName,
                        buildGroups = BioluminescenceServerSubMenu::buildGroups
                    ))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.entities").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.entities.desc"))
                        .build())
                    .option(subScreenButton(
                        name = Component.translatable("entity.squ_abyssal_bloom.barnacle"),
                        description = OptionDescription.createBuilder()
                            .text(Component.translatable("config.squ_abyssal_bloom.barnacle.desc"))
                            .customImage(BarnacleSubMenu.previewRenderer())
                            .build(),
                        screenTitle = Component.translatable("entity.squ_abyssal_bloom.barnacle"),
                        buildGroups = BarnacleSubMenu::buildGroups
                    ))
                    .option(subScreenButton(
                        name = Component.translatable("entity.squ_abyssal_bloom.brine"),
                        description = OptionDescription.createBuilder()
                            .text(Component.translatable("config.squ_abyssal_bloom.brine.desc"))
                            .customImage(BrineSubMenu.previewRenderer())
                            .build(),
                        screenTitle = Component.translatable("entity.squ_abyssal_bloom.brine"),
                        buildGroups = BrineSubMenu::buildGroups
                    ))
                    .option(subScreenButton(
                        name = Component.translatable("entity.squ_abyssal_bloom.red_slobberer"),
                        description = OptionDescription.createBuilder()
                            .text(Component.translatable("config.squ_abyssal_bloom.red_slobberer.desc"))
                            .customImage(RedSlobbererSubMenu.previewRenderer())
                            .build(),
                        screenTitle = Component.translatable("entity.squ_abyssal_bloom.red_slobberer"),
                        buildGroups = RedSlobbererSubMenu::buildGroups
                    ))
                    .option(subScreenButton(
                        name = Component.translatable("entity.squ_abyssal_bloom.mackerel"),
                        description = OptionDescription.createBuilder()
                            .text(Component.translatable("config.squ_abyssal_bloom.mackerel.desc"))
                            .customImage(MackerelSubMenu.previewRenderer())
                            .build(),
                        screenTitle = Component.translatable("entity.squ_abyssal_bloom.mackerel"),
                        buildGroups = MackerelSubMenu::buildGroups
                    ))
                    .option(subScreenButton(
                        name = Component.translatable("config.squ_abyssal_bloom.group.fish_schools"),
                        description = OptionDescription.createBuilder()
                            .text(Component.translatable("config.squ_abyssal_bloom.group.fish_schools.desc"))
                            .customImage(StaticImageRenderer("fish_school", 1920, 991))
                            .build(),
                        screenTitle = Component.translatable("config.squ_abyssal_bloom.group.fish_schools"),
                        buildGroups = FishSchoolSubMenu::buildGroups
                    ))
                    .build())

                .build())

            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("config.squ_abyssal_bloom.worldgen")
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GREEN))
                .tooltip(Component.translatable("config.squ_abyssal_bloom.worldgen.tooltip"))

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.ocean_zones").withStyle(ChatFormatting.DARK_AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.ocean_zones.desc"))
                        .build())
                    .option(shallowDeepOpt)
                    .option(deepAbyssalOpt)
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.terrain_zones").withStyle(ChatFormatting.GREEN))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.terrain_zones.desc"))
                        .build())
                    .option(shallowFloorOpt)
                    .option(deepFloorOpt)
                    .option(abyssalFloorOpt)
                    .option(serverInt(ModServerConfig.SHALLOW_CLEARANCE, format = ModUtilities.blocksFormatInt()))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.topo").withStyle(ChatFormatting.AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.topo.desc"))
                        .build())
                    .option(serverDouble(ModServerConfig.WARP_AMP, step = 5.0, format = ModUtilities.blocksFormatDouble()))
                    .option(serverDouble(ModServerConfig.WARP2_AMP, step = 2.0, format = ModUtilities.blocksFormatDouble()))
                    .option(serverDouble(ModServerConfig.TOPO_LARGE_AMP, step = 5.0, format = ModUtilities.blocksFormatDouble()))
                    .option(serverDouble(ModServerConfig.TOPO_AMP, step = 2.0, format = ModUtilities.blocksFormatDouble()))
                    .option(serverDouble(ModServerConfig.TOPO_MID_AMP, step = 1.0, format = ModUtilities.blocksFormatDouble()))
                    .option(serverDouble(ModServerConfig.WALL_AMP, step = 2.0, format = ModUtilities.blocksFormatDouble()))
                    .option(serverDouble(ModServerConfig.DETAIL_AMP, step = 1.0, format = ModUtilities.blocksFormatDouble()))
                    .option(serverDouble(ModServerConfig.MICRO_AMP, step = 0.5, format = ModUtilities.blocksFormatDouble()))
                    .option(serverDouble(ModServerConfig.WEIRDNESS_AMP, step = 1.0, format = ModUtilities.blocksFormatDouble()))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.faults").withStyle(ChatFormatting.GOLD))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.faults.desc"))
                        .build())
                    .option(serverDouble(ModServerConfig.FAULT_FREQUENCY_PERCENT, step = 1.0) { Component.literal(String.format("%.0f%%", it)) })
                    .option(serverDouble(ModServerConfig.FAULT_BLEND, step = 0.001) { Component.literal(String.format("%.3f", it)) })
                    .option(serverDouble(ModServerConfig.FAULT_OFFSET_AMP, step = 1.0, format = ModUtilities.blocksFormatDouble()))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.trenches").withStyle(ChatFormatting.DARK_RED))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.trenches.desc"))
                        .build())
                    .option(serverDouble(ModServerConfig.TRENCH_FREQUENCY_PERCENT, step = 1.0) { Component.literal(String.format("%.0f%%", it)) })
                    .option(serverDouble(ModServerConfig.TRENCH_DEPTH_AMP, step = 1.0, format = ModUtilities.blocksFormatDouble()))
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.terraces").withStyle(ChatFormatting.YELLOW))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.terraces.desc"))
                        .build())
                    .option(serverDouble(ModServerConfig.TERRACE_FREQUENCY_PERCENT, step = 1.0) { Component.literal(String.format("%.0f%%", it)) })
                    .option(serverDouble(ModServerConfig.TERRACE_STEP, step = 1.0, format = ModUtilities.blocksFormatDouble()))
                    .build())

                .build())

        if (compatCategory != null) {
            libraryBuilder.category(compatCategory)
        }

        return libraryBuilder
            .build()
            .generateScreen(parent)
    }
}
