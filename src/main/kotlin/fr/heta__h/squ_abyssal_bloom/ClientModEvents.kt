package fr.heta__h.squ_abyssal_bloom

import fr.heta__h.squ_abyssal_bloom.compat.iris.IrisPipelineBootstrap
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.pipeline.BioluminescentRenderPipelines
import fr.heta__h.squ_abyssal_bloom.util.cache.AbstractFishTypeCache
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
        IrisPipelineBootstrap.registerBioluminescentSurfaces(
            BioluminescentRenderPipelines.SHADER_UNDERWATER_SURFACE,
            BioluminescentRenderPipelines.SHADER_VISIBILITY_COMPENSATION
        )
        AbstractFishTypeCache.load()

        event.container.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { _, parent ->
                ModConfig.createConfigScreen(parent)
            }
        )

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