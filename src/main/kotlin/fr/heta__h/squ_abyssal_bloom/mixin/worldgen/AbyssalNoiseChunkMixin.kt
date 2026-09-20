package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.util.accessor.IAbyssalNoiseChunk
import fr.heta__h.squ_abyssal_bloom.util.worldgen.AbyssalWorldgenScope
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.AbyssalShapingContext
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.AbyssalTerrainSettings
import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.ColumnSample
import fr.heta__h.squ_abyssal_bloom.worldgen.ModNoises
import fr.heta__h.squ_abyssal_bloom.worldgen.terrain.AbyssalFloorShaper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.biome.Climate
import net.minecraft.world.level.levelgen.Aquifer
import net.minecraft.world.level.levelgen.DensityFunction
import net.minecraft.world.level.levelgen.DensityFunctions
import net.minecraft.world.level.levelgen.NoiseChunk
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.NoiseRouter
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
import kotlin.math.roundToInt

@Mixin(value = [NoiseChunk::class], priority = 1500)
abstract class AbyssalNoiseChunkMixin : IAbyssalNoiseChunk {

    @Unique
    private var floorGrid: IntArray? = null

    @Unique
    private var minX: Int = 0

    @Unique
    private var minZ: Int = 0

    @Unique
    private var cachedSeaLevel: Int = 63

    @Unique
    private var overworldNoiseChunk: Boolean = false

    @Shadow
    abstract fun blockX(): Int

    @Shadow
    abstract fun blockY(): Int

    @Shadow
    abstract fun blockZ(): Int

    override fun getFloorGrid(): IntArray? = floorGrid

    @Inject(
        method = [
            "<init>(ILnet/minecraft/world/level/levelgen/RandomState;IILnet/minecraft/world/level/levelgen/NoiseSettings;Lnet/minecraft/world/level/levelgen/DensityFunctions\$BeardifierOrMarker;Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/world/level/levelgen/Aquifer\$FluidPicker;Lnet/minecraft/world/level/levelgen/blending/Blender;)V"
        ],
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
        if (!AbyssalWorldgenScope.isOverworld(randomState)) {
            return
        }

        overworldNoiseChunk = true

        val continentsDf = randomState.router().continents()
        if (
            continentsDf.minValue() == 0.0 &&
            continentsDf.maxValue() == 0.0
        ) {
            return
        }

        minX = chunkMinBlockX
        minZ = chunkMinBlockZ
        cachedSeaLevel = settings.seaLevel()

        val terrainSettings = AbyssalTerrainSettings.capture(
            cachedSeaLevel,
            noiseSettings.minY()
        )

        val shaping = AbyssalShapingContext(
            topoNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_TOPO),
            wallNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_WALL),
            detailNoise = randomState.getOrCreateNoise(ModNoises.ABYSSAL_DETAIL),
            settings = terrainSettings,
            blender = blender
        )

        val finalDensityDf = randomState.router().finalDensity()
        val erosionDf = randomState.router().erosion()
        val ridgesDf = randomState.router().ridges()
        val temperatureDf = randomState.router().temperature()
        val vegetationDf = randomState.router().vegetation()

        val grid = IntArray(256) { Int.MIN_VALUE }
        var anyModified = false

        val coveredBlocks = (
                cellCountXZ * noiseSettings.cellWidth
                ).coerceIn(0, 16)

        if (coveredBlocks == 0) {
            return
        }

        for (localX in 0 until coveredBlocks) {
            for (localZ in 0 until coveredBlocks) {
                val worldX = chunkMinBlockX + localX
                val worldZ = chunkMinBlockZ + localZ

                val context = DensityFunction.SinglePointContext(
                    worldX,
                    0,
                    worldZ
                )

                val continentalness = continentsDf.compute(context)

                if (
                    !AbyssalFloorShaper.isWithinOceanBand(
                        shaping,
                        continentalness
                    )
                ) {
                    continue
                }

                val blendAlpha = if (blender.isEmpty()) {
                    1.0
                } else {
                    blender
                        .blendOffsetAndFactor(worldX, worldZ)
                        .alpha()
                        .coerceIn(0.0, 1.0)
                }

                if (blendAlpha <= 0.0) {
                    continue
                }

                val column = ColumnSample(
                    worldX = worldX,
                    worldZ = worldZ,
                    cont = continentalness,
                    erosion = erosionDf.compute(context),
                    ridges = ridgesDf.compute(context),
                    temperature = temperatureDf.compute(context),
                    vegetation = vegetationDf.compute(context)
                )

                val shapedFloorY = AbyssalFloorShaper.computeFloor(
                    shaping,
                    column,
                    finalDensityDf
                )

                val floorY = if (blendAlpha < 1.0) {
                    val vanillaFloorY =
                        AbyssalFloorShaper.scanVanillaFloor(
                            shaping,
                            column,
                            finalDensityDf,
                            shaping.deepHardLimit
                        )

                    (
                            vanillaFloorY +
                                    (shapedFloorY - vanillaFloorY) * blendAlpha
                            )
                        .roundToInt()
                        .coerceIn(
                            shaping.abyssalHardLimit,
                            shaping.maxFloorY
                        )
                } else {
                    shapedFloorY
                }

                if (floorY >= cachedSeaLevel) {
                    continue
                }

                grid[localX + localZ * 16] = floorY
                anyModified = true
            }
        }

        if (anyModified) {
            floorGrid = grid
        }
    }

    @Inject(
        method = ["cachedClimateSampler"],
        at = [At("RETURN")]
    )
    private fun onCachedClimateSampler(
        noises: NoiseRouter,
        spawnTarget: List<Climate.ParameterPoint>,
        cir: CallbackInfoReturnable<Climate.Sampler>
    ) {
        if (overworldNoiseChunk) {
            AbyssalWorldgenScope.registerSampler(cir.returnValue)
        }
    }

    @Inject(
        method = ["getInterpolatedState"],
        at = [At("RETURN")],
        cancellable = true
    )
    private fun onGetInterpolatedState(
        cir: CallbackInfoReturnable<BlockState?>
    ) {
        val grid = floorGrid ?: return

        val localX = blockX() - minX
        val localZ = blockZ() - minZ

        if (localX !in 0..15 || localZ !in 0..15) {
            return
        }

        val floorY = grid[localX + localZ * 16]

        if (floorY == Int.MIN_VALUE) {
            return
        }

        val y = blockY()
        val currentState = cir.returnValue

        if (currentState?.`is`(Blocks.BEDROCK) == true) {
            return
        }

        if (y >= cachedSeaLevel) {
            cir.returnValue = Blocks.AIR.defaultBlockState()
            return
        }

        if (y > floorY) {
            cir.returnValue = Blocks.WATER.defaultBlockState()
            return
        }

        if (
            currentState == null ||
            currentState.isAir ||
            !currentState.fluidState.isEmpty
        ) {
            cir.returnValue = if (y < 0) {
                Blocks.DEEPSLATE.defaultBlockState()
            } else {
                Blocks.STONE.defaultBlockState()
            }
        }
    }
}
