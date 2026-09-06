package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.core.BioluminescentCore

data class BioluminescentTopology(
    val cores: List<BioluminescentCore>,
    val skeleton: BioluminescentSkeleton,
    val coastDirectionX: Double,
    val coastDirectionZ: Double
)
