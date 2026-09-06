package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class BioluminescentBloomPulse internal constructor(
    internal val originX: Double,
    internal val originZ: Double,
    internal val startedAt: Long,
    private val maxRadius: Double
) {
    companion object {
        const val PULSE_SPEED = 0.45
        const val RING_CORE_WIDTH = 1.8
        const val RING_WIDTH = 4.5
        const val ATTACK_TICKS = 8.0
        const val END_FADE_START_FRACTION = 0.8
    }

    private val lifetimeTicks: Double = (maxRadius + RING_WIDTH) / PULSE_SPEED

    fun intensityAt(worldX: Double, worldZ: Double, renderGameTime: Double): Double {
        val frontRadius = frontRadiusAt(renderGameTime) ?: return 0.0
        val deltaX = worldX - originX
        val deltaZ = worldZ - originZ
        val distanceSquared = deltaX * deltaX + deltaZ * deltaZ
        val outerRadius = frontRadius + RING_WIDTH
        if (distanceSquared > outerRadius * outerRadius) return 0.0
        val innerRadius = frontRadius - RING_WIDTH
        if (innerRadius > 0.0 && distanceSquared < innerRadius * innerRadius) return 0.0
        val distance = sqrt(distanceSquared)
        return intensityForRingError(abs(distance - frontRadius), frontRadius, renderGameTime)
    }

    fun maxIntensityIn(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Double {
        val frontRadius = frontRadiusAt(renderGameTime) ?: return 0.0
        val nearDistance = nearestDistanceTo(minX, minZ, maxX, maxZ)
        val farDistance = farthestDistanceTo(minX, minZ, maxX, maxZ)
        val ringError = when {
            frontRadius < nearDistance -> nearDistance - frontRadius
            frontRadius > farDistance -> frontRadius - farDistance
            else -> 0.0
        }
        return intensityForRingError(ringError, frontRadius, renderGameTime)
    }

    fun affects(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Boolean {
        val frontRadius = frontRadiusAt(renderGameTime) ?: return false
        return nearestDistanceTo(minX, minZ, maxX, maxZ) <= frontRadius + RING_WIDTH &&
            farthestDistanceTo(minX, minZ, maxX, maxZ) >= frontRadius - RING_WIDTH
    }

    fun isExpiredAt(gameTime: Long): Boolean {
        return gameTime < startedAt || gameTime - startedAt >= lifetimeTicks
    }

    internal fun frontStrengthAt(renderGameTime: Double): Double {
        val frontRadius = frontRadiusAt(renderGameTime) ?: return 0.0
        return intensityForRingError(0.0, frontRadius, renderGameTime)
    }

    internal fun frontRadiusAt(renderGameTime: Double): Double? {
        val age = renderGameTime - startedAt
        if (age < 0.0 || age >= lifetimeTicks) return null
        return age * PULSE_SPEED
    }

    private fun intensityForRingError(
        ringError: Double,
        frontRadius: Double,
        renderGameTime: Double
    ): Double {
        val band = 1.0 - ModUtilities.smooth(RING_CORE_WIDTH, RING_WIDTH, ringError)
        if (band <= 0.0) return 0.0
        val attack = ModUtilities.smooth(0.0, ATTACK_TICKS, renderGameTime - startedAt)
        val endFade = 1.0 - ModUtilities.smooth(
            maxRadius * END_FADE_START_FRACTION,
            maxRadius + RING_WIDTH,
            frontRadius
        )
        return band * attack * endFade
    }

    private fun nearestDistanceTo(minX: Double, minZ: Double, maxX: Double, maxZ: Double): Double {
        val deltaX = originX - originX.coerceIn(minX, maxX)
        val deltaZ = originZ - originZ.coerceIn(minZ, maxZ)
        return sqrt(deltaX * deltaX + deltaZ * deltaZ)
    }

    private fun farthestDistanceTo(minX: Double, minZ: Double, maxX: Double, maxZ: Double): Double {
        val deltaX = max(abs(minX - originX), abs(maxX - originX))
        val deltaZ = max(abs(minZ - originZ), abs(maxZ - originZ))
        return sqrt(deltaX * deltaX + deltaZ * deltaZ)
    }
}
