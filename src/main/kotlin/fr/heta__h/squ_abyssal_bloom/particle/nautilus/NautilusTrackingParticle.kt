package fr.heta__h.squ_abyssal_bloom.client.particle.nautilus

import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.SingleQuadParticle
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NautilusTrackingParticle(
    level: ClientLevel,
    x: Double, y: Double, z: Double,
    sprite: TextureAtlasSprite,
    private val targetEntityId: Int
) : SingleQuadParticle(level, x, y, z, 0.0, 0.0, 0.0, sprite) {

    private val targetEntity: AbstractNautilus? = level.getEntity(targetEntityId) as? AbstractNautilus

    private val initialQuadSize: Float

    init {
        gravity = 0.0f
        hasPhysics = false

        initialQuadSize = 0.1f * (random.nextFloat() * 0.5f + 0.5f) * 2.0f
        quadSize = initialQuadSize

        lifetime = 50 + random.nextInt(20)
    }

    override fun tick() {
        if (targetEntity == null || !targetEntity.isAlive) {
            this.remove()
            return
        }

        val animTime = targetEntity.tickCount.toFloat()

        val orbitAngle = animTime * NautilusLayer.ORBIT_SPEED
        val verticalOffset = sin((orbitAngle * NautilusLayer.VERTICAL_PERIODS).toDouble()) * NautilusLayer.VERTICAL_AMPLITUDE

        var f1 = sin((animTime * 0.1f).toDouble()).toFloat() / 2f + 0.5f
        f1 = f1 * f1 + f1
        val eyeLocalY = (-0.2f + f1 * 0.2f) * NautilusLayer.CONDUIT_SCALE

        val lookAheadTime = 3.0f
        val futureOrbitAngle = (animTime + lookAheadTime) * NautilusLayer.ORBIT_SPEED

        val worldOffsetX = cos(futureOrbitAngle.toDouble()) * NautilusLayer.ORBIT_RADIUS
        val worldOffsetZ = sin(futureOrbitAngle.toDouble()) * NautilusLayer.ORBIT_RADIUS * -1.0

        val targetX = targetEntity.x + worldOffsetX
        val targetY = targetEntity.y + NautilusLayer.BASE_HEIGHT + verticalOffset + eyeLocalY
        val targetZ = targetEntity.z + worldOffsetZ

        val dx = targetX - this.x
        val dy = targetY - this.y
        val dz = targetZ - this.z

        val distanceSq = dx * dx + dy * dy + dz * dz
        val dist = sqrt(distanceSq).toFloat()

        if (distanceSq < 0.06) {
            this.remove()
            return
        }

        val distanceMultiplier = (dist / 1.5f).coerceIn(0.01f, 1.0f)

        val pullStrengthXZ = 0.04f + (0.08f * (1.0f - distanceMultiplier))
        val pullStrengthY = 0.15f + (0.25f * (1.0f - distanceMultiplier))

        xd += dx * pullStrengthXZ
        zd += dz * pullStrengthXZ
        yd += dy * pullStrengthY

        xd *= 0.72
        zd *= 0.72
        yd *= 0.50

        this.quadSize = this.initialQuadSize * distanceMultiplier
        this.oRoll = this.roll
        this.roll += (1.0f - distanceMultiplier) * 0.8f

        super.tick()
    }

    override fun getLayer(): Layer {
        return Layer.TRANSLUCENT
    }

}