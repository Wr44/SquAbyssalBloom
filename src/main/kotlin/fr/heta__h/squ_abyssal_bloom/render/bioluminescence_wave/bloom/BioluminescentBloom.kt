package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.bloom

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationResult
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import net.minecraft.core.BlockPos
import net.minecraft.world.level.levelgen.RandomSupport
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class BioluminescentBloom(
    val id: UUID,
    val position: BlockPos,
    val visualSeed: Long,
    val maxHarvests: Int,
    var remainingHarvests: Int,
    var lifecycle: PlanktonBloomLifecycle,
    var activatedAtGameTime: Long?
) {
    companion object {
        private const val HALO_RADIUS = 2.6
        private const val LOBE_ORBIT_RADIUS = 1.6
        private const val LOBE_RADIUS = 1.1
        private const val LOBE_ANGLE_JITTER_SALT = 0x2B0B0FC001884045L
        private const val PULSE_PHASE_SALT = 0x0FC19DC68B8CD5B5L
        private const val SATELLITE_SALT = 0x3C6EF372FE94F82BL
        private const val SATELLITE_FIELD_RADIUS = 3.4
        private const val SATELLITE_MIN_COUNT = 16
        private const val SATELLITE_COUNT_SPREAD = 9
        private const val SATELLITE_MIN_RADIUS = 0.25
        private const val SATELLITE_RADIUS_SPREAD = 0.65
        private const val SATELLITE_MIN_ALPHA = 0.15f
        private const val SATELLITE_ALPHA_SPREAD = 0.3f
        private const val SATELLITE_CENTER_BIAS = 0.7

        const val PULSE_PERIOD_TICKS = 150.0
        const val MIN_PULSE_INTENSITY = 0.55
        const val MAX_PULSE_INTENSITY = 1.0
        const val ACTIVATION_FADE_TICKS = 40.0
        const val TERMINAL_FADE_TICKS = 30.0

        private fun findNearestCellIndex(domain: BioluminescentWaterDomain, worldX: Int, worldZ: Int): Int? {
            for (radius in 1..8) {
                for (offsetX in -radius..radius) {
                    for (offsetZ in -radius..radius) {
                        if (max(abs(offsetX), abs(offsetZ)) != radius) continue
                        domain.cellIndexAt(worldX + offsetX, worldZ + offsetZ)?.let { return it }
                    }
                }
            }
            return null
        }

        private fun lobeAngle(visualSeed: Long, index: Int, count: Int): Double {
            val mixed = RandomSupport.mixStafford13(visualSeed xor LOBE_ANGLE_JITTER_SALT xor index.toLong())
            val jitter = (ModUtilities.stableUnitValue(mixed) - 0.5) * (PI / 6.0)
            return (2.0 * PI * index / count) + jitter
        }

        private fun satelliteUnit(visualSeed: Long, index: Int, channel: Int): Double {
            val mixed = RandomSupport.mixStafford13(visualSeed xor SATELLITE_SALT xor (index * 11L + channel))
            return ModUtilities.stableUnitValue(mixed)
        }

        private fun buildSatellites(visualSeed: Long): List<BioluminescentBloomSatellite> {
            val count = SATELLITE_MIN_COUNT + (satelliteUnit(visualSeed, -1, 0) * SATELLITE_COUNT_SPREAD).toInt()
            return List(count) { index ->
                val angle = satelliteUnit(visualSeed, index, 1) * PI * 2.0
                val distance = satelliteUnit(visualSeed, index, 2).pow(SATELLITE_CENTER_BIAS) * SATELLITE_FIELD_RADIUS
                BioluminescentBloomSatellite(
                    offsetX = cos(angle) * distance,
                    offsetZ = sin(angle) * distance,
                    radius = SATELLITE_MIN_RADIUS + satelliteUnit(visualSeed, index, 3) * SATELLITE_RADIUS_SPREAD,
                    colorMix = satelliteUnit(visualSeed, index, 4),
                    phaseOffset = satelliteUnit(visualSeed, index, 5) * PI * 2.0,
                    alphaScale = (SATELLITE_MIN_ALPHA + satelliteUnit(visualSeed, index, 6) * SATELLITE_ALPHA_SPREAD).toFloat()
                )
            }
        }
    }

    var geometry: BioluminescentBloomGeometry? = null
        private set

    var terminalAtGameTime: Long? = null

    private var lastRenderTop: Boolean? = null

    fun resolveRenderTop(cameraY: Double, surfaceY: Double, hysteresis: Double): Boolean {
        val previous = lastRenderTop
        val resolved = when {
            previous == null -> cameraY >= surfaceY
            previous && cameraY < surfaceY - hysteresis -> false
            !previous && cameraY > surfaceY + hysteresis -> true
            else -> previous
        }
        lastRenderTop = resolved
        return resolved
    }

    val depletionFactor: Float
        get() = remainingHarvests.toFloat() / maxHarvests.toFloat()

    private var geometryResolutionFailed = false

    private val colorPhase: Double by lazy {
        ModUtilities.stableUnitValue(RandomSupport.mixStafford13(visualSeed xor PULSE_PHASE_SALT)) * PI * 2.0
    }

    fun pulseIntensityAt(renderGameTime: Double, serverGameTimeOffset: Long): Float {
        val activatedAt = activatedAtGameTime ?: return 0.0f
        val elapsed = (renderGameTime + serverGameTimeOffset - activatedAt).coerceAtLeast(0.0)
        val phase = colorPhase + elapsed * PI * 2.0 / PULSE_PERIOD_TICKS
        val wave = 0.5 + 0.5 * sin(phase)
        val eased = ModUtilities.smooth(0.0, 1.0, wave)
        return (MIN_PULSE_INTENSITY + (MAX_PULSE_INTENSITY - MIN_PULSE_INTENSITY) * eased).toFloat()
    }

    fun appearanceFadeAt(renderGameTime: Double, serverGameTimeOffset: Long): Float {
        val activatedAt = activatedAtGameTime ?: return 0.0f
        return ModUtilities.smooth(0.0, ACTIVATION_FADE_TICKS, renderGameTime + serverGameTimeOffset - activatedAt).toFloat()
    }

    fun terminalFadeAt(renderGameTime: Double): Float {
        val terminalAt = terminalAtGameTime ?: return 1.0f
        return (1.0 - ModUtilities.smooth(0.0, TERMINAL_FADE_TICKS, renderGameTime - terminalAt)).toFloat()
    }

    fun terminalFadeComplete(renderGameTime: Double): Boolean {
        val terminalAt = terminalAtGameTime ?: return false
        return renderGameTime - terminalAt >= TERMINAL_FADE_TICKS
    }

    fun ensureGeometry(data: BioluminescentZoneGenerationResult?) {
        if (geometry != null || geometryResolutionFailed || data == null) return
        val cellIndex = data.domain.cellIndexAt(position.x, position.z)
            ?: findNearestCellIndex(data.domain, position.x, position.z)
        if (cellIndex == null) {
            geometryResolutionFailed = true
            return
        }

        val cell = data.domain.cells[cellIndex]
        val lobeAngles = DoubleArray(maxHarvests) { index -> lobeAngle(visualSeed, index, maxHarvests) }

        geometry = BioluminescentBloomGeometry(
            centerX = position.x + 0.5,
            centerZ = position.z + 0.5,
            surfaceY = cell.surfaceY,
            waterDepth = cell.waterDepth,
            haloRadius = HALO_RADIUS,
            satellites = buildSatellites(visualSeed),
            lobeAngles = lobeAngles,
            lobeOrbitRadius = LOBE_ORBIT_RADIUS,
            lobeRadius = LOBE_RADIUS
        )
    }
}
