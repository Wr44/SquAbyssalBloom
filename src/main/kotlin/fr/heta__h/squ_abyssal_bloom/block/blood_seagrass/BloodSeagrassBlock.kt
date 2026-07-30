package fr.heta__h.squ_abyssal_bloom.block.blood_seagrass

import fr.heta__h.squ_abyssal_bloom.block.ModBlockStateProperties
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getEnchantLevel
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SeagrassBlock
import net.minecraft.world.level.block.TallSeagrassBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.neoforged.neoforge.common.ItemAbilities

class BloodSeagrassBlock(properties: Properties) : SeagrassBlock(properties) {

    companion object {
        val HAS_BULB: BooleanProperty = BooleanProperty.create("has_bulb")
        val SPROUTING: BooleanProperty = ModBlockStateProperties.SPROUTING
    }

    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HAS_BULB, false)
                .setValue(SPROUTING, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(HAS_BULB, SPROUTING)
    }

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (state.getValue(SPROUTING) && !state.getValue(HAS_BULB) && random.nextInt(8) == 0) {
            level.setBlock(pos, state.setValue(HAS_BULB, true), 3)
            level.sendBlockUpdated(pos, state, state.setValue(HAS_BULB, true), 3)
        }
    }

    override fun isValidBonemealTarget(level: LevelReader, pos: BlockPos, state: BlockState): Boolean {
        if (state.getValue(SPROUTING)) {
            return !state.getValue(HAS_BULB)
        }
        return level.getBlockState(pos.above()).`is`(Blocks.WATER)
    }

    override fun isBonemealSuccess(level: net.minecraft.world.level.Level, random: RandomSource, pos: BlockPos, state: BlockState): Boolean = true

    override fun performBonemeal(level: ServerLevel, random: RandomSource, pos: BlockPos, state: BlockState) {
        if (state.getValue(SPROUTING) && !state.getValue(HAS_BULB)) {
            level.setBlock(pos, state.setValue(HAS_BULB, true), 3)
            level.sendBlockUpdated(pos, state, state.setValue(HAS_BULB, true), 3)
            return
        }

        val lowerState = ModBlocks.TALL_BLOOD_SEAGRASS.get().defaultBlockState()
        val upperState = lowerState.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER)
        val above = pos.above()
        level.setBlock(pos, lowerState, 2)
        level.setBlock(above, upperState, 2)
    }

    override fun getDrops(state: BlockState, builder: LootParams.Builder): MutableList<ItemStack> {
        val loot = mutableListOf<ItemStack>()

        val tool = builder.getOptionalParameter(LootContextParams.TOOL) ?: ItemStack.EMPTY
        val toolStack = tool as? ItemStack ?: ItemStack.EMPTY

        val isSprouting = state.getValue(SPROUTING)
        val isShears = toolStack.canPerformAction(ItemAbilities.SHEARS_HARVEST)
        val hasSilkTouch = getEnchantLevel(toolStack, builder.level, "silk_touch", "minecraft") > 0

        val shouldDrop = if (isSprouting) isShears || hasSilkTouch else isShears

        if (shouldDrop) {
            val grassStack = ItemStack(ModBlocks.BLOOD_SEAGRASS.get())
            if (isSprouting) {
                grassStack.set(ModDataComponents.IS_SPROUTING.get(), true)
            }
            loot.add(grassStack)
        }

        return loot
    }

    override fun getCloneItemStack(level: LevelReader, pos: BlockPos, state: BlockState, includeData: Boolean): ItemStack {
        val stack = ItemStack(ModBlocks.BLOOD_SEAGRASS.get())
        if (state.getValue(SPROUTING)) {
            stack.set(ModDataComponents.IS_SPROUTING.get(), true)
        }
        return stack
    }

    override fun getParticlePos(pos: BlockPos): BlockPos {
        return pos
    }
}