package fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

object BioluminescentCompensation {

    const val VISIBILITY_EPSILON = 0.01
    const val VISIBILITY_EPSILON_FLOAT = 0.01f
    const val RENDER_SIDE_HYSTERESIS = 0.05

    private const val SHALLOW_DEPTH = 4.0
    private const val DEEP_DEPTH = 24.0
    private const val SHALLOW_MULTIPLIER = 0.65
    private const val GRAZING_VISIBILITY_START = 0.15
    private const val TOP_VIEW_VISIBILITY_FULL = 0.65
    private const val MIN_DISTANCE = 1.0e-6

    fun depthFactor(waterDepth: Double): Double {
        return ModUtilities.smooth(SHALLOW_DEPTH, DEEP_DEPTH, waterDepth)
    }

    fun depthMultiplier(depthFactor: Double): Double {
        return ModUtilities.lerp(SHALLOW_MULTIPLIER, ModConfig.shaderBioluminescenceDeepWaterBoost, depthFactor)
    }

    fun angleFactor(centerX: Double, surfaceY: Double, centerZ: Double, cameraPosition: Vec3): Double {
        val deltaX = centerX - cameraPosition.x
        val deltaY = surfaceY - cameraPosition.y
        val deltaZ = centerZ - cameraPosition.z
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
        val viewAlignment = if (distance > MIN_DISTANCE) abs(deltaY) / distance else 1.0
        val aligned = ModUtilities.smooth(GRAZING_VISIBILITY_START, TOP_VIEW_VISIBILITY_FULL, viewAlignment)
        return ModUtilities.lerp(ModConfig.shaderBioluminescenceGrazingStrength, 1.0, aligned)
    }

    fun resolveRenderTop(previous: Boolean?, cameraY: Double, surfaceY: Double): Boolean {
        return when {
            previous == null -> cameraY >= surfaceY
            previous && cameraY < surfaceY - RENDER_SIDE_HYSTERESIS -> false
            !previous && cameraY > surfaceY + RENDER_SIDE_HYSTERESIS -> true
            else -> previous
        }
    }
}
