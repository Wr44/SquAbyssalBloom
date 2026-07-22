package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology.RedSlobbererReefManager
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.phys.Vec3

class RedSlobbererBottomStrollGoal(
    private val redSlobberer: RedSlobbererEntity,
    speedModifier: Double
) : RandomStrollGoal(redSlobberer, speedModifier) {

    private companion object {
        const val POSITION_ATTEMPTS = 12
        const val HORIZONTAL_RADIUS = 10
        const val MIN_DISTANCE_SQR = 4.0
    }

    override fun getPosition(): Vec3? {
        val level = redSlobberer.level()
        val serverLevel = level as? ServerLevel
        val reefAnchor = serverLevel?.let { currentLevel ->
            RedSlobbererReefManager.forLevel(currentLevel).activeReefAnchor(redSlobberer)
        }
        val center = reefAnchor?.let(Vec3::atBottomCenterOf) ?: redSlobberer.position()
        val radius = if (reefAnchor == null) {
            HORIZONTAL_RADIUS.toDouble()
        } else {
            ModServerConfig.RED_SLOBBERER_REEF_RESIDENCE_RADIUS.get().coerceAtLeast(2.0)
        }
        return RedSlobbererReefNavigation.findBottomTargetAround(
            redSlobberer = redSlobberer,
            center = center,
            horizontalRadius = radius,
            attempts = POSITION_ATTEMPTS,
            minimumDistanceSqr = MIN_DISTANCE_SQR
        )
    }

}
