package fr.heta__h.squ_abyssal_bloom

import fr.deotexh.cubicbezier.CubicBezier
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.item.ModCreativeModeTabs
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.client.Minecraft
import net.minecraft.client.animation.Keyframe
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterJsonAnimationTypesEvent
import net.neoforged.neoforge.data.event.GatherDataEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.joml.Vector3f
import org.joml.Vector3fc
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist


@Mod(Squ_abyssal_bloom.ID)
@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object Squ_abyssal_bloom {
    const val ID = "squ_abyssal_bloom"

    
    val LOGGER: Logger = LogManager.getLogger(ID)

    init {
        LOGGER.log(Level.INFO, "Hello world!")

        
        ModBlocks.REGISTRY.register(MOD_BUS)
        ModEntities.register(MOD_BUS)
        ModItems.register(MOD_BUS)
        ModCreativeModeTabs.register(MOD_BUS)

        val obj = runForDist(clientTarget = {
            MOD_BUS.addListener(::onClientSetup)
            Minecraft.getInstance()
        }, serverTarget = {
            MOD_BUS.addListener(::onServerSetup)
            "test"
        })

        println(obj)
    }

    
    private fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.log(Level.INFO, "Initializing client...")
    }

    
    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.log(Level.INFO, "Server starting...")
    }

    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.log(Level.INFO, "Hello! This is working!")
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





















}
