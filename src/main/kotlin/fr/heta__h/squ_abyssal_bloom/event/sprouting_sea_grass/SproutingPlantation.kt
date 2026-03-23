package fr.heta__h.squ_abyssal_bloom.event.sprouting_sea_grass

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object SproutingPlantation {

    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        val player = event.entity
        val level = event.level
        val stack = event.itemStack
        val clickedPos = event.pos
        val face = event.face ?: return

        if (!stack.`is`(Items.SEAGRASS)) return
        if (stack.get(ModDataComponents.IS_SPROUTING.get()) != true) return

        val placePos = clickedPos.relative(face)

        if (level.getBlockState(placePos).block == ModBlocks.SPROUTING_SEAGRASS.get()) return

        val fluidState = level.getFluidState(placePos)
        if (!fluidState.isSource) return
        if (!fluidState.`is`(net.minecraft.tags.FluidTags.WATER)) return

        val targetState = level.getBlockState(placePos)
        if (!targetState.canBeReplaced()) return

        val groundPos = placePos.below()
        val groundState = level.getBlockState(groundPos)
        if (!groundState.isFaceSturdy(level, groundPos, net.minecraft.core.Direction.UP)) return

        val customState = ModBlocks.SPROUTING_SEAGRASS.get().defaultBlockState()
        if (!customState.canSurvive(level, placePos)) return

        if (!level.isClientSide) {
            level.setBlock(
                placePos,
                ModBlocks.SPROUTING_SEAGRASS.get().defaultBlockState(),
                3
            )

            if (!player.abilities.instabuild) {
                stack.shrink(1)
            }
            val sound = ModBlocks.SPROUTING_SEAGRASS.get().defaultBlockState().soundType
            level.playSound(null, placePos, sound.placeSound, SoundSource.BLOCKS, (sound.volume + 1.0f) / 2.0f, sound.pitch * 0.8f)
        }

        player.swing(event.hand)
        event.isCanceled = true
    }


    @SubscribeEvent
    fun onItemTooltip(event: net.neoforged.neoforge.event.entity.player.ItemTooltipEvent) {
        val stack = event.itemStack

        if (stack.`is`(Items.SEAGRASS) && stack.get(ModDataComponents.IS_SPROUTING.get()) == true) {
            event.toolTip.add(
                Component.translatable("tooltip.squ_abyssal_bloom.sprouting_gene")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC)
            )        }
    }
}