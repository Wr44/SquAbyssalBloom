package fr.heta__h.squ_abyssal_bloom.particle.nautilus

import fr.heta__h.squ_abyssal_bloom.client.particle.nautilus.NautilusTrackingParticle
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.util.RandomSource

class NautilusTrackingParticleProvider(private val sprites: SpriteSet) : ParticleProvider<SimpleParticleType> {
    override fun createParticle(
        type: SimpleParticleType,
        level: ClientLevel,
        x: Double, y: Double, z: Double,
        xd: Double, yd: Double, zd: Double,
        random: RandomSource
    ): Particle? {
        val entityId = xd.toInt()

        return NautilusTrackingParticle(level, x, y, z, sprites.get(random), entityId)
    }
}