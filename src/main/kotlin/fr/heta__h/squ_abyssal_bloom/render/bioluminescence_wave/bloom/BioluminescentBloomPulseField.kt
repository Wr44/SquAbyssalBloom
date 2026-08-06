package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterBounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import net.minecraft.world.level.levelgen.RandomSupport
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class BioluminescentBloomPulseField {
    companion object {
        const val VISIBILITY_TICK_DT = 1.0 / 20.0
        const val VISIBILITY_FADE_RATE = 4.0
        const val MAX_ACTIVE_PULSES = 32
        private const val PULSE_PHASE_SALT = 0x1F83D9ABFB41BD6BL
    }

    private val pulses = ArrayDeque<BioluminescentBloomPulse>()
    private val lastPulseGameTime = HashMap<UUID, Long>()

    var visibilityStrength: Double = 0.0
        private set

    fun tick(
        blooms: Collection<BioluminescentBloom>,
        bounds: BioluminescentWaterBounds,
        gameTime: Long
    ): List<BioluminescentBloom> {
        pulses.removeIf { pulse -> pulse.isExpiredAt(gameTime) }
        val visibilityTarget = if (pulses.isNotEmpty()) 1.0 else 0.0
        visibilityStrength = ModUtilities.smoothTowards(
            visibilityStrength,
            visibilityTarget,
            VISIBILITY_TICK_DT,
            VISIBILITY_FADE_RATE
        ).coerceIn(0.0, 1.0)

        if (lastPulseGameTime.isNotEmpty()) {
            val presentIds = blooms.mapTo(HashSet()) { bloom -> bloom.id }
            lastPulseGameTime.keys.retainAll(presentIds)
        }

        val interval = ModConfig.bioluminescenceBloomPulseIntervalTicks.toLong().coerceAtLeast(1L)
        var emitted: MutableList<BioluminescentBloom>? = null
        for (bloom in blooms) {
            if (bloom.lifecycle != PlanktonBloomLifecycle.ACTIVE) continue
            val geometry = bloom.geometry ?: continue
            val previous = lastPulseGameTime[bloom.id]
            if (previous == null) {
                lastPulseGameTime[bloom.id] = gameTime - initialPhaseOf(bloom.visualSeed, interval)
                continue
            }
            if (gameTime - previous < interval) continue
            if (pulses.size >= MAX_ACTIVE_PULSES) break
            lastPulseGameTime[bloom.id] = gameTime
            pulses.addLast(
                BioluminescentBloomPulse(
                    geometry.centerX,
                    geometry.centerZ,
                    gameTime,
                    maxRadiusFor(geometry.centerX, geometry.centerZ, bounds)
                )
            )
            (emitted ?: mutableListOf<BioluminescentBloom>().also { emitted = it }).add(bloom)
        }
        return emitted ?: emptyList()
    }

    fun intensityAt(worldX: Double, worldZ: Double, renderGameTime: Double): Float {
        var intensity = 0.0
        for (pulse in pulses) {
            intensity = max(intensity, pulse.intensityAt(worldX, worldZ, renderGameTime))
            if (intensity >= 0.999) break
        }
        return intensity.toFloat().coerceIn(0.0f, 1.0f)
    }

    fun affects(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Boolean {
        return pulses.any { pulse -> pulse.affects(minX, minZ, maxX, maxZ, renderGameTime) }
    }

    fun maxIntensityIn(
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Float {
        var intensity = 0.0
        for (pulse in pulses) {
            intensity = max(intensity, pulse.maxIntensityIn(minX, minZ, maxX, maxZ, renderGameTime))
            if (intensity >= 0.999) break
        }
        return intensity.toFloat().coerceIn(0.0f, 1.0f)
    }

    fun clear() {
        pulses.clear()
        lastPulseGameTime.clear()
        visibilityStrength = 0.0
    }

    private fun initialPhaseOf(visualSeed: Long, interval: Long): Long {
        val mixed = RandomSupport.mixStafford13(visualSeed xor PULSE_PHASE_SALT)
        return (ModUtilities.stableUnitValue(mixed) * interval).toLong()
    }

    private fun maxRadiusFor(centerX: Double, centerZ: Double, bounds: BioluminescentWaterBounds): Double {
        val farDeltaX = max(abs(bounds.minX - centerX), abs(bounds.maxX + 1.0 - centerX))
        val farDeltaZ = max(abs(bounds.minZ - centerZ), abs(bounds.maxZ + 1.0 - centerZ))
        return sqrt(farDeltaX * farDeltaX + farDeltaZ * farDeltaZ)
    }
}
