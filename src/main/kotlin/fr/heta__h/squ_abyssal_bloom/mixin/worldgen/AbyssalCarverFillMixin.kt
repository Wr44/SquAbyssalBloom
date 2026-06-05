package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import net.minecraft.core.BlockPos
import net.minecraft.core.QuartPos
import net.minecraft.world.level.StructureManager
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.server.level.WorldGenRegion
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(NoiseBasedChunkGenerator::class)
abstract class AbyssalCarverFillMixin {

    @Shadow abstract fun getSeaLevel(): Int

    @Inject(
        method = ["applyCarvers"],
        at = [At("RETURN")]
    )
    private fun onApplyCarversReturn(
        region: WorldGenRegion,
        seed: Long,
        randomState: RandomState,
        biomeManager: BiomeManager,
        structureManager: StructureManager,
        chunk: ChunkAccess,
        ci: CallbackInfo
    ) {
        val seaLevel = getSeaLevel()
        val chunkPos = chunk.pos
        val water = Blocks.WATER.defaultBlockState()
        val mutable = BlockPos.MutableBlockPos()

        for (localX in 0..15) {
            for (localZ in 0..15) {
                val quartX = QuartPos.fromBlock(chunkPos.minBlockX + localX)
                val quartZ = QuartPos.fromBlock(chunkPos.minBlockZ + localZ)
                if (!chunk.getNoiseBiome(quartX, 10, quartZ).`is`(ModTags.Biomes.IS_ABYSSAL)) continue

                val worldX = chunkPos.minBlockX + localX
                val worldZ = chunkPos.minBlockZ + localZ

                for (y in chunk.minY until seaLevel) {
                    mutable.set(worldX, y, worldZ)
                    if (chunk.getBlockState(mutable).isAir) {
                        chunk.setBlockState(mutable, water)
                    }
                }
            }
        }
    }
}