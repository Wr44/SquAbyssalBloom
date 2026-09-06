package fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveSize

object BioluminescentZonePresets {
    val SMALL = BioluminescentZonePreset(
        size = BioluminescenceWaveSize.SMALL,
        geodesicRadiusRange = BioluminescenceWaveSize.SMALL.geodesicRadiusRange,
        analysisMargin = BioluminescenceWaveSize.SMALL.analysisMargin,
        maxWaterCells = BioluminescenceWaveSize.SMALL.maximumWaterCells,
        minWaterCells = BioluminescenceWaveSize.SMALL.minimumWaterCells,
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
        size = BioluminescenceWaveSize.LARGE,
        geodesicRadiusRange = BioluminescenceWaveSize.LARGE.geodesicRadiusRange,
        analysisMargin = BioluminescenceWaveSize.LARGE.analysisMargin,
        maxWaterCells = BioluminescenceWaveSize.LARGE.maximumWaterCells,
        minWaterCells = BioluminescenceWaveSize.LARGE.minimumWaterCells,
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

}
