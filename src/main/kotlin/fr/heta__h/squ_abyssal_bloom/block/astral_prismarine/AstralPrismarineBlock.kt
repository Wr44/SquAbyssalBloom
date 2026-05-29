package fr.heta__h.squ_abyssal_bloom.block.astral_prismarine

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty

class AstralPrismarineBlock(properties: Properties) : Block(properties) {
    companion object {
        @JvmField
        val ACTIVE: BooleanProperty = BlockStateProperties.LIT
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(ACTIVE)
    }
}