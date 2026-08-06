package fr.heta__h.squ_abyssal_bloom

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.effect.ModPotions
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.FishCollectiveManager
import fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school.influence.FishSchoolInfluenceRegistry
import fr.heta__h.squ_abyssal_bloom.entity.ai.stealth.StealthRetargetRegistry
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.control.BarnacleStealthRetargetProvider
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.RedSlobbererFishInfluenceProvider
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.RedSlobbererReefManager
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceLevelManager
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceServerManager
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.bloom.PlanktonBloomManager
import fr.heta__h.squ_abyssal_bloom.item.ModCreativeModeTabs
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.feature.ModFeatures
import fr.heta__h.squ_abyssal_bloom.util.worldgen.AbyssalWorldgenScope
import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import fr.heta__h.squ_abyssal_bloom.worldgen.ocean.OceanTerritory
import net.minecraft.server.level.ServerLevel
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

@Mod(SquAbyssalBloom.ID)
@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object SquAbyssalBloom {
    const val ID = "squ_abyssal_bloom"
    val LOGGER: Logger = LogManager.getLogger(ID)

    init {
        LOGGER.info("Hello world!")


        // Register
        ModEntities.register(MOD_BUS)
        ModItems.register(MOD_BUS)
        ModBlocks.register(MOD_BUS)
        ModEffects.register(MOD_BUS)
        ModPotions.register(MOD_BUS)
        ModFeatures.register(MOD_BUS)
        ModAttachments.register(MOD_BUS)
        ModSounds.register(MOD_BUS)
        ModCreativeModeTabs.register(MOD_BUS)
        ModParticles.register(MOD_BUS)
        ModDataComponents.register(MOD_BUS)

        // Mod Register
        FishSchoolInfluenceRegistry.register(RedSlobbererFishInfluenceProvider())
        StealthRetargetRegistry.register(BarnacleStealthRetargetProvider())

        MOD_BUS.addListener(ModBiomes::registerRegions)

        LOADING_CONTEXT.activeContainer.registerConfig(ModConfig.Type.COMMON, ModServerConfig.SPEC)

        runForDist(
            clientTarget = { },
            serverTarget = {
                MOD_BUS.addListener(::onServerSetup)
            }
        )
    }

    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        ServerConfigCache.syncFromSpec()
        LOGGER.info("Server starting...")
    }

    @SubscribeEvent
    fun onServerConfigLoad(event: ModConfigEvent.Loading) {
        if (event.config.spec === ModServerConfig.SPEC) {
            ServerConfigCache.syncFromSpec()
        }
    }

    @SubscribeEvent
    fun onServerConfigReload(event: ModConfigEvent.Reloading) {
        if (event.config.spec === ModServerConfig.SPEC) {
            ServerConfigCache.syncFromSpec()
        }
    }

    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.info("Hello! This is working!")
    }

    @SubscribeEvent
    fun onRegisterAttributes(event: EntityAttributeCreationEvent) {
        ModEntities.onRegisterAttributes(event)
    }

    @SubscribeEvent
    fun onRegisterSpawnPlacements(event: RegisterSpawnPlacementsEvent) {
        ModEntities.registerSpawnPlacements(event)
    }

    @SubscribeEvent
    fun registerBrewingRecipes(event: RegisterBrewingRecipesEvent) {
        ModPotions.registerBrewingRecipes(event)
    }

    @SubscribeEvent
    fun onServerAboutToStart(event: ServerAboutToStartEvent) {
        AbyssalWorldgenScope.clear()
        OceanTerritory.seed(event.server.worldGenSettings.options().seed(), ModServerConfig.OCEAN_TERRITORY_EXTRA_ZOOMS.get())
    }

    @SubscribeEvent
    fun onServerStopped(event: ServerStoppedEvent) {
        AbyssalWorldgenScope.clear()
        BioluminescenceLevelManager.releaseServerLevels(event.server)
        BioluminescenceServerManager.releaseServer(event.server)
    }

    @SubscribeEvent
    fun onLevelLoad(event: LevelEvent.Load) {
        val serverLevel = event.level as? ServerLevel ?: return
        AbyssalWorldgenScope.register(serverLevel)
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        val serverLevel = event.level as? ServerLevel ?: return
        AbyssalWorldgenScope.unregister(serverLevel)
        FishCollectiveManager.releaseLevel(serverLevel)
        RedSlobbererReefManager.releaseLevel(serverLevel)
        BioluminescenceLevelManager.releaseLevel(serverLevel)
        PlanktonBloomManager.releaseLevel(serverLevel)
    }
}
