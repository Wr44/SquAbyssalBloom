package fr.heta__h.squ_abyssal_bloom.entity.custom.brine

import fr.heta__h.squ_abyssal_bloom.entity.ai.navigation.PathRecalculationPolicy
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.hasCollisionFreeAquaticCorridor
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.floor

class BrinePathController(private val brine: BrineEntity) {
    private val refreshPolicy = PathRecalculationPolicy(PATH_RECALCULATION_TICKS, TARGET_MOVE_THRESHOLD_SQR)
    private var trackedTargetId = -1
    private var pathing = false

    fun moveToward(target: LivingEntity, speed: Double): Boolean {
        if (trackedTargetId != target.id) {
            trackedTargetId = target.id
            invalidatePath()
        }

        val destination = Vec3(floor(target.x) + 0.5, brine.y, floor(target.z) + 0.5)
        if (pathing && brine.navigation.isDone) pathing = false

        val pathDone = !pathing || brine.navigation.isDone || brine.horizontalCollision
        if (refreshPolicy.shouldRecalculate(brine.tickCount, destination, pathDone)) {
            val destinationPos = BlockPos.containing(destination)
            pathing = if (
                brine.level().isLoaded(destinationPos) &&
                !hasCollisionFreeAquaticCorridor(brine, destination)
            ) {
                brine.navigation.moveTo(destination.x, destination.y, destination.z, 1, NAVIGATION_SPEED)
            } else {
                false
            }

            if (!pathing) brine.navigation.stop()
        }

        if (!pathing) return false
        brine.navigation.setSpeedModifier(NAVIGATION_SPEED)
        moveControl.requestPathVelocity(speed)
        return true
    }

    fun stop() {
        trackedTargetId = -1
        invalidatePath()
        moveControl.cancelRequest()
    }

    private fun invalidatePath() {
        pathing = false
        refreshPolicy.invalidate()
        brine.navigation.stop()
    }

    private val moveControl: BrineMoveControl
        get() = brine.moveControl as BrineMoveControl

    private companion object {
        const val PATH_RECALCULATION_TICKS = 10
        const val TARGET_MOVE_THRESHOLD_SQR = 2.25
        const val NAVIGATION_SPEED = 1.0
    }
}
