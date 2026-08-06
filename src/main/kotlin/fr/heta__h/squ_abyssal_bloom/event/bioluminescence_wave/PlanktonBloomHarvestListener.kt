package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.bloom.PlanktonBloomManager
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object PlanktonBloomHarvestListener {

    @SubscribeEvent
    fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
        val level = event.level
        if (level.isClientSide) return
        if (!event.itemStack.`is`(Items.GLASS_BOTTLE)) return

        val hitResult = fluidRaycast(level, event.entity)
        if (hitResult.type != HitResult.Type.BLOCK) return

        val serverLevel = level as ServerLevel
        val radius = ModServerConfig.BIOLUMINESCENCE_BLOOM_HARVEST_RADIUS.get()
        val manager = PlanktonBloomManager.forLevel(serverLevel)
        if (manager.harvestNear(hitResult.location, radius) !is PlanktonBloomManager.HarvestOutcome.Granted) return

        val player = event.entity
        event.itemStack.consume(1, player)
        val filled = ItemStack(ModItems.PLANKTON_BOTTLE.get())
        if (!player.inventory.add(filled)) {
            player.drop(filled, false)
        }
        level.playSound(
            null, player.x, player.y, player.z,
            SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 1.0f
        )
        player.swing(event.hand)
        event.isCanceled = true
    }

    private fun fluidRaycast(level: Level, player: Player): BlockHitResult {
        val start = player.eyePosition
        val look = player.getViewVector(1.0f)
        val reach = player.blockInteractionRange()
        val end = start.add(look.x * reach, look.y * reach, look.z * reach)
        return level.clip(ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player))
    }
}
