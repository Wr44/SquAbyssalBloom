package fr.heta__h.squ_abyssal_bloom.block.underwater_torch

import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.TorchBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids

class UnderwaterTorchBlock(properties: Properties) :
    TorchBlock(ModParticles.UNDERWATER_CRYSTAL_GAZ.get(), properties),
    SimpleWaterloggedBlock {

    companion object {
        @JvmField
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
    }

    init {
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, true))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(WATERLOGGED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        if (!context.level.getFluidState(context.clickedPos).`is`(Fluids.WATER)) {
            return null
        }

        return super.getStateForPlacement(context)?.setValue(WATERLOGGED, true)
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        return level.getFluidState(pos).`is`(Fluids.WATER) && super.canSurvive(state, level, pos)
    }

    override fun updateShape(
        state: BlockState,
        level: LevelReader,
        ticks: ScheduledTickAccess,
        pos: BlockPos,
        directionToNeighbour: Direction,
        neighbourPos: BlockPos,
        neighbourState: BlockState,
        random: RandomSource
    ): BlockState {
        if (!state.getValue(WATERLOGGED)) {
            return Blocks.AIR.defaultBlockState()
        }

        ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        return super.updateShape(
            state,
            level,
            ticks,
            pos,
            directionToNeighbour,
            neighbourPos,
            neighbourState,
            random
        )
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) {
            Fluids.WATER.getSource(false)
        } else {
            super.getFluidState(state)
        }
    }

    override fun pickupBlock(
        user: LivingEntity?,
        level: LevelAccessor,
        pos: BlockPos,
        state: BlockState
    ): ItemStack = ItemStack.EMPTY

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        val x = pos.x + 0.5
        val y = pos.y + 0.7
        val z = pos.z + 0.5

        level.addParticle(ModParticles.UNDERWATER_TORCH_BUBBLE.get(), x, y, z, 0.0, 0.0, 0.0)
        level.addParticle(flameParticle, x, y, z, 0.0, 0.0, 0.0)
    }
}
