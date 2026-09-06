package fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.util.RandomSource

class BioluminescentWaterParticleProvider(
    private val sprites: SpriteSet
) : ParticleProvider<BioluminescentWaterParticleOptions> {

    override fun createParticle(
        type: BioluminescentWaterParticleOptions,
        level: ClientLevel,
        x: Double, y: Double, z: Double,
        xd: Double, yd: Double, zd: Double,
        random: RandomSource
    ): Particle {
        return BioluminescentWaterParticle(
            level, x, y, z,
            sprites.get(random),
            type.color,
            type.alpha
        )
    }
}
