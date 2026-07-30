package fr.heta__h.squ_abyssal_bloom.particle.underwater_torch

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.BubbleColumnUpParticle
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.particles.ParticleTypes

class UnderwaterTorchBubbleParticle(

    level: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    xd: Double,
    yd: Double,
    zd: Double,
    sprite: TextureAtlasSprite
) : BubbleColumnUpParticle(level, x, y, z, xd, yd, zd, sprite) {

    companion object {
        private const val MIN_UPWARD_SPEED = 0.08
        private const val BASE_LIFETIME = 24
        private const val LIFETIME_VARIANCE = 12
    }

    init {
        lifetime = BASE_LIFETIME + random.nextInt(LIFETIME_VARIANCE)
    }

    override fun tick() {
        val wasAlive = isAlive
        yd = maxOf(yd, MIN_UPWARD_SPEED)

        super.tick()

        if (wasAlive && !isAlive) {
            level.addParticle(
                ParticleTypes.BUBBLE_POP,
                x, y, z,
                0.0, 0.0, 0.0
            )
        }
    }
}
