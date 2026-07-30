package fr.heta__h.squ_abyssal_bloom.block.blood_seagrass

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.TallSeagrassBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

class TallBloodSeagrassBlock(properties: BlockBehaviour.Properties) : TallSeagrassBlock(properties) {

    override fun getCloneItemStack(level: LevelReader, pos: BlockPos, state: BlockState, includeData: Boolean): ItemStack {
        return ItemStack(ModBlocks.TALL_BLOOD_SEAGRASS.get())
    }
}