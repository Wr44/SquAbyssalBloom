package fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain

import fr.heta__h.squ_abyssal_bloom.worldgen.terrain.AbyssalFloorShaper.ABYSSAL_DEEP_MARGIN
import net.minecraft.world.level.levelgen.synth.NormalNoise

class AbyssalShapingContext(
    val topoNoise: NormalNoise,
    val wallNoise: NormalNoise,
    val detailNoise: NormalNoise,
    val shallowDeepEdge: Double,
    val deepAbyssalEdge: Double,
    val seaLevel: Int,
    val deepHardLimit: Int,
    val abyssalHardLimit: Int
) {
    val abyssalDeepSplit = deepAbyssalEdge + ABYSSAL_DEEP_MARGIN
}