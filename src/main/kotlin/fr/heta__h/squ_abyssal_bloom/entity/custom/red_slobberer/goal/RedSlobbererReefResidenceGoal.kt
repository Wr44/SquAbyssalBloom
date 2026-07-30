package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.RedSlobbererReefManager
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

class RedSlobbererReefResidenceGoal(
    private val redSlobberer: RedSlobbererEntity,
    private val speedModifier: Double
) : Goal() {

    private companion object {
        const val TARGET_ATTEMPTS = 24
        const val INNER_TARGET_RADIUS_FACTOR = 0.45
        const val START_HYSTERESIS = 0.75
        const val STOP_RADIUS_FACTOR = 0.82
        const val REPATH_INTERVAL_TICKS = 20
    }

    private var anchor: BlockPos? = null
    private var target: Vec3? = null
    private var repathTicks = 0

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        val level = redSlobberer.level() as? ServerLevel ?: return false
        if (!redSlobberer.isAlive || !redSlobberer.isUnderWater) return false
        val currentAnchor = RedSlobbererReefManager.forLevel(level)
            .activeReefAnchor(redSlobberer) ?: return false
        val radius = configuredRadius()
        if (horizontalDistanceSqr(currentAnchor) <= square(radius + START_HYSTERESIS)) return false

        anchor = currentAnchor
        target = findReturnTarget(currentAnchor, radius) ?: return false
        return true
    }

    override fun canContinueToUse(): Boolean {
        val level = redSlobberer.level() as? ServerLevel ?: return false
        if (!redSlobberer.isAlive || !redSlobberer.isUnderWater) return false
        val currentAnchor = RedSlobbererReefManager.forLevel(level)
            .activeReefAnchor(redSlobberer) ?: return false
        anchor = currentAnchor
        return horizontalDistanceSqr(currentAnchor) >
            square(configuredRadius() * STOP_RADIUS_FACTOR)
    }

    override fun start() {
        repathTicks = 0
        moveToTarget()
    }

    override fun tick() {
        if (repathTicks > 0) repathTicks--
        if (!redSlobberer.navigation.isDone && repathTicks > 0) return

        val currentAnchor = anchor ?: return
        target = findReturnTarget(currentAnchor, configuredRadius()) ?: target
        moveToTarget()
    }

    override fun stop() {
        redSlobberer.navigation.stop()
        anchor = null
        target = null
        repathTicks = 0
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    private fun moveToTarget() {
        val currentTarget = target ?: return
        redSlobberer.navigation.moveTo(
            currentTarget.x,
            currentTarget.y,
            currentTarget.z,
            speedModifier
        )
        repathTicks = REPATH_INTERVAL_TICKS
    }

    private fun findReturnTarget(anchor: BlockPos, residenceRadius: Double): Vec3? {
        return RedSlobbererReefNavigation.findBottomTargetAround(
            redSlobberer = redSlobberer,
            center = Vec3.atBottomCenterOf(anchor),
            horizontalRadius = (residenceRadius * INNER_TARGET_RADIUS_FACTOR).coerceAtLeast(2.0),
            attempts = TARGET_ATTEMPTS,
            minimumDistanceSqr = 0.0
        )
    }

    private fun horizontalDistanceSqr(anchor: BlockPos): Double {
        val dx = redSlobberer.x - (anchor.x + 0.5)
        val dz = redSlobberer.z - (anchor.z + 0.5)
        return dx * dx + dz * dz
    }

    private fun configuredRadius(): Double =
        ModServerConfig.RED_SLOBBERER_REEF_RESIDENCE_RADIUS.get().coerceAtLeast(2.0)

    private fun square(value: Double): Double = value * value
}
