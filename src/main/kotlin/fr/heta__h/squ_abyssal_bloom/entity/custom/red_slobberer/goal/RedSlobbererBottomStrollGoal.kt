package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.phys.Vec3

class RedSlobbererBottomStrollGoal(
    private val redSlobberer: RedSlobbererEntity,
    speedModifier: Double
) : RandomStrollGoal(redSlobberer, speedModifier) {

    private companion object {
        const val POSITION_ATTEMPTS = 12
        const val HORIZONTAL_RADIUS = 10
        const val MAX_DESCENT = 3
        const val MIN_DISTANCE_SQR = 4.0
    }

    override fun getPosition(): Vec3? {
        val level = redSlobberer.level()
        val currentY = redSlobberer.blockY
        val candidate = BlockPos.MutableBlockPos()

        repeat(POSITION_ATTEMPTS) {
            val targetX = redSlobberer.blockX +
                redSlobberer.random.nextInt(-HORIZONTAL_RADIUS, HORIZONTAL_RADIUS + 1)
            val targetZ = redSlobberer.blockZ +
                redSlobberer.random.nextInt(-HORIZONTAL_RADIUS, HORIZONTAL_RADIUS + 1)

            candidate.set(targetX, currentY, targetZ)
            if (!level.isLoaded(candidate)) return@repeat

            val maximumAscent = redSlobberer.maximumClimbHeight.toInt()
            for (targetY in currentY + maximumAscent downTo currentY - MAX_DESCENT) {
                candidate.setY(targetY)
                if (!level.getFluidState(candidate).`is`(FluidTags.WATER)) continue

                val supportPos = candidate.below()
                if (level.getBlockState(supportPos).getCollisionShape(level, supportPos).isEmpty) continue

                val target = Vec3.atBottomCenterOf(candidate)
                val offsetX = target.x - redSlobberer.x
                val offsetZ = target.z - redSlobberer.z
                if (offsetX * offsetX + offsetZ * offsetZ < MIN_DISTANCE_SQR) continue

                val displacement = target.subtract(redSlobberer.position())
                if (!level.noCollision(redSlobberer, redSlobberer.boundingBox.move(displacement))) continue

                return target
            }
        }

        return null
    }

}
