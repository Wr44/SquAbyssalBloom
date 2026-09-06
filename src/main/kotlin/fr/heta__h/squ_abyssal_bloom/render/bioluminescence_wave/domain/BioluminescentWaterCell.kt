package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain

import net.minecraft.core.BlockPos

data class BioluminescentWaterCell(
    val waterPos: BlockPos,
    val surfaceY: Double,
    val waterDepth: Double
)
