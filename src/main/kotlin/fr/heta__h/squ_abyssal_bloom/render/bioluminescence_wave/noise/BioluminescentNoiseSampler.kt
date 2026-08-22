package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.worldgen.ModNoises
import net.minecraft.world.level.levelgen.synth.NormalNoise

internal class BioluminescentNoiseSampler(
    seed: Long,
    colorSeed: Long = seed
) {
    companion object {
        const val LARGE_SEED_SALT = 0x243F6A8885A308D3L
        const val DETAIL_SEED_SALT = 0x13198A2E03707344L
        const val COLOR_SEED_SALT = 0x3C6EF372FE94F82AL
    }

    private val largeNoise = ModUtilities.createNormalNoise(
        ModNoises.ABYSSAL_TOPO_PARAMETERS,
        seed,
        LARGE_SEED_SALT
    )
    private val detailNoise = ModUtilities.createNormalNoise(
        ModNoises.ABYSSAL_DETAIL_PARAMETERS,
        seed,
        DETAIL_SEED_SALT
    )
    private val colorNoise = ModUtilities.createNormalNoise(
        ModNoises.ABYSSAL_WALL_PARAMETERS,
        colorSeed,
        COLOR_SEED_SALT
    )

    fun sampleLarge(x: Double, z: Double): Double = ModUtilities.sampleNoise2d(largeNoise, x, z)

    fun sampleDetail(x: Double, z: Double): Double = ModUtilities.sampleNoise2d(detailNoise, x, z)

    fun sampleColor(x: Double, z: Double): Double = ModUtilities.sampleNoise2d(colorNoise, x, z)

}
