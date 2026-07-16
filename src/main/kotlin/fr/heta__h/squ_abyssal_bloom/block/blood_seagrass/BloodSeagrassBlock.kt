package fr.heta__h.squ_abyssal_bloom.block.blood_seagrass

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.SeagrassBlock
import net.minecraft.world.level.block.TallSeagrassBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

class BloodSeagrassBlock(properties: Properties) : SeagrassBlock(properties) {

    override fun performBonemeal(level: ServerLevel, random: RandomSource, pos: BlockPos, state: BlockState) {
        val lowerState = ModBlocks.TALL_BLOOD_SEAGRASS.get().defaultBlockState()
        val upperState = lowerState.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER)
        val above = pos.above()
        level.setBlock(pos, lowerState, 2)
        level.setBlock(above, upperState, 2)
    }
}