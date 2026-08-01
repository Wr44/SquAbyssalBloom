package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise.BioluminescentNoiseSampler
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePreset
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.floor

class BioluminescentReactionDiffusion(
    private val macroField: BioluminescentMacroField,
    private val preset: BioluminescentZonePreset,
    private val zoneSeed: Long
) {
    val scale = preset.reactionDiffusionScale
    val width = macroField.domain.bounds.width * scale
    val length = macroField.domain.bounds.length * scale
    val feed: Double
    val kill: Double
    val targetIterations = preset.reactionDiffusionIterations

    private val mask = BooleanArray(width * length)
    private val seedNoiseSampler = BioluminescentNoiseSampler(zoneSeed xor INITIALIZATION_SALT)
    private var u = FloatArray(width * length) { 1.0f }
    private var v = FloatArray(width * length)
    private var nextU = FloatArray(width * length) { 1.0f }
    private var nextV = FloatArray(width * length)
    private val activeIndices: IntArray
    private val neighborIndices: IntArray

    var completedIterations: Int = 0
        private set

    val complete: Boolean
        get() = completedIterations >= targetIterations

    companion object {
        const val DIFFUSION_U = 0.16
        const val DIFFUSION_V = 0.08
        const val FEED_MINIMUM = 0.028
        const val FEED_MAXIMUM = 0.040
        const val KILL_MINIMUM = 0.058
        const val KILL_MAXIMUM = 0.066
        const val CARDINAL_WEIGHT = 0.20
        const val DIAGONAL_WEIGHT = 0.05
        const val REACTION_MASK_MINIMUM = 0.025
        const val SEED_LARGE_NOISE_SCALE = 0.78
        const val SEED_DETAIL_NOISE_SCALE = 0.62
        const val SEED_LARGE_WEIGHT = 0.56
        const val SEED_DETAIL_WEIGHT = 0.32
        const val CORE_SEED_BIAS = 0.08
        const val CONNECTION_SEED_BIAS = 0.07
        const val SUPPORT_SEED_BIAS = 0.03
        const val SEED_FIELD_LOW = 0.47
        const val SEED_FIELD_HIGH = 0.65
        const val MINIMUM_SEED_AMOUNT = 0.002
        const val PATTERN_LOW = 0.065
        const val PATTERN_HIGH = 0.64
        const val NEIGHBOR_COUNT = 8
        const val PARAMETER_SALT = 0x5BE0CD19137E2179L
        const val INITIALIZATION_SALT = 0x243F6A8885A308D3L
    }

    init {
        val parameterSeed = RandomSupport.mixStafford13(zoneSeed xor PARAMETER_SALT)
        feed = FEED_MINIMUM + stableUnitValue(parameterSeed) * (FEED_MAXIMUM - FEED_MINIMUM)
        kill = KILL_MINIMUM + stableUnitValue(RandomSupport.mixStafford13(parameterSeed)) *
            (KILL_MAXIMUM - KILL_MINIMUM)
        initialize()
        activeIndices = createActiveIndices()
        neighborIndices = createNeighborIndices()
    }

    fun advance(iterationBudget: Int) {
        val iterations = minOf(iterationBudget, targetIterations - completedIterations)
        repeat(iterations) {
            iterate()
            completedIterations++
        }
    }

    fun patternAt(worldX: Double, worldZ: Double): Double {
        val gridX = (worldX - macroField.domain.bounds.minimumX) * scale - 0.5
        val gridZ = (worldZ - macroField.domain.bounds.minimumZ) * scale - 0.5
        val x0 = floor(gridX).toInt()
        val z0 = floor(gridZ).toInt()
        val fractionX = gridX - x0
        val fractionZ = gridZ - z0
        val first = mix(samplePattern(x0, z0), samplePattern(x0 + 1, z0), fractionX)
        val second = mix(samplePattern(x0, z0 + 1), samplePattern(x0 + 1, z0 + 1), fractionX)
        return mix(first, second, fractionZ)
    }

    private fun initialize() {
        val macroSample = BioluminescentMacroSample()
        for (gridZ in 0 until length) {
            for (gridX in 0 until width) {
                val worldX = macroField.domain.bounds.minimumX + (gridX + 0.5) / scale
                val worldZ = macroField.domain.bounds.minimumZ + (gridZ + 0.5) / scale
                val cellIndex = macroField.domain.cellIndexAt(floor(worldX).toInt(), floor(worldZ).toInt())
                    ?: continue
                macroField.sampleAt(worldX, worldZ, cellIndex, macroSample)
                val support = macroSample.support
                if (support <= REACTION_MASK_MINIMUM) continue
                val index = gridZ * width + gridX
                mask[index] = true
                val core = macroSample.core
                val connection = macroSample.connection
                val seedLarge = seedNoiseSampler.sampleLarge(
                    (worldX + 181.0) * SEED_LARGE_NOISE_SCALE,
                    (worldZ - 223.0) * SEED_LARGE_NOISE_SCALE
                )
                val seedDetail = seedNoiseSampler.sampleDetail(
                    (worldX - 79.0) * SEED_DETAIL_NOISE_SCALE,
                    (worldZ + 131.0) * SEED_DETAIL_NOISE_SCALE
                )
                val structuralBias = ModUtilities.smooth(0.03, 0.78, core) * CORE_SEED_BIAS +
                    ModUtilities.smooth(0.08, 0.82, connection) * CONNECTION_SEED_BIAS +
                    support * SUPPORT_SEED_BIAS
                val seedField = seedLarge * SEED_LARGE_WEIGHT +
                    seedDetail * SEED_DETAIL_WEIGHT + structuralBias
                val seedAmount = ModUtilities.smooth(SEED_FIELD_LOW, SEED_FIELD_HIGH, seedField)
                if (seedAmount <= MINIMUM_SEED_AMOUNT) continue
                val concentration = (
                    core * 0.40 + connection * 0.22 + seedLarge * 0.22 + seedDetail * 0.16
                    ).coerceIn(0.0, 1.0)
                v[index] = (0.17 + seedAmount * 0.34 + concentration * 0.10)
                    .coerceIn(0.0, 0.68).toFloat()
                u[index] = (1.0f - v[index] * 0.48f).coerceIn(0.0f, 1.0f)
            }
        }
    }

    private fun iterate() {
        for (activeOffset in activeIndices.indices) {
            val index = activeIndices[activeOffset]
            val currentU = u[index].toDouble()
            val currentV = v[index].toDouble()
            val neighborOffset = activeOffset * NEIGHBOR_COUNT
            val laplacianU = laplacian(u, neighborOffset, currentU)
            val laplacianV = laplacian(v, neighborOffset, currentV)
            val reaction = currentU * currentV * currentV
            nextU[index] = (
                currentU + DIFFUSION_U * laplacianU - reaction + feed * (1.0 - currentU)
                ).coerceIn(0.0, 1.0).toFloat()
            nextV[index] = (
                currentV + DIFFUSION_V * laplacianV + reaction - (feed + kill) * currentV
                ).coerceIn(0.0, 1.0).toFloat()
        }
        val previousU = u
        u = nextU
        nextU = previousU
        val previousV = v
        v = nextV
        nextV = previousV
    }

    private fun laplacian(values: FloatArray, neighborOffset: Int, center: Double): Double {
        var result = -center
        result += values[neighborIndices[neighborOffset]].toDouble() * CARDINAL_WEIGHT
        result += values[neighborIndices[neighborOffset + 1]].toDouble() * CARDINAL_WEIGHT
        result += values[neighborIndices[neighborOffset + 2]].toDouble() * CARDINAL_WEIGHT
        result += values[neighborIndices[neighborOffset + 3]].toDouble() * CARDINAL_WEIGHT
        result += values[neighborIndices[neighborOffset + 4]].toDouble() * DIAGONAL_WEIGHT
        result += values[neighborIndices[neighborOffset + 5]].toDouble() * DIAGONAL_WEIGHT
        result += values[neighborIndices[neighborOffset + 6]].toDouble() * DIAGONAL_WEIGHT
        result += values[neighborIndices[neighborOffset + 7]].toDouble() * DIAGONAL_WEIGHT
        return result
    }

    private fun createNeighborIndices(): IntArray {
        val result = IntArray(activeIndices.size * NEIGHBOR_COUNT)
        for (activeOffset in activeIndices.indices) {
            val index = activeIndices[activeOffset]
            val x = index % width
            val z = index / width
            val offset = activeOffset * NEIGHBOR_COUNT
            result[offset] = neighborIndex(x - 1, z, index)
            result[offset + 1] = neighborIndex(x + 1, z, index)
            result[offset + 2] = neighborIndex(x, z - 1, index)
            result[offset + 3] = neighborIndex(x, z + 1, index)
            result[offset + 4] = neighborIndex(x - 1, z - 1, index)
            result[offset + 5] = neighborIndex(x + 1, z - 1, index)
            result[offset + 6] = neighborIndex(x - 1, z + 1, index)
            result[offset + 7] = neighborIndex(x + 1, z + 1, index)
        }
        return result
    }

    private fun createActiveIndices(): IntArray {
        var count = 0
        for (active in mask) if (active) count++
        val result = IntArray(count)
        var cursor = 0
        for (index in mask.indices) if (mask[index]) result[cursor++] = index
        return result
    }

    private fun neighborIndex(x: Int, z: Int, fallback: Int): Int {
        if (x !in 0 until width || z !in 0 until length) return fallback
        val index = z * width + x
        return if (mask[index]) index else fallback
    }

    private fun samplePattern(x: Int, z: Int): Double {
        if (x !in 0 until width || z !in 0 until length) return 0.0
        val index = z * width + x
        if (!mask[index]) return 0.0
        val value = v[index] * 1.46f + (1.0f - u[index]) * 0.30f
        return ModUtilities.smooth(PATTERN_LOW, PATTERN_HIGH, value.toDouble())
    }

    private fun mix(first: Double, second: Double, amount: Double): Double {
        return first + (second - first) * amount.coerceIn(0.0, 1.0)
    }

    private fun stableUnitValue(value: Long): Double {
        return ((value ushr 40) and 0xFFFFFFL).toDouble() / 0xFFFFFFL.toDouble()
    }
}
