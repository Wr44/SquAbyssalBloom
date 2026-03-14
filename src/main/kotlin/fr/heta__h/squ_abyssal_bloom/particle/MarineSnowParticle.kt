package fr.heta__h.squ_abyssal_bloom.particle

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.AbyssDepthCache
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.SingleQuadParticle
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import kotlin.math.cos
import kotlin.math.sin

class MarineSnowParticle(
    level: ClientLevel, x: Double, y: Double, z: Double, sprite: TextureAtlasSprite,
    private val tintR: Float, private val tintG: Float, private val tintB: Float,
    private val swayPhase: Float
) : SingleQuadParticle(level, x, y, z, 0.0, -0.002, 0.0, sprite) {

    companion object {
        val TINTS = arrayOf(
            Triple(1f, 1f, 1f),
            Triple(0.85f, 0.92f, 1f),
            Triple(0.96f, 0.94f, 0.88f),
            Triple(0.78f, 0.80f, 0.82f)
        )
    }

    init {
        gravity = 0.0f
        hasPhysics = false
        quadSize = 0.04f + (random.nextFloat() * 0.05f)
        lifetime = 200 + random.nextInt(201)
        setColor(tintR, tintG, tintB)
    }

    override fun tick() {
        yd = -0.008
        val amp = (0.0015 * ModConfig.marineSnowSwayAmplitude).toFloat()
        xd = (sin(age * 0.04f + swayPhase) * amp).toDouble()
        zd = (cos(age * 0.04f + swayPhase * 1.3f) * amp).toDouble()

        val fadeAlpha: Float = when {
            age < 20 -> age / 20.0f
            age > lifetime - 20 -> (lifetime - age) / 20.0f
            else -> 1.0f
        }

        alpha = (fadeAlpha * AbyssDepthCache.displayedDepthFactor.toFloat()).coerceIn(0.0f, 1.0f)

        super.tick()
    }

    override fun getLayer(): Layer {
        return Layer.TRANSLUCENT
    }

    override fun getLightColor(partialTick: Float): Int {
        val fadeTicks = 30.0f
        val currentAge = this.age + partialTick

        val factor = (currentAge / fadeTicks).coerceIn(0.0f, 1.0f)

        val worldLight = super.getLightColor(partialTick)
        val worldBlockLight = (worldLight shr 4) and 0xF

        val targetBlockLight = 10

        val currentBlockLight = (worldBlockLight + (targetBlockLight - worldBlockLight) * factor).toInt()

        return (0 shl 20) or (currentBlockLight shl 4)
    }
}
