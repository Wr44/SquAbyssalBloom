package fr.heta__h.squ_abyssal_bloom.block.calcareous_deposit

import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


class CalcareousDepositBlock(properties: Properties) : Block(properties), SimpleWaterloggedBlock {

    companion object {
        @JvmField
        val AGE: IntegerProperty = BlockStateProperties.AGE_3

        @JvmField
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED

        @JvmField
        val FACING = BlockStateProperties.HORIZONTAL_FACING

        const val MAX_AGE = 3

        private data class Cuboid(
            val minX: Double, val minY: Double, val minZ: Double,
            val maxX: Double, val maxY: Double, val maxZ: Double
        )

        private val STAGE_CUBOIDS: Array<Array<Cuboid>> = arrayOf(
            arrayOf(Cuboid(6.0, 0.0, 6.0, 10.0, 2.0, 10.0)),
            arrayOf(
                Cuboid(4.0, 0.0, 5.0, 10.0, 3.0, 11.0),
                Cuboid(9.0, 0.0, 7.0, 13.0, 2.0, 12.0)
            ),
            arrayOf(
                Cuboid(2.0, 0.0, 4.0, 10.0, 4.0, 12.0),
                Cuboid(8.0, 0.0, 2.0, 14.0, 3.0, 9.0),
                Cuboid(9.0, 0.0, 9.0, 13.0, 5.0, 14.0)
            ),
            arrayOf(
                Cuboid(1.0, 0.0, 3.0, 10.0, 5.0, 13.0),
                Cuboid(7.0, 0.0, 1.0, 15.0, 4.0, 10.0),
                Cuboid(8.0, 0.0, 8.0, 14.0, 7.0, 15.0),
                Cuboid(3.0, 0.0, 10.0, 8.0, 3.0, 15.0)
            )
        )

        private val SHAPES: Map<Direction, Array<VoxelShape>> = mapOf(
            Direction.NORTH to createStageShapes(0),
            Direction.EAST to createStageShapes(1),
            Direction.SOUTH to createStageShapes(2),
            Direction.WEST to createStageShapes(3)
        )

        private fun createStageShapes(quarterTurns: Int): Array<VoxelShape> =
            Array(STAGE_CUBOIDS.size) { age ->
                var result = Shapes.empty()
                for (cuboid in STAGE_CUBOIDS[age]) {
                    val rotated = rotate(cuboid, quarterTurns)
                    result = Shapes.or(
                        result,
                        box(
                            rotated.minX,
                            rotated.minY,
                            rotated.minZ,
                            rotated.maxX,
                            rotated.maxY,
                            rotated.maxZ
                        )
                    )
                }
                result.optimize()
            }

        private fun rotate(original: Cuboid, quarterTurns: Int): Cuboid {
            var rotated = original
            repeat(Math.floorMod(quarterTurns, 4)) {
                rotated = Cuboid(
                    minX = 16.0 - rotated.maxZ,
                    minY = rotated.minY,
                    minZ = rotated.minX,
                    maxX = 16.0 - rotated.minZ,
                    maxY = rotated.maxY,
                    maxZ = rotated.maxX
                )
            }
            return rotated
        }
    }

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(AGE, 0)
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, true)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(AGE, FACING, WATERLOGGED)
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        val supportPos = pos.below()
        val support = level.getBlockState(supportPos)
        return support.`is`(ModTags.Blocks.CALCAREOUS_DEPOSIT_SUPPORTS) &&
            support.isFaceSturdy(level, supportPos, Direction.UP)
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

        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level))

        if (directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState()

        return super.updateShape(
            state,
            level, ticks, pos,
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

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = SHAPES.getValue(state.getValue(FACING))[state.getValue(AGE)]

    @Deprecated("Minecraft still calls this hook for pick-block")
    override fun getCloneItemStack(
        level: LevelReader,
        pos: BlockPos,
        state: BlockState,
        includeData: Boolean
    ): ItemStack = ItemStack(ModItems.CALCAREOUS_FRAGMENT.get())
}
