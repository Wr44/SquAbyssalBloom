package fr.heta__h.squ_abyssal_bloom.event.sprouting_seagrass

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.block.blood_seagrass.BloodSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.block.sprouting_seagrass.SproutingSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object HarvestPrismarineBulb {

    @SubscribeEvent
    fun onHarvestPrismarineBulb(event: PlayerInteractEvent.RightClickBlock) {

        val level = event.level
        val pos = event.pos
        val state = level.getBlockState(pos)
        val player = event.entity
        val heldItem = player.getItemInHand(event.hand)

        if (heldItem.`is`(Items.BONE_MEAL)) return
        if (event.hand == InteractionHand.OFF_HAND) return

        val bulbProperty = when (state.block) {
            ModBlocks.SPROUTING_SEAGRASS.get() -> SproutingSeagrassBlock.HAS_BULB
            ModBlocks.BLOOD_SEAGRASS.get() -> BloodSeagrassBlock.HAS_BULB
            else -> return
        }

        if (state.getValue(bulbProperty)) {

            if (!level.isClientSide) {
                Block.popResource(level, pos, ItemStack(ModItems.PRISMARINE_BULB.get()))

                level.setBlock(pos, state.setValue(bulbProperty, false), 3)

                level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 0.8f + level.random.nextFloat() * 0.4f)
            }

            player.swing(event.hand)

            event.isCanceled = true
        }
    }
}