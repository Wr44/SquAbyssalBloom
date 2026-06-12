package fr.heta__h.squ_abyssal_bloom.util.cache

import fr.heta__h.squ_abyssal_bloom.util.worldgen.terrain.AbyssalChunkData
import java.util.concurrent.ConcurrentHashMap

object AbyssalChunkDataCache {
    private val cache = ConcurrentHashMap<Long, AbyssalChunkData>()

    fun store(chunkX: Int, chunkZ: Int, abyssalMask: BooleanArray, carverMask: BooleanArray, floorGrid: IntArray) {
        cache[pack(chunkX, chunkZ)] = AbyssalChunkData(abyssalMask, carverMask, floorGrid)
    }

    fun consume(chunkX: Int, chunkZ: Int): AbyssalChunkData? =
        cache.remove(pack(chunkX, chunkZ))

    private fun pack(x: Int, z: Int): Long = x.toLong().shl(32) or (z.toLong() and 0xFFFFFFFFL)
}