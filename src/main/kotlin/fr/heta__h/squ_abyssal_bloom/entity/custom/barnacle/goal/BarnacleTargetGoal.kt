package fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.control.BarnacleTargeting
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.target.TargetGoal
import java.util.EnumSet


class BarnacleTargetGoal(private val barnacle: BarnacleEntity) : TargetGoal(barnacle, true, false) {
    private var nextScanTick = Int.MIN_VALUE
    private var lastSeenTick = Int.MIN_VALUE

    init {
        flags = EnumSet.of(Goal.Flag.TARGET)
    }

    override fun canUse(): Boolean {
        if (!barnacle.isUnderWater || barnacle.tickCount < nextScanTick) return false
        nextScanTick = barnacle.tickCount + BarnacleEntity.TARGET_SCAN_INTERVAL_TICKS

        targetMob = BarnacleTargeting.findNearestTarget(barnacle, barnacle.detectionRange)
        return targetMob != null
    }

    override fun start() {
        barnacle.target = targetMob
        targetMob = barnacle.target // LivingChangeTargetEvent may replace or clear the candidate (it's normal btw).
        lastSeenTick = barnacle.tickCount
        super.start()
    }

    override fun canContinueToUse(): Boolean {
        var current = barnacle.target ?: targetMob ?: return false
        if (!BarnacleTargeting.isEligibleTarget(barnacle, current, barnacle.detectionRange)) return false

        if (barnacle.sensing.hasLineOfSight(current)) {
            lastSeenTick = barnacle.tickCount
        } else if (!barnacle.isHoldingTarget && barnacle.tickCount - lastSeenTick > UNSEEN_MEMORY_TICKS) {
            return false
        }

        if (!barnacle.isHoldingTarget && barnacle.tickCount >= nextScanTick) {
            nextScanTick = barnacle.tickCount + BarnacleEntity.TARGET_SCAN_INTERVAL_TICKS
            if (!BarnacleTargeting.isPreferredBarnacle(
                    barnacle,
                    current,
                    barnacle.detectionRange
                )
            ) {
                return false
            }
        }

        if (barnacle.target !== current) {
            barnacle.target = current
            current = barnacle.target ?: return false
        }
        targetMob = current
        return true
    }

    override fun stop() {
        super.stop()
        nextScanTick = barnacle.tickCount + BarnacleEntity.TARGET_SCAN_INTERVAL_TICKS
    }

    private companion object {
        const val UNSEEN_MEMORY_TICKS = 60
    }
}
