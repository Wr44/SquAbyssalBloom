package fr.heta__h.squ_abyssal_bloom.entity.ai.navigation

import net.minecraft.world.phys.Vec3


class PathRecalculationPolicy(
    private val cooldownTicks: Int,
    private val destinationMoveThresholdSqr: Double
) {
    private var nextAllowedTick = Int.MIN_VALUE
    private var lastDestination: Vec3? = null

    fun shouldRecalculate(tick: Int, destination: Vec3, pathDone: Boolean): Boolean {
        if (tick < nextAllowedTick) return false

        val previous = lastDestination
        val destinationMoved = previous == null || previous.distanceToSqr(destination) >= destinationMoveThresholdSqr
        if (!pathDone && !destinationMoved) return false

        lastDestination = destination
        nextAllowedTick = tick + cooldownTicks
        return true
    }

    fun invalidate() {
        nextAllowedTick = Int.MIN_VALUE
        lastDestination = null
    }
}
