package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone

object BioluminescentZonePresets {
    val SMALL = BioluminescentZonePreset(
        size = BioluminescentZoneSize.SMALL,
        geodesicRadiusRange = 20..36,
        analysisMargin = 8,
        maxWaterCells = 4096,
        minWaterCells = 96,
        coreCountRange = 2..4,
        macroCoverageRange = 0.35..0.55,
        visiblePixelCoverageRange = 0.30..0.50,
        coreRadiusRange = 8.0..15.0,
        connectionWidthRange = 2.6..5.0,
        additionalConnectionCountRange = 0..1,
        reactionDiffusionScale = 1,
        reactionDiffusionIterations = 120,
        porosityRange = 0.50..0.70,
        minConnectionEmission = 0.07
    )

    val LARGE = BioluminescentZonePreset(
        size = BioluminescentZoneSize.LARGE,
        geodesicRadiusRange = 48..80,
        analysisMargin = 18,
        maxWaterCells = 16384,
        minWaterCells = 384,
        coreCountRange = 5..8,
        macroCoverageRange = 0.72..0.88,
        visiblePixelCoverageRange = 0.44..0.64,
        coreRadiusRange = 14.0..28.0,
        connectionWidthRange = 4.4..8.4,
        additionalConnectionCountRange = 2..3,
        reactionDiffusionScale = 1,
        reactionDiffusionIterations = 160,
        porosityRange = 0.40..0.60,
        minConnectionEmission = 0.08
    )

    fun forSize(size: BioluminescentZoneSize): BioluminescentZonePreset = when (size) {
        BioluminescentZoneSize.SMALL -> SMALL
        BioluminescentZoneSize.LARGE -> LARGE
    }
}
