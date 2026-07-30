package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.core.BlockPos
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
        const val SAMPLE_START_HEIGHT_MARGIN = 0.15
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
        const val NORMAL_RESPONSE = 0.08
        const val MAX_NORMAL_ROTATION_DEGREES_PER_TICK = 0.6
        const val MIN_NORMAL_ROTATION_DEGREES = 0.08
        const val MIN_VECTOR_LENGTH_SQR = 1.0E-8

        const val NATURAL_STEP_RISE_PER_TICK = 0.2
        const val MAX_STEP_CATCH_UP_PER_TICK = 0.12
        const val MAX_BANKED_STEP_OFFSET = 2.0
        const val TELEPORT_RISE_THRESHOLD = 3.0

        const val MAX_CLIMB_BLOCKS = 2
        const val MAX_CLIMB_HEIGHT = 2.05
        const val CLIMB_RISE_PER_TICK = 0.1
        const val CLIMB_PROBE_FORWARD_MARGIN = 0.05
        const val CLIMB_PROBE_HEIGHT_FRACTION = 0.2
        const val CLIMB_EXIT_EPSILON = 0.02
        const val CLIMB_NORMAL_RESPONSE = 0.3
        const val CLIMB_NORMAL_ROTATION_DEGREES_PER_TICK = 15.0

        val UP: Vec3 = Vec3(0.0, 1.0, 0.0)
        val MAX_TILT_GRADIENT: Double = tan(MAX_TERRAIN_TILT_DEGREES * PI / 180.0)

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

    data class GroundSample(
        val x: Double,
        val z: Double,
        val height: Double
    )

    data class TerrainPlane(
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

    private var lastY: Double? = null
    private var previousStepOffset: Double = 0.0
    private var currentStepOffset: Double = 0.0

    private enum class ClimbState {
        NONE,
        CLIMBING
    }

    private var climbState = ClimbState.NONE
    private var climbTargetTopY = 0.0
    private var climbWallNormal: Vec3 = UP

    var hasGroundContact: Boolean = false
        private set

    private var hasStableSupport: Boolean = false

    fun tick() {
        previousNormal = currentNormal
        previousStepOffset = currentStepOffset
        updateStepOffsetTracking()

        val bodyBottom = redSlobberer.boundingBox.minY
        if (climbState == ClimbState.CLIMBING && bodyBottom >= climbTargetTopY - CLIMB_EXIT_EPSILON) {
            climbState = ClimbState.NONE
        }

        val samples = sampleGroundAcrossBody()
        hasGroundContact = samples.any { bodyBottom - it.height <= MAX_GROUND_CONTACT_GAP }
        val supportingSamples = findConnectedSupportingSamples(samples, bodyBottom)
        hasStableSupport = supportingSamples.size >= MIN_GROUND_SAMPLE_COUNT &&
            hasSupportAcrossBody(supportingSamples)

        if (
            climbState != ClimbState.CLIMBING &&
            !hasStableSupport &&
            redSlobberer.isInLocomotionPropulsionPhase
        ) {
            tryStartClimb(bodyBottom)
        }

        val targetNormal = if (climbState == ClimbState.CLIMBING) {
            climbWallNormal
        } else {
            val rawTerrainNormal = if (hasGroundContact) {
                calculateTerrainNormal(samples, bodyBottom)
            } else {
                null
            }
            rawTerrainNormal?.let { normal ->
                attenuateTiltForSupport(normal, calculateSupportCoverage(supportingSamples))
            } ?: UP
        }
        currentNormal = if (climbState == ClimbState.CLIMBING || !hasGroundContact) {
            smoothNormal(
                currentNormal,
                targetNormal,
                CLIMB_NORMAL_ROTATION_DEGREES_PER_TICK,
                CLIMB_NORMAL_RESPONSE
            )
        } else {
            smoothNormal(currentNormal, targetNormal, MAX_NORMAL_ROTATION_DEGREES_PER_TICK, NORMAL_RESPONSE)
        }
    }

    fun getInterpolatedNormal(partialTick: Float): Vec3 {
        val alpha = partialTick.toDouble().coerceIn(0.0, 1.0)
        val interpolated = previousNormal.scale(1.0 - alpha).add(currentNormal.scale(alpha))
        return if (interpolated.lengthSqr() > MIN_VECTOR_LENGTH_SQR) interpolated.normalize() else UP
    }

    fun getStepRenderOffset(partialTick: Float): Double {
        val alpha = partialTick.toDouble().coerceIn(0.0, 1.0)
        return previousStepOffset * (1.0 - alpha) + currentStepOffset * alpha
    }

    fun createTangentMovement(input: Vec3, speed: Float, yawDegrees: Float): Vec3? {
        if (climbState == ClimbState.CLIMBING) return Vec3(0.0, CLIMB_RISE_PER_TICK, 0.0)
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

    private fun updateStepOffsetTracking() {
        val y = redSlobberer.y
        val previousY = lastY
        lastY = y
        if (previousY == null) return

        val rise = y - previousY
        if (rise > NATURAL_STEP_RISE_PER_TICK) {
            val bankedRise = rise - NATURAL_STEP_RISE_PER_TICK
            currentStepOffset = if (bankedRise > TELEPORT_RISE_THRESHOLD) {
                0.0
            } else {
                (currentStepOffset + bankedRise).coerceAtMost(MAX_BANKED_STEP_OFFSET)
            }
        }

        if (currentStepOffset > 0.0) {
            currentStepOffset = (currentStepOffset - MAX_STEP_CATCH_UP_PER_TICK).coerceAtLeast(0.0)
        }
    }

    private fun tryStartClimb(bodyBottom: Double) {
        val level = redSlobberer.level()
        val yawRadians = redSlobberer.yRot * PI / 180.0
        val dirX = -sin(yawRadians)
        val dirZ = cos(yawRadians)

        val probeDistance = redSlobberer.bbWidth / 2.0 + CLIMB_PROBE_FORWARD_MARGIN
        val probeX = redSlobberer.x + dirX * probeDistance
        val probeZ = redSlobberer.z + dirZ * probeDistance
        val baseWallPos = BlockPos.containing(
            probeX,
            bodyBottom + redSlobberer.bbHeight * CLIMB_PROBE_HEIGHT_FRACTION,
            probeZ
        )

        if (level.getBlockState(baseWallPos).getCollisionShape(level, baseWallPos).isEmpty) return

        var topSolidY = baseWallPos.y
        val maxSolidY = baseWallPos.y + MAX_CLIMB_BLOCKS
        while (topSolidY < maxSolidY) {
            val nextPos = BlockPos(baseWallPos.x, topSolidY + 1, baseWallPos.z)
            if (level.getBlockState(nextPos).getCollisionShape(level, nextPos).isEmpty) break
            topSolidY++
        }
        if (topSolidY >= maxSolidY) return

        val ledgeTopY = topSolidY + 1.0
        val rise = ledgeTopY - bodyBottom
        // Anything vanilla can already step over silently (maxUpStep) is left to it —
        // the explicit climb only takes over for ledges taller than that.
        if (rise <= redSlobberer.maximumStepHeight || rise > MAX_CLIMB_HEIGHT) return

        climbTargetTopY = ledgeTopY
        climbWallNormal = Vec3(-dirX, 0.0, -dirZ)
        climbState = ClimbState.CLIMBING
    }

    private fun sampleGroundAcrossBody(): List<GroundSample> {
        val radius = redSlobberer.bbWidth * SAMPLE_RADIUS_FACTOR
        val bodyBottom = redSlobberer.boundingBox.minY
        val samples = ArrayList<GroundSample>(SAMPLE_DIRECTIONS.size)

        for (direction in SAMPLE_DIRECTIONS) {
            val offsetX = direction[0] * radius
            val offsetZ = direction[1] * radius
            val sampleX = redSlobberer.x + offsetX
            val sampleZ = redSlobberer.z + offsetZ
            val horizontalOffset = sqrt(offsetX * offsetX + offsetZ * offsetZ)
            val height = sampleGroundHeight(sampleX, sampleZ, bodyBottom, horizontalOffset) ?: continue
            samples.add(GroundSample(sampleX, sampleZ, height))
        }

        return samples
    }

    private fun sampleGroundHeight(x: Double, z: Double, bodyBottom: Double, horizontalOffset: Double): Double? {
        val maxHeightAboveBodyBottom = MAX_SURFACE_HEIGHT_ABOVE_BODY_BOTTOM +
            horizontalOffset * MAX_TILT_GRADIENT
        val from = Vec3(x, bodyBottom + maxHeightAboveBodyBottom + SAMPLE_START_HEIGHT_MARGIN, z)
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
        return if (surfaceHeight <= bodyBottom + maxHeightAboveBodyBottom) {
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
        if (gradientLength > MAX_TILT_GRADIENT) {
            val scale = MAX_TILT_GRADIENT / gradientLength
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
                val distanceSqr = dx * dx + dz * dz
                val isNeighbor = distanceSqr <= maximumNeighborDistanceSqr
                val maxContinuousHeight = MAX_SUPPORT_STEP_HEIGHT + sqrt(distanceSqr) * MAX_TILT_GRADIENT
                val hasContinuousHeight = abs(candidate.height - supportingSample.height) <=
                    maxContinuousHeight
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
        val centerX = redSlobberer.x
        val centerZ = redSlobberer.z
        val weights = DoubleArray(samples.size)
        var weightSum = 0.0
        var weightedX = 0.0
        var weightedZ = 0.0
        var weightedHeight = 0.0

        for (index in samples.indices) {
            val sample = samples[index]
            val dx = sample.x - centerX
            val dz = sample.z - centerZ
            val weight = 1.0 / (1.0 + dx * dx + dz * dz)
            weights[index] = weight
            weightSum += weight
            weightedX += weight * sample.x
            weightedZ += weight * sample.z
            weightedHeight += weight * sample.height
        }
        if (weightSum < MIN_PLANE_DETERMINANT) return null

        val meanX = weightedX / weightSum
        val meanZ = weightedZ / weightSum
        val meanHeight = weightedHeight / weightSum

        var covarianceXX = 0.0
        var covarianceXZ = 0.0
        var covarianceZZ = 0.0
        var covarianceXHeight = 0.0
        var covarianceZHeight = 0.0

        for (index in samples.indices) {
            val sample = samples[index]
            val weight = weights[index]
            val centeredX = sample.x - meanX
            val centeredZ = sample.z - meanZ
            val centeredHeight = sample.height - meanHeight
            covarianceXX += weight * centeredX * centeredX
            covarianceXZ += weight * centeredX * centeredZ
            covarianceZZ += weight * centeredZ * centeredZ
            covarianceXHeight += weight * centeredX * centeredHeight
            covarianceZHeight += weight * centeredZ * centeredHeight
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

    private fun smoothNormal(
        from: Vec3,
        target: Vec3,
        maxRotationDegreesPerTick: Double,
        response: Double
    ): Vec3 {
        val angle = acos(from.dot(target).coerceIn(-1.0, 1.0))
        val minimumAngle = MIN_NORMAL_ROTATION_DEGREES * PI / 180.0
        if (angle <= minimumAngle) return from

        val maximumAngle = maxRotationDegreesPerTick * PI / 180.0
        val blend = min(response, maximumAngle / angle).coerceIn(0.0, 1.0)
        val blended = from.scale(1.0 - blend).add(target.scale(blend))
        return if (blended.lengthSqr() > MIN_VECTOR_LENGTH_SQR) blended.normalize() else UP
    }
}
