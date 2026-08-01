package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterCell
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationResult
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationSnapshot
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationStage
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerator
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPalette
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

class BioluminescentZone(
    val zoneSeed: Long,
    val anchor: BlockPos,
    val palette: BioluminescentPalette,
    val preset: BioluminescentZonePreset,
    initialCell: BioluminescentWaterCell,
    val createdAt: Long,
    val lifetime: Long,
    val colorPhase: Double,
    val requestedByCommand: Boolean
) : AutoCloseable {
    private var generator: BioluminescentZoneGenerator? = BioluminescentZoneGenerator(
        anchor,
        initialCell,
        preset,
        zoneSeed,
        palette
    )
    private val brightnessScale = brightnessScaleFromSeed()
    private val flickers = createFlickerSchedule()

    var spatialData: BioluminescentZoneGenerationResult? = null
        private set

    var activatedAt: Long? = null
        private set

    var generationStage = BioluminescentZoneGenerationStage.COLLECT_WATER_DOMAIN
        private set

    var failureReason: String? = null
        private set

    var generationCpuNanos: Long = 0L
        private set

    val tiles: List<BioluminescentZoneTile>
        get() = spatialData?.tiles ?: emptyList()

    val isReady: Boolean
        get() = generationStage == BioluminescentZoneGenerationStage.READY

    val isFailed: Boolean
        get() = generationStage == BioluminescentZoneGenerationStage.FAILED

    val targetMacroCoverage: Double
        get() = generator?.targetMacroCoverage ?: spatialData?.macroField?.targetCoverage ?: 0.0

    val targetVisibleCoverage: Double
        get() = generator?.targetVisibleCoverage ?: spatialData?.emissionField?.targetVisibleCoverage ?: 0.0

    fun advanceGeneration(
        level: ClientLevel,
        textureManager: TextureManager,
        identifierFactory: () -> Identifier,
        maximumTileCount: Int,
        gameTime: Long
    ) {
        val activeGenerator = generator ?: return
        activeGenerator.advance(level, textureManager, identifierFactory, maximumTileCount)
        generationStage = activeGenerator.stage
        generationCpuNanos = activeGenerator.cpuNanos
        if (activeGenerator.stage == BioluminescentZoneGenerationStage.READY) {
            spatialData = checkNotNull(activeGenerator.takeCompletedResult())
            activatedAt = gameTime
            activeGenerator.close()
            generator = null
        } else if (activeGenerator.stage == BioluminescentZoneGenerationStage.FAILED) {
            failureReason = activeGenerator.failureReason ?: "echec de generation inconnu"
            activeGenerator.close()
            generator = null
        }
    }

    fun generationSnapshot(): BioluminescentZoneGenerationSnapshot {
        generator?.let { return it.snapshot() }
        val data = spatialData
        return BioluminescentZoneGenerationSnapshot(
            generationStage,
            data?.domain?.size ?: 0,
            data?.domain?.geodesicRadius ?: 0,
            data?.topology?.cores?.size ?: 0,
            data?.reactionDiffusion?.iterations ?: 0,
            preset.reactionDiffusionIterations,
            data?.tiles?.size ?: 0,
            data?.tiles?.count(BioluminescentZoneTile::uploaded) ?: 0,
            generationCpuNanos,
            failureReason
        )
    }

    fun temporalIntensityAt(gameTime: Long): Float {
        val start = activatedAt ?: return 0.0f
        val elapsed = (gameTime - start).coerceAtLeast(0L)
        val lifecycle = lifecycleIntensity(elapsed)
        if (lifecycle <= 0.0) return 0.0f
        val pulsePhase = colorPhase + elapsed * PI * 2.0 / PULSE_PERIOD_TICKS
        val pulse = 0.92 + 0.08 * sin(pulsePhase)
        return (lifecycle * pulse * flickerIntensity(elapsed) * brightnessScale)
            .toFloat().coerceIn(0.0f, MAXIMUM_RENDER_INTENSITY)
    }

    fun isCompleteAt(gameTime: Long): Boolean {
        val start = activatedAt ?: return false
        return gameTime - start >= lifetime
    }

    fun horizontalDistanceSquared(worldX: Double, worldZ: Double): Double {
        val deltaX = anchor.x + 0.5 - worldX
        val deltaZ = anchor.z + 0.5 - worldZ
        return deltaX * deltaX + deltaZ * deltaZ
    }

    override fun close() {
        generator?.close()
        generator = null
        spatialData?.tiles?.forEach(BioluminescentZoneTile::close)
        spatialData = null
        activatedAt = null
    }

    private fun lifecycleIntensity(elapsedTicks: Long): Double {
        if (elapsedTicks >= lifetime) return 0.0
        val progress = elapsedTicks.toDouble() / lifetime
        return when {
            progress < APPEARANCE_END -> ModUtilities.smooth(0.0, APPEARANCE_END, progress)
            progress < DISAPPEARANCE_START -> 1.0
            else -> 1.0 - ModUtilities.smooth(DISAPPEARANCE_START, 1.0, progress)
        }
    }

    private fun flickerIntensity(elapsedTicks: Long): Double {
        var intensity = 1.0
        for (flicker in flickers) {
            if (elapsedTicks < flicker.startOffset) break
            intensity = max(intensity, flicker.intensityAt(elapsedTicks))
        }
        return intensity
    }

    private fun brightnessScaleFromSeed(): Double {
        val mixed = RandomSupport.mixStafford13(zoneSeed xor BRIGHTNESS_SEED_SALT)
        return 0.97 + ((mixed ushr 40) and 0xFFFFL).toDouble() / 65535.0 * 0.06
    }

    private fun createFlickerSchedule(): List<FlickerEvent> {
        val random = XoroshiroRandomSource(RandomSupport.mixStafford13(zoneSeed xor FLICKER_SEED_SALT))
        val stableStart = (lifetime * STABLE_PHASE_START).toLong()
        val stableEnd = (lifetime * STABLE_PHASE_END).toLong()
        val result = ArrayList<FlickerEvent>()
        var start = stableStart + randomInRange(random, MIN_FLICKER_INTERVAL, MAX_FLICKER_INTERVAL)
        while (start + MIN_FLICKER_DURATION < stableEnd) {
            val duration = randomInRange(random, MIN_FLICKER_DURATION, MAX_FLICKER_DURATION).toInt()
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
            val eventEnd = start + max(duration, secondDelay + secondDuration)
            if (eventEnd < stableEnd) {
                result.add(FlickerEvent(start, duration, peak, secondDelay, secondDuration, secondPeak))
            }
            start = eventEnd + randomInRange(random, MIN_FLICKER_INTERVAL, MAX_FLICKER_INTERVAL)
        }
        return result
    }

    private fun randomInRange(random: XoroshiroRandomSource, minimum: Long, maximum: Long): Long {
        return minimum + random.nextInt((maximum - minimum + 1L).toInt())
    }

    private data class FlickerEvent(
        val startOffset: Long,
        val duration: Int,
        val peak: Double,
        val secondDelay: Int,
        val secondDuration: Int,
        val secondPeak: Double
    ) {
        fun intensityAt(elapsedTicks: Long): Double {
            val first = flashIntensity(elapsedTicks, startOffset, duration, peak)
            if (secondDuration == 0) return first
            return max(
                first,
                flashIntensity(elapsedTicks, startOffset + secondDelay, secondDuration, secondPeak)
            )
        }

        private fun flashIntensity(
            elapsedTicks: Long,
            start: Long,
            flashDuration: Int,
            flashPeak: Double
        ): Double {
            if (elapsedTicks < start || elapsedTicks > start + flashDuration) return 1.0
            val progress = (elapsedTicks - start).toDouble() / flashDuration
            val envelope = if (progress < FLICKER_RISE_END) {
                ModUtilities.smooth(0.0, FLICKER_RISE_END, progress)
            } else {
                1.0 - ModUtilities.smooth(FLICKER_RISE_END, 1.0, progress)
            }
            return 1.0 + (flashPeak - 1.0) * envelope
        }
    }

    private companion object {
        const val MIN_FLICKER_INTERVAL = 180L
        const val MAX_FLICKER_INTERVAL = 240L
        const val MIN_FLICKER_DURATION = 32L
        const val MAX_FLICKER_DURATION = 52L
        const val MIN_FLICKER_PEAK = 1.08
        const val MAX_FLICKER_PEAK = 1.16
        const val DOUBLE_FLICKER_CHANCE = 0.04
        const val STABLE_PHASE_START = 0.20
        const val STABLE_PHASE_END = 0.80
        const val FLICKER_RISE_END = 0.42
        const val PULSE_PERIOD_TICKS = 200.0
        const val APPEARANCE_END = 0.20
        const val DISAPPEARANCE_START = 0.80
        const val MAXIMUM_RENDER_INTENSITY = 1.15f
        const val BRIGHTNESS_SEED_SALT = 0x6A09E667F3BCC909L
        const val FLICKER_SEED_SALT = 0x3B67AE8584CAA73BL
    }
}
