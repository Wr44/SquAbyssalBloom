package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class RedSlobbererTerrainAlignment(
    private val redSlobberer: RedSlobbererEntity
) {

    private companion object {
        const val SAMPLE_RADIUS_FACTOR = 0.42
        const val SAMPLE_START_HEIGHT = 0.15
        const val SAMPLE_DEPTH_BELOW_BODY = 2.5
        const val MAX_SURFACE_HEIGHT_ABOVE_BODY_BOTTOM = 0.025
        const val MAX_GROUND_CONTACT_GAP = 0.3
        const val MAX_SUPPORT_STEP_HEIGHT = 0.6
        const val MAX_SUPPORT_NEIGHBOR_DISTANCE_FACTOR = 1.05
        const val MIN_SUPPORT_SPAN_FACTOR = 1.2
        const val MIN_SPARSE_SUPPORT_TILT_FACTOR = 0.3
        const val MIN_GROUND_SAMPLE_COUNT = 4
        const val MIN_PLANE_DETERMINANT = 1.0E-6
        const val MAX_PLANE_RESIDUAL = 0.7
        const val MAX_TERRAIN_TILT_DEGREES = 45.0
        const val NORMAL_RESPONSE = 0.16
        const val MAX_NORMAL_ROTATION_DEGREES_PER_TICK = 1.25
        const val MIN_NORMAL_ROTATION_DEGREES = 0.08
        const val MIN_VECTOR_LENGTH_SQR = 1.0E-8

        val UP: Vec3 = Vec3(0.0, 1.0, 0.0)

        val SAMPLE_DIRECTIONS: Array<DoubleArray> = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(-1.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(0.0, -1.0),
            doubleArrayOf(0.7071067811865476, 0.7071067811865476),
            doubleArrayOf(0.7071067811865476, -0.7071067811865476),
            doubleArrayOf(-0.7071067811865476, 0.7071067811865476),
            doubleArrayOf(-0.7071067811865476, -0.7071067811865476)
        )
    }

    private data class GroundSample(
        val x: Double,
        val z: Double,
        val height: Double
    )

    private data class TerrainPlane(
        val gradientX: Double,
        val gradientZ: Double,
        val intercept: Double
    ) {
        fun heightAt(x: Double, z: Double): Double {
            return gradientX * x + gradientZ * z + intercept
        }
    }

    private var previousNormal: Vec3 = UP
    private var currentNormal: Vec3 = UP

    var hasGroundContact: Boolean = false
        private set

    private var hasStableSupport: Boolean = false

    fun tick() {
        previousNormal = currentNormal

        val samples = sampleGroundAcrossBody()
        val bodyBottom = redSlobberer.boundingBox.minY
        hasGroundContact = samples.any { bodyBottom - it.height <= MAX_GROUND_CONTACT_GAP }
        val supportingSamples = findConnectedSupportingSamples(samples, bodyBottom)
        hasStableSupport = supportingSamples.size >= MIN_GROUND_SAMPLE_COUNT &&
            hasSupportAcrossBody(supportingSamples)

        val rawTerrainNormal = if (hasGroundContact) {
            calculateTerrainNormal(samples, bodyBottom)
        } else {
            null
        }
        val targetNormal = rawTerrainNormal?.let { normal ->
            attenuateTiltForSupport(normal, calculateSupportCoverage(supportingSamples))
        }
        currentNormal = smoothNormal(currentNormal, targetNormal ?: UP)
    }

    fun getInterpolatedNormal(partialTick: Float): Vec3 {
        val alpha = partialTick.toDouble().coerceIn(0.0, 1.0)
        val interpolated = previousNormal.scale(1.0 - alpha).add(currentNormal.scale(alpha))
        return if (interpolated.lengthSqr() > MIN_VECTOR_LENGTH_SQR) interpolated.normalize() else UP
    }

    fun createTangentMovement(input: Vec3, speed: Float, yawDegrees: Float): Vec3? {
        if (!hasStableSupport) return null

        val inputLengthSqr = input.lengthSqr()
        if (inputLengthSqr < MIN_VECTOR_LENGTH_SQR) return Vec3.ZERO

        val yawRadians = yawDegrees * PI / 180.0
        val horizontalForward = Vec3(-sin(yawRadians), 0.0, cos(yawRadians))
        val tangentForwardUnnormalized = horizontalForward.subtract(
            currentNormal.scale(horizontalForward.dot(currentNormal))
        )
        if (tangentForwardUnnormalized.lengthSqr() < MIN_VECTOR_LENGTH_SQR) return null

        val tangentForward = tangentForwardUnnormalized.normalize()
        val tangentSide = currentNormal.cross(tangentForward).normalize()
        val inputScale = if (inputLengthSqr > 1.0) 1.0 / sqrt(inputLengthSqr) else 1.0

        return tangentSide.scale(input.x)
            .add(currentNormal.scale(input.y))
            .add(tangentForward.scale(input.z))
            .scale(speed * inputScale)
    }

    private fun sampleGroundAcrossBody(): List<GroundSample> {
        val radius = redSlobberer.bbWidth * SAMPLE_RADIUS_FACTOR
        val bodyBottom = redSlobberer.boundingBox.minY
        val samples = ArrayList<GroundSample>(SAMPLE_DIRECTIONS.size)

        for (direction in SAMPLE_DIRECTIONS) {
            val sampleX = redSlobberer.x + direction[0] * radius
            val sampleZ = redSlobberer.z + direction[1] * radius
            val height = sampleGroundHeight(sampleX, sampleZ, bodyBottom) ?: continue
            samples.add(GroundSample(sampleX, sampleZ, height))
        }

        return samples
    }

    private fun sampleGroundHeight(x: Double, z: Double, bodyBottom: Double): Double? {
        val from = Vec3(x, bodyBottom + SAMPLE_START_HEIGHT, z)
        val to = Vec3(x, bodyBottom - SAMPLE_DEPTH_BELOW_BODY, z)
        val hitResult = redSlobberer.level().clip(
            ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                redSlobberer
            )
        )
        if (hitResult.type != HitResult.Type.BLOCK) return null

        val surfaceHeight = hitResult.location.y
        return if (surfaceHeight <= bodyBottom + MAX_SURFACE_HEIGHT_ABOVE_BODY_BOTTOM) {
            surfaceHeight
        } else {
            null
        }
    }

    private fun calculateTerrainNormal(samples: List<GroundSample>, bodyBottom: Double): Vec3? {
        if (samples.size < MIN_GROUND_SAMPLE_COUNT) return null

        var plane = fitPlane(samples) ?: return null
        val filteredSamples = samples.filter { sample ->
            val isActualContact = bodyBottom - sample.height <= MAX_GROUND_CONTACT_GAP
            isActualContact || abs(sample.height - plane.heightAt(sample.x, sample.z)) <= MAX_PLANE_RESIDUAL
        }
        if (
            filteredSamples.size >= MIN_GROUND_SAMPLE_COUNT &&
            filteredSamples.size < samples.size
        ) {
            plane = fitPlane(filteredSamples) ?: plane
        }

        var gradientX = plane.gradientX
        var gradientZ = plane.gradientZ
        val gradientLength = sqrt(gradientX * gradientX + gradientZ * gradientZ)
        val maximumGradient = tan(MAX_TERRAIN_TILT_DEGREES * PI / 180.0)
        if (gradientLength > maximumGradient) {
            val scale = maximumGradient / gradientLength
            gradientX *= scale
            gradientZ *= scale
        }

        return Vec3(-gradientX, 1.0, -gradientZ).normalize()
    }

    private fun attenuateTiltForSupport(normal: Vec3, supportCoverage: Double): Vec3 {
        val coverageResponse = supportCoverage * supportCoverage
        val tiltFactor = MIN_SPARSE_SUPPORT_TILT_FACTOR +
            (1.0 - MIN_SPARSE_SUPPORT_TILT_FACTOR) * coverageResponse
        return UP.scale(1.0 - tiltFactor).add(normal.scale(tiltFactor)).normalize()
    }

    private fun findConnectedSupportingSamples(
        samples: List<GroundSample>,
        bodyBottom: Double
    ): List<GroundSample> {
        val connected = BooleanArray(samples.size)
        val pending = ArrayDeque<Int>()

        for (index in samples.indices) {
            if (bodyBottom - samples[index].height <= MAX_GROUND_CONTACT_GAP) {
                connected[index] = true
                pending.addLast(index)
            }
        }

        val maximumNeighborDistance = redSlobberer.bbWidth * SAMPLE_RADIUS_FACTOR *
            MAX_SUPPORT_NEIGHBOR_DISTANCE_FACTOR
        val maximumNeighborDistanceSqr = maximumNeighborDistance * maximumNeighborDistance

        while (pending.isNotEmpty()) {
            val supportingSample = samples[pending.removeFirst()]
            for (candidateIndex in samples.indices) {
                if (connected[candidateIndex]) continue

                val candidate = samples[candidateIndex]
                val dx = candidate.x - supportingSample.x
                val dz = candidate.z - supportingSample.z
                val isNeighbor = dx * dx + dz * dz <= maximumNeighborDistanceSqr
                val hasContinuousHeight = abs(candidate.height - supportingSample.height) <=
                    MAX_SUPPORT_STEP_HEIGHT
                if (isNeighbor && hasContinuousHeight) {
                    connected[candidateIndex] = true
                    pending.addLast(candidateIndex)
                }
            }
        }

        return samples.filterIndexed { index, _ -> connected[index] }
    }

    private fun hasSupportAcrossBody(samples: List<GroundSample>): Boolean {
        val requiredSpan = redSlobberer.bbWidth * SAMPLE_RADIUS_FACTOR * MIN_SUPPORT_SPAN_FACTOR
        val spanX = samples.maxOf { it.x } - samples.minOf { it.x }
        val spanZ = samples.maxOf { it.z } - samples.minOf { it.z }
        return spanX >= requiredSpan && spanZ >= requiredSpan
    }

    private fun calculateSupportCoverage(samples: List<GroundSample>): Double {
        if (samples.isEmpty()) return 0.0

        val sampledDiameter = redSlobberer.bbWidth * SAMPLE_RADIUS_FACTOR * 2.0
        val coverageX = ((samples.maxOf { it.x } - samples.minOf { it.x }) / sampledDiameter)
            .coerceIn(0.0, 1.0)
        val coverageZ = ((samples.maxOf { it.z } - samples.minOf { it.z }) / sampledDiameter)
            .coerceIn(0.0, 1.0)
        return min(coverageX, coverageZ)
    }

    private fun fitPlane(samples: List<GroundSample>): TerrainPlane? {
        val meanX = samples.sumOf { it.x } / samples.size
        val meanZ = samples.sumOf { it.z } / samples.size
        val meanHeight = samples.sumOf { it.height } / samples.size

        var covarianceXX = 0.0
        var covarianceXZ = 0.0
        var covarianceZZ = 0.0
        var covarianceXHeight = 0.0
        var covarianceZHeight = 0.0

        for (sample in samples) {
            val centeredX = sample.x - meanX
            val centeredZ = sample.z - meanZ
            val centeredHeight = sample.height - meanHeight
            covarianceXX += centeredX * centeredX
            covarianceXZ += centeredX * centeredZ
            covarianceZZ += centeredZ * centeredZ
            covarianceXHeight += centeredX * centeredHeight
            covarianceZHeight += centeredZ * centeredHeight
        }

        val determinant = covarianceXX * covarianceZZ - covarianceXZ * covarianceXZ
        if (abs(determinant) < MIN_PLANE_DETERMINANT) return null

        val gradientX = (
            covarianceXHeight * covarianceZZ - covarianceZHeight * covarianceXZ
            ) / determinant
        val gradientZ = (
            covarianceZHeight * covarianceXX - covarianceXHeight * covarianceXZ
            ) / determinant
        val intercept = meanHeight - gradientX * meanX - gradientZ * meanZ
        return TerrainPlane(gradientX, gradientZ, intercept)
    }

    private fun smoothNormal(from: Vec3, target: Vec3): Vec3 {
        val angle = acos(from.dot(target).coerceIn(-1.0, 1.0))
        val minimumAngle = MIN_NORMAL_ROTATION_DEGREES * PI / 180.0
        if (angle <= minimumAngle) return from

        val maximumAngle = MAX_NORMAL_ROTATION_DEGREES_PER_TICK * PI / 180.0
        val blend = min(NORMAL_RESPONSE, maximumAngle / angle).coerceIn(0.0, 1.0)
        val blended = from.scale(1.0 - blend).add(target.scale(blend))
        return if (blended.lengthSqr() > MIN_VECTOR_LENGTH_SQR) blended.normalize() else UP
    }
}
