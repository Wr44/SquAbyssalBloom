package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.util.accessor.IAbyssalNoiseChunk
import fr.heta__h.squ_abyssal_bloom.util.worldgen.AbyssalWorldgenScope
import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.NoiseChunk
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.world.level.levelgen.SurfaceRules
import net.minecraft.world.level.levelgen.SurfaceSystem
import net.minecraft.world.level.levelgen.WorldGenerationContext
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private const val SAND_DEPTH = 3

@Mixin(value = [SurfaceSystem::class], priority = 2000)
abstract class SurfaceSystemMixin {

    @Inject(method = ["buildSurface"], at = [At("RETURN")])
    private fun sandSurface(
        randomState: RandomState,
        biomeManager: BiomeManager,
        biomes: Registry<Biome>,
        useLegacyRandom: Boolean,
        generationContext: WorldGenerationContext,
        protoChunk: ChunkAccess,
        noiseChunk: NoiseChunk,
        ruleSource: SurfaceRules.RuleSource,
        ci: CallbackInfo
    ) {
        if (!AbyssalWorldgenScope.isOverworld(randomState)) return
        val floorGrid = (noiseChunk as IAbyssalNoiseChunk).getFloorGrid() ?: return

        val chunkPos = protoChunk.pos
        val minBlockX = chunkPos.minBlockX
        val minBlockZ = chunkPos.minBlockZ
        val mutPos = BlockPos.MutableBlockPos()

        for (x in 0 until 16) {
            for (z in 0 until 16) {
                if (floorGrid[x + z * 16] == Int.MIN_VALUE) continue

                val blockX = minBlockX + x
                val blockZ = minBlockZ + z
                val floorY = protoChunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)

                val biome = biomeManager.getBiome(mutPos.set(blockX, floorY, blockZ))
                if (!biome.`is`(ModBiomes.BLOOD_VALLEY)) continue

                for (y in floorY downTo floorY - SAND_DEPTH + 1) {
                    if (y < protoChunk.minY) break
                    mutPos.set(blockX, y, blockZ)
                    if (!isReplaceableTerrain(protoChunk.getBlockState(mutPos))) break
                    protoChunk.setBlockState(mutPos, Blocks.SAND.defaultBlockState())
                }
            }
        }
    }

    private fun isReplaceableTerrain(state: net.minecraft.world.level.block.state.BlockState): Boolean =
        state.`is`(BlockTags.BASE_STONE_OVERWORLD) ||
            state.`is`(Blocks.DIRT) ||
            state.`is`(Blocks.COARSE_DIRT) ||
            state.`is`(Blocks.GRAVEL) ||
            state.`is`(Blocks.CLAY) ||
            state.`is`(Blocks.SAND) ||
            state.`is`(Blocks.SANDSTONE)
}
