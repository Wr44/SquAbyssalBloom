package fr.heta__h.squ_abyssal_bloom.event.sprouting_seagrass

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.block.blood_seagrass.BloodSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object SeagrassPlantation {

    private data class Variant(
        val seedItem: net.minecraft.world.item.Item,
        val placedBlock: () -> Block,
        val applyGene: (BlockState) -> BlockState = { it }
    )

    private val VARIANTS by lazy {
        listOf(
            Variant(Items.SEAGRASS, { ModBlocks.SPROUTING_SEAGRASS.get() }),
            Variant(ModBlocks.BLOOD_SEAGRASS.get().asItem(), { ModBlocks.BLOOD_SEAGRASS.get() }) { state ->
                state.setValue(BloodSeagrassBlock.SPROUTING, true)
            }
        )
    }

    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        val player = event.entity
        val level = event.level
        val stack = event.itemStack
        val clickedPos = event.pos
        val face = event.face ?: return

        val variant = VARIANTS.firstOrNull { stack.`is`(it.seedItem) } ?: return

        val placePos = clickedPos.relative(face)
        val placeState = level.getBlockState(placePos)

        if (placeState.block == variant.placedBlock()) {
            event.isCanceled = true
            event.cancellationResult = InteractionResult.CONSUME
            return
        }

        if (stack.get(ModDataComponents.IS_SPROUTING.get()) != true) return

        event.isCanceled = true
        event.cancellationResult = InteractionResult.CONSUME

        if (placeState.`is`(Blocks.SEAGRASS) || placeState.`is`(Blocks.TALL_SEAGRASS)) return

        val fluidState = level.getFluidState(placePos)
        if (!fluidState.isSource) return
        if (!fluidState.`is`(FluidTags.WATER)) return

        if (!placeState.canBeReplaced()) return

        val groundPos = placePos.below()
        val groundState = level.getBlockState(groundPos)
        if (!groundState.isFaceSturdy(level, groundPos, Direction.UP)) return

        val customState = variant.applyGene(variant.placedBlock().defaultBlockState())
        if (!customState.canSurvive(level, placePos)) return

        if (!level.isClientSide) {
            level.setBlock(placePos, customState, 3)
            stack.consume(1, player)
            val sound = customState.soundType
            level.playSound(null, placePos, sound.placeSound, SoundSource.BLOCKS, (sound.volume + 1.0f) / 2.0f, sound.pitch * 0.8f)
        }
        player.swing(event.hand)
    }

    @SubscribeEvent
    fun onItemTooltip(event: ItemTooltipEvent) {
        val stack = event.itemStack

        val isSeedItem = stack.`is`(Items.SEAGRASS) || stack.`is`(ModBlocks.BLOOD_SEAGRASS.get().asItem())

        if (isSeedItem && stack.get(ModDataComponents.IS_SPROUTING.get()) == true) {
            event.toolTip.add(
                Component.translatable("tooltip.squ_abyssal_bloom.sprouting_gene")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC)
            )
        }
    }
}