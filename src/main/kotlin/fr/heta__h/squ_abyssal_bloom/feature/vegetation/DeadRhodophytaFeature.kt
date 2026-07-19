package fr.heta__h.squ_abyssal_bloom.feature.vegetation

import com.mojang.serialization.Codec
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Plane
import net.minecraft.util.RandomSource
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration

private const val BASE_LEVELS = 4
private const val BASE_RADIUS = 1.6
private const val BRANCH_SPAWN_CHANCE = 0.3f
private const val MAX_LEAN = 6

class DeadRhodophytaFeature(codec: Codec<NoneFeatureConfiguration>) : Feature<NoneFeatureConfiguration>(codec) {

    override fun place(context: FeaturePlaceContext<NoneFeatureConfiguration>): Boolean {
        val level = context.level()
        val random = context.random()
        val origin = context.origin()
        val state = ModBlocks.DEAD_RHODOPHYTA.get().defaultBlockState()

        val leanDirection = Plane.HORIZONTAL.getRandomDirection(random)
        val trunkHeight = random.nextInt(12) + 12
        var reachedHeight = 0
        var appliedLean = 0

        val basePos = origin.mutable()

        for (i in 0 until trunkHeight) {
            val t = i.toFloat() / trunkHeight
            val targetLean = (t * t * MAX_LEAN).toInt()
            while (appliedLean < targetLean) {
                basePos.move(leanDirection)
                appliedLean++
            }

            if (!placeAlgaeBlock(level, basePos, state)) {
                break
            }
            reachedHeight = i + 1

            if (i < BASE_LEVELS) {
                val currentRadius = BASE_RADIUS * (1.0 - (i.toDouble() / BASE_LEVELS))
                val radiusSq = currentRadius * currentRadius

                if (radiusSq > 0) {
                    val bound = Math.ceil(currentRadius).toInt()
                    for (dx in -bound..bound) {
                        for (dz in -bound..bound) {
                            if (dx == 0 && dz == 0) continue
                            if ((dx * dx + dz * dz) <= radiusSq && random.nextFloat() < 0.75f) {
                                placeAlgaeBlock(level, basePos.offset(dx, 0, dz), state)
                            }
                        }
                    }
                }
            }

            if (i >= 2 && random.nextFloat() < BRANCH_SPAWN_CHANCE) {
                val branchDirection = Plane.HORIZONTAL.getRandomDirection(random)
                placeDroopingBranch(level, random, basePos.immutable(), state, branchDirection)
            }

            basePos.move(Direction.UP)
        }

        if (reachedHeight == 0) return false

        scatterDebris(level, random, origin, state)

        return true
    }

    private fun placeDroopingBranch(
        level: LevelAccessor,
        random: RandomSource,
        start: BlockPos,
        state: BlockState,
        direction: Direction
    ) {
        val mutPos = start.mutable()
        mutPos.move(direction)

        val outLength = random.nextInt(2) + 1
        var j = 0
        while (j < outLength && placeAlgaeBlock(level, mutPos, state)) {
            mutPos.move(direction)
            j++
        }

        var k = 0
        while (k < 30) {
            val below = mutPos.below()
            if (level.getBlockState(below).isSolid) {
                break
            }
            if (!placeAlgaeBlock(level, mutPos, state)) {
                break
            }
            mutPos.move(Direction.DOWN)
            k++
        }
    }

    private fun scatterDebris(level: LevelAccessor, random: RandomSource, origin: BlockPos, state: BlockState) {
        val debrisCount = random.nextInt(6) + 5
        repeat(debrisCount) {
            val dx = random.nextInt(9) - random.nextInt(9)
            val dz = random.nextInt(9) - random.nextInt(9)
            val debrisPos = origin.offset(dx, 0, dz)

            if (level.getBlockState(debrisPos).`is`(Blocks.WATER) && level.getBlockState(debrisPos.below()).isSolid) {
                level.setBlock(debrisPos, state, 2)
                if (random.nextFloat() < 0.4f) {
                    val secondPos = debrisPos.relative(Plane.HORIZONTAL.getRandomDirection(random))
                    if (level.getBlockState(secondPos).`is`(Blocks.WATER)) {
                        level.setBlock(secondPos, state, 2)
                    }
                }
            }
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
}