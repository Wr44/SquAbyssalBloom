package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise.BioluminescentCellularNoise
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise.BioluminescentNoiseSampler
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPalette
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePreset
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

class BioluminescentEmissionGenerator(
    private val macroField: BioluminescentMacroField,
    private val reactionDiffusion: BioluminescentReactionDiffusion,
    private val preset: BioluminescentZonePreset,
    private val palette: BioluminescentPalette,
    private val zoneSeed: Long,
    val targetVisibleCoverage: Double
) {
    enum class Stage {
        SAMPLE_PATTERNS,
        CALIBRATE_POROSITY,
        FINALIZE_EMISSION,
        COMPLETE
    }

    companion object {
        const val PIXELS_PER_CELL =
            BioluminescentEmissionField.PIXELS_PER_BLOCK * BioluminescentEmissionField.PIXELS_PER_BLOCK
        const val MACRO_SUPPORT_MINIMUM = 0.035
        const val CONCENTRATION_NOISE_SCALE = 0.30
        const val LARGE_NOISE_SCALE = 0.88
        const val MEDIUM_NOISE_SCALE = 1.72
        const val MICRO_CLUSTER_NOISE_SCALE = 2.45
        const val DETAIL_NOISE_SCALE = 2.35
        const val FINE_NOISE_SCALE = 3.35
        const val HOLE_DETAIL_NOISE_SCALE = 2.85
        const val REACTION_WEIGHT = 0.30
        const val FRACTAL_WEIGHT = 0.47
        const val CELLULAR_WEIGHT = 0.11
        const val MICRO_RIDGE_WEIGHT = 0.12
        const val CELLULAR_PRIMARY_SIZE = 2.65
        const val CELLULAR_SECONDARY_SIZE = 4.35
        const val CELLULAR_WARP_AMPLITUDE = 1.25
        const val REACTION_WARP_FACTOR = 0.55
        const val SECONDARY_WARP_FACTOR = 0.68
        const val HOLE_START_MAXIMUM = 0.75
        const val HOLE_START_POROSITY_INFLUENCE = 0.22
        const val HOLE_TRANSITION = 0.15
        const val POROUS_FEATHER = 0.065
        const val CORE_POROSITY_BIAS = 0.09
        const val CONNECTION_VARIATION = 0.20
        const val CONNECTION_PATTERN_FLOOR = 0.10
        const val CONNECTION_PATTERN_LOW = 0.32
        const val CONNECTION_PATTERN_HIGH = 0.74
        const val CONNECTION_HOLE_FLOOR = 0.14
        const val CONNECTION_SPINE_EXPONENT = 1.55
        const val VISIBLE_EMISSION_THRESHOLD = 0.032
        const val HIGHLIGHT_MEASUREMENT_THRESHOLD = 0.72
        const val MINIMUM_STORED_EMISSION = 0.006
        const val ALPHA_EXPOSURE = 2.55
        const val COLOR_EXPOSURE = 1.75
        const val ALPHA_CURVE = 0.62
        const val CALIBRATION_STEPS = 8
        const val COVERAGE_TOLERANCE = 0.035
        const val COLOR_NOISE_SCALE = 0.34
        const val COLOR_SECONDARY_SCALE = 0.16
        const val COLOR_ACCENT_SCALE = 0.09
        const val COLOR_ACCENT_MIX = 0.72
        const val HIGHLIGHT_START = 0.62
        const val HIGHLIGHT_END = 0.84
        const val HIGHLIGHT_MIX_MAXIMUM = 0.72
        const val CELLULAR_SEED_SALT = 0x1F83D9ABFB41BD6BL
        const val CELLULAR_SECONDARY_SEED_SALT = 0x254FF53A5F1D36F1L
        const val COLOR_SEED_SALT = 0x510E527FADE682D1L
        const val POROSITY_SEED_SALT = 0x5BE0CD19137E2179L
    }

    private val pixelCount = macroField.domain.size * PIXELS_PER_CELL
    private val macroSupport = FloatArray(pixelCount)
    private val organicPattern = FloatArray(pixelCount)
    private val holeAvailability = FloatArray(pixelCount)
    private val connectionSupport = FloatArray(pixelCount)
    private val coreSupport = FloatArray(pixelCount)
    private val concentration = FloatArray(pixelCount)
    private val finalAlpha = FloatArray(pixelCount)
    private val finalColors = IntArray(pixelCount)
    private val luminousCells = BooleanArray(macroField.domain.size)
    private val noiseSampler = BioluminescentNoiseSampler(zoneSeed, zoneSeed xor COLOR_SEED_SALT)
    private val cellularNoise = BioluminescentCellularNoise(
        zoneSeed xor CELLULAR_SEED_SALT,
        CELLULAR_PRIMARY_SIZE
    )
    private val secondaryCellularNoise = BioluminescentCellularNoise(
        zoneSeed xor CELLULAR_SECONDARY_SEED_SALT,
        CELLULAR_SECONDARY_SIZE
    )
    private val porosity = choosePorosity()

    private var samplingCursor = 0
    private var finalizationCursor = 0
    private var calibrationStep = 0
    private var calibrationCursor = 0
    private var calibrationVisible = 0
    private var calibrationCandidate = 0.5
    private var calibrationLow = 0.0
    private var calibrationHigh = 1.0
    private var supportPixels = 0
    private var luminousPixels = 0
    private var visibleAlphaSum = 0.0
    private var highlightPixels = 0

    var stage: Stage = Stage.SAMPLE_PATTERNS
        private set

    val porousThreshold: Double
        get() = (calibrationLow + calibrationHigh) * 0.5

    fun advance(sampleBudget: Int, calibrationBudget: Int, finalizationBudget: Int) {
        when (stage) {
            Stage.SAMPLE_PATTERNS -> samplePatterns(sampleBudget)
            Stage.CALIBRATE_POROSITY -> calibratePorosity(calibrationBudget)
            Stage.FINALIZE_EMISSION -> finalizeEmission(finalizationBudget)
            Stage.COMPLETE -> Unit
        }
    }

    fun build(): BioluminescentEmissionField {
        check(stage == Stage.COMPLETE)
        val achieved = if (supportPixels == 0) 0.0 else luminousPixels.toDouble() / supportPixels
        check(achieved in (preset.visiblePixelCoverageRange.start - COVERAGE_TOLERANCE)..
            (preset.visiblePixelCoverageRange.endInclusive + COVERAGE_TOLERANCE)
        ) {
            "couverture visible hors preset: ${(achieved * 100.0).roundToInt()}%"
        }
        return BioluminescentEmissionField(
            macroField.domain,
            macroField,
            targetVisibleCoverage,
            achieved,
            porousThreshold,
            supportPixels,
            luminousPixels,
            if (luminousPixels == 0) 0.0 else visibleAlphaSum / luminousPixels,
            highlightPixels,
            finalAlpha,
            finalColors,
            luminousCells
        )
    }

    private fun samplePatterns(budget: Int) {
        val end = minOf(pixelCount, samplingCursor + budget)
        for (pixelIndex in samplingCursor until end) {
            val sample = worldSample(pixelIndex)
            val macro = macroField.supportAt(sample.worldX, sample.worldZ, sample.cellIndex)
            macroSupport[pixelIndex] = macro.toFloat()
            if (macro < MACRO_SUPPORT_MINIMUM) continue
            supportPixels++
            val concentrationNoise = noiseSampler.sampleLarge(
                (sample.worldX - 137.0) * CONCENTRATION_NOISE_SCALE,
                (sample.worldZ + 193.0) * CONCENTRATION_NOISE_SCALE
            )
            val largeNoise = noiseSampler.sampleLarge(
                sample.worldX * LARGE_NOISE_SCALE,
                sample.worldZ * LARGE_NOISE_SCALE
            ) * 0.62 + noiseSampler.sampleLarge(
                (sample.worldX + 47.0) * MEDIUM_NOISE_SCALE,
                (sample.worldZ - 83.0) * MEDIUM_NOISE_SCALE
            ) * 0.38
            val mediumNoise = noiseSampler.sampleLarge(
                (sample.worldX - 241.0) * MICRO_CLUSTER_NOISE_SCALE,
                (sample.worldZ + 157.0) * MICRO_CLUSTER_NOISE_SCALE
            )
            val detailNoise = noiseSampler.sampleDetail(
                sample.worldX * DETAIL_NOISE_SCALE,
                sample.worldZ * DETAIL_NOISE_SCALE
            ) * 0.64 + noiseSampler.sampleDetail(
                (sample.worldX + 73.0) * FINE_NOISE_SCALE,
                (sample.worldZ - 109.0) * FINE_NOISE_SCALE
            ) * 0.36
            val holeDetail = noiseSampler.sampleDetail(
                (sample.worldX - 313.0) * HOLE_DETAIL_NOISE_SCALE,
                (sample.worldZ + 269.0) * HOLE_DETAIL_NOISE_SCALE
            )
            val fractalNoise = concentrationNoise * 0.24 + largeNoise * 0.28 +
                mediumNoise * 0.25 + detailNoise * 0.23
            val warpX = (concentrationNoise * 2.0 - 1.0) * CELLULAR_WARP_AMPLITUDE
            val warpZ = (largeNoise * 2.0 - 1.0) * CELLULAR_WARP_AMPLITUDE
            val reaction = reactionDiffusion.patternAt(
                sample.worldX + warpX * REACTION_WARP_FACTOR,
                sample.worldZ + warpZ * REACTION_WARP_FACTOR
            )
            val cellular = cellularNoise.sample(sample.worldX + warpX, sample.worldZ + warpZ)
            val secondaryCellular = secondaryCellularNoise.sample(
                sample.worldX - warpZ * SECONDARY_WARP_FACTOR,
                sample.worldZ + warpX * SECONDARY_WARP_FACTOR
            )
            val cellularPattern = (
                cellular.nearest * 0.62 + secondaryCellular.nearest * 0.38
                ) * (0.68 + detailNoise * 0.32)
            val microRidge = 1.0 - abs(detailNoise * 2.0 - 1.0)
            val reactionPattern = reaction * (0.48 + largeNoise * 0.28 + detailNoise * 0.24)
            val organic = reactionPattern * REACTION_WEIGHT +
                fractalNoise * FRACTAL_WEIGHT +
                cellularPattern * CELLULAR_WEIGHT +
                microRidge * MICRO_RIDGE_WEIGHT
            organicPattern[pixelIndex] = organic.toFloat()

            val holeNoise = mediumNoise * 0.30 + detailNoise * 0.43 + holeDetail * 0.27
            val holeStart = HOLE_START_MAXIMUM - porosity * HOLE_START_POROSITY_INFLUENCE
            val hole = 1.0 - ModUtilities.smooth(holeStart, holeStart + HOLE_TRANSITION, holeNoise)
            holeAvailability[pixelIndex] = hole.toFloat()

            val core = macroField.coreInfluenceAt(sample.worldX, sample.worldZ)
            coreSupport[pixelIndex] = core.toFloat()
            val connection = macroField.connectionInfluenceAt(sample.worldX, sample.worldZ, sample.cellIndex)
            val broadConcentration = ModUtilities.smooth(
                0.18,
                0.76,
                concentrationNoise * 0.62 + largeNoise * 0.38
            )
            val sparkle = ModUtilities.smooth(
                0.66,
                0.90,
                detailNoise * 0.78 + secondaryCellular.nearest * 0.22
            )
            concentration[pixelIndex] = (
                0.18 + broadConcentration * 0.66 + reaction * 0.16 +
                    sparkle * 0.38 + core * 0.22
                ).coerceIn(0.0, 1.36).toFloat()
            connectionSupport[pixelIndex] = (macro * connection.pow(CONNECTION_SPINE_EXPONENT)).toFloat()
        }
        samplingCursor = end
        if (samplingCursor >= pixelCount) {
            check(supportPixels > 0) { "support macroscopique sans pixel" }
            stage = Stage.CALIBRATE_POROSITY
        }
    }

    private fun calibratePorosity(budget: Int) {
        if (calibrationCursor == 0) {
            calibrationCandidate = (calibrationLow + calibrationHigh) * 0.5
            calibrationVisible = 0
        }
        val end = minOf(pixelCount, calibrationCursor + budget)
        for (index in calibrationCursor until end) {
            if (macroSupport[index] < MACRO_SUPPORT_MINIMUM) continue
            if (emissionAt(index, calibrationCandidate) >= VISIBLE_EMISSION_THRESHOLD) {
                calibrationVisible++
            }
        }
        calibrationCursor = end
        if (calibrationCursor < pixelCount) return

        val coverage = calibrationVisible.toDouble() / supportPixels
        if (coverage > targetVisibleCoverage) {
            calibrationLow = calibrationCandidate
        } else {
            calibrationHigh = calibrationCandidate
        }
        calibrationStep++
        calibrationCursor = 0
        if (calibrationStep >= CALIBRATION_STEPS) stage = Stage.FINALIZE_EMISSION
    }

    private fun finalizeEmission(budget: Int) {
        val end = minOf(pixelCount, finalizationCursor + budget)
        for (index in finalizationCursor until end) {
            val emission = emissionAt(index, porousThreshold)
            if (emission < MINIMUM_STORED_EMISSION) continue
            val alphaEmission = exposedEmission(emission, ALPHA_EXPOSURE)
            val colorEmission = exposedEmission(emission, COLOR_EXPOSURE)
            val alpha = alphaEmission.pow(ALPHA_CURVE).coerceIn(0.0, 1.0)
            finalAlpha[index] = alpha.toFloat()
            val sample = worldSample(index)
            finalColors[index] = colorAt(sample.worldX, sample.worldZ, colorEmission)
            luminousCells[sample.cellIndex] = true
            if (emission >= VISIBLE_EMISSION_THRESHOLD) {
                luminousPixels++
                visibleAlphaSum += alpha
                if (colorEmission >= HIGHLIGHT_MEASUREMENT_THRESHOLD) highlightPixels++
            }
        }
        finalizationCursor = end
        if (finalizationCursor >= pixelCount) stage = Stage.COMPLETE
    }

    private fun emissionAt(index: Int, threshold: Double): Double {
        val macro = macroSupport[index].toDouble()
        if (macro < MACRO_SUPPORT_MINIMUM) return 0.0
        val organic = organicPattern[index].toDouble()
        val holes = holeAvailability[index].toDouble()
        val localThreshold = threshold - coreSupport[index] * CORE_POROSITY_BIAS
        val porousMask = ModUtilities.smooth(
            localThreshold - POROUS_FEATHER,
            localThreshold + POROUS_FEATHER,
            organic * holes
        )
        val baseEmission = macro * porousMask * holes * concentration[index].toDouble()
        val connectionPattern = CONNECTION_PATTERN_FLOOR + ModUtilities.smooth(
            CONNECTION_PATTERN_LOW,
            CONNECTION_PATTERN_HIGH,
            organic
        ) * (1.0 - CONNECTION_PATTERN_FLOOR)
        val connectionEmission = connectionSupport[index].toDouble() *
            (preset.minimumConnectionEmission + organic * CONNECTION_VARIATION) *
            connectionPattern *
            (CONNECTION_HOLE_FLOOR + holes * (1.0 - CONNECTION_HOLE_FLOOR))
        return maxOf(baseEmission, connectionEmission).coerceIn(0.0, 1.0)
    }

    private fun exposedEmission(emission: Double, exposure: Double): Double {
        return (1.0 - exp(-emission * exposure)).coerceIn(0.0, 1.0)
    }

    private fun colorAt(worldX: Double, worldZ: Double, emission: Double): Int {
        val primaryNoise = noiseSampler.sampleColor(
            worldX * COLOR_NOISE_SCALE,
            worldZ * COLOR_NOISE_SCALE
        )
        val secondaryNoise = noiseSampler.sampleColor(
            (worldX + 211.0) * COLOR_SECONDARY_SCALE,
            (worldZ - 179.0) * COLOR_SECONDARY_SCALE
        )
        val accentNoise = noiseSampler.sampleColor(
            (worldX - 347.0) * COLOR_ACCENT_SCALE,
            (worldZ + 281.0) * COLOR_ACCENT_SCALE
        )
        val mix = ModUtilities.smooth(0.28, 0.72, primaryNoise * 0.68 + secondaryNoise * 0.32)
        val base = lerpColor(palette.firstColor, palette.secondColor, mix)
        val accentMix = ModUtilities.smooth(0.48, 0.82, accentNoise) * COLOR_ACCENT_MIX
        val accented = lerpColor(base, palette.accentColor, accentMix)
        val chromaticStrength = 0.18 + ModUtilities.smooth(0.015, 0.66, emission) * 0.82
        val shaded = lerpColor(palette.shadowColor, accented, chromaticStrength)
        val highlight = ModUtilities.smooth(HIGHLIGHT_START, HIGHLIGHT_END, emission) *
            HIGHLIGHT_MIX_MAXIMUM
        return lerpColor(shaded, palette.highlightColor, highlight)
    }

    private fun worldSample(pixelIndex: Int): PixelSample {
        val cellIndex = pixelIndex / PIXELS_PER_CELL
        val cellPixel = pixelIndex % PIXELS_PER_CELL
        val localX = cellPixel % BioluminescentEmissionField.PIXELS_PER_BLOCK
        val localZ = cellPixel / BioluminescentEmissionField.PIXELS_PER_BLOCK
        val position = macroField.domain.cells[cellIndex].waterPos
        return PixelSample(
            cellIndex,
            position.x + (localX + 0.5) / BioluminescentEmissionField.PIXELS_PER_BLOCK,
            position.z + (localZ + 0.5) / BioluminescentEmissionField.PIXELS_PER_BLOCK
        )
    }

    private fun choosePorosity(): Double {
        val value = stableUnitValue(RandomSupport.mixStafford13(zoneSeed xor POROSITY_SEED_SALT))
        return preset.porosityRange.start +
            value * (preset.porosityRange.endInclusive - preset.porosityRange.start)
    }

    private fun lerpColor(first: Int, second: Int, amount: Double): Int {
        val red = lerpChannel(first shr 16 and 0xFF, second shr 16 and 0xFF, amount)
        val green = lerpChannel(first shr 8 and 0xFF, second shr 8 and 0xFF, amount)
        val blue = lerpChannel(first and 0xFF, second and 0xFF, amount)
        return (red shl 16) or (green shl 8) or blue
    }

    private fun lerpChannel(first: Int, second: Int, amount: Double): Int {
        return (first + (second - first) * amount.coerceIn(0.0, 1.0))
            .roundToInt().coerceIn(0, 255)
    }

    private fun stableUnitValue(value: Long): Double {
        return ((value ushr 40) and 0xFFFFFFL).toDouble() / 0xFFFFFFL.toDouble()
    }

    private data class PixelSample(val cellIndex: Int, val worldX: Double, val worldZ: Double)
}
