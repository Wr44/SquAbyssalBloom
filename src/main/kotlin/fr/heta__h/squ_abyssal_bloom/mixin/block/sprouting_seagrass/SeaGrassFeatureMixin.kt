package fr.heta__h.squ_abyssal_bloom.mixin.block.sprouting_seagrass

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.block.sprouting_seagrass.SproutingSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.TallSeagrassBlock
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.SeagrassFeature
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(SeagrassFeature::class)
abstract class SeaGrassFeatureMixin {

    @Inject(method = ["place"], at = [At("HEAD")], cancellable = true)
    private fun replaceSeagrass(
        context: FeaturePlaceContext<ProbabilityFeatureConfiguration>,
        cir: CallbackInfoReturnable<Boolean>
    ) {
        val random = context.random()
        val level: WorldGenLevel = context.level()
        val origin = context.origin()
        val config = context.config()

        val x = random.nextInt(8) - random.nextInt(8)
        val z = random.nextInt(8) - random.nextInt(8)
        val y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.x + x, origin.z + z)
        val grassPos = BlockPos(origin.x + x, y, origin.z + z)

        if (!level.getBlockState(grassPos).`is`(Blocks.WATER)) {
            cir.returnValue = false
            return
        }

        val isTall = random.nextDouble() < config.probability

        val baseState = if (isTall) {
            Blocks.TALL_SEAGRASS.defaultBlockState()
        } else if (random.nextFloat() < ModUtilities.SPROUTING_CHANCE) {
            ModBlocks.SPROUTING_SEAGRASS.get().defaultBlockState().setValue(SproutingSeagrassBlock.HAS_BULB, false)
        } else {
            Blocks.SEAGRASS.defaultBlockState()
        }

        if (!baseState.canSurvive(level, grassPos)) {
            cir.returnValue = false
            return
        }

        if (isTall) {
            val upperState = baseState.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER)
            val above = grassPos.above()
            if (level.getBlockState(above).`is`(Blocks.WATER)) {
                level.setBlock(grassPos, baseState, 2)
                level.setBlock(above, upperState, 2)
            }
        } else {
            level.setBlock(grassPos, baseState, 2)
        }

        cir.returnValue = true
    }
}
