package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

import fr.heta__h.squ_abyssal_bloom.worldgen.ModNoises
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import net.minecraft.world.level.levelgen.synth.NormalNoise

class BioluminescentRegionalNoiseSampler(seed: Long) {
    private val noise = NormalNoise.create(
        XoroshiroRandomSource(RandomSupport.mixStafford13(seed xor REGIONAL_SEED_SALT)),
        ModNoises.ABYSSAL_TOPO_PARAMETERS
    )

    fun sample(worldX: Double, worldZ: Double): Double {
        return (0.5 + noise.getValue(worldX * REGIONAL_SCALE, 0.0, worldZ * REGIONAL_SCALE) * 0.5)
            .coerceIn(0.0, 1.0)
    }

    private companion object {
        const val REGIONAL_SCALE = 0.004
        const val REGIONAL_SEED_SALT = 0x1F83D9ABFB41BD6BL
    }
}
