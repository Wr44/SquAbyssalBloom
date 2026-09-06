package fr.heta__h.squ_abyssal_bloom.particle.crystal_jelly

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.GlowParticle
import net.minecraft.client.particle.SpriteSet
import net.minecraft.util.RandomSource

class CrystalJellyGlowParticle(
    level: ClientLevel,
    x: Double, y: Double, z: Double,
    yd: Double,
    sprites: SpriteSet,
    random: RandomSource
) : GlowParticle(level, x, y, z, 0.5 - random.nextDouble(), yd, 0.5 - random.nextDouble(), sprites) {

    private companion object {
        const val TINT_RED = 0.24f
        const val TINT_GREEN = 0.72f
        const val TINT_BLUE = 1.0f
        const val VERTICAL_DAMPING = 0.2
        const val HORIZONTAL_DAMPING = 0.1
        const val LIFETIME_SCALE = 8.0
    }

    init {
        setColor(TINT_RED, TINT_GREEN, TINT_BLUE)
        this.yd *= VERTICAL_DAMPING
        this.xd *= HORIZONTAL_DAMPING
        this.zd *= HORIZONTAL_DAMPING
        setLifetime((LIFETIME_SCALE / (random.nextDouble() * 0.8 + 0.2)).toInt())
    }
}
