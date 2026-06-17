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
        val seaLevel = region.seaLevel
        val water = Blocks.WATER.defaultBlockState()
        val stone = Blocks.STONE.defaultBlockState()
        val mutable = BlockPos.MutableBlockPos()

        val data = AbyssalChunkDataCache.consume(chunk.pos.x, chunk.pos.z)
        if (data != null) {
            for (localX in 0..15) {
                for (localZ in 0..15) {
                    if (!data.carverMask[localX + localZ * 16]) continue

                    val floorY = data.floorGrid[localX + localZ * 16]
                    if (floorY == Int.MIN_VALUE) continue

                    val worldX = chunk.pos.minBlockX + localX
                    val worldZ = chunk.pos.minBlockZ + localZ
                    val crustBottom = floorY - 4

                    for (y in crustBottom..floorY) {
                        mutable.set(worldX, y, worldZ)
                        val state = chunk.getBlockState(mutable)
                        if (state.`is`(Blocks.WATER) || state.`is`(Blocks.LAVA)) {
                            val fill = if (y < 0) Blocks.DEEPSLATE.defaultBlockState() else Blocks.STONE.defaultBlockState()
                            chunk.setBlockState(mutable, fill)
                        }
                    }
                }
            }
        }

        for (localX in 0..15) {
            for (localZ in 0..15) {
                val worldX = chunk.pos.minBlockX + localX
                val worldZ = chunk.pos.minBlockZ + localZ

                mutable.set(worldX, seaLevel - 1, worldZ)
                if (!chunk.getBlockState(mutable).`is`(Blocks.WATER)) continue

                for (y in seaLevel - 2 downTo chunk.minY) {
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