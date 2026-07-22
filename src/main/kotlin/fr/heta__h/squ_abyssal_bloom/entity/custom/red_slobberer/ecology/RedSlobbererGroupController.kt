package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

class RedSlobbererGroupController(
    private val redSlobberer: RedSlobbererEntity
) {

    companion object {
        private const val SCAN_INTERVAL_TICKS = 40
        private const val GROUP_RADIUS = 32.0
        private const val VERTICAL_GROUP_RADIUS = 12.0

        fun findNearby(
            source: Entity,
            horizontalRadius: Double,
            verticalRadius: Double
        ): List<RedSlobbererEntity> {
            return source.level().getEntitiesOfClass(
                RedSlobbererEntity::class.java,
                source.boundingBox.inflate(horizontalRadius, verticalRadius, horizontalRadius)
            ) { candidate ->
                candidate.isAlive && candidate.isUnderWater
            }
        }

        fun centerOf(members: List<RedSlobbererEntity>, fallback: Vec3): Vec3 {
            if (members.isEmpty()) return fallback

            var x = 0.0
            var y = 0.0
            var z = 0.0
            for (member in members) {
                x += member.x
                y += member.y
                z += member.z
            }

            val count = members.size.toDouble()
            return Vec3(x / count, y / count, z / count)
        }
    }

    private var nextScanTick = 0
    private var cachedMembers: List<RedSlobbererEntity> = listOf(redSlobberer)

    fun tick() {
        refreshIfNeeded()
    }

    fun members(): List<RedSlobbererEntity> {
        refreshIfNeeded()
        if (cachedMembers.any { !it.isAlive || !it.isUnderWater }) {
            cachedMembers = cachedMembers.filter { it.isAlive && it.isUnderWater }
        }
        return cachedMembers
    }

    fun center(): Vec3 {
        return centerOf(members(), redSlobberer.position())
    }

    private fun refreshIfNeeded() {
        if (redSlobberer.level().isClientSide || redSlobberer.tickCount < nextScanTick) return

        nextScanTick = redSlobberer.tickCount + SCAN_INTERVAL_TICKS
        cachedMembers = findNearby(redSlobberer, GROUP_RADIUS, VERTICAL_GROUP_RADIUS)
            .sortedBy { it.uuid }

        if (redSlobberer.isUnderWater && cachedMembers.none { it === redSlobberer }) {
            cachedMembers = (cachedMembers + redSlobberer).sortedBy { it.uuid }
        }
    }

}
