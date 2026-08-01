package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone

data class BioluminescentZonePreset(
    val size: BioluminescentZoneSize,
    val geodesicRadiusRange: IntRange,
    val analysisMargin: Int,
    val maximumWaterCells: Int,
    val minimumWaterCells: Int,
    val coreCountRange: IntRange,
    val macroCoverageRange: ClosedFloatingPointRange<Double>,
    val visiblePixelCoverageRange: ClosedFloatingPointRange<Double>,
    val coreRadiusRange: ClosedFloatingPointRange<Double>,
    val connectionWidthRange: ClosedFloatingPointRange<Double>,
    val reactionDiffusionScale: Int,
    val reactionDiffusionIterations: Int,
    val porosityRange: ClosedFloatingPointRange<Double>,
    val artificialBoundaryFade: Double,
    val minimumConnectionEmission: Double
)
