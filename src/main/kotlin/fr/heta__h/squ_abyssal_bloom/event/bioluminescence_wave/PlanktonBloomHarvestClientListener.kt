package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Items
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object PlanktonBloomHarvestClientListener {

    @SubscribeEvent
    fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
        val level = event.level
        if (!level.isClientSide) return
        if (!event.itemStack.`is`(Items.GLASS_BOTTLE)) return

        val hitResult = PlanktonBloomHarvestListener.fluidRaycast(level, event.entity)
        if (hitResult.type != HitResult.Type.BLOCK) return
        if (!hasHarvestableBloomNear(hitResult.location)) return

        event.cancellationResult = InteractionResult.SUCCESS
        event.isCanceled = true
    }

    private fun hasHarvestableBloomNear(hitPosition: Vec3): Boolean {
        val radius = ModServerConfig.BIOLUMINESCENCE_BLOOM_HARVEST_RADIUS.get()
        val radiusSqr = radius * radius
        return BioluminescentZoneManager.activeZones.any { zone ->
            zone.blooms.values.any { bloom ->
                bloom.lifecycle == PlanktonBloomLifecycle.ACTIVE &&
                    Vec3.atCenterOf(bloom.position).distanceToSqr(hitPosition) <= radiusSqr
            }
        }
    }
}
