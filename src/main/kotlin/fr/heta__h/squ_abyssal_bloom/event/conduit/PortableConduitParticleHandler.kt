package fr.heta__h.squ_abyssal_bloom.event.conduit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object PortableConduitParticleHandler {

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {

        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val level = minecraft.level ?: return

        if (minecraft.isPaused) return

        if (level.gameTime % 3 != 0L) return

        level.getEntitiesOfClass(AbstractNautilus::class.java, player.boundingBox.inflate(ConduitDomainHandler.PORTABLE_RADIUS))
            .forEach { nautilus ->
                if (nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT).item != NautilusLayerItems.CONDUIT) return@forEach

                val random = level.random

                val startX = nautilus.x + (random.nextFloat() - 0.5f) * 0.8f
                val startY = nautilus.y + (nautilus.bbHeight / 2.0) + (random.nextFloat() - 0.5f) * 0.8f
                val startZ = nautilus.z + (random.nextFloat() - 0.5f) * 0.8f

                level.addParticle(
                    ModParticles.NAUTILUS_TRACKING_PARTICLE.get(),
                    startX, startY, startZ,
                    nautilus.id.toDouble(),
                    0.0,
                    0.0
                )
            }
    }
}