package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet

class RedSlobbererFollowGroupGoal(
    private val redSlobberer: RedSlobbererEntity,
    private val speedModifier: Double
) : Goal() {

    private companion object {
        const val SEARCH_INTERVAL_TICKS = 40
        const val PATH_RECALCULATION_INTERVAL_TICKS = 20
        const val START_FOLLOW_DISTANCE_SQR = 144.0
        const val STOP_FOLLOW_DISTANCE_SQR = 64.0
        const val MAX_FOLLOW_DISTANCE_SQR = 1024.0
    }

    private var leader: RedSlobbererEntity? = null
    private var pathRecalculationTicks = 0
    private var nextSearchTick = 0

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        if (redSlobberer.level().isClientSide || redSlobberer.isBaby || !redSlobberer.isUnderWater) return false
        if (nextSearchTick > 0) {
            nextSearchTick--
            return false
        }
        nextSearchTick = SEARCH_INTERVAL_TICKS

        val candidate = redSlobberer.groupController.leader()
        if (candidate === redSlobberer || !candidate.isAlive) return false

        val distanceSqr = redSlobberer.distanceToSqr(candidate)
        if (distanceSqr <= START_FOLLOW_DISTANCE_SQR || distanceSqr > MAX_FOLLOW_DISTANCE_SQR) return false

        leader = candidate
        return true
    }

    override fun canContinueToUse(): Boolean {
        val currentLeader = leader ?: return false
        if (!currentLeader.isAlive || !redSlobberer.isUnderWater) return false

        val distanceSqr = redSlobberer.distanceToSqr(currentLeader)
        return distanceSqr > STOP_FOLLOW_DISTANCE_SQR && distanceSqr <= MAX_FOLLOW_DISTANCE_SQR
    }

    override fun start() {
        pathRecalculationTicks = PATH_RECALCULATION_INTERVAL_TICKS
        moveToLeader()
    }

    override fun tick() {
        if (--pathRecalculationTicks <= 0) {
            pathRecalculationTicks = PATH_RECALCULATION_INTERVAL_TICKS
            moveToLeader()
        }
    }

    override fun stop() {
        leader = null
        redSlobberer.navigation.stop()
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    private fun moveToLeader() {
        leader?.let { redSlobberer.navigation.moveTo(it, speedModifier) }
    }

}
