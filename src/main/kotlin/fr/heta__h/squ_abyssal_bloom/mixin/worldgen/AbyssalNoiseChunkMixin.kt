package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.util.accessor.IAbyssalNoiseChunk
import fr.heta__h.squ_abyssal_bloom.util.cache.AbyssalChunkDataCache
import fr.heta__h.squ_abyssal_bloom.worldgen.ModNoises
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

private const val MUSHROOM_MIN = -1.05
private const val MUSHROOM_TRANSITION = 0.05
private const val MUSHROOM_ABYSSAL_MARGIN = 0.02

private const val CONTINENTAL_FULL = -0.92

private const val ABYSSAL_DEEP_MARGIN = 0.015

private const val SHALLOW_FLOOR_Y = 35
private const val DEEP_FLOOR_TARGET = 10
private const val TARGET_FLOOR_Y = -40

private const val TOPO_LARGE_SCALE = 0.003
private const val TOPO_SCALE = 0.010
private const val TOPO_MID_SCALE = 0.030
private const val WALL_SCALE = 0.050
private const val DETAIL_SCALE = 0.120
private const val MICRO_SCALE = 0.280

private const val DEEP_WALL_AMP = 8.0
private const val DEEP_DETAIL_AMP = 4.0
private const val DEEP_MICRO_AMP = 2.0
private const val DEEP_WEIRDNESS_AMP = 6.0

private const val TOPO_LARGE_AMP = 42.0
private const val TOPO_AMP = 20.0
private const val TOPO_MID_AMP = 11.0
private const val WALL_AMP = 14.0
private const val DETAIL_AMP = 7.0
private const val MICRO_AMP = 4.0
private const val WEIRDNESS_AMP = 11.0

