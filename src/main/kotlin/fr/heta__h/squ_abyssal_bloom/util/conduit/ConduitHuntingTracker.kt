package fr.heta__h.squ_abyssal_bloom.util.conduit

import java.util.Collections
import java.util.UUID

object ConduitHuntingTracker {
    private val hitMobsThisTick = Collections.synchronizedSet(mutableSetOf<UUID>())

    @JvmStatic
    fun tryMarkHit(uuid: UUID): Boolean = hitMobsThisTick.add(uuid)

    @JvmStatic
    fun clearHits() = hitMobsThisTick.clear()
}