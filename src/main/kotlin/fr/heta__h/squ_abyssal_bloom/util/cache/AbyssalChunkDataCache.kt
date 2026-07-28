package fr.heta__h.squ_abyssal_bloom.util.cache

import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.AbyssalChunkData
import net.minecraft.world.level.levelgen.RandomState
import java.util.concurrent.ConcurrentHashMap

object AbyssalChunkDataCache {
    private class CacheKey(
        private val randomState: RandomState,
        private val chunkPos: Long
    ) {
        override fun equals(other: Any?): Boolean =
            this === other ||
                other is CacheKey &&
                randomState === other.randomState &&
                chunkPos == other.chunkPos

        override fun hashCode(): Int =
            31 * System.identityHashCode(randomState) + chunkPos.hashCode()
    }

    private val cache = ConcurrentHashMap<CacheKey, AbyssalChunkData>()

    fun store(
        randomState: RandomState,
        chunkX: Int,
        chunkZ: Int,
        abyssalMask: BooleanArray,
        carverMask: BooleanArray,
        floorGrid: IntArray
    ) {
        cache[CacheKey(randomState, pack(chunkX, chunkZ))] =
            AbyssalChunkData(abyssalMask, carverMask, floorGrid)
    }

    fun consume(randomState: RandomState, chunkX: Int, chunkZ: Int): AbyssalChunkData? =
        cache.remove(CacheKey(randomState, pack(chunkX, chunkZ)))

    fun clear() {
        cache.clear()
    }

    private fun pack(x: Int, z: Int): Long = x.toLong().shl(32) or (z.toLong() and 0xFFFFFFFFL)
}
