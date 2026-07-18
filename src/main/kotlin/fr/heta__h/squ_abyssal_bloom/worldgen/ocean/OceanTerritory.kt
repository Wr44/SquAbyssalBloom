package fr.heta__h.squ_abyssal_bloom.worldgen.ocean

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object OceanTerritory {

    private const val DEFAULT_EXTRA_ZOOMS = 6
    private const val CACHE_LIMIT = 1 shl 16

    @Volatile private var worldSeed = 0L
    @Volatile private var totalZooms = 1 + DEFAULT_EXTRA_ZOOMS
    @Volatile private var candidates: List<String> = emptyList()
    @Volatile private var weights: Map<String, Int> = emptyMap()

    private data class CacheKey(val x: Int, val z: Int, val level: Int)
    private val levelCache = ConcurrentHashMap<CacheKey, String>()

    fun setCandidates(namespaces: Set<String>, defaultWeight: Int = 10, namespaceWeights: Map<String, Int> = emptyMap()) {
        candidates = namespaces.toList().sorted()
        weights = candidates.associateWith { namespaceWeights.getOrDefault(it, defaultWeight) }
        levelCache.clear()
    }

    fun seed(worldSeed: Long, extraZooms: Int = DEFAULT_EXTRA_ZOOMS) {
        this.worldSeed = worldSeed
        this.totalZooms = 1 + extraZooms.coerceIn(0, 10)
        levelCache.clear()
    }

    fun ownerAt(x: Int, z: Int): String {
        if (candidates.isEmpty()) return ""
        if (candidates.size == 1) return candidates[0]
        return sample(x, z, totalZooms)
    }

    private fun sample(x: Int, z: Int, level: Int): String {
        val key = CacheKey(x, z, level)
        levelCache[key]?.let { return it }

        val value = compute(x, z, level)
        if (levelCache.size < CACHE_LIMIT) levelCache[key] = value
        return value
    }

    private fun compute(x: Int, z: Int, level: Int): String {
        if (level == 0) return pickWeighted(x, z)

        val parentX = x shr 1
        val parentZ = z shr 1

        if (x and 1 == 0 && z and 1 == 0) return sample(parentX, parentZ, level - 1)

        val a = sample(parentX, parentZ, level - 1)
        val b = sample(parentX, parentZ + 1, level - 1)
        val c = sample(parentX + 1, parentZ, level - 1)
        val d = sample(parentX + 1, parentZ + 1, level - 1)
        val rng = cellRandom(x, z, level)

        return if (level == 1) listOf(a, b, c, d).random(rng) else majorityOrRandom(a, b, c, d, rng)
    }

    private fun majorityOrRandom(a: String, b: String, c: String, d: String, rng: Random): String = when {
        b == c && c == d -> b
        a == b && a == c -> a
        a == b && a == d -> a
        a == c && a == d -> a
        a == b && c != d -> a
        a == c && b != d -> a
        a == d && b != c -> a
        b == c && a != d -> b
        b == d && a != c -> b
        c == d && a != b -> c
        else -> listOf(a, b, c, d).random(rng)
    }

    private fun pickWeighted(x: Int, z: Int): String {
        val rng = cellRandom(x, z, 0)
        val total = candidates.sumOf { weights.getOrDefault(it, 1) }
        var roll = rng.nextInt(total)
        for (namespace in candidates) {
            roll -= weights.getOrDefault(namespace, 1)
            if (roll < 0) return namespace
        }
        return candidates.last()
    }

    private fun cellRandom(x: Int, z: Int, level: Int): Random {
        var h = worldSeed
        h = h * 31 + level
        h = h * 31 + x
        h = h * 31 + z
        return Random(h)
    }
}