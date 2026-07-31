package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

import fr.heta__h.squ_abyssal_bloom.worldgen.ModNoises
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import net.minecraft.world.level.levelgen.synth.NormalNoise

internal class BioluminescentNoiseSampler(
    seed: Long
) {
   companion object {
        const val LARGE_SEED_SALT = 0x243F6A8885A308D3L
        const val DETAIL_SEED_SALT = 0x13198A2E03707344L
        const val COLOR_SEED_SALT = 0x3C6EF372FE94F82AL
    }

    private val largeNoise = createNoise(ModNoises.ABYSSAL_TOPO_PARAMETERS, seed, LARGE_SEED_SALT)
    private val detailNoise = createNoise(ModNoises.ABYSSAL_DETAIL_PARAMETERS, seed, DETAIL_SEED_SALT)
    private val colorNoise = createNoise(ModNoises.ABYSSAL_WALL_PARAMETERS, seed, COLOR_SEED_SALT)

    fun sampleLarge(x: Double, z: Double): Double = sample2d(largeNoise, x, z)

    fun sampleDetail(x: Double, z: Double): Double = sample2d(detailNoise, x, z)

    fun sampleColor(x: Double, z: Double): Double = sample2d(colorNoise, x, z)

    private fun createNoise(
        parameters: NormalNoise.NoiseParameters,
        seed: Long,
        salt: Long
    ): NormalNoise {
        val random = XoroshiroRandomSource(RandomSupport.mixStafford13(seed xor salt))
        return NormalNoise.create(random, parameters)
    }

    private fun sample2d(noise: NormalNoise, x: Double, z: Double): Double {
        return (0.5 + noise.getValue(x, 0.0, z) * 0.5).coerceIn(0.0, 1.0)
    }
}
