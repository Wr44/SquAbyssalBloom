package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.util.cache.AbyssalChunkDataCache
import net.minecraft.core.BlockPos
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.world.level.StructureManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [NoiseBasedChunkGenerator::class], priority = 1500)
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
        val (mask, floorGrid) = AbyssalChunkDataCache.consume(chunk.pos.x, chunk.pos.z) ?: return

        val seaLevel = region.seaLevel
        val water = Blocks.WATER.defaultBlockState()
        val mutable = BlockPos.MutableBlockPos()

        for (localX in 0..15) {
            for (localZ in 0..15) {
                if (!mask[localX + localZ * 16]) continue

                val floorY = floorGrid[localX + localZ * 16]
                if (floorY == Int.MIN_VALUE) continue

                val worldX = chunk.pos.minBlockX + localX
                val worldZ = chunk.pos.minBlockZ + localZ

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