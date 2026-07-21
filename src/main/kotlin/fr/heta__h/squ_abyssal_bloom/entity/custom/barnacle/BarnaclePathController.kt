package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.ai.navigation.PathRecalculationPolicy
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.hasCollisionFreeAquaticCorridor
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation
import net.minecraft.world.phys.Vec3

class BarnaclePathController(private val barnacle: BarnacleEntity) {
    private val refreshPolicy = PathRecalculationPolicy(PATH_RECALCULATION_TICKS, TARGET_MOVE_THRESHOLD_SQR)
    private var pathing = false
    private var trackedTargetId = -1
    private var nextDirectCorridorCheckTick = Int.MIN_VALUE
    private var appliedDetectionRange = Float.NaN

    fun moveToward(target: LivingEntity, speed: Double, fallbackDirection: Vec3) {
        updateNavigationRangeIfNeeded()
        if (trackedTargetId != target.id) {
            trackedTargetId = target.id
            invalidatePath()
        }

        val destination = target.position().add(0.0, 1.0, 0.0)
        updatePath(destination) { barnacle.navigation.moveTo(target, NAVIGATION_SPEED) }
        requestVelocity(speed, fallbackDirection)
    }

    fun moveAway(direction: Vec3, speed: Double) {
        trackedTargetId = -1
        val destination = barnacle.position().add(direction.scale(FLEE_PATH_DISTANCE))
        updatePath(destination) {
            barnacle.navigation.moveTo(destination.x, destination.y, destination.z, 1, NAVIGATION_SPEED)
        }
        requestVelocity(speed, direction)
    }

    fun moveInDirection(direction: Vec3, speed: Double) {
        trackedTargetId = -1
        val destination = barnacle.position().add(direction.scale(IDLE_PATH_DISTANCE))
        updatePath(destination) {
            barnacle.navigation.moveTo(destination.x, destination.y, destination.z, 1, NAVIGATION_SPEED)
        }
        requestVelocity(speed, direction)
    }

    fun pause() {
        moveControl.cancelRequest()
    }

    fun stop() {
        pathing = false
        trackedTargetId = -1
        nextDirectCorridorCheckTick = Int.MIN_VALUE
        refreshPolicy.invalidate()
        barnacle.navigation.stop()
        moveControl.cancelRequest()
    }

    private fun updatePath(destination: Vec3, startPath: () -> Boolean) {
        if (pathing && barnacle.navigation.isDone) pathing = false

        val destinationPos = BlockPos.containing(destination)
        val destinationLoaded = barnacle.level().isLoaded(destinationPos)
        val destinationDistanceSqr = barnacle.position().distanceToSqr(destination)

        // Once the complete short corridor to the target is clear, leave the old final
        // waypoint immediately instead of lunging toward it for another refresh period.
        if (
            pathing &&
            destinationLoaded &&
            destinationDistanceSqr <= directCorridorReleaseDistanceSqr() &&
            barnacle.tickCount >= nextDirectCorridorCheckTick
        ) {
            nextDirectCorridorCheckTick = barnacle.tickCount + DIRECT_CORRIDOR_CHECK_TICKS
            if (hasCollisionFreeAquaticCorridor(barnacle, destination)) {
                pathing = false
                barnacle.navigation.stop()
                refreshPolicy.invalidate()
                return
            }
        }

        val pathDone = !pathing || barnacle.navigation.isDone || barnacle.horizontalCollision
        if (!refreshPolicy.shouldRecalculate(barnacle.tickCount, destination, pathDone)) return

        if (!destinationLoaded || hasCollisionFreeAquaticCorridor(barnacle, destination)) {
            pathing = false
            barnacle.navigation.stop()
            return
        }

        pathing = startPath()
        nextDirectCorridorCheckTick = barnacle.tickCount + DIRECT_CORRIDOR_CHECK_TICKS
        if (!pathing) barnacle.navigation.stop()
    }

    private fun requestVelocity(speed: Double, fallbackDirection: Vec3) {
        if (pathing) barnacle.navigation.setSpeedModifier(NAVIGATION_SPEED)
        moveControl.requestPathVelocity(speed, fallbackDirection)
    }

    private fun invalidatePath() {
        pathing = false
        nextDirectCorridorCheckTick = Int.MIN_VALUE
        refreshPolicy.invalidate()
        barnacle.navigation.stop()
    }

    private val moveControl: BarnacleMoveControl
        get() = barnacle.moveControl as BarnacleMoveControl

    private fun directCorridorReleaseDistanceSqr(): Double {
        val distance = ModServerConfig.BARNACLE_DIRECT_CORRIDOR_DISTANCE.get()
        return distance * distance
    }

    private fun updateNavigationRangeIfNeeded() {
        val detectionRange = barnacle.detectionRange.toFloat()
        if (detectionRange == appliedDetectionRange) return
        (barnacle.navigation as? WaterBoundPathNavigation)?.setRequiredPathLength(detectionRange)
        appliedDetectionRange = detectionRange
        refreshPolicy.invalidate()
    }

    private companion object {
        const val PATH_RECALCULATION_TICKS = 10
        const val TARGET_MOVE_THRESHOLD_SQR = 2.25
        const val FLEE_PATH_DISTANCE = 12.0
        const val IDLE_PATH_DISTANCE = 10.0
        const val NAVIGATION_SPEED = 1.0
        const val DIRECT_CORRIDOR_CHECK_TICKS = 3
    }
}
