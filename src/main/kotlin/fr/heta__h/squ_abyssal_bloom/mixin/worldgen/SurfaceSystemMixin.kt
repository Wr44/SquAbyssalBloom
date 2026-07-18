package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
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

private const val DEPTH = 2

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
        val chunkPos = protoChunk.pos
        val minBlockX = chunkPos.minBlockX
        val minBlockZ = chunkPos.minBlockZ
        val mutPos = BlockPos.MutableBlockPos()

        for (x in 0 until 16) {
            for (z in 0 until 16) {
                val blockX = minBlockX + x
                val blockZ = minBlockZ + z
                val floorY = protoChunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)

                val biome = biomeManager.getBiome(mutPos.set(blockX, floorY, blockZ))
                if (!biome.`is`(ModBiomes.BLOOD_VALLEY)) continue

                for (y in floorY downTo floorY - DEPTH) {
                    if (y < protoChunk.minY) break
                    mutPos.set(blockX, y, blockZ)
                    protoChunk.setBlockState(mutPos, Blocks.SAND.defaultBlockState())
                }
            }
        }
    }
}