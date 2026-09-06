package fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain

import net.minecraft.world.level.levelgen.DensityFunction
import net.minecraft.world.level.levelgen.blending.Blender
import net.minecraft.world.level.levelgen.synth.NormalNoise

class AbyssalShapingContext(
    val topoNoise: NormalNoise,
    val wallNoise: NormalNoise,
    val detailNoise: NormalNoise,
    val settings: AbyssalTerrainSettings,
    val blender: Blender
) {
    private class BlendedPointContext(
        private val x: Int,
        private val y: Int,
        private val z: Int,
        private val blender: Blender
    ) : DensityFunction.FunctionContext {
        override fun blockX(): Int = x
        override fun blockY(): Int = y
        override fun blockZ(): Int = z
        override fun getBlender(): Blender = blender
    }

    val shallowDeepEdge: Double get() = settings.shallowDeepEdge
    val abyssalDeepSplit: Double get() = settings.abyssalDeepSplit
    val continentalFull: Double get() = settings.continentalFull
    val seaLevel: Int get() = settings.seaLevel
    val deepHardLimit: Int get() = settings.hardLimit
    val abyssalHardLimit: Int get() = settings.hardLimit
    val maxFloorY: Int get() = settings.maxFloorY

    fun densityContext(x: Int, y: Int, z: Int): DensityFunction.FunctionContext =
        BlendedPointContext(x, y, z, blender)
}