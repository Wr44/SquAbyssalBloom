package fr.heta__h.squ_abyssal_bloom.util.bioluminescence

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

class BioluminescentBloomZone(
    val zoneSeed: Long,
    val anchor: BlockPos,
    val palette: BioluminescentPalette,
    val createdAt: Long,
    val lifetime: Long,
    val colorPhase: Double,
    val requestedSize: BioluminescentBloomSize,
    val targetBloomCount: Int,
    val coastDirectionX: Double,
    val coastDirectionZ: Double
) : AutoCloseable {
    val blooms = mutableListOf<BioluminescentBloom>()
    val brightnessScale: Float
    internal val analyzedWaterCells = HashSet<Long>()
    internal val coveredWaterCells = HashSet<Long>()

    internal var nextBloomIndex = 1
    internal var placementAttempt = 0
    internal var nextPlacementTick = createdAt + INITIAL_PLACEMENT_DELAY_TICKS
    internal var generationComplete = targetBloomCount <= 1
    internal var connectedComponentCount = if (targetBloomCount > 0) 1 else 0

    private val flickers: List<FlickerEvent>

    init {
        require(lifetime > 0L)
        require(targetBloomCount > 0)
        val mixedSeed = RandomSupport.mixStafford13(zoneSeed xor BRIGHTNESS_SEED_SALT)
        brightnessScale = (0.97 + ((mixedSeed ushr 40) and 0xFFFFL).toDouble() / 65535.0 * 0.06).toFloat()
        flickers = createFlickerSchedule()
    }

    fun pulseIntensityAt(gameTime: Long): Float {
        val elapsed = if (gameTime <= createdAt) 0.0 else (gameTime - createdAt).toDouble()
        val phase = colorPhase + elapsed * PI * 2.0 / PULSE_PERIOD_TICKS
        return (0.92 + 0.08 * sin(phase)).toFloat()
    }

    fun flickerIntensityAt(gameTime: Long): Float {
        var intensity = 1.0
        for (flicker in flickers) {
            if (gameTime < flicker.startTick) break
            intensity = max(intensity, flicker.intensityAt(gameTime))
        }
        return intensity.toFloat()
    }

    fun coverageRatio(): Double {
        if (analyzedWaterCells.isEmpty()) return 0.0
        return coveredWaterCells.size.toDouble() / analyzedWaterCells.size.toDouble()
    }

    override fun close() {
        blooms.forEach(BioluminescentBloom::close)
        blooms.clear()
        analyzedWaterCells.clear()
        coveredWaterCells.clear()
        generationComplete = true
    }

    private fun createFlickerSchedule(): List<FlickerEvent> {
        val random = XoroshiroRandomSource(
            RandomSupport.mixStafford13(zoneSeed xor FLICKER_SEED_SALT)
        )
        val stableStart = createdAt + (lifetime.toDouble() * STABLE_PHASE_START).toLong()
        val stableEnd = createdAt + (lifetime.toDouble() * STABLE_PHASE_END).toLong()
        val result = ArrayList<FlickerEvent>()
        var startTick = stableStart + randomInRange(random, MIN_FLICKER_INTERVAL_TICKS, MAX_FLICKER_INTERVAL_TICKS)

        while (startTick + MIN_FLICKER_DURATION_TICKS < stableEnd) {
            val duration = randomInRange(random, MIN_FLICKER_DURATION_TICKS, MAX_FLICKER_DURATION_TICKS).toInt()
            val peak = MIN_FLICKER_PEAK + random.nextDouble() * (MAX_FLICKER_PEAK - MIN_FLICKER_PEAK)
            val doublePeak = random.nextDouble() < DOUBLE_FLICKER_CHANCE
            val secondDelay = if (doublePeak) duration + randomInRange(random, 8L, 12L).toInt() else 0
            val secondDuration = if (doublePeak) {
                max(18, (duration * (0.58 + random.nextDouble() * 0.14)).toInt())
            } else {
                0
            }
            val secondPeak = if (doublePeak) {
                1.0 + (peak - 1.0) * (0.72 + random.nextDouble() * 0.18)
            } else {
                1.0
            }

            val eventEnd = startTick + max(duration, secondDelay + secondDuration)
            if (eventEnd < stableEnd) {
                result.add(
                    FlickerEvent(
                        startTick = startTick,
                        duration = duration,
                        peak = peak,
                        secondDelay = secondDelay,
                        secondDuration = secondDuration,
                        secondPeak = secondPeak
                    )
                )
            }
            startTick = eventEnd + randomInRange(random, MIN_FLICKER_INTERVAL_TICKS, MAX_FLICKER_INTERVAL_TICKS)
        }

        return result
    }

    private fun randomInRange(random: XoroshiroRandomSource, minimum: Long, maximum: Long): Long {
        return minimum + random.nextInt((maximum - minimum + 1L).toInt())
    }

    private data class FlickerEvent(
        val startTick: Long,
        val duration: Int,
        val peak: Double,
        val secondDelay: Int,
        val secondDuration: Int,
        val secondPeak: Double
    ) {
        fun intensityAt(gameTime: Long): Double {
            val first = flashIntensity(gameTime, startTick, duration, peak)
            if (secondDuration == 0) return first
            val second = flashIntensity(
                gameTime,
                startTick + secondDelay,
                secondDuration,
                secondPeak
            )
            return max(first, second)
        }

        private fun flashIntensity(
            gameTime: Long,
            flashStart: Long,
            flashDuration: Int,
            flashPeak: Double
        ): Double {
            if (gameTime < flashStart || gameTime > flashStart + flashDuration) return 1.0
            val progress = (gameTime - flashStart).toDouble() / flashDuration.toDouble()
            val envelope = if (progress < FLICKER_RISE_END) {
                ModUtilities.smooth(0.0, FLICKER_RISE_END, progress)
            } else {
                1.0 - ModUtilities.smooth(FLICKER_RISE_END, 1.0, progress)
            }
            return 1.0 + (flashPeak - 1.0) * envelope
        }
    }

    private companion object {
        const val INITIAL_PLACEMENT_DELAY_TICKS = 5L
        const val MIN_FLICKER_INTERVAL_TICKS = 180L
        const val MAX_FLICKER_INTERVAL_TICKS = 240L
        const val MIN_FLICKER_DURATION_TICKS = 32L
        const val MAX_FLICKER_DURATION_TICKS = 52L
        const val MIN_FLICKER_PEAK = 1.08
        const val MAX_FLICKER_PEAK = 1.16
        const val DOUBLE_FLICKER_CHANCE = 0.04
        const val STABLE_PHASE_START = 0.20
        const val STABLE_PHASE_END = 0.80
        const val FLICKER_RISE_END = 0.42
        const val PULSE_PERIOD_TICKS = 200.0
        const val BRIGHTNESS_SEED_SALT = 0x6A09E667F3BCC909L
        const val FLICKER_SEED_SALT = 0x3B67AE8584CAA73BL
    }
}
