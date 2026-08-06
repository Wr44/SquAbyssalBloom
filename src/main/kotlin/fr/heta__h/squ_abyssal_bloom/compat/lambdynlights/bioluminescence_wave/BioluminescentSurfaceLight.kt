package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.bioluminescence_wave

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.BlockPos
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class BioluminescentSurfaceLight private constructor(
    val chunkOriginX: Int,
    val chunkOriginZ: Int,
    private val boundingBox: DynamicLightBehavior.BoundingBox,
    private val candidateX: IntArray,
    private val candidateZ: IntArray,
    private val candidateY: DoubleArray,
    private val candidateLuminance: DoubleArray,
    private val bandNearestIndex: Array<IntArray>
) : DynamicLightBehavior {
    companion object {
        const val CHUNK_SIZE = 16
        private const val BAND_COUNT = 4
        private const val BOUNDING_BOX_MARGIN = 1
        private const val DOWNWARD_DISTANCE_MULTIPLIER = 0.6
        private const val LEVEL_QUANTIZATION_STEPS = 30.0
        private const val SMOOTHING_RATE = 6.0
        private const val SNAP_THRESHOLD = 0.002
        private val NEIGHBOR_OFFSET_X = intArrayOf(1, -1, 0, 0)
        private val NEIGHBOR_OFFSET_Z = intArrayOf(0, 0, 1, -1)

        fun build(
            chunkOriginX: Int,
            chunkOriginZ: Int,
            domain: BioluminescentWaterDomain,
            emissionField: BioluminescentEmissionField
        ): BioluminescentSurfaceLight? {
            val localCount = CHUNK_SIZE * CHUNK_SIZE
            val localCellIndex = IntArray(localCount) { -1 }
            val localLuminance = DoubleArray(localCount)
            val localSurfaceY = DoubleArray(localCount)

            var qualifiedCount = 0
            var minWorldX = Int.MAX_VALUE
            var maxWorldX = Int.MIN_VALUE
            var minWorldZ = Int.MAX_VALUE
            var maxWorldZ = Int.MIN_VALUE
            var minSurfaceY = Double.POSITIVE_INFINITY
            var maxSurfaceY = Double.NEGATIVE_INFINITY

            for (localZ in 0 until CHUNK_SIZE) {
                for (localX in 0 until CHUNK_SIZE) {
                    val worldX = chunkOriginX + localX
                    val worldZ = chunkOriginZ + localZ
                    val cellIndex = domain.cellIndexAt(worldX, worldZ) ?: continue
                    val luminance = BioluminescentLuminanceMap.columnLuminance(emissionField, cellIndex)
                    if (luminance <= 0.0) continue

                    val localIndex = localZ * CHUNK_SIZE + localX
                    localCellIndex[localIndex] = cellIndex
                    localLuminance[localIndex] = luminance
                    val surfaceY = domain.cells[cellIndex].surfaceY
                    localSurfaceY[localIndex] = surfaceY

                    qualifiedCount++
                    minWorldX = min(minWorldX, worldX)
                    maxWorldX = max(maxWorldX, worldX)
                    minWorldZ = min(minWorldZ, worldZ)
                    maxWorldZ = max(maxWorldZ, worldZ)
                    minSurfaceY = min(minSurfaceY, surfaceY)
                    maxSurfaceY = max(maxSurfaceY, surfaceY)
                }
            }
            if (qualifiedCount == 0) return null

            val candidateX = IntArray(qualifiedCount)
            val candidateZ = IntArray(qualifiedCount)
            val candidateY = DoubleArray(qualifiedCount)
            val candidateLuminance = DoubleArray(qualifiedCount)
            val candidateBand = IntArray(qualifiedCount)
            val localCandidateIndex = IntArray(localCount) { -1 }

            var candidateCursor = 0
            for (localIndex in 0 until localCount) {
                if (localCellIndex[localIndex] < 0) continue
                val luminance = localLuminance[localIndex]
                candidateX[candidateCursor] = chunkOriginX + localIndex % CHUNK_SIZE
                candidateZ[candidateCursor] = chunkOriginZ + localIndex / CHUNK_SIZE
                candidateY[candidateCursor] = localSurfaceY[localIndex]
                candidateLuminance[candidateCursor] = luminance
                candidateBand[candidateCursor] = bandFor(luminance)
                localCandidateIndex[localIndex] = candidateCursor
                candidateCursor++
            }

            val bandNearestIndex = Array(BAND_COUNT) { band ->
                nearestByBand(localCandidateIndex, candidateBand, band, localCount)
            }

            val boundingBox = DynamicLightBehavior.BoundingBox(
                minWorldX,
                floor(minSurfaceY).toInt() - BOUNDING_BOX_MARGIN,
                minWorldZ,
                maxWorldX + 1,
                ceil(maxSurfaceY).toInt() + BOUNDING_BOX_MARGIN,
                maxWorldZ + 1
            )

            return BioluminescentSurfaceLight(
                chunkOriginX,
                chunkOriginZ,
                boundingBox,
                candidateX,
                candidateZ,
                candidateY,
                candidateLuminance,
                bandNearestIndex
            )
        }

        private fun bandFor(luminance: Double): Int {
            val step = BioluminescentLuminanceMap.MAX_LUMINANCE / BAND_COUNT
            return (luminance / step).toInt().coerceIn(0, BAND_COUNT - 1)
        }

        private fun nearestByBand(
            localCandidateIndex: IntArray,
            candidateBand: IntArray,
            band: Int,
            localCount: Int
        ): IntArray {
            val nearest = IntArray(localCount) { -1 }
            val queue = ArrayDeque<Int>()
            for (localIndex in 0 until localCount) {
                val candidateIndex = localCandidateIndex[localIndex]
                if (candidateIndex >= 0 && candidateBand[candidateIndex] == band) {
                    nearest[localIndex] = candidateIndex
                    queue.addLast(localIndex)
                }
            }
            while (queue.isNotEmpty()) {
                val localIndex = queue.removeFirst()
                val localX = localIndex % CHUNK_SIZE
                val localZ = localIndex / CHUNK_SIZE
                val sourceCandidate = nearest[localIndex]
                for (direction in 0 until 4) {
                    val neighborX = localX + NEIGHBOR_OFFSET_X[direction]
                    val neighborZ = localZ + NEIGHBOR_OFFSET_Z[direction]
                    if (neighborX !in 0 until CHUNK_SIZE || neighborZ !in 0 until CHUNK_SIZE) continue
                    val neighborIndex = neighborZ * CHUNK_SIZE + neighborX
                    if (nearest[neighborIndex] >= 0) continue
                    nearest[neighborIndex] = sourceCandidate
                    queue.addLast(neighborIndex)
                }
            }
            return nearest
        }
    }

    private var intensityFactor = 0.0
    private var lastReportedLevel = -1
    private var dirty = true
    private var removed = false

    fun setIntensityFactor(target: Double, smoothingDelta: Double) {
        intensityFactor = if (smoothingDelta <= 0.0) {
            target
        } else {
            ModUtilities.smoothTowards(intensityFactor, target, smoothingDelta, SMOOTHING_RATE)
        }
        if (abs(intensityFactor - target) < SNAP_THRESHOLD) intensityFactor = target

        val level = (intensityFactor * LEVEL_QUANTIZATION_STEPS).roundToInt()
        if (level != lastReportedLevel) {
            lastReportedLevel = level
            dirty = true
        }
    }

    fun markRemoved() {
        removed = true
        dirty = true
    }

    override fun lightAtPos(pos: BlockPos, falloffRatio: Double): Double {
        if (intensityFactor <= 0.0) return 0.0

        val localX = (pos.x - chunkOriginX).coerceIn(0, CHUNK_SIZE - 1)
        val localZ = (pos.z - chunkOriginZ).coerceIn(0, CHUNK_SIZE - 1)
        val localIndex = localZ * CHUNK_SIZE + localX

        var best = 0.0
        for (band in bandNearestIndex.indices) {
            val candidateIndex = bandNearestIndex[band][localIndex]
            if (candidateIndex < 0) continue

            val deltaX = pos.x + 0.5 - candidateX[candidateIndex]
            val deltaZ = pos.z + 0.5 - candidateZ[candidateIndex]
            val rawDeltaY = candidateY[candidateIndex] - (pos.y + 0.5)
            val deltaY = if (rawDeltaY > 0.0) rawDeltaY * DOWNWARD_DISTANCE_MULTIPLIER else rawDeltaY
            val distance = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

            val value = candidateLuminance[candidateIndex] * intensityFactor - distance * falloffRatio
            if (value > best) best = value
        }
        return best.coerceIn(0.0, BioluminescentLuminanceMap.MAX_LUMINANCE)
    }

    override fun getBoundingBox(): DynamicLightBehavior.BoundingBox = boundingBox

    override fun hasChanged(): Boolean {
        if (!dirty) return false
        dirty = false
        return true
    }

    override fun isRemoved(): Boolean = removed

}
