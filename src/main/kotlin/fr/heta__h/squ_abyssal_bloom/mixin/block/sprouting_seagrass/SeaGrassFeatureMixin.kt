package fr.heta__h.squ_abyssal_bloom.mixin.block.sprouting_seagrass

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.block.sprouting_seagrass.SproutingSeagrassBlock
import fr.heta__h.squ_abyssal_bloom.util.block.Sprouting
import net.minecraft.core.BlockPos
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.feature.SeagrassFeature
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

@Mixin(SeagrassFeature::class)
abstract class SeaGrassFeatureMixin {
    @Redirect(
        method = ["place"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
        )
    )
    private fun replaceSeagrass(
        level: WorldGenLevel,
        pos: BlockPos,
        state: BlockState,
        flags: Int
    ): Boolean
    {
        if (state.`is`(Blocks.SEAGRASS)) {
            val random = level.random

            if (random.nextFloat() < Sprouting.SPROUTING_CHANCE) {
                val customState = ModBlocks.SPROUTING_SEAGRASS.get().defaultBlockState().setValue(SproutingSeagrassBlock.HAS_BULB, false)

                return level.setBlock(pos, customState, flags)
            }
        }

        return level.setBlock(pos, state, flags)
    }
}