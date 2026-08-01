package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone

object BioluminescentZonePresets {
    val SMALL = BioluminescentZonePreset(
        size = BioluminescentZoneSize.SMALL,
        geodesicRadiusRange = 10..20,
        analysisMargin = 3,
        maximumWaterCells = 1000,
        minimumWaterCells = 32,
        coreCountRange = 1..2,
        macroCoverageRange = 0.15..0.30,
        visiblePixelCoverageRange = 0.25..0.45,
        coreRadiusRange = 4.0..8.0,
        connectionWidthRange = 1.2..2.5,
        reactionDiffusionScale = 1,
        reactionDiffusionIterations = 80,
        porosityRange = 0.55..0.75,
        artificialBoundaryFade = 2.5,
        minimumConnectionEmission = 0.05
    )

    val MEDIUM = BioluminescentZonePreset(
        size = BioluminescentZoneSize.MEDIUM,
        geodesicRadiusRange = 20..36,
        analysisMargin = 5,
        maximumWaterCells = 3000,
        minimumWaterCells = 96,
        coreCountRange = 2..4,
        macroCoverageRange = 0.35..0.55,
        visiblePixelCoverageRange = 0.30..0.50,
        coreRadiusRange = 7.0..13.0,
        connectionWidthRange = 2.0..4.2,
        reactionDiffusionScale = 1,
        reactionDiffusionIterations = 120,
        porosityRange = 0.50..0.70,
        artificialBoundaryFade = 3.5,
        minimumConnectionEmission = 0.07
    )

    val LARGE = BioluminescentZonePreset(
        size = BioluminescentZoneSize.LARGE,
        geodesicRadiusRange = 36..64,
        analysisMargin = 8,
        maximumWaterCells = 8192,
        minimumWaterCells = 256,
        coreCountRange = 4..7,
        macroCoverageRange = 0.65..0.85,
        visiblePixelCoverageRange = 0.35..0.60,
        coreRadiusRange = 10.0..20.0,
        connectionWidthRange = 2.8..6.0,
        reactionDiffusionScale = 1,
        reactionDiffusionIterations = 160,
        porosityRange = 0.40..0.65,
        artificialBoundaryFade = 5.0,
        minimumConnectionEmission = 0.09
    )

    fun forSize(size: BioluminescentZoneSize): BioluminescentZonePreset {
        return when (size) {
            BioluminescentZoneSize.SMALL -> SMALL
            BioluminescentZoneSize.MEDIUM -> MEDIUM
            BioluminescentZoneSize.LARGE -> LARGE
        }
    }
}
