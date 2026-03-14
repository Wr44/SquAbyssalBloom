package fr.heta__h.squ_abyssal_bloom.particle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.util.RandomSource
import kotlin.math.PI

class MarineSnowParticleProvider(private val sprites: SpriteSet) : ParticleProvider<SimpleParticleType> {
    override fun createParticle(
        type: SimpleParticleType,
        level: ClientLevel,
        x: Double, y: Double, z: Double,
        xd: Double, yd: Double, zd: Double,
        random: RandomSource
    ): Particle? {
        val tint = MarineSnowParticle.TINTS[random.nextInt(4)]
        val phase = random.nextFloat() * (PI * 2).toFloat()
        return MarineSnowParticle(level, x, y, z, sprites.get(random), tint.first, tint.second, tint.third, phase)
    }
}
