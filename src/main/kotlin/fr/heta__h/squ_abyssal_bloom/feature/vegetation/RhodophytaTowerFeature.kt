package fr.heta__h.squ_abyssal_bloom.feature.vegetation

import com.mojang.serialization.Codec
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Plane
import net.minecraft.util.RandomSource
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration
import kotlin.math.abs
import kotlin.math.pow

private const val MAX_BRANCH_DEPTH = 6
private const val BASE_SUB_BRANCH_CHANCE = 0.65f
private const val DEPTH_DECAY = 0.75f
private const val BASE_LEVELS = 5
private const val BASE_RADIUS = 2

class RhodophytaTowerFeature(codec: Codec<NoneFeatureConfiguration>) : Feature<NoneFeatureConfiguration>(codec) {

    override fun place(context: FeaturePlaceContext<NoneFeatureConfiguration>): Boolean {
        val level = context.level()
        val random = context.random()
        val origin = context.origin()
        val state = ModBlocks.RHODOPHYTA.get().defaultBlockState()

        val mutPos = origin.mutable()
        val trunkHeight = random.nextInt(6) + 15
        var reachedHeight = 0

        for (i in 0 until trunkHeight) {
            if (!RedCoralShapes.placeCoralBlock(level, random, mutPos, state)) {
                break
            }
            reachedHeight = i + 1

            if (i < BASE_LEVELS) {
                val radius = BASE_RADIUS - (i * BASE_RADIUS / BASE_LEVELS)
                if (radius > 0) {
                    for (dx in -radius..radius) {
                        for (dz in -radius..radius) {
                            if (dx == 0 && dz == 0) continue
                            if (abs(dx) + abs(dz) > radius) continue
                            RedCoralShapes.placeCoralBlock(level, random, mutPos.offset(dx, 0, dz), state)
                        }
                    }
                }
            }

            if (i >= 2 && random.nextFloat() < 0.65f) {
                val nBranchesAtLevel = random.nextInt(3) + 1
                repeat(nBranchesAtLevel) {
                    val branchDirection = Plane.HORIZONTAL.getRandomDirection(random)
                    placeBranch(level, random, mutPos.immutable(), state, branchDirection, depth = 0)
                }
            }

            mutPos.move(Direction.UP)
        }

        if (reachedHeight == 0) return false

        repeat(random.nextInt(3) + 4) {
            val branchDirection = Plane.HORIZONTAL.getRandomDirection(random)
            placeBranch(level, random, mutPos.immutable(), state, branchDirection, depth = 0)
        }

        return true
    }

    private fun placeBranch(
        level: LevelAccessor,
        random: RandomSource,
        start: BlockPos,
        state: BlockState,
        direction: Direction,
        depth: Int
    ) {
        val mutPos = start.mutable()
        mutPos.move(direction)

        val branchLength = random.nextInt(5) + 3
        var segmentLength = 0
        val subBranchChance = BASE_SUB_BRANCH_CHANCE * DEPTH_DECAY.pow(depth)

        var j = 0
        while (j < branchLength && RedCoralShapes.placeCoralBlock(level, random, mutPos, state)) {
            segmentLength++
            mutPos.move(direction)
            if (j == 0 || segmentLength >= 2 && random.nextFloat() < 0.3f) {
                mutPos.move(Direction.UP)
                segmentLength = 0
            }

            if (depth < MAX_BRANCH_DEPTH && j > 0 && random.nextFloat() < subBranchChance) {
                val subDirection = Plane.HORIZONTAL.getRandomDirection(random)
                placeBranch(level, random, mutPos.immutable(), state, subDirection, depth = depth + 1)
            }

            j++
        }
    }
}