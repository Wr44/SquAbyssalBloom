package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone

import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveSize

data class BioluminescentZonePreset(
    val size: BioluminescenceWaveSize,
    val geodesicRadiusRange: IntRange,
    val analysisMargin: Int,
    val maxWaterCells: Int,
    val minWaterCells: Int,
    val coreCountRange: IntRange,
    val macroCoverageRange: ClosedFloatingPointRange<Double>,
    val visiblePixelCoverageRange: ClosedFloatingPointRange<Double>,
    val coreRadiusRange: ClosedFloatingPointRange<Double>,
    val connectionWidthRange: ClosedFloatingPointRange<Double>,
    val additionalConnectionCountRange: IntRange,
    val reactionDiffusionScale: Int,
    val reactionDiffusionIterations: Int,
    val porosityRange: ClosedFloatingPointRange<Double>,
    val minConnectionEmission: Double
)
