package fr.heta__h.squ_abyssal_bloom.block.sprouting_sea_grass

import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getEnchantLevel
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SeagrassBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.neoforged.neoforge.common.ItemAbilities


class SproutingSeagrassBlock(properties: Properties) : SeagrassBlock(properties) {

    companion object {
        val HAS_BULB: BooleanProperty = BooleanProperty.create("has_bulb")
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
    }

    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HAS_BULB, false)
                .setValue(WATERLOGGED, true)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(HAS_BULB, WATERLOGGED)
    }

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        super.randomTick(state, level, pos, random)

        if (!state.getValue(HAS_BULB)) {
            if (random.nextInt(8) == 0) {
                level.setBlock(pos, state.setValue(HAS_BULB, true), 3)
                level.sendBlockUpdated(pos, state, state.setValue(HAS_BULB, true), 3)
            }
        }
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false)
        else super.getFluidState(state)
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        return level.getFluidState(pos).isSource && super.canSurvive(state, level, pos)
    }

    override fun getDrops(state: BlockState, builder: LootParams.Builder): MutableList<ItemStack> {
        val loot = mutableListOf<ItemStack>()

        val tool = builder.getOptionalParameter(LootContextParams.TOOL) ?: ItemStack.EMPTY

        val toolStack = tool as? ItemStack ?: ItemStack.EMPTY

        val hasBulb = state.getValue(HAS_BULB)

        if (hasBulb) {
            loot.add(ItemStack(ModItems.PRISMARINE_BULB.get()))
        }

        val isShears = toolStack.canPerformAction(ItemAbilities.SHEARS_HARVEST)

        val hasSilkTouch = getEnchantLevel(toolStack, builder.level, "silk_touch", "minecraft") > 0

        if (isShears || hasSilkTouch) {
            val grassStack = ItemStack(Items.SEAGRASS)


            grassStack.set(ModDataComponents.IS_SPROUTING.get(), true)


            loot.add(grassStack)
        }

        return loot
    }

    override fun isValidBonemealTarget(level: LevelReader, pos: BlockPos, state: BlockState): Boolean {
        return !state.getValue(HAS_BULB)
    }

    override fun isBonemealSuccess(level: Level, random: RandomSource, pos: BlockPos, state: BlockState): Boolean = true

    override fun performBonemeal(level: ServerLevel, random: RandomSource, pos: BlockPos, state: BlockState) {
        if (state.getValue(HAS_BULB)) return
        level.setBlock(pos, state.setValue(HAS_BULB, true), 3)
        level.sendBlockUpdated(pos, state, state.setValue(HAS_BULB, true), 3)
    }

    override fun getCloneItemStack(level: LevelReader, pos: BlockPos, state: BlockState, includeData: Boolean): ItemStack {
        val stack = ItemStack(Items.SEAGRASS)

        stack.set(ModDataComponents.IS_SPROUTING.get(), true)
        return stack
    }

    override fun getParticlePos(pos: BlockPos): BlockPos {
        return pos
    }
}
