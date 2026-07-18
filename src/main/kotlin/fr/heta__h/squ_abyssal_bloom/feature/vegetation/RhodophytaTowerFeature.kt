package fr.heta__h.squ_abyssal_bloom.feature.vegetation

import com.mojang.serialization.Codec
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Plane
import net.minecraft.util.RandomSource
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.BaseCoralWallFanBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration
import kotlin.math.pow

private const val MAX_BRANCH_DEPTH = 4
private const val BASE_SUB_BRANCH_CHANCE = 0.35f
private const val DEPTH_DECAY = 0.65f
private const val BASE_LEVELS = 5
private const val BASE_RADIUS = 2.0
private const val BRANCH_SPAWN_CHANCE = 0.35f
private const val BRANCH_UP_BIAS = 0.35f
private const val KNOT_CHANCE = 0.06f
private const val BRANCH_SIDE_DECORATE_CHANCE = 0.35f
private const val BRANCH_TOP_DECORATE_CHANCE = 0.2f

class RhodophytaTowerFeature(codec: Codec<NoneFeatureConfiguration>) : Feature<NoneFeatureConfiguration>(codec) {

    override fun place(context: FeaturePlaceContext<NoneFeatureConfiguration>): Boolean {
        val level = context.level()
        val random = context.random()
        val origin = context.origin()
        val state = ModBlocks.RHODOPHYTA.get().defaultBlockState()

        val mutPos = origin.mutable()
        val trunkHeight = random.nextInt(15) + 10
        var reachedHeight = 0

        for (i in 0 until trunkHeight) {
            if (!placeAlgaeBlock(level, mutPos, state)) {
                break
            }
            reachedHeight = i + 1

            decorateSides(level, random, mutPos, reservedDirections = emptySet(), chance = BRANCH_SIDE_DECORATE_CHANCE)

            if (i < BASE_LEVELS) {
                val currentRadius = BASE_RADIUS * (1.0 - (i.toDouble() / BASE_LEVELS))
                val radiusSq = currentRadius * currentRadius

                if (radiusSq > 0) {
                    val bound = Math.ceil(currentRadius).toInt()
                    for (dx in -bound..bound) {
                        for (dz in -bound..bound) {
                            if (dx == 0 && dz == 0) continue
                            if ((dx * dx + dz * dz) <= radiusSq) {
                                placeAlgaeBlock(level, mutPos.offset(dx, 0, dz), state)
                            }
                        }
                    }
                }
            } else if (random.nextFloat() < KNOT_CHANCE) {
                placeTuft(level, mutPos, state)
            }

            val heightFactor = (i.toFloat() / trunkHeight).coerceIn(0f, 1f)

            if (random.nextFloat() < BRANCH_SPAWN_CHANCE) {
                val nBranchesAtLevel = random.nextInt(2) + 1
                val chosenDirections = mutableSetOf<Direction>()
                while (chosenDirections.size < nBranchesAtLevel) {
                    chosenDirections.add(Plane.HORIZONTAL.getRandomDirection(random))
                }

                for (branchDirection in chosenDirections) {
                    placeBranch(level, random, mutPos, state, branchDirection, depth = 0, heightFactor = heightFactor)
                }
            }

            mutPos.move(Direction.UP)
        }

        if (reachedHeight == 0) return false

        val topBranchesCount = random.nextInt(3) + 4
        val topDirections = mutableSetOf<Direction>()
        while (topDirections.size < topBranchesCount.coerceAtMost(4)) {
            topDirections.add(Plane.HORIZONTAL.getRandomDirection(random))
        }
        for (branchDirection in topDirections) {
            placeBranch(level, random, mutPos, state, branchDirection, depth = 0, heightFactor = 1f)
        }

        return true
    }

    private fun placeBranch(
        level: LevelAccessor,
        random: RandomSource,
        start: BlockPos,
        state: BlockState,
        direction: Direction,
        depth: Int,
        heightFactor: Float = 1f
    ) {
        val mutPos = start.mutable()
        mutPos.move(direction)

        val minLen = 2 + (heightFactor * 2).toInt()
        val maxLen = 4 + (heightFactor * 5).toInt()
        val branchLength = random.nextInt((maxLen - minLen + 1).coerceAtLeast(1)) + minLen
        var segmentLength = 0
        val subBranchChance = BASE_SUB_BRANCH_CHANCE * DEPTH_DECAY.pow(depth)

        val perpendicular = if (direction == Direction.NORTH || direction == Direction.SOUTH) Direction.EAST else Direction.NORTH

        var lastPos: BlockPos? = null
        var j = 0
        while (j < branchLength && placeAlgaeBlock(level, mutPos, state)) {
            val thicknessT = 1f - (j.toFloat() / branchLength)
            if (thicknessT > 0.35f) {
                placeAlgaeBlock(level, mutPos.relative(perpendicular), state)
            }

            decorateSides(level, random, mutPos, reservedDirections = setOf(direction, direction.opposite), chance = BRANCH_SIDE_DECORATE_CHANCE)
            if (random.nextFloat() < BRANCH_TOP_DECORATE_CHANCE) {
                val abovePos = mutPos.above()
                if (level.getBlockState(abovePos).`is`(Blocks.WATER)) {
                    level.setBlock(abovePos, Blocks.FIRE_CORAL.defaultBlockState(), 2)
                }
            }

            lastPos = mutPos.immutable()
            segmentLength++
            mutPos.move(direction)
            if (j == 0 || (segmentLength >= 2 && random.nextFloat() < BRANCH_UP_BIAS)) {
                mutPos.move(Direction.UP)
                segmentLength = 0
            }

            if (depth < MAX_BRANCH_DEPTH && j > 0 && random.nextFloat() < subBranchChance) {
                val subDirection = if (random.nextBoolean()) direction.clockWise else direction.counterClockWise
                placeBranch(level, random, mutPos, state, subDirection, depth = depth + 1, heightFactor = heightFactor)
            }

            j++
        }

        lastPos?.let { placeTuft(level, it, state) }
    }

    private fun placeTuft(level: LevelAccessor, center: BlockPos, state: BlockState) {
        for (direction in Direction.entries) {
            placeAlgaeBlock(level, center.relative(direction), state)
        }
    }

    private fun placeAlgaeBlock(level: LevelAccessor, pos: BlockPos, state: BlockState): Boolean {
        val above = pos.above()
        val targetBlockState = level.getBlockState(pos)

        if ((targetBlockState.`is`(Blocks.WATER) || targetBlockState.`is`(state.block)) && level.getBlockState(above).`is`(Blocks.WATER)) {
            level.setBlock(pos, state, 3)
            return true
        }

        return false
    }

    private fun decorateSides(level: LevelAccessor, random: RandomSource, pos: BlockPos, reservedDirections: Set<Direction>, chance: Float) {
        for (direction in Plane.HORIZONTAL) {
            if (direction in reservedDirections) continue

            if (random.nextFloat() < chance) {
                val relativePos = pos.relative(direction)
                if (level.getBlockState(relativePos).`is`(Blocks.WATER)) {
                    val coralFanState = Blocks.FIRE_CORAL_WALL_FAN.defaultBlockState()
                        .setValue(BaseCoralWallFanBlock.FACING, direction)
                        .setValue(BlockStateProperties.WATERLOGGED, true)

                    level.setBlock(relativePos, coralFanState, 2)
                }
            }
        }
    }
}