package fr.heta__h.squ_abyssal_bloom.util.cache

import java.util.concurrent.ConcurrentHashMap

object AbyssalChunkDataCache {
    private val cache = ConcurrentHashMap<Long, BooleanArray>()

    fun store(chunkX: Int, chunkZ: Int, mask: BooleanArray) {
        cache[pack(chunkX, chunkZ)] = mask
    }

    fun consume(chunkX: Int, chunkZ: Int): BooleanArray? =
        cache.remove(pack(chunkX, chunkZ))

    private fun pack(x: Int, z: Int): Long = x.toLong().shl(32) or (z.toLong() and 0xFFFFFFFFL)
}
