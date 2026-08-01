package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import it.unimi.dsi.fastutil.longs.Long2IntMap

class BioluminescentWaterDomain internal constructor(
    val cells: List<BioluminescentWaterCell>,
    private val indexByKey: Long2IntMap,
    private val neighbors: IntArray,
    val geodesicDistanceFromAnchor: IntArray,
    val boundaryDepth: IntArray,
    val bounds: BioluminescentWaterBounds,
    val anchorIndex: Int,
    val geodesicRadius: Int,
    val analysisGeodesicRadius: Int
) {
    val localCellIndices: IntArray = createLocalCellIndices()

    val size: Int
        get() = cells.size

    val localSize: Int
        get() = localCellIndices.size

    fun isLocalCell(index: Int): Boolean {
        return geodesicDistanceFromAnchor[index] <= geodesicRadius
    }

    fun cellIndexAt(worldX: Int, worldZ: Int): Int? {
        val index = indexByKey.get(ModUtilities.bioluminescentCellKey(worldX, worldZ))
        return if (index >= 0) index else null
    }

    fun neighbor(index: Int, direction: Int): Int = neighbors[index * 4 + direction]

    inline fun forEachNeighbor(index: Int, action: (Int) -> Unit) {
        for (direction in 0 until 4) {
            val neighbor = neighbor(index, direction)
            if (neighbor >= 0) action(neighbor)
        }
    }

    private fun createLocalCellIndices(): IntArray {
        var count = 0
        for (distance in geodesicDistanceFromAnchor) {
            if (distance <= geodesicRadius) count++
        }
        val result = IntArray(count)
        var cursor = 0
        for (index in geodesicDistanceFromAnchor.indices) {
            if (geodesicDistanceFromAnchor[index] <= geodesicRadius) result[cursor++] = index
        }
        return result
    }
}
