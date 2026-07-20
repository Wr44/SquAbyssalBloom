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
import kotlin.math.cos
import kotlin.math.sin

private const val BASE_LEVELS = 4
private const val BASE_RADIUS = 1.6
private const val HOOK_VERTICAL_STEPS = 6
private const val HOOK_MAX_HORIZONTAL = 6
private const val MIN_DROP = 2
private const val MAX_DROP = 6
private const val GROUND_CLEARANCE = 4
private const val TRUNK_BRANCH_CHANCE = 0.25f

class DeadRhodophytaFeature(codec: Codec<NoneFeatureConfiguration>) : Feature<NoneFeatureConfiguration>(codec) {

    override fun place(context: FeaturePlaceContext<NoneFeatureConfiguration>): Boolean {
        val level = context.level()
        val random = context.random()
        val origin = context.origin()
        val state = ModBlocks.DEAD_RHODOPHYTA.get().defaultBlockState()

        val leanDirection = Plane.HORIZONTAL.getRandomDirection(random)
        val straightHeight = random.nextInt(8) + 8
        var reachedHeight = 0

        val basePos = origin.mutable()
        var lastGoodPos = origin.immutable()

        for (i in 0 until straightHeight) {
            if (!placeGrowingBlock(level, basePos, state)) break
            reachedHeight = i + 1
            lastGoodPos = basePos.immutable()

            if (i < BASE_LEVELS) {
                val currentRadius = BASE_RADIUS * (1.0 - (i.toDouble() / BASE_LEVELS))
                val radiusSq = currentRadius * currentRadius
                if (radiusSq > 0) {
                    val bound = Math.ceil(currentRadius).toInt()
                    for (dx in -bound..bound) {
                        for (dz in -bound..bound) {
                            if (dx == 0 && dz == 0) continue
                            if ((dx * dx + dz * dz) <= radiusSq && random.nextFloat() < 0.75f) {
                                placeGrowingBlock(level, basePos.offset(dx, 0, dz), state)
                            }
                        }
                    }
                }
            } else if (random.nextFloat() < TRUNK_BRANCH_CHANCE) {
                val branchDirection = Plane.HORIZONTAL.getRandomDirection(random)
                placeDroopingBranch(level, random, lastGoodPos, state, branchDirection)
            }

            basePos.move(Direction.UP)
        }

        if (reachedHeight == 0) return false

        var hookPos = lastGoodPos
        var horizontalDone = 0
        for (step in 1..HOOK_VERTICAL_STEPS) {
            val t = step.toFloat() / HOOK_VERTICAL_STEPS
            val targetHorizontal = (t * t * t * HOOK_MAX_HORIZONTAL).toInt()

            while (horizontalDone < targetHorizontal) {
                val next = hookPos.relative(leanDirection)
                if (!placeGrowingBlock(level, next, state)) break
                hookPos = next
                horizontalDone++
            }

            val up = hookPos.above()
            if (!placeGrowingBlock(level, up, state)) break
            hookPos = up
        }

        val crownBranches = random.nextInt(3) + 5
        val chosenDirections = mutableSetOf<Direction>()
        while (chosenDirections.size < crownBranches.coerceAtMost(4)) {
            chosenDirections.add(Plane.HORIZONTAL.getRandomDirection(random))
        }
        for (branchDirection in chosenDirections) {
            placeDroopingBranch(level, random, hookPos, state, branchDirection)
        }
        placeDroopingBranch(level, random, hookPos, state, leanDirection)

        scatterDebris(level, random, origin, state)
        scatterFallenBranches(level, random, origin, state)

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
        while (j < outLength && placeFallingBlock(level, mutPos, state)) {
            mutPos.move(direction)
            j++
        }

        var k = 0
        while (k < MAX_DROP) {
            if (isNearGround(level, mutPos)) break
            if (!placeFallingBlock(level, mutPos, state)) break
            if (k >= MIN_DROP && random.nextFloat() < 0.3f) break
            mutPos.move(Direction.DOWN)
            if (random.nextFloat() < 0.15f) {
                mutPos.move(direction)
            }
            k++
        }
    }

    private fun isNearGround(level: LevelAccessor, pos: BlockPos): Boolean {
        val mutPos = pos.mutable()
        for (i in 0 until GROUND_CLEARANCE) {
            if (level.getBlockState(mutPos).isSolid) return true
            mutPos.move(Direction.DOWN)
        }
        return false
    }

    private fun scatterFallenBranches(level: LevelAccessor, random: RandomSource, origin: BlockPos, state: BlockState) {
        val logCount = random.nextInt(4) + 3
        repeat(logCount) {
            val angle = random.nextFloat() * (Math.PI.toFloat() * 2f)
            val dist = random.nextInt(9) + 5
            val dx = (cos(angle.toDouble()) * dist).toInt()
            val dz = (sin(angle.toDouble()) * dist).toInt()
            val floorPos = origin.offset(dx, 0, dz).mutable()

            var tries = 0
            while (tries < 30 && !level.getBlockState(floorPos.below()).isSolid) {
                if (!level.getBlockState(floorPos).`is`(Blocks.WATER)) return@repeat
                floorPos.move(Direction.DOWN)
                tries++
            }
            if (!level.getBlockState(floorPos).`is`(Blocks.WATER)) return@repeat

            var direction = Plane.HORIZONTAL.getRandomDirection(random)
            val length = random.nextInt(6) + 5
            var j = 0
            while (j < length) {
                if (!level.getBlockState(floorPos).`is`(Blocks.WATER) && !level.getBlockState(floorPos).`is`(state.block)) break

                if (!level.getBlockState(floorPos.below()).isSolid) {
                    var adjustTries = 0
                    while (adjustTries < 4 && !level.getBlockState(floorPos.below()).isSolid && level.getBlockState(floorPos).`is`(Blocks.WATER)) {
                        floorPos.move(Direction.DOWN)
                        adjustTries++
                    }
                    if (!level.getBlockState(floorPos.below()).isSolid) break
                }

                level.setBlock(floorPos, state, 2)

                if (j > 0 && random.nextFloat() < 0.4f) {
                    direction = if (random.nextBoolean()) direction.clockWise else direction.counterClockWise
                }

                floorPos.move(direction)
                j++
            }
        }
    }

    private fun scatterDebris(level: LevelAccessor, random: RandomSource, origin: BlockPos, state: BlockState) {
        val debrisCount = random.nextInt(6) + 5
        repeat(debrisCount) {
            val angle = random.nextFloat() * (Math.PI.toFloat() * 2f)
            val dist = random.nextInt(6) + 3
            val dx = (cos(angle.toDouble()) * dist).toInt()
            val dz = (sin(angle.toDouble()) * dist).toInt()
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

    private fun placeGrowingBlock(level: LevelAccessor, pos: BlockPos, state: BlockState): Boolean {
        val above = pos.above()
        val targetBlockState = level.getBlockState(pos)

        if ((targetBlockState.`is`(Blocks.WATER) || targetBlockState.`is`(state.block)) && level.getBlockState(above).`is`(Blocks.WATER)) {
            level.setBlock(pos, state, 3)
            return true
        }

        return false
    }

    private fun placeFallingBlock(level: LevelAccessor, pos: BlockPos, state: BlockState): Boolean {
        val targetBlockState = level.getBlockState(pos)

        if (targetBlockState.`is`(Blocks.WATER) || targetBlockState.`is`(state.block)) {
            level.setBlock(pos, state, 3)
            return true
        }

        return false
    }
}