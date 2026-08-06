package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.LevelReader
import java.util.ArrayDeque
import kotlin.math.abs

class BioluminescentWaterDomainCollector(
    private val anchor: BlockPos,
    initialCell: BioluminescentWaterCell,
    private val localGeodesicRadius: Int,
    analysisMargin: Int,
    private val maxWaterCells: Int
) {
    companion object {
        const val WATER_LEVEL_TOLERANCE = 3
        const val MAX_DEPTH_SCAN = 32
        val CARDINAL_OFFSETS = arrayOf(
            intArrayOf(1, 0),
            intArrayOf(-1, 0),
            intArrayOf(0, 1),
            intArrayOf(0, -1)
        )

        fun scanWaterDepth(level: LevelReader, surfaceBlock: BlockPos): Double {
            val limitY = maxOf(level.minY, surfaceBlock.y - MAX_DEPTH_SCAN)
            var y = surfaceBlock.y
            var depth = 0
            while (y > limitY) {
                val pos = BlockPos(surfaceBlock.x, y - 1, surfaceBlock.z)
                if (!level.getFluidState(pos).`is`(FluidTags.WATER)) break
                y--
                depth++
            }
            return depth.toDouble()
        }

        fun sampleWaterCell(
            level: LevelReader,
            worldX: Int,
            worldZ: Int,
            referenceY: Int
        ): BioluminescentWaterCell? {
            val startY = minOf(level.maxY - 1, referenceY + WATER_LEVEL_TOLERANCE)
            val minY = maxOf(level.minY, referenceY - WATER_LEVEL_TOLERANCE)
            if (startY < minY) return null
            val waterBlock = ModUtilities.findWaterBlockBelow(level, worldX, worldZ, startY, minY) ?: return null
            val surfaceBlock = ModUtilities.findTopWaterBlock(level, waterBlock) ?: return null
            if (surfaceBlock.y >= level.maxY - 1) return null
            if (!ModUtilities.isRenderableWaterSurface(level, surfaceBlock)) return null
            return BioluminescentWaterCell(
                surfaceBlock,
                ModUtilities.getFluidSurfaceHeight(level, surfaceBlock),
                scanWaterDepth(level, surfaceBlock)
            )
        }
    }

    private val analysisGeodesicRadius = localGeodesicRadius + analysisMargin
    private val cells = ArrayList<BioluminescentWaterCell>()
    private val distances = IntArrayList()
    private val indexByKey = Long2IntOpenHashMap()
    private val sampledColumns = mutableMapOf<Long, BioluminescentWaterCell?>()
    private val queue = ArrayDeque<Int>()
    private var currentCellIndex = -1
    private var currentDirection = 0

    var complete = false
        private set

    val cellCount: Int
        get() = cells.size

    init {
        indexByKey.defaultReturnValue(-1)
        val key = ModUtilities.horizontalPositionKey(initialCell.waterPos.x, initialCell.waterPos.z)
        cells.add(initialCell)
        distances.add(0)
        indexByKey.put(key, 0)
        sampledColumns[key] = initialCell
        queue.addLast(0)
    }

    fun advance(level: LevelReader, budget: Int) {
        if (complete) return
        var processed = 0
        while (cells.size < maxWaterCells && processed < budget) {
            if (currentCellIndex < 0) {
                if (queue.isEmpty()) break
                val index = queue.removeFirst()
                if (distances.getInt(index) >= analysisGeodesicRadius) {
                    processed++
                    continue
                }
                currentCellIndex = index
                currentDirection = 0
            }

            val cell = cells[currentCellIndex]
            val distance = distances.getInt(currentCellIndex)
            while (currentDirection < CARDINAL_OFFSETS.size &&
                cells.size < maxWaterCells && processed < budget
            ) {
                val offset = CARDINAL_OFFSETS[currentDirection]
                val worldX = cell.waterPos.x + offset[0]
                val worldZ = cell.waterPos.z + offset[1]
                val key = ModUtilities.horizontalPositionKey(worldX, worldZ)
                if (!sampledColumns.containsKey(key)) {
                    if (!ModUtilities.hasLoadedChunk(level, worldX shr 4, worldZ shr 4)) return
                    sampledColumns[key] = sampleWaterCell(level, worldX, worldZ, cell.waterPos.y)
                }
                val waterCell = sampledColumns.getValue(key)
                if (waterCell != null && abs(waterCell.waterPos.y - anchor.y) <= WATER_LEVEL_TOLERANCE) {
                    addCell(waterCell, distance + 1)
                }
                currentDirection++
                processed++
            }
            if (currentDirection >= CARDINAL_OFFSETS.size) currentCellIndex = -1
        }
        complete = (queue.isEmpty() && currentCellIndex < 0) || cells.size >= maxWaterCells
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
                    ModUtilities.horizontalPositionKey(position.x + offset[0], position.z + offset[1])
                )
            }
        }
        val naturalBoundarySeeds = immutableCells.indices.filter { index ->
            (0 until 4).any { direction -> neighbors[index * 4 + direction] < 0 }
        }
        val boundaryDepth = ModUtilities.graphDistances(
            immutableCells.size,
            neighbors,
            naturalBoundarySeeds
        )
        val domain = BioluminescentWaterDomain(
            cells = immutableCells,
            indexByKey = immutableIndex,
            neighbors = neighbors,
            geodesicDistanceFromAnchor = distances.toIntArray(),
            boundaryDepth = boundaryDepth,
            bounds = createBounds(immutableCells),
            anchorIndex = immutableIndex.get(ModUtilities.horizontalPositionKey(anchor.x, anchor.z))
                .coerceAtLeast(0),
            geodesicRadius = localGeodesicRadius,
            analysisGeodesicRadius = analysisGeodesicRadius
        )
        cells.clear()
        distances.clear()
        indexByKey.clear()
        sampledColumns.clear()
        queue.clear()
        currentCellIndex = -1
        currentDirection = 0
        return domain
    }

    private fun addCell(cell: BioluminescentWaterCell, distance: Int) {
        val key = ModUtilities.horizontalPositionKey(cell.waterPos.x, cell.waterPos.z)
        if (indexByKey.containsKey(key)) return
        val newIndex = cells.size
        cells.add(cell)
        distances.add(distance)
        indexByKey.put(key, newIndex)
        queue.addLast(newIndex)
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
