package fr.heta__h.squ_abyssal_bloom.block.brine_bubble_column

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BubbleColumnBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty


class BrineBubbleColumnBlock(properties: Properties) : BubbleColumnBlock(properties) {
    companion object {
        val REFRESHED: BooleanProperty = BooleanProperty.create("refreshed")
        const val STALE_CHECK_TICKS = 20
    }

    init {
        registerDefaultState(defaultBlockState().setValue(REFRESHED, true))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(REFRESHED)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (!state.getValue(REFRESHED)) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), UPDATE_ALL)
            return
        }

        level.setBlock(pos, state.setValue(REFRESHED, false), UPDATE_NONE)
        level.scheduleTick(pos, this, STALE_CHECK_TICKS)
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean = true
}