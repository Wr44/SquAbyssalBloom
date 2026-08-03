package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.interaction

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import kotlin.math.sqrt

class BioluminescentMovementWave internal constructor(
    internal val originX: Double,
    internal val originZ: Double,
    internal val startedAt: Long,
    private val amplitude: Double,
    private val radius: Double
) {
    companion object {
        const val CORE_RADIUS_FRACTION = 0.75
        const val ATTACK_TICKS = 3.0
        const val HOLD_TICKS = 3.0
        const val RELEASE_TICKS = 24.0
        const val LIFETIME_TICKS = ATTACK_TICKS + HOLD_TICKS + RELEASE_TICKS
    }

    fun intensityAt(worldX: Double, worldZ: Double, renderGameTime: Double): Double {
        val age = renderGameTime - startedAt
        if (age !in 0.0..<LIFETIME_TICKS) return 0.0
        val deltaX = worldX - originX
        val deltaZ = worldZ - originZ
        val distance = sqrt(deltaX * deltaX + deltaZ * deltaZ)
        if (distance >= radius) return 0.0

        val radialFade = 1.0 - ModUtilities.smooth(radius * CORE_RADIUS_FRACTION, radius, distance)
        return radialFade * envelopeAt(age) * amplitude
    }

    fun affects(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Boolean {
        val age = renderGameTime - startedAt
        if (age < 0.0 || age >= LIFETIME_TICKS) return false
        val nearestX = originX.coerceIn(minX, maxX)
        val nearestZ = originZ.coerceIn(minZ, maxZ)
        val deltaX = originX - nearestX
        val deltaZ = originZ - nearestZ
        return deltaX * deltaX + deltaZ * deltaZ <= radius * radius
    }

    fun isExpiredAt(gameTime: Long): Boolean {
        return gameTime < startedAt || gameTime - startedAt >= LIFETIME_TICKS
    }

    private fun envelopeAt(age: Double): Double {
        return when {
            age < ATTACK_TICKS -> ModUtilities.smooth(0.0, ATTACK_TICKS, age)
            age < ATTACK_TICKS + HOLD_TICKS -> 1.0
            else -> 1.0 - ModUtilities.smooth(
                0.0,
                RELEASE_TICKS,
                age - ATTACK_TICKS - HOLD_TICKS
            )
        }
    }

}
