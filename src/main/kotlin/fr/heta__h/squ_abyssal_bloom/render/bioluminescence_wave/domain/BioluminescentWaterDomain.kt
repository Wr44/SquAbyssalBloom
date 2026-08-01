package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities

class BioluminescentWaterDomain internal constructor(
    val cells: List<BioluminescentWaterCell>,
    private val indexByKey: Map<Long, Int>,
    private val neighbors: IntArray,
    val geodesicDistanceFromAnchor: IntArray,
    val boundaryDepth: IntArray,
    val artificialBoundaryDistance: IntArray,
    val bounds: BioluminescentWaterBounds,
    val anchorIndex: Int,
    val geodesicRadius: Int,
    val analysisGeodesicRadius: Int
) {
    val localCellIndices: IntArray = geodesicDistanceFromAnchor.indices
        .filter { index -> geodesicDistanceFromAnchor[index] <= geodesicRadius }
        .toIntArray()

    val size: Int
        get() = cells.size

    val localSize: Int
        get() = localCellIndices.size

    fun isLocalCell(index: Int): Boolean {
        return geodesicDistanceFromAnchor[index] <= geodesicRadius
    }

    fun cellIndexAt(worldX: Int, worldZ: Int): Int? {
        return indexByKey[ModUtilities.bioluminescentCellKey(worldX, worldZ)]
    }

    fun neighbor(index: Int, direction: Int): Int = neighbors[index * 4 + direction]

    inline fun forEachNeighbor(index: Int, action: (Int) -> Unit) {
        for (direction in 0 until 4) {
            val neighbor = neighbor(index, direction)
            if (neighbor >= 0) action(neighbor)
        }
    }
}
