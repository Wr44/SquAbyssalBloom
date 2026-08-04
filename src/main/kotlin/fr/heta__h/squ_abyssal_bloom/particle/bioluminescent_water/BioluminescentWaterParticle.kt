package fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.SingleQuadParticle
import net.minecraft.client.renderer.texture.TextureAtlasSprite

class BioluminescentWaterParticle(
    level: ClientLevel,
    x: Double, y: Double, z: Double,
    sprite: TextureAtlasSprite,
    color: Int,
    private val targetAlpha: Float
) : SingleQuadParticle(level, x, y, z, 0.0, 0.0, 0.0, sprite) {

    companion object {
        private const val BASE_QUAD_SIZE = 0.05f
        private const val SIZE_RANDOM_VARIANCE = 0.035f
        private const val LIFETIME_BASE = 28
        private const val LIFETIME_VARIANCE = 16
        private const val RISE_HEIGHT_MIN = 1.0
        private const val RISE_HEIGHT_VARIANCE = 1.0
        private const val FADE_IN_TICKS = 3.0f
        private const val FADE_OUT_TICKS = 8.0f
    }

    private val baseY = y
    private val riseHeight = RISE_HEIGHT_MIN + random.nextDouble() * RISE_HEIGHT_VARIANCE

    init {
        hasPhysics = false
        quadSize = BASE_QUAD_SIZE + random.nextFloat() * SIZE_RANDOM_VARIANCE
        lifetime = LIFETIME_BASE + random.nextInt(LIFETIME_VARIANCE)
        setColor(
            (color shr 16 and 0xFF) / 255f,
            (color shr 8 and 0xFF) / 255f,
            (color and 0xFF) / 255f
        )
        alpha = 0.0f
    }

    override fun tick() {
        xo = x
        yo = y
        zo = z
        if (age++ >= lifetime) {
            remove()
            return
        }

        val progress = age.toDouble() / lifetime.toDouble()
        val height = riseHeight * 4.0 * progress * (1.0 - progress)
        setPos(x, baseY + height, z)

        alpha = (targetAlpha * fadeFactor(progress.toFloat())).coerceIn(0.0f, 1.0f)
    }

    private fun fadeFactor(progress: Float): Float {
        val fadeInEnd = FADE_IN_TICKS / lifetime
        val fadeOutStart = 1.0f - FADE_OUT_TICKS / lifetime
        return when {
            progress < fadeInEnd -> progress / fadeInEnd
            progress > fadeOutStart -> (1.0f - progress) / (1.0f - fadeOutStart)
            else -> 1.0f
        }
    }

    override fun getLightCoords(partialTick: Float): Int = 0xF000F0


    override fun getLayer(): Layer = Layer.TRANSLUCENT

}