private const val TRANSITION_WEIRDNESS_AMP = 0.025
private const val TEMP_FLOOR_AMP = 3.5
private const val HUMIDITY_DETAIL_FACTOR = 0.18
private const val WEIRDNESS_WALL_FACTOR = 0.25

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

        val shallowDeepEdge = ServerConfigCache.effectiveShallowDeep.toDouble()
        val deepAbyssalEdge = ServerConfigCache.effectiveDeepAbyssal.toDouble()
        val abyssalDeepSplit = deepAbyssalEdge + ABYSSAL_DEEP_MARGIN

        val finalDensityDf = randomState.router().finalDensity()
        val erosionDf = randomState.router().erosion()
        val ridgesDf = randomState.router().ridges()
        val temperatureDf = randomState.router().temperature()
        val vegetationDf = randomState.router().vegetation()
        val topoNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_TOPO)
        val wallNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_WALL)
        val detailNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_DETAIL)
        val hardLimit = noiseSettings.minY() + 5

        val grid = IntArray(256) { Int.MIN_VALUE }
        val abyssalMask = BooleanArray(256)
        var anyModified = false

        for (localX in 0..15) {
            for (localZ in 0..15) {
                val worldX = chunkMinBlockX + localX
                val worldZ = chunkMinBlockZ + localZ
                val ctx = DensityFunction.SinglePointContext(worldX, 0, worldZ)
                val cont = continentsDf.compute(ctx)

                if (cont > shallowDeepEdge || cont < MUSHROOM_MIN) continue

                if (finalDensityDf.compute(DensityFunction.SinglePointContext(worldX, cachedSeaLevel + 6, worldZ)) > 0.0) continue

                val erosion = erosionDf.compute(ctx)
                val ridges = ridgesDf.compute(ctx)
                val temperature = temperatureDf.compute(ctx)
                val vegetation = vegetationDf.compute(ctx)

                val erosionFactor = (1.0 - erosion * 0.50).coerceIn(0.32, 1.55)
                val tempDepthMod = -temperature * TEMP_FLOOR_AMP
                val humidityDetailMod = (1.0 + vegetation * HUMIDITY_DETAIL_FACTOR).coerceIn(0.75, 1.35)
                val weirdnessWallMod = (1.0 + kotlin.math.abs(ridges) * WEIRDNESS_WALL_FACTOR).coerceIn(0.80, 1.50)
                val effectiveAbyssalSplit = abyssalDeepSplit + ridges * TRANSITION_WEIRDNESS_AMP

                val wallVal = wallNoise.getValue(worldX * WALL_SCALE, 0.0, worldZ * WALL_SCALE)
                val detailVal = detailNoise.getValue(worldX * DETAIL_SCALE, 0.0, worldZ * DETAIL_SCALE)
                val microVal = detailNoise.getValue(worldX * MICRO_SCALE, 0.0, worldZ * MICRO_SCALE)

                val floorY: Int

                if (cont > abyssalDeepSplit) {
                    val rawT = ((cont - shallowDeepEdge) / (effectiveAbyssalSplit - shallowDeepEdge)).coerceIn(0.0, 1.0)
                    val deepT = 1.0 - (1.0 - rawT) * (1.0 - rawT) * (1.0 - rawT)
                    val base = SHALLOW_FLOOR_Y + deepT * (DEEP_FLOOR_TARGET - SHALLOW_FLOOR_Y)

                    floorY = (base
                            + tempDepthMod * deepT
                            + wallVal * DEEP_WALL_AMP * deepT * erosionFactor * weirdnessWallMod
                            + detailVal * DEEP_DETAIL_AMP * deepT * humidityDetailMod
                            + microVal * DEEP_MICRO_AMP * deepT * humidityDetailMod
                            + ridges * DEEP_WEIRDNESS_AMP * deepT
                            ).toInt()
                        .coerceAtMost(SHALLOW_FLOOR_Y - 1)
                        .coerceAtLeast(hardLimit)
                } else {
                    val rawT = ((cont - effectiveAbyssalSplit) / (CONTINENTAL_FULL - effectiveAbyssalSplit)).coerceIn(0.0, 1.0)
                    val steepT = 1.0 - (1.0 - rawT) * (1.0 - rawT) * (1.0 - rawT)
                    val mushroomProxT = ((cont - (MUSHROOM_MIN + MUSHROOM_ABYSSAL_MARGIN)) / MUSHROOM_TRANSITION).coerceIn(0.0, 1.0)
                    val effectiveSteepT = steepT * mushroomProxT
                    if (effectiveSteepT < 0.001) continue

                    val base = DEEP_FLOOR_TARGET + effectiveSteepT * (TARGET_FLOOR_Y - DEEP_FLOOR_TARGET)

                    val topoLarge = topoNoise.getValue(worldX * TOPO_LARGE_SCALE, 0.0, worldZ * TOPO_LARGE_SCALE)
                    val topo = topoNoise.getValue(worldX * TOPO_SCALE, 0.0, worldZ * TOPO_SCALE)
                    val topoMid = topoNoise.getValue(worldX * TOPO_MID_SCALE, 0.0, worldZ * TOPO_MID_SCALE)

                    val topoRaw = (topoLarge * TOPO_LARGE_AMP + topo * TOPO_AMP + topoMid * TOPO_MID_AMP) * effectiveSteepT * erosionFactor
                    val topoVar = topoRaw.coerceAtLeast(-6.0 * effectiveSteepT)
                    val wallVar = wallVal * WALL_AMP * effectiveSteepT * erosionFactor * weirdnessWallMod
                    val detailVar = (detailVal * DETAIL_AMP + microVal * MICRO_AMP) * effectiveSteepT * humidityDetailMod
                    val weirdnessVar = ridges * WEIRDNESS_AMP * effectiveSteepT
                    val tempVar = tempDepthMod * effectiveSteepT

                    floorY = (base + topoVar + wallVar + detailVar + weirdnessVar + tempVar).toInt()
                        .coerceAtLeast(noiseSettings.minY() + 14)
                }

                if (floorY < SHALLOW_FLOOR_Y) {
                    grid[localX + localZ * 16] = floorY
                    abyssalMask[localX + localZ * 16] = true
                    anyModified = true
                }
            }
        }

        if (anyModified) {
            floorGrid = grid
            AbyssalChunkDataCache.store(chunkMinBlockX shr 4, chunkMinBlockZ shr 4, abyssalMask, grid)
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
        if (blockY() <= floorY) return

        val current = cir.returnValue
        if (current != null && current.`is`(Blocks.WATER)) return
        cir.returnValue = Blocks.WATER.defaultBlockState()
    }
}