package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.worldgen.ModNoises

class BioluminescentRegionalNoiseSampler(seed: Long) {
    companion object {
        const val REGIONAL_SCALE = 0.004
        const val REGIONAL_SEED_SALT = 0x1F83D9ABFB41BD6BL
    }

    private val noise = ModUtilities.createNormalNoise(
        ModNoises.ABYSSAL_TOPO_PARAMETERS,
        seed,
        REGIONAL_SEED_SALT
    )

    fun sample(worldX: Double, worldZ: Double): Double {
        return ModUtilities.sampleNoise2d(
            noise,
            worldX * REGIONAL_SCALE,
            worldZ * REGIONAL_SCALE
        )
    }

}
