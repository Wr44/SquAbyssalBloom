package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import kotlin.math.abs

class BioluminescentWaterDomainCollector(
    private val anchor: BlockPos,
    initialCell: BioluminescentWaterCell,
    private val localGeodesicRadius: Int,
    analysisMargin: Int,
    private val maximumWaterCells: Int
) {

    private val analysisGeodesicRadius = localGeodesicRadius + analysisMargin
    private val cells = ArrayList<BioluminescentWaterCell>()
    private val distances = IntArrayList()
    private val indexByKey = Long2IntOpenHashMap()
    private val scanned = LongOpenHashSet()
    private val queue = java.util.ArrayDeque<Int>()

    companion object {
        const val WATER_LEVEL_TOLERANCE = 3
        val CARDINAL_OFFSETS = arrayOf(
            intArrayOf(1, 0),
            intArrayOf(-1, 0),
            intArrayOf(0, 1),
            intArrayOf(0, -1)
        )
    }


    var complete: Boolean = false
        private set

    val cellCount: Int
        get() = cells.size

    init {
        indexByKey.defaultReturnValue(-1)
        val key = ModUtilities.bioluminescentCellKey(initialCell.waterPos.x, initialCell.waterPos.z)
        cells.add(initialCell)
        distances.add(0)
        indexByKey.put(key, 0)
        scanned.add(key)
        queue.addLast(0)
    }

    fun advance(level: ClientLevel, budget: Int) {
        if (complete) return
        var processed = 0
        while (queue.isNotEmpty() && cells.size < maximumWaterCells && processed < budget) {
            val index = queue.removeFirst()
            val cell = cells[index]
            val distance = distances.getInt(index)
            processed++
            if (distance >= analysisGeodesicRadius) continue
            for (offset in CARDINAL_OFFSETS) {
                val worldX = cell.waterPos.x + offset[0]
                val worldZ = cell.waterPos.z + offset[1]
                val key = ModUtilities.bioluminescentCellKey(worldX, worldZ)
                if (!scanned.add(key)) continue
                if (!level.hasChunk(worldX shr 4, worldZ shr 4)) continue
                val waterCell = findWaterCell(level, worldX, worldZ, cell.waterPos.y) ?: continue
                if (abs(waterCell.waterPos.y - anchor.y) > WATER_LEVEL_TOLERANCE) continue
                val newIndex = cells.size
                cells.add(waterCell)
                distances.add(distance + 1)
                indexByKey.put(key, newIndex)
                queue.addLast(newIndex)
                if (cells.size >= maximumWaterCells) break
            }
        }
        complete = queue.isEmpty() || cells.size >= maximumWaterCells
    }

    fun build(): BioluminescentWaterDomain {
        check(complete)
        val immutableCells = cells.toList()
        val immutableIndex = Long2IntOpenHashMap(indexByKey)
        immutableIndex.defaultReturnValue(-1)
        immutableIndex.trim()
        val neighbors = IntArray(immutableCells.size * 4) { -1 }
        for (index in immutableCells.indices) {
            val position = immutableCells[index].waterPos
            for (direction in CARDINAL_OFFSETS.indices) {
                val offset = CARDINAL_OFFSETS[direction]
                neighbors[index * 4 + direction] = immutableIndex.get(
                    ModUtilities.bioluminescentCellKey(position.x + offset[0], position.z + offset[1])
                )
            }
        }
        val naturalBoundarySeeds = immutableCells.indices.filter { index ->
            (0 until 4).any { direction -> neighbors[index * 4 + direction] < 0 }
        }
        val boundaryDepth = graphDistances(immutableCells.size, neighbors, naturalBoundarySeeds)
        val domain = BioluminescentWaterDomain(
            cells = immutableCells,
            indexByKey = immutableIndex,
            neighbors = neighbors,
            geodesicDistanceFromAnchor = distances.toIntArray(),
            boundaryDepth = boundaryDepth,
            bounds = createBounds(immutableCells),
            anchorIndex = immutableIndex.get(ModUtilities.bioluminescentCellKey(anchor.x, anchor.z))
                .coerceAtLeast(0),
            geodesicRadius = localGeodesicRadius,
            analysisGeodesicRadius = analysisGeodesicRadius
        )
        cells.clear()
        distances.clear()
        indexByKey.clear()
        scanned.clear()
        queue.clear()
        return domain
    }

    private fun findWaterCell(
        level: ClientLevel,
        worldX: Int,
        worldZ: Int,
        referenceY: Int
    ): BioluminescentWaterCell? {
        val waterBlock = ModUtilities.findWaterBlockBelow(
            level,
            worldX,
            worldZ,
            minOf(level.maxY - 1, referenceY + WATER_LEVEL_TOLERANCE),
            maxOf(level.minY, referenceY - WATER_LEVEL_TOLERANCE)
        ) ?: return null
        val topWaterBlock = ModUtilities.findTopWaterBlock(level, waterBlock) ?: return null
        if (!ModUtilities.isRenderableWaterSurface(level, topWaterBlock)) return null
        return BioluminescentWaterCell(
            topWaterBlock,
            ModUtilities.getFluidSurfaceHeight(level, topWaterBlock)
        )
    }

    private fun graphDistances(size: Int, neighbors: IntArray, seeds: Collection<Int>): IntArray {
        val result = IntArray(size) { -1 }
        val work = java.util.ArrayDeque<Int>()
        for (seed in seeds) {
            if (result[seed] >= 0) continue
            result[seed] = 0
            work.addLast(seed)
        }
        while (work.isNotEmpty()) {
            val index = work.removeFirst()
            for (direction in 0 until 4) {
                val neighbor = neighbors[index * 4 + direction]
                if (neighbor < 0 || result[neighbor] >= 0) continue
                result[neighbor] = result[index] + 1
                work.addLast(neighbor)
            }
        }
        return result
    }

    private fun createBounds(waterCells: List<BioluminescentWaterCell>): BioluminescentWaterBounds {
        return BioluminescentWaterBounds(
            waterCells.minOf { it.waterPos.x },
            waterCells.minOf { it.waterPos.y },
            waterCells.minOf { it.waterPos.z },
            waterCells.maxOf { it.waterPos.x },
            waterCells.maxOf { it.waterPos.y },
            waterCells.maxOf { it.waterPos.z }
        )
    }
}
