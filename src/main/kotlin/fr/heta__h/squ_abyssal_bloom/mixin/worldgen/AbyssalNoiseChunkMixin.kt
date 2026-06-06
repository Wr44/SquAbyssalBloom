package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.util.accessor.IAbyssalNoiseChunk
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

private const val CONTINENTAL_FULL = -0.92

private const val SHALLOW_FLOOR_Y = 40
private const val DEEP_FLOOR_TARGET = 15
private const val TARGET_FLOOR_Y = -55

private const val TOPO_LARGE_SCALE = 0.003
private const val TOPO_SCALE = 0.010
private const val TOPO_MID_SCALE = 0.030
private const val WALL_SCALE = 0.050
private const val DETAIL_SCALE = 0.120
private const val MICRO_SCALE = 0.280

private const val DEEP_WALL_AMP = 10.0
private const val DEEP_DETAIL_AMP = 5.0
private const val DEEP_MICRO_AMP = 3.0
private const val DEEP_WEIRDNESS_AMP = 8.0

private const val TOPO_LARGE_AMP = 65.0
private const val TOPO_AMP = 30.0
private const val TOPO_MID_AMP = 15.0
private const val WALL_AMP = 20.0
private const val DETAIL_AMP = 9.0
private const val MICRO_AMP = 4.0
private const val WEIRDNESS_AMP = 22.0

@Mixin(NoiseChunk::class)
abstract class AbyssalNoiseChunkMixin : IAbyssalNoiseChunk {

    @Unique private var sab_floorGrid: IntArray? = null
    @Unique private var sab_minX: Int = 0
    @Unique private var sab_minZ: Int = 0

    @Shadow abstract fun blockX(): Int
    @Shadow abstract fun blockY(): Int
    @Shadow abstract fun blockZ(): Int

    override fun abyssalBloom_getFloorGrid(): IntArray? = sab_floorGrid
    override fun abyssalBloom_setFloorGrid(grid: IntArray?) { sab_floorGrid = grid }
    override fun abyssalBloom_getChunkMinX(): Int = sab_minX
    override fun abyssalBloom_setChunkMinX(x: Int) { sab_minX = x }
    override fun abyssalBloom_getChunkMinZ(): Int = sab_minZ
    override fun abyssalBloom_setChunkMinZ(z: Int) { sab_minZ = z }

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
        sab_minX = chunkMinBlockX
        sab_minZ = chunkMinBlockZ

        val continentalEdge = ServerConfigCache.effectiveShallowDeep
        val deepAbyssalEdge = ServerConfigCache.effectiveDeepAbyssal

        val continentsDf = randomState.router().continents()
        val erosionDf = randomState.router().erosion()
        val ridgesDf = randomState.router().ridges()
        val topoNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_TOPO)
        val wallNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_WALL)
        val detailNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_DETAIL)
        val hardLimit = noiseSettings.minY() + 5
        val grid = IntArray(256) { Int.MIN_VALUE }
        var anyModified = false

        for (localX in 0..15) {
            for (localZ in 0..15) {
                val worldX = chunkMinBlockX + localX
                val worldZ = chunkMinBlockZ + localZ
                val ctx = DensityFunction.SinglePointContext(worldX, 0, worldZ)

                val cont = continentsDf.compute(ctx)

                if (cont > continentalEdge) continue
                if (cont < MUSHROOM_MIN) continue

                val wallVal = wallNoise.getValue(worldX * WALL_SCALE, 0.0, worldZ * WALL_SCALE)
                val detailVal = detailNoise.getValue(worldX * DETAIL_SCALE, 0.0, worldZ * DETAIL_SCALE)
                val microVal = detailNoise.getValue(worldX * MICRO_SCALE, 0.0, worldZ * MICRO_SCALE)
                val erosion = erosionDf.compute(ctx)
                val ridges = ridgesDf.compute(ctx)
                val erosionFactor = (1.0 - erosion * 0.55).coerceIn(0.3, 1.7)

                val floorY: Int

                if (cont > deepAbyssalEdge) {
                    val rawT = ((cont - continentalEdge) / (deepAbyssalEdge - continentalEdge)).coerceIn(0.0, 1.0)
                    val deepT = rawT * rawT * (3.0 - 2.0 * rawT)

                    val base = SHALLOW_FLOOR_Y + deepT * (DEEP_FLOOR_TARGET - SHALLOW_FLOOR_Y)

                    floorY = (base
                            + wallVal * DEEP_WALL_AMP * deepT * erosionFactor
                            + detailVal * DEEP_DETAIL_AMP * deepT
                            + microVal * DEEP_MICRO_AMP * deepT
                            + ridges * DEEP_WEIRDNESS_AMP * deepT
                            ).toInt().coerceAtLeast(hardLimit)
                } else {
                    val rawT = ((cont - deepAbyssalEdge) / (CONTINENTAL_FULL - deepAbyssalEdge)).coerceIn(0.0, 1.0)
                    val smoothT = rawT * rawT * (3.0 - 2.0 * rawT)

                    val mushroomProxT = ((cont - MUSHROOM_MIN) / MUSHROOM_TRANSITION).coerceIn(0.0, 1.0)
                    val effectiveSmoothT = smoothT * mushroomProxT

                    val base = DEEP_FLOOR_TARGET + effectiveSmoothT * (TARGET_FLOOR_Y - DEEP_FLOOR_TARGET)

                    val topoLarge = topoNoise.getValue(worldX * TOPO_LARGE_SCALE, 0.0, worldZ * TOPO_LARGE_SCALE)
                    val topo = topoNoise.getValue(worldX * TOPO_SCALE, 0.0, worldZ * TOPO_SCALE)
                    val topoMid = topoNoise.getValue(worldX * TOPO_MID_SCALE, 0.0, worldZ * TOPO_MID_SCALE)

                    val topoVar = (topoLarge * TOPO_LARGE_AMP + topo * TOPO_AMP + topoMid * TOPO_MID_AMP) * effectiveSmoothT * erosionFactor
                    val wallVar = wallVal * WALL_AMP * effectiveSmoothT * erosionFactor
                    val detailVar = (detailVal * DETAIL_AMP + microVal * MICRO_AMP) * effectiveSmoothT
                    val weirdnessVar = ridges * WEIRDNESS_AMP * effectiveSmoothT

                    floorY = (base + topoVar + wallVar + detailVar + weirdnessVar).toInt().coerceAtLeast(hardLimit)
                }

                grid[localX + localZ * 16] = floorY
                anyModified = true
            }
        }

        sab_floorGrid = if (anyModified) grid else null
    }

    @Inject(
        method = ["getInterpolatedState"],
        at = [At("RETURN")],
        cancellable = true
    )
    private fun onGetInterpolatedState(cir: CallbackInfoReturnable<BlockState?>) {
        val grid = sab_floorGrid ?: return
        val localX = blockX() - sab_minX
        val localZ = blockZ() - sab_minZ
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) return
        val floorY = grid[localX + localZ * 16]
        if (floorY == Int.MIN_VALUE) return
        val current = cir.returnValue
        if ((current == null || current.blocksMotion()) && blockY() > floorY) {
            cir.returnValue = Blocks.WATER.defaultBlockState()
        }
    }
}