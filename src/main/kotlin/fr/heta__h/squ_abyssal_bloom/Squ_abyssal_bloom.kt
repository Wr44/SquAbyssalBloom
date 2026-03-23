package fr.heta__h.squ_abyssal_bloom

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.effect.ModPotions
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.item.ModCreativeModeTabs
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.particle.MarineSnowParticleProvider
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModAttachments
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.chunk.ChunkSectionLayer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist


@Mod(Squ_abyssal_bloom.ID)
@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object Squ_abyssal_bloom {
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

        runForDist(
            clientTarget = {
                MOD_BUS.addListener(::onClientSetup)
                MOD_BUS.addListener { event: RegisterParticleProvidersEvent ->
                    event.registerSpriteSet(ModParticles.MARINE_SNOW.get()) { sprites ->
                        MarineSnowParticleProvider(sprites)
                    }
                }
            },
            serverTarget = {
                MOD_BUS.addListener(::onServerSetup)
            }
        )
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.info("Initializing client...")
        ModConfig.loadConfig()
        event.container.registerExtensionPoint(
            net.neoforged.neoforge.client.gui.IConfigScreenFactory::class.java,
            net.neoforged.neoforge.client.gui.IConfigScreenFactory { _, parent ->
                ModConfig.createConfigScreen(parent)
            }
        )
        event.enqueueWork {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SPROUTING_SEAGRASS.get(), ChunkSectionLayer.CUTOUT)
        }
    }

    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.info("Server starting...")
    }

    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.info("Hello! This is working!")
    }

    @SubscribeEvent
    fun registerEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        ModEntities.registerEntityRenderers(event)
    }

    @SubscribeEvent
    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        ModEntities.registerLayerDefinitions(event)
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
    fun onAddLayers(event: EntityRenderersEvent.AddLayers) {
        ModEntities.onAddLayers(event)
    }

    @SubscribeEvent
    fun registerBrewingRecipes(event: RegisterBrewingRecipesEvent) {
        ModPotions.registerBrewingRecipes(event)
    }
}
