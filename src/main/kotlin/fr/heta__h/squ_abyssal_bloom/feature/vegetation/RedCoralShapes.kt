package fr.heta__h.squ_abyssal_bloom.feature.vegetation

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Plane
import net.minecraft.tags.BlockTags
import net.minecraft.util.RandomSource
import net.minecraft.util.Util
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.BaseCoralWallFanBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SeaPickleBlock
import net.minecraft.world.level.block.state.BlockState

object RedCoralShapes {

    fun placeCoralBlock(level: LevelAccessor, random: RandomSource, pos: BlockPos, state: BlockState): Boolean {
        val above = pos.above()
        val targetBlockState = level.getBlockState(pos)

        if ((targetBlockState.`is`(Blocks.WATER) || targetBlockState.`is`(BlockTags.CORALS)) && level.getBlockState(above).`is`(Blocks.WATER)) {
            level.setBlock(pos, state, 3)

            if (random.nextFloat() < 0.25f) {
                level.setBlock(above, Blocks.FIRE_CORAL.defaultBlockState(), 2)
            } else if (random.nextFloat() < 0.05f) {
                level.setBlock(above, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, random.nextInt(4) + 1), 2)
            }

            for (direction in Plane.HORIZONTAL) {
                if (random.nextFloat() < 0.2f) {
                    val relativePos = pos.relative(direction)
                    if (level.getBlockState(relativePos).`is`(Blocks.WATER)) {
                        var coralFanState = Blocks.FIRE_CORAL_WALL_FAN.defaultBlockState()
                        if (coralFanState.hasProperty(BaseCoralWallFanBlock.FACING)) {
                            coralFanState = coralFanState.setValue(BaseCoralWallFanBlock.FACING, direction)
                        }
                        level.setBlock(relativePos, coralFanState, 2)
                    }
                }
            }

            return true
        }

        return false
    }

    fun placeTree(level: LevelAccessor, random: RandomSource, origin: BlockPos, state: BlockState): Boolean {
        val mutPos = origin.mutable()
        val trunckHeight = random.nextInt(3) + 1

        for (i in 0 until trunckHeight) {
            if (!placeCoralBlock(level, random, mutPos, state)) {
                return true
            }
            mutPos.move(Direction.UP)
        }

        val trunckTopPos = mutPos.immutable()
        val nBranches = random.nextInt(3) + 2
        val directions = Plane.HORIZONTAL.shuffledCopy(random)

        for (branchDirection in directions.subList(0, nBranches)) {
            mutPos.set(trunckTopPos)
            mutPos.move(branchDirection)
            val branchHeight = random.nextInt(5) + 2
            var segmentLength = 0

            var j = 0
            while (j < branchHeight && placeCoralBlock(level, random, mutPos, state)) {
                segmentLength++
                mutPos.move(Direction.UP)
                if (j == 0 || segmentLength >= 2 && random.nextFloat() < 0.25f) {
                    mutPos.move(branchDirection)
                    segmentLength = 0
                }
                j++
            }
        }

        return true
    }

    fun placeClaw(level: LevelAccessor, random: RandomSource, origin: BlockPos, state: BlockState): Boolean {
        if (!placeCoralBlock(level, random, origin, state)) {
            return false
        }

        val clawDirection = Plane.HORIZONTAL.getRandomDirection(random)
        val nBranches = random.nextInt(2) + 2
        val possibleDirections = Util.toShuffledList(
            java.util.stream.Stream.of(clawDirection, clawDirection.clockWise, clawDirection.counterClockWise),
            random
        )

        for (branchDirection in possibleDirections.subList(0, nBranches)) {
            val mutPos = origin.mutable()
            val sidewayLength = random.nextInt(2) + 1
            mutPos.move(branchDirection)

            val inwayLenth: Int
            val segmentDirection: Direction

            if (branchDirection == clawDirection) {
                segmentDirection = clawDirection
                inwayLenth = random.nextInt(3) + 2
            } else {
                mutPos.move(Direction.UP)
                val segmentPossibleDirections = arrayOf(branchDirection, Direction.UP)
                segmentDirection = Util.getRandom(segmentPossibleDirections, random)
                inwayLenth = random.nextInt(3) + 3
            }

            var i = 0
            while (i < sidewayLength && placeCoralBlock(level, random, mutPos, state)) {
                mutPos.move(segmentDirection)
                i++
            }

            mutPos.move(segmentDirection.opposite)
            mutPos.move(Direction.UP)

            for (k in 0 until inwayLenth) {
                mutPos.move(clawDirection)
                if (!placeCoralBlock(level, random, mutPos, state)) {
                    break
                }
                if (random.nextFloat() < 0.25f) {
                    mutPos.move(Direction.UP)
                }
            }
        }

        return true
    }

    fun placeMushroom(level: LevelAccessor, random: RandomSource, origin: BlockPos, state: BlockState): Boolean {
        val height = random.nextInt(3) + 3
        val width = random.nextInt(3) + 3
        val length = random.nextInt(3) + 3
        val sinkValue = random.nextInt(3) + 1
        val mutPos = origin.mutable()

        for (x in 0..width) {
            for (y in 0..height) {
                for (z in 0..length) {
                    mutPos.set(x + origin.x, y + origin.y, z + origin.z)
                    mutPos.move(Direction.DOWN, sinkValue)

                    val isShell = (x != 0 && x != width || y != 0 && y != height) &&
                            (z != 0 && z != length || y != 0 && y != height) &&
                            (x != 0 && x != width || z != 0 && z != length) &&
                            (x == 0 || x == width || y == 0 || y == height || z == 0 || z == length)

                    if (isShell && random.nextFloat() >= 0.1f) {
                        placeCoralBlock(level, random, mutPos, state)
                    }
                }
            }
        }

        return true
    }
}