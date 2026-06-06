package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache
import net.minecraft.core.BlockPos
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.DensityFunction
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.world.level.StructureManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(NoiseBasedChunkGenerator::class)
abstract class AbyssalCarverFillMixin {

    @Inject(
        method = ["applyCarvers(Lnet/minecraft/server/level/WorldGenRegion;JLnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;)V"],
        at = [At("RETURN")]
    )
    private fun onApplyCarvers(
        region: WorldGenRegion,
        seed: Long,
        randomState: RandomState,
        biomeManager: BiomeManager,
        structureManager: StructureManager,
        chunk: ChunkAccess,
        ci: CallbackInfo
    ) {
        val shallowDeep = ServerConfigCache.effectiveShallowDeep
        val seaLevel = region.seaLevel
        val water = Blocks.WATER.defaultBlockState()
        val mutable = BlockPos.MutableBlockPos()

        for (localX in 0..15) {
            for (localZ in 0..15) {
                val worldX = chunk.pos.minBlockX + localX
                val worldZ = chunk.pos.minBlockZ + localZ
                val ctx = DensityFunction.SinglePointContext(worldX, 0, worldZ)
                val cont = randomState.router().continents().compute(ctx).toFloat()

                if (cont > shallowDeep || cont < -1.05f) continue

                for (y in chunk.minY until seaLevel) {
                    mutable.set(worldX, y, worldZ)
                    val state = chunk.getBlockState(mutable)
                    if (state.isAir || state.`is`(Blocks.LAVA)) {
                        chunk.setBlockState(mutable, water)
                    }
                }
            }
        }
    }
}