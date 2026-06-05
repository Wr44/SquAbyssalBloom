package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

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

private const val CONTINENTAL_EDGE = -0.7
private const val CONTINENTAL_FULL = -0.85
private const val VANILLA_OCEAN_Y = 40
private const val TARGET_FLOOR_Y = -30
private const val TOPO_SCALE = 0.008
private const val TOPO_AMP = 45.0
private const val WALL_SCALE = 0.03
private const val WALL_AMP = 28.0
private const val DETAIL_SCALE = 0.08
private const val DETAIL_AMP = 15.0

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

        val continentsDf = randomState.router().continents()
        val topoNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_TOPO)
        val wallNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_WALL)
        val detailNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_DETAIL)
        val hardLimit = noiseSettings.minY() + 5
        val grid = IntArray(256) { Int.MIN_VALUE }
        var anyAbyssal = false

        for (localX in 0..15) {
            for (localZ in 0..15) {
                val worldX = chunkMinBlockX + localX
                val worldZ = chunkMinBlockZ + localZ

                val continentalness = continentsDf.compute(
                    DensityFunction.SinglePointContext(worldX, 0, worldZ)
                )
                if (continentalness > CONTINENTAL_EDGE) continue

                val rawT = ((continentalness - CONTINENTAL_EDGE) / (CONTINENTAL_FULL - CONTINENTAL_EDGE))
                    .coerceIn(0.0, 1.0)
                val smoothT = rawT * rawT * (3.0 - 2.0 * rawT)

                val cliffFactor = topoNoise.getValue(worldX * 0.003, 0.0, worldZ * 0.003)
                val transitionExp = 0.2 + (1.0 - cliffFactor) / 2.0 * 2.8
                val shapeT = rawT.pow(transitionExp)
                val transitionT = shapeT * shapeT * (3.0 - 2.0 * shapeT)

                val baseFloorY = VANILLA_OCEAN_Y + transitionT * (TARGET_FLOOR_Y - VANILLA_OCEAN_Y)
                val topoVar = topoNoise.getValue(worldX * TOPO_SCALE, 0.0, worldZ * TOPO_SCALE) * TOPO_AMP * smoothT
                val wallVar = wallNoise.getValue(worldX * WALL_SCALE, 0.0, worldZ * WALL_SCALE) * WALL_AMP * smoothT
                val detailVar = detailNoise.getValue(worldX * DETAIL_SCALE, 0.0, worldZ * DETAIL_SCALE) * DETAIL_AMP * smoothT

                val floorY = (baseFloorY + topoVar + wallVar + detailVar).toInt()
                    .coerceAtLeast(hardLimit)
                grid[localX + localZ * 16] = floorY
                anyAbyssal = true
            }
        }

        sab_floorGrid = if (anyAbyssal) grid else null
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
        val isSolid = current == null || current.blocksMotion()
        if (isSolid && blockY() > floorY) {
            cir.returnValue = Blocks.WATER.defaultBlockState()
        }
    }
}