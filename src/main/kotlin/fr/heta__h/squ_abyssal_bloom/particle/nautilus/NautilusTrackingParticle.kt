package fr.heta__h.squ_abyssal_bloom.client.particle.nautilus

import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import net.minecraft.client.multiplayer.ClientLevel
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

    companion object {
        private const val BASE_QUAD_SIZE = 0.1f
        private const val SIZE_RANDOM_MULTIPLIER = 0.5f
        private const val SIZE_RANDOM_OFFSET = 0.5f
        private const val SIZE_GLOBAL_SCALE = 2.0f

        private const val LIFETIME_BASE = 50
        private const val LIFETIME_VARIANCE = 20

        private const val ROLL_SPEED = 0.8f

        private const val EYE_ANIM_SPEED = 0.1f
        private const val EYE_ANIM_DIVISOR = 2f
        private const val EYE_ANIM_OFFSET = 0.5f
        private const val EYE_BASE_Y_OFFSET = -0.2f
        private const val EYE_FLOAT_AMPLITUDE = 0.2f
        private const val Z_AXIS_MIRROR = -1.0

        private const val LOOK_AHEAD_TIME = 3.0f
        private const val HITBOX_DISTANCE_SQ = 0.06

        private const val DIST_MULTIPLIER_DIVISOR = 1.5f
        private const val DIST_MULTIPLIER_MIN = 0.01f
        private const val DIST_MULTIPLIER_MAX = 1.0f

        private const val PULL_XZ_BASE = 0.04f
        private const val PULL_XZ_SCALE = 0.08f
        private const val PULL_Y_BASE = 0.15f
        private const val PULL_Y_SCALE = 0.25f

        private const val FRICTION_XZ = 0.72
        private const val FRICTION_Y = 0.50
    }

    private val targetEntity: AbstractNautilus? = level.getEntity(targetEntityId) as? AbstractNautilus
    private val initialQuadSize: Float

    init {
        gravity = 0.0f
        hasPhysics = false

        initialQuadSize = BASE_QUAD_SIZE * (random.nextFloat() * SIZE_RANDOM_MULTIPLIER + SIZE_RANDOM_OFFSET) * SIZE_GLOBAL_SCALE
        quadSize = initialQuadSize

        lifetime = LIFETIME_BASE + random.nextInt(LIFETIME_VARIANCE)
    }

    override fun tick() {
        if (targetEntity == null || !targetEntity.isAlive) {
            this.remove()
            return
        }

        val animTime = targetEntity.tickCount.toFloat()

        val orbitAngle = animTime * NautilusLayer.ORBIT_SPEED
        val verticalOffset = sin((orbitAngle * NautilusLayer.VERTICAL_PERIODS).toDouble()) * NautilusLayer.VERTICAL_AMPLITUDE

        var f1 = sin((animTime * EYE_ANIM_SPEED).toDouble()).toFloat() / EYE_ANIM_DIVISOR + EYE_ANIM_OFFSET
        f1 = f1 * f1 + f1
        val eyeLocalY = (EYE_BASE_Y_OFFSET + f1 * EYE_FLOAT_AMPLITUDE) * NautilusLayer.CONDUIT_SCALE

        val futureOrbitAngle = (animTime + LOOK_AHEAD_TIME) * NautilusLayer.ORBIT_SPEED

        val worldOffsetX = cos(futureOrbitAngle.toDouble()) * NautilusLayer.ORBIT_RADIUS
        val worldOffsetZ = sin(futureOrbitAngle.toDouble()) * NautilusLayer.ORBIT_RADIUS * Z_AXIS_MIRROR

        val targetX = targetEntity.x + worldOffsetX
        val targetY = targetEntity.y + NautilusLayer.BASE_HEIGHT + verticalOffset + eyeLocalY
        val targetZ = targetEntity.z + worldOffsetZ

        val dx = targetX - this.x
        val dy = targetY - this.y
        val dz = targetZ - this.z

        val distanceSq = dx * dx + dy * dy + dz * dz
        val dist = sqrt(distanceSq).toFloat()

        if (distanceSq < HITBOX_DISTANCE_SQ) {
            this.remove()
            return
        }

        val distanceMultiplier = (dist / DIST_MULTIPLIER_DIVISOR).coerceIn(DIST_MULTIPLIER_MIN, DIST_MULTIPLIER_MAX)

        val pullStrengthXZ = PULL_XZ_BASE + (PULL_XZ_SCALE * (1.0f - distanceMultiplier))
        val pullStrengthY = PULL_Y_BASE + (PULL_Y_SCALE * (1.0f - distanceMultiplier))

        xd += dx * pullStrengthXZ
        zd += dz * pullStrengthXZ
        yd += dy * pullStrengthY

        xd *= FRICTION_XZ
        zd *= FRICTION_XZ
        yd *= FRICTION_Y

        this.quadSize = this.initialQuadSize * distanceMultiplier
        this.oRoll = this.roll
        this.roll += (1.0f - distanceMultiplier) * ROLL_SPEED

        super.tick()
    }

    override fun getLayer(): Layer {
        return Layer.TRANSLUCENT
    }
}