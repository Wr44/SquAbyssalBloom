package fr.heta__h.squ_abyssal_bloom.feature.vegetation

import com.mojang.serialization.Codec
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration

class RedCoralReefFeature(codec: Codec<NoneFeatureConfiguration>) : Feature<NoneFeatureConfiguration>(codec) {

    override fun place(context: FeaturePlaceContext<NoneFeatureConfiguration>): Boolean {
        val level = context.level()
        val random = context.random()
        val origin = context.origin()
        val state = Blocks.FIRE_CORAL_BLOCK.defaultBlockState()

        var placed = false
        val individuals = random.nextInt(4) + 3

        repeat(individuals) {
            val x = origin.x + random.nextInt(8) - random.nextInt(8)
            val z = origin.z + random.nextInt(8) - random.nextInt(8)
            val pos = BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z), z)

            val success = when (random.nextInt(3)) {
                0 -> RedCoralShapes.placeTree(level, random, pos, state)
                1 -> RedCoralShapes.placeClaw(level, random, pos, state)
                else -> RedCoralShapes.placeMushroom(level, random, pos, state)
            }

            if (success) placed = true
        }

        return placed
    }
}
