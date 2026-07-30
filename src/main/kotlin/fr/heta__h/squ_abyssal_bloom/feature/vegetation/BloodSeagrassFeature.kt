package fr.heta__h.squ_abyssal_bloom.feature.vegetation

import com.mojang.serialization.Codec
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.block.blood_seagrass.BloodSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.util.block.Sprouting
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.TallSeagrassBlock
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration

class BloodSeagrassFeature(codec: Codec<ProbabilityFeatureConfiguration>) : Feature<ProbabilityFeatureConfiguration>(codec) {

    override fun place(context: FeaturePlaceContext<ProbabilityFeatureConfiguration>): Boolean {
        var pos = context.origin()
        val level = context.level()
        val random = context.random()
        val config = context.config()

        while (level.getBlockState(pos).isAir && pos.y > level.minY + 1) {
            pos = pos.below()
        }

        var placed = false
        val tries = 8 + random.nextInt(7)
        val seagrassState = ModBlocks.BLOOD_SEAGRASS.get().defaultBlockState()
        val tallSeagrassState = ModBlocks.TALL_BLOOD_SEAGRASS.get().defaultBlockState()

        repeat(tries) {
            val offsetPos = pos.offset(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8))

            if (!level.getBlockState(offsetPos).`is`(Blocks.WATER) || !seagrassState.canSurvive(level, offsetPos)) {
                return@repeat
            }

            if (random.nextDouble() < config.probability) {
                val upperPos = offsetPos.above()
                val lowerState = tallSeagrassState.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.LOWER)
                if (!level.getBlockState(upperPos).`is`(Blocks.WATER) || !lowerState.canSurvive(level, offsetPos)) {
                    return@repeat
                }

                if (level.setBlock(offsetPos, lowerState, 2)) {
                    val upperState = tallSeagrassState.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER)
                    if (level.setBlock(upperPos, upperState, 2)) {
                        placed = true
                    } else {
                        level.setBlock(offsetPos, Blocks.WATER.defaultBlockState(), 2)
                    }
                }
            } else {
                val finalState = if (random.nextFloat() < Sprouting.SPROUTING_CHANCE) {
                    seagrassState.setValue(BloodSeagrassBlock.SPROUTING, true)
                } else {
                    seagrassState
                }
                placed = level.setBlock(offsetPos, finalState, 2) || placed
            }
        }

        return placed
    }
}
