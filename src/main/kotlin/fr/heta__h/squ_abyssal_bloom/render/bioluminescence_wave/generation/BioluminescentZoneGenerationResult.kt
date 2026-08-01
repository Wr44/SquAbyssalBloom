package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentMacroField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton.BioluminescentTopology
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile

data class BioluminescentZoneGenerationResult(
    val domain: BioluminescentWaterDomain,
    val topology: BioluminescentTopology,
    val macroField: BioluminescentMacroField,
    val emissionField: BioluminescentEmissionField,
    val reactionDiffusion: BioluminescentReactionDiffusionStats,
    val tiles: List<BioluminescentZoneTile>,
    val cpuNanos: Long,
    val uploadCount: Int
) {
    val preparedPixelCount: Int
        get() = emissionField.pixelCount

    val texturePixelCount: Int
        get() = tiles.sumOf(BioluminescentZoneTile::pixelCount)

    val renderQuadCount: Int
        get() = tiles.sumOf { tile -> tile.renderQuads.size }
}
