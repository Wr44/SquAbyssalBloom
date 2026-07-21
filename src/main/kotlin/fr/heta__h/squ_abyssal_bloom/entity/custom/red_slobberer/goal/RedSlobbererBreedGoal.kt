package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.world.entity.ai.goal.BreedGoal

class RedSlobbererBreedGoal(
    private val redSlobberer: RedSlobbererEntity,
    private val movementSpeed: Double
) : BreedGoal(redSlobberer, movementSpeed) {

    private var courtshipTicks = 0
    private var breedingCompleted = false

    override fun start() {
        courtshipTicks = 0
        breedingCompleted = false
    }

    override fun tick() {
        val currentPartner = partner ?: return
        redSlobberer.lookControl.setLookAt(
            currentPartner,
            MAX_LOOK_TURN_DEGREES,
            redSlobberer.maxHeadXRot.toFloat()
        )
        redSlobberer.navigation.moveTo(currentPartner, movementSpeed)
        courtshipTicks++

        val combinedRadius = (redSlobberer.bbWidth + currentPartner.bbWidth) * 0.5
        val acceptedCenterDistance = combinedRadius + MAX_BREEDING_EDGE_GAP
        if (
            !breedingCompleted &&
            courtshipTicks >= adjustedTickDelay(COURTSHIP_DURATION_TICKS) &&
            redSlobberer.distanceToSqr(currentPartner) <= acceptedCenterDistance * acceptedCenterDistance
        ) {
            breedingCompleted = true
            breed()
            redSlobberer.navigation.stop()
        }
    }

    override fun stop() {
        courtshipTicks = 0
        breedingCompleted = false
        super.stop()
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    private companion object {
        const val COURTSHIP_DURATION_TICKS = 60
        const val MAX_BREEDING_EDGE_GAP = 0.75
        const val MAX_LOOK_TURN_DEGREES = 5.0f
    }
}
