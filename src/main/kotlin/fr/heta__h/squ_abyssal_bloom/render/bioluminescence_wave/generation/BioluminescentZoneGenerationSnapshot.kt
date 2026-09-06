package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation

import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentZoneGenerationStage

data class BioluminescentZoneGenerationSnapshot(
    val stage: BioluminescentZoneGenerationStage,
    val waterCells: Int,
    val geodesicRadius: Int,
    val selectedCores: Int,
    val reactionIterations: Int,
    val targetReactionIterations: Int,
    val preparedTiles: Int,
    val uploadedTiles: Int,
    val cpuNanos: Long,
    val failureReason: String?
)
