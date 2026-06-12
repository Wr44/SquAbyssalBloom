package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.util.accessor.IAbyssalNoiseChunk
import fr.heta__h.squ_abyssal_bloom.util.cache.AbyssalChunkDataCache
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.AbyssalShapingContext
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.ColumnSample
import fr.heta__h.squ_abyssal_bloom.worldgen.ModNoises
import fr.heta__h.squ_abyssal_bloom.worldgen.terrain.AbyssalFloorShaper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Aquifer
import net.minecraft.world.level.levelgen.DensityFunction
import net.minecraft.world.level.levelgen.DensityFunctions
import net.minecraft.world.level.levelgen.NoiseChunk
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.NoiseSettings
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.world.level.levelgen.blending.Blender
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(value = [NoiseChunk::class], priority = 1500)
abstract class AbyssalNoiseChunkMixin : IAbyssalNoiseChunk {

    @Unique private var floorGrid: IntArray? = null
    @Unique private var minX: Int = 0
    @Unique private var minZ: Int = 0
    @Unique private var cachedSeaLevel: Int = 63

    @Shadow abstract fun blockX(): Int
    @Shadow abstract fun blockY(): Int
    @Shadow abstract fun blockZ(): Int

    override fun getFloorGrid(): IntArray? = floorGrid
    override fun setFloorGrid(grid: IntArray?) { floorGrid = grid }
    override fun getChunkMinX(): Int = minX
    override fun setChunkMinX(x: Int) { minX = x }
    override fun getChunkMinZ(): Int = minZ
    override fun setChunkMinZ(z: Int) { minZ = z }

    @Inject(
        method = ["<init>(ILnet/minecraft/world/level/levelgen/RandomState;IILnet/minecraft/world/level/levelgen/NoiseSettings;Lnet/minecraft/world/level/levelgen/DensityFunctions\$BeardifierOrMarker;Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/world/level/levelgen/Aquifer\$FluidPicker;Lnet/minecraft/world/level/levelgen/blending/Blender;)V"],
        at = [At("RETURN")]
    )
    private fun onInit(
        cellCountXZ: Int,
        randomState: RandomState,
        chunkMinBlockX: Int,
        chunkMinBlockZ: Int,
        noiseSettings: NoiseSettings,
        beardifier: DensityFunctions.BeardifierOrMarker,
        settings: NoiseGeneratorSettings,
        globalFluidPicker: Aquifer.FluidPicker,
        blender: Blender,
        ci: CallbackInfo
    ) {
        val continentsDf = randomState.router().continents()
        if (continentsDf.minValue() == 0.0 && continentsDf.maxValue() == 0.0) return

        minX = chunkMinBlockX
        minZ = chunkMinBlockZ
        cachedSeaLevel = settings.seaLevel()

        val shaping = AbyssalShapingContext(
            topoNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_TOPO),
            wallNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_WALL),
            detailNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_DETAIL),
            shallowDeepEdge = ServerConfigCache.effectiveShallowDeep.toDouble(),
            deepAbyssalEdge = ServerConfigCache.effectiveDeepAbyssal.toDouble(),
            seaLevel = cachedSeaLevel,
            deepHardLimit = noiseSettings.minY() + 5,
            abyssalHardLimit = noiseSettings.minY() + 14
        )

        val finalDensityDf = randomState.router().finalDensity()
        val erosionDf = randomState.router().erosion()
        val ridgesDf = randomState.router().ridges()
        val temperatureDf = randomState.router().temperature()
        val vegetationDf = randomState.router().vegetation()

        val grid = IntArray(256) { Int.MIN_VALUE }
        val abyssalMask = BooleanArray(256)
        val carverMask = BooleanArray(256)
        var anyModified = false

        for (localX in 0..15) {
            for (localZ in 0..15) {
                val worldX = chunkMinBlockX + localX
                val worldZ = chunkMinBlockZ + localZ
                val ctx = DensityFunction.SinglePointContext(worldX, 0, worldZ)
                val cont = continentsDf.compute(ctx)

                if (!AbyssalFloorShaper.isWithinOceanBand(shaping, cont)) continue
                if (finalDensityDf.compute(DensityFunction.SinglePointContext(worldX, cachedSeaLevel + 6, worldZ)) > 0.0) continue

                val column = ColumnSample(
                    worldX = worldX,
                    worldZ = worldZ,
                    cont = cont,
                    erosion = erosionDf.compute(ctx),
                    ridges = ridgesDf.compute(ctx),
                    temperature = temperatureDf.compute(ctx),
                    vegetation = vegetationDf.compute(ctx)
                )

                val floorY = AbyssalFloorShaper.computeFloor(shaping, column, finalDensityDf)

                if (floorY < cachedSeaLevel) {
                    grid[localX + localZ * 16] = floorY
                    carverMask[localX + localZ * 16] = true
                    if (floorY < AbyssalFloorShaper.SHALLOW_FLOOR_Y) {
                        abyssalMask[localX + localZ * 16] = true
                    }
                    anyModified = true
                }
            }
        }

        if (anyModified) {
            floorGrid = grid
            AbyssalChunkDataCache.store(chunkMinBlockX shr 4, chunkMinBlockZ shr 4, abyssalMask, carverMask, grid)
        }
    }

    @Inject(
        method = ["getInterpolatedState"],
        at = [At("RETURN")],
        cancellable = true
    )
    private fun onGetInterpolatedState(cir: CallbackInfoReturnable<BlockState?>) {
        val grid = floorGrid ?: return
        if (blockY() >= cachedSeaLevel) return
        val localX = blockX() - minX
        val localZ = blockZ() - minZ
        if (localX !in 0..15 || localZ !in 0..15) return
        val floorY = grid[localX + localZ * 16]
        if (floorY == Int.MIN_VALUE) return

        val current = cir.returnValue

        if (blockY() <= floorY) {
            if (current != null && current.`is`(Blocks.WATER)) {
                cir.returnValue = Blocks.STONE.defaultBlockState()
            }
            return
        }

        if (current != null && current.`is`(Blocks.WATER)) return
        cir.returnValue = Blocks.WATER.defaultBlockState()
    }
}