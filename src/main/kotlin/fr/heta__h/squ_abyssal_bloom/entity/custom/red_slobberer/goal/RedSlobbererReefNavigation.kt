package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

internal object RedSlobbererReefNavigation {
    private const val MAXIMUM_VERTICAL_SEARCH_ABOVE = 4
    private const val MAXIMUM_VERTICAL_SEARCH_BELOW = 10

    fun findBottomTargetAround(
        redSlobberer: RedSlobbererEntity,
        center: Vec3,
        horizontalRadius: Double,
        attempts: Int,
        minimumDistanceSqr: Double
    ): Vec3? {
        val level = redSlobberer.level()
        val integerRadius = ceil(horizontalRadius).toInt().coerceAtLeast(1)
        val radiusSqr = horizontalRadius * horizontalRadius
        val currentY = redSlobberer.blockY
        val candidate = BlockPos.MutableBlockPos()
        val centerPos = BlockPos.containing(center)

        repeat(attempts) {
            val dx = redSlobberer.random.nextInt(-integerRadius, integerRadius + 1)
            val dz = redSlobberer.random.nextInt(-integerRadius, integerRadius + 1)
            if (dx * dx + dz * dz > radiusSqr) return@repeat

            val targetX = centerPos.x + dx
            val targetZ = centerPos.z + dz
            candidate.set(targetX, currentY, targetZ)
            if (!level.isLoaded(candidate)) return@repeat

            val maximumY = currentY +
                maxOf(redSlobberer.maximumStepHeight.toInt(), MAXIMUM_VERTICAL_SEARCH_ABOVE)
            val minimumY = currentY - MAXIMUM_VERTICAL_SEARCH_BELOW
            for (targetY in maximumY downTo minimumY) {
                candidate.setY(targetY)
                if (!level.getFluidState(candidate).`is`(FluidTags.WATER)) continue

                val supportPos = candidate.below()
                if (level.getBlockState(supportPos).getCollisionShape(level, supportPos).isEmpty) {
                    continue
                }

                val target = Vec3.atBottomCenterOf(candidate)
                val offsetX = target.x - redSlobberer.x
                val offsetZ = target.z - redSlobberer.z
                if (offsetX * offsetX + offsetZ * offsetZ < minimumDistanceSqr) continue

                val displacement = target.subtract(redSlobberer.position())
                if (!level.noCollision(redSlobberer, redSlobberer.boundingBox.move(displacement))) {
                    continue
                }
                return target
            }
        }
        return null
    }
}
