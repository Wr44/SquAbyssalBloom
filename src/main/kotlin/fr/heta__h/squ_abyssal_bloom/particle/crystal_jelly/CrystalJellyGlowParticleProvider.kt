package fr.heta__h.squ_abyssal_bloom.particle.crystal_jelly

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.util.RandomSource

class CrystalJellyGlowParticleProvider(
    private val sprites: SpriteSet
) : ParticleProvider<SimpleParticleType> {

    override fun createParticle(
        type: SimpleParticleType,
        level: ClientLevel,
        x: Double, y: Double, z: Double,
        xd: Double, yd: Double, zd: Double,
        random: RandomSource
    ): Particle = CrystalJellyGlowParticle(level, x, y, z, yd, sprites, random)
}
