package fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.worldgen.terrain.AbyssalFloorShaper.ABYSSAL_DEEP_MARGIN
import fr.heta__h.squ_abyssal_bloom.worldgen.terrain.AbyssalFloorShaper.MUSHROOM_MIN
import fr.heta__h.squ_abyssal_bloom.worldgen.terrain.AbyssalFloorShaper.MUSHROOM_TRANSITION
import fr.heta__h.squ_abyssal_bloom.worldgen.terrain.AbyssalFloorShaper.OCEAN_MAX_CONT
import kotlin.math.max

data class AbyssalTerrainSettings(
    val shallowDeepEdge: Double,
    val deepAbyssalEdge: Double,
    val abyssalDeepSplit: Double,
    val continentalFull: Double,
    val seaLevel: Int,
    val hardLimit: Int,
    val bedrockFloorY: Int,
    val maxFloorY: Int,
    val shallowFloorY: Int,
    val deepFloorY: Int,
    val abyssalFloorY: Int,
    val warpAmp: Double,
    val warp2Amp: Double,
    val topoLargeAmp: Double,
    val topoAmp: Double,
    val topoMidAmp: Double,
    val wallAmp: Double,
    val detailAmp: Double,
    val microAmp: Double,
    val weirdnessAmp: Double,
    val faultThreshold: Double,
    val faultFrequencyPercent: Double,
    val faultBlend: Double,
    val faultOffsetAmp: Double,
    val trenchThreshold: Double,
    val trenchFrequencyPercent: Double,
    val trenchWallSteepness: Double,
    val terraceStep: Double,
    val terraceMaskThreshold: Double,
    val terraceFrequencyPercent: Double
) {
    companion object {
        const val MIN_BOUNDARY_GAP = 0.05
        private const val MIN_ABYSSAL_SPAN = 0.05
        private const val DEFAULT_ABYSSAL_SPAN = 0.115
        private const val NUMERIC_EPSILON = 1.0E-6

        fun capture(seaLevel: Int, minY: Int): AbyssalTerrainSettings {
            val minimumDeepEdge =
                MUSHROOM_MIN + MUSHROOM_TRANSITION + MIN_ABYSSAL_SPAN - ABYSSAL_DEEP_MARGIN
            val shallowDeepEdge = ModServerConfig.SHALLOW_DEEP_BOUNDARY.get()
                .coerceIn(minimumDeepEdge + MIN_BOUNDARY_GAP, OCEAN_MAX_CONT)
            val deepAbyssalEdge = ModServerConfig.DEEP_ABYSSAL_BOUNDARY.get()
                .coerceIn(minimumDeepEdge, shallowDeepEdge - MIN_BOUNDARY_GAP)
            val abyssalDeepSplit = deepAbyssalEdge + ABYSSAL_DEEP_MARGIN
            val continentalFull = max(
                MUSHROOM_MIN + MUSHROOM_TRANSITION,
                abyssalDeepSplit - DEFAULT_ABYSSAL_SPAN
            )

            val hardLimit = minY + 5
            val maxFloorY = max(hardLimit, seaLevel - ModServerConfig.SHALLOW_CLEARANCE.get())
            val shallowFloorY = ModServerConfig.SHALLOW_FLOOR_Y.get().coerceIn(hardLimit, maxFloorY)
            val deepFloorY = ModServerConfig.DEEP_FLOOR_TARGET.get()
                .coerceIn(hardLimit, max(hardLimit, shallowFloorY - 1))
            val abyssalFloorY = ModServerConfig.TARGET_FLOOR_Y.get()
                .coerceIn(hardLimit, max(hardLimit, deepFloorY - 1))

            val faultFrequencyPercent = ModServerConfig.FAULT_FREQUENCY_PERCENT.get().coerceIn(0.0, 100.0)
            val trenchFrequencyPercent = ModServerConfig.TRENCH_FREQUENCY_PERCENT.get().coerceIn(0.0, 100.0)
            val terraceFrequencyPercent = ModServerConfig.TERRACE_FREQUENCY_PERCENT.get().coerceIn(0.0, 100.0)

            return AbyssalTerrainSettings(
                shallowDeepEdge = shallowDeepEdge,
                deepAbyssalEdge = deepAbyssalEdge,
                abyssalDeepSplit = abyssalDeepSplit,
                continentalFull = continentalFull,
                seaLevel = seaLevel,
                hardLimit = hardLimit,
                bedrockFloorY = minY,
                maxFloorY = maxFloorY,
                shallowFloorY = shallowFloorY,
                deepFloorY = deepFloorY,
                abyssalFloorY = abyssalFloorY,
                warpAmp = ModServerConfig.WARP_AMP.get(),
                warp2Amp = ModServerConfig.WARP2_AMP.get(),
                topoLargeAmp = ModServerConfig.TOPO_LARGE_AMP.get(),
                topoAmp = ModServerConfig.TOPO_AMP.get(),
                topoMidAmp = ModServerConfig.TOPO_MID_AMP.get(),
                wallAmp = ModServerConfig.WALL_AMP.get(),
                detailAmp = ModServerConfig.DETAIL_AMP.get(),
                microAmp = ModServerConfig.MICRO_AMP.get(),
                weirdnessAmp = ModServerConfig.WEIRDNESS_AMP.get(),
                faultThreshold = NoiseThresholdCalibration.faultThresholdForPercent(faultFrequencyPercent),
                faultFrequencyPercent = faultFrequencyPercent,
                faultBlend = ModServerConfig.FAULT_BLEND.get().coerceAtLeast(NUMERIC_EPSILON),
                faultOffsetAmp = ModServerConfig.FAULT_OFFSET_AMP.get(),
                trenchThreshold = NoiseThresholdCalibration.trenchThresholdForPercent(trenchFrequencyPercent),
                trenchFrequencyPercent = trenchFrequencyPercent,
                trenchWallSteepness = ModServerConfig.TRENCH_WALL_STEEPNESS.get().coerceIn(0.5, 6.0),
                terraceStep = ModServerConfig.TERRACE_STEP.get().coerceAtLeast(NUMERIC_EPSILON),
                terraceMaskThreshold = NoiseThresholdCalibration.terraceThresholdForPercent(terraceFrequencyPercent),
                terraceFrequencyPercent = terraceFrequencyPercent
            )
        }
    }
}
