package fr.heta__h.squ_abyssal_bloom.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleAnimation
import fr.heta__h.squ_abyssal_bloom.entity.client.brine.BrineAnimation
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
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

        val shallowDeepOpt = Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.shallowDeepBoundary"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shallowDeepBoundary.desc")))
            .binding(Binding.generic(-0.45, { ServerConfigCache.shallowDeepBoundary }, { ServerConfigCache.shallowDeepBoundary = it }))
            .controller { opt ->
                DoubleSliderControllerBuilder.create(opt)
                    .range(-0.95, -0.21)
                    .step(0.001)
                    .formatValue { v -> Component.literal(String.format("%.3f", v)) }
            }
            .build()

        val deepAbyssalOpt = Option.createBuilder<Double>()
            .name(Component.translatable("config.squ_abyssal_bloom.deepAbyssalBoundary"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.deepAbyssalBoundary.desc")))
            .binding(Binding.generic(-0.70, { ServerConfigCache.deepAbyssalBoundary }, { ServerConfigCache.deepAbyssalBoundary = it }))
            .controller { opt ->
                DoubleSliderControllerBuilder.create(opt)
                    .range(-1.049, -0.30)
                    .step(0.001)
                    .formatValue { v -> Component.literal(String.format("%.3f", v)) }
            }
            .build()

        shallowDeepOpt.addListener { _, newShallow ->
            if (deepAbyssalOpt.pendingValue() >= newShallow) deepAbyssalOpt.requestSet(newShallow - 0.001)
        }
        deepAbyssalOpt.addListener { _, newDeep ->
            if (shallowDeepOpt.pendingValue() <= newDeep) shallowDeepOpt.requestSet(newDeep + 0.001)
        }

        val shallowFloorOpt = Option.createBuilder<Int>()
            .name(Component.translatable("config.squ_abyssal_bloom.shallowFloorY"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shallowFloorY.desc")))
            .binding(Binding.generic(32, { ServerConfigCache.shallowFloorY }, { ServerConfigCache.shallowFloorY = it }))
            .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(-60, 60).step(1).formatValue { v -> Component.literal("Y=$v") } }
            .build()

        val deepFloorOpt = Option.createBuilder<Int>()
            .name(Component.translatable("config.squ_abyssal_bloom.deepFloorTarget"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.deepFloorTarget.desc")))
            .binding(Binding.generic(11, { ServerConfigCache.deepFloorTarget }, { ServerConfigCache.deepFloorTarget = it }))
            .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(-60, 60).step(1).formatValue { v -> Component.literal("Y=$v") } }
            .build()

        val abyssalFloorOpt = Option.createBuilder<Int>()
            .name(Component.translatable("config.squ_abyssal_bloom.targetFloorY"))
            .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.targetFloorY.desc")))
            .binding(Binding.generic(-35, { ServerConfigCache.targetFloorY }, { ServerConfigCache.targetFloorY = it }))
            .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(-64, 20).step(1).formatValue { v -> Component.literal("Y=$v") } }
            .build()

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
                        .binding(Binding.generic(
                            true,
                            { ServerConfigCache.strictBarnacleSpawning },
                            { ServerConfigCache.strictBarnacleSpawning = it }
                        ))
                        .controller(TickBoxControllerBuilder::create)
                        .build())
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
                    .option(Option.createBuilder<Boolean>()
                        .name(Component.translatable("config.squ_abyssal_bloom.template"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.template")))
                        .binding(Binding.generic(false, { false }, {}))
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
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.shallowClearance"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.shallowClearance.desc")))
                        .binding(Binding.generic(8, { ServerConfigCache.shallowClearance }, { ServerConfigCache.shallowClearance = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(1, 30).step(1) }
                        .build())
                    .option(Option.createBuilder<Int>()
                        .name(Component.translatable("config.squ_abyssal_bloom.guyotClearance"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.guyotClearance.desc")))
                        .binding(Binding.generic(5, { ServerConfigCache.guyotClearance }, { ServerConfigCache.guyotClearance = it }))
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(1, 30).step(1) }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.topo").withStyle(ChatFormatting.AQUA))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.topo.desc"))
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.warpAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.warpAmp.desc")))
                        .binding(Binding.generic(60.0, { ServerConfigCache.warpAmp }, { ServerConfigCache.warpAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 200.0).step(5.0) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.warp2Amp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.warp2Amp.desc")))
                        .binding(Binding.generic(26.0, { ServerConfigCache.warp2Amp }, { ServerConfigCache.warp2Amp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 100.0).step(2.0) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.topoLargeAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.topoLargeAmp.desc")))
                        .binding(Binding.generic(55.0, { ServerConfigCache.topoLargeAmp }, { ServerConfigCache.topoLargeAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 150.0).step(5.0) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.topoAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.topoAmp.desc")))
                        .binding(Binding.generic(24.0, { ServerConfigCache.topoAmp }, { ServerConfigCache.topoAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 100.0).step(2.0) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.topoMidAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.topoMidAmp.desc")))
                        .binding(Binding.generic(11.0, { ServerConfigCache.topoMidAmp }, { ServerConfigCache.topoMidAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 60.0).step(1.0) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.wallAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.wallAmp.desc")))
                        .binding(Binding.generic(14.0, { ServerConfigCache.wallAmp }, { ServerConfigCache.wallAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 80.0).step(2.0) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.detailAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.detailAmp.desc")))
                        .binding(Binding.generic(7.0, { ServerConfigCache.detailAmp }, { ServerConfigCache.detailAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 40.0).step(1.0) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.microAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.microAmp.desc")))
                        .binding(Binding.generic(4.0, { ServerConfigCache.microAmp }, { ServerConfigCache.microAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 20.0).step(0.5) }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.weirdnessAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.weirdnessAmp.desc")))
                        .binding(Binding.generic(11.0, { ServerConfigCache.weirdnessAmp }, { ServerConfigCache.weirdnessAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 50.0).step(1.0) }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.faults").withStyle(ChatFormatting.GOLD))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.faults.desc"))
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.faultThreshold"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.faultThreshold.desc")))
                        .binding(Binding.generic(0.012, { ServerConfigCache.faultThreshold }, { ServerConfigCache.faultThreshold = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.001, 0.15).step(0.001).formatValue { v -> Component.literal(String.format("%.3f", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.faultBlend"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.faultBlend.desc")))
                        .binding(Binding.generic(0.045, { ServerConfigCache.faultBlend }, { ServerConfigCache.faultBlend = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.001, 0.2).step(0.001).formatValue { v -> Component.literal(String.format("%.3f", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.faultOffsetAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.faultOffsetAmp.desc")))
                        .binding(Binding.generic(10.0, { ServerConfigCache.faultOffsetAmp }, { ServerConfigCache.faultOffsetAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 22.0).step(1.0) }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.trenches").withStyle(ChatFormatting.DARK_RED))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.trenches.desc"))
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.trenchThreshold"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.trenchThreshold.desc")))
                        .binding(Binding.generic(0.88, { ServerConfigCache.trenchThreshold }, { ServerConfigCache.trenchThreshold = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.5, 0.99).step(0.01).formatValue { v -> Component.literal(String.format("%.2f", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.trenchDepthAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.trenchDepthAmp.desc")))
                        .binding(Binding.generic(10.0, { ServerConfigCache.trenchDepthAmp }, { ServerConfigCache.trenchDepthAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 22.0).step(1.0) }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.seamounts").withStyle(ChatFormatting.DARK_GREEN))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.seamounts.desc"))
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.seamountThreshold"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.seamountThreshold.desc")))
                        .binding(Binding.generic(0.62, { ServerConfigCache.seamountThreshold }, { ServerConfigCache.seamountThreshold = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.3, 0.99).step(0.01).formatValue { v -> Component.literal(String.format("%.2f", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.seamountAmp"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.seamountAmp.desc")))
                        .binding(Binding.generic(70.0, { ServerConfigCache.seamountAmp }, { ServerConfigCache.seamountAmp = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 200.0).step(5.0) }
                        .build())
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("config.squ_abyssal_bloom.group.terraces").withStyle(ChatFormatting.YELLOW))
                    .description(OptionDescription.createBuilder()
                        .text(Component.translatable("config.squ_abyssal_bloom.group.terraces.desc"))
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.terraceMaskThreshold"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.terraceMaskThreshold.desc")))
                        .binding(Binding.generic(0.80, { ServerConfigCache.terraceMaskThreshold }, { ServerConfigCache.terraceMaskThreshold = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.3, 0.99).step(0.01).formatValue { v -> Component.literal(String.format("%.2f", v)) } }
                        .build())
                    .option(Option.createBuilder<Double>()
                        .name(Component.translatable("config.squ_abyssal_bloom.terraceStep"))
                        .description(OptionDescription.of(Component.translatable("config.squ_abyssal_bloom.terraceStep.desc")))
                        .binding(Binding.generic(13.0, { ServerConfigCache.terraceStep }, { ServerConfigCache.terraceStep = it }))
                        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(2.0, 40.0).step(1.0) }
                        .build())
                    .build())

                .build())

            .build()
            .generateScreen(parent)
    }
}