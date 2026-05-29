package fr.heta__h.squ_abyssal_bloom.client

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.chunk.ChunkSectionLayer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object ClientModEvents {

    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        SquAbyssalBloom.LOGGER.info("Initializing client...")
        ModConfig.loadConfig()

        event.container.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { _, parent ->
                ModConfig.createConfigScreen(parent)
            }
        )

        event.enqueueWork {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SPROUTING_SEAGRASS.get(), ChunkSectionLayer.CUTOUT)
        }
    }

    @SubscribeEvent
    fun registerParticleProviders(event: RegisterParticleProvidersEvent) {
        ModParticles.registerParticleProviders(event)
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
    fun onAddLayers(event: EntityRenderersEvent.AddLayers) {
        ModEntities.onAddLayers(event)
    }
}