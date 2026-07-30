package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.sqrt

class RedSlobbererCollectiveCohesionGoal(
    private val redSlobberer: RedSlobbererEntity,
    private val speedModifier: Double
) : Goal() {

    private companion object {
        const val SEARCH_INTERVAL_TICKS = 40
        const val PATH_RECALCULATION_INTERVAL_TICKS = 20
        const val MAXIMUM_GOAL_TICKS = 80
        const val COMFORT_RADIUS = 8.0
        const val COMFORT_RADIUS_SQR = COMFORT_RADIUS * COMFORT_RADIUS
        const val SEPARATION_RADIUS = 5.0
        const val SEPARATION_RADIUS_SQR = SEPARATION_RADIUS * SEPARATION_RADIUS
        const val TARGET_DISTANCE = 6.0
        const val MINIMUM_VECTOR_LENGTH_SQR = 1.0E-8
    }

    private var nextSearchTick = 0
    private var pathRecalculationTicks = 0
    private var remainingGoalTicks = 0
    private var target: Vec3? = null

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        if (redSlobberer.level().isClientSide || redSlobberer.isBaby || !redSlobberer.isUnderWater) {
            return false
        }
        if (nextSearchTick > 0) {
            nextSearchTick--
            return false
        }
        nextSearchTick = SEARCH_INTERVAL_TICKS
        target = calculateCollectiveTarget()
        return target != null
    }

    override fun canContinueToUse(): Boolean {
        return remainingGoalTicks > 0 &&
            redSlobberer.isUnderWater &&
            redSlobberer.groupController.members().size >= 2 &&
            (!redSlobberer.navigation.isDone || calculateCollectiveTarget() != null)
    }

    override fun start() {
        remainingGoalTicks = MAXIMUM_GOAL_TICKS
        pathRecalculationTicks = 0
    }

    override fun tick() {
        remainingGoalTicks--
        if (--pathRecalculationTicks > 0) return
        pathRecalculationTicks = PATH_RECALCULATION_INTERVAL_TICKS
        target = calculateCollectiveTarget()
        target?.let { destination ->
            redSlobberer.navigation.moveTo(
                destination.x,
                destination.y,
                destination.z,
                speedModifier
            )
        }
    }

    override fun stop() {
        target = null
        redSlobberer.navigation.stop()
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    private fun calculateCollectiveTarget(): Vec3? {
        val neighbors = redSlobberer.groupController.members()
            .filter { member -> member !== redSlobberer && member.isAlive && member.isUnderWater }
        if (neighbors.isEmpty()) return null

        val allMembers = neighbors + redSlobberer
        val center = redSlobberer.groupController.center()
        val centerX = center.x - redSlobberer.x
        val centerZ = center.z - redSlobberer.z
        val centerDistanceSqr = centerX * centerX + centerZ * centerZ

        var forceX = 0.0
        var forceZ = 0.0
        if (centerDistanceSqr > COMFORT_RADIUS_SQR) {
            val centerDistance = sqrt(centerDistanceSqr)
            val cohesionStrength = ((centerDistance - COMFORT_RADIUS) / COMFORT_RADIUS)
                .coerceIn(0.0, 1.0)
            forceX += centerX / centerDistance * cohesionStrength
            forceZ += centerZ / centerDistance * cohesionStrength
        }

        for (neighbor in allMembers) {
            if (neighbor === redSlobberer) continue
            val awayX = redSlobberer.x - neighbor.x
            val awayZ = redSlobberer.z - neighbor.z
            val distanceSqr = awayX * awayX + awayZ * awayZ
            if (distanceSqr <= MINIMUM_VECTOR_LENGTH_SQR || distanceSqr >= SEPARATION_RADIUS_SQR) continue
            val distance = sqrt(distanceSqr)
            val strength = (1.0 - distance / SEPARATION_RADIUS).let { it * it } * 1.35
            forceX += awayX / distance * strength
            forceZ += awayZ / distance * strength
        }

        val forceLengthSqr = forceX * forceX + forceZ * forceZ
        if (forceLengthSqr <= MINIMUM_VECTOR_LENGTH_SQR) return null
        val inverseForceLength = 1.0 / sqrt(forceLengthSqr)
        return Vec3(
            redSlobberer.x + forceX * inverseForceLength * TARGET_DISTANCE,
            redSlobberer.y,
            redSlobberer.z + forceZ * inverseForceLength * TARGET_DISTANCE
        )
    }
}
