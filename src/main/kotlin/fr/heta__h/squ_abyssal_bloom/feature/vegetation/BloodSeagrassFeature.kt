package fr.heta__h.squ_abyssal_bloom.feature.vegetation

import com.mojang.serialization.Codec
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
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

        for (i in 0 until tries) {
            val offsetPos = pos.offset(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8))
            val state = ModBlocks.BLOOD_SEAGRASS.get().defaultBlockState()

            if (level.getBlockState(offsetPos).`is`(Blocks.WATER) && state.canSurvive(level, offsetPos)) {
                if (random.nextDouble() < config.probability) {
                    val tallState = ModBlocks.TALL_BLOOD_SEAGRASS.get().defaultBlockState()
                    level.setBlock(offsetPos, tallState.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.LOWER), 2)
                    level.setBlock(offsetPos.above(), tallState.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER), 2)
                } else {
                    level.setBlock(offsetPos, state, 2)
                }
                placed = true
            }
        }

        return placed
    }
}