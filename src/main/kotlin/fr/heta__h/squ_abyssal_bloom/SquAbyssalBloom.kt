package fr.heta__h.squ_abyssal_bloom

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.effect.ModPotions
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.item.ModCreativeModeTabs
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.config.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
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

        ModEntities.register(MOD_BUS)
        ModItems.register(MOD_BUS)
        ModBlocks.register(MOD_BUS)
        ModEffects.register(MOD_BUS)
        ModPotions.register(MOD_BUS)
        ModAttachments.register(MOD_BUS)
        ModSounds.register(MOD_BUS)
        ModCreativeModeTabs.register(MOD_BUS)
        ModParticles.register(MOD_BUS)
        ModDataComponents.register(MOD_BUS)

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
}