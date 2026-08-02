package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
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

        fun scanWaterDepth(level: ClientLevel, surfaceBlock: BlockPos): Double {
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
    }

    private val analysisGeodesicRadius = localGeodesicRadius + analysisMargin
    private val cells = ArrayList<BioluminescentWaterCell>()
    private val distances = IntArrayList()
    private val indexByKey = Long2IntOpenHashMap()
    private val scanned = LongOpenHashSet()
    private val queue = ArrayDeque<Int>()

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
        scanned.add(key)
        queue.addLast(0)
    }

    fun advance(level: ClientLevel, budget: Int) {
        if (complete) return
        var processed = 0
        while (queue.isNotEmpty() && cells.size < maxWaterCells && processed < budget) {
            val index = queue.removeFirst()
            val cell = cells[index]
            val distance = distances.getInt(index)
            processed++
            if (distance >= analysisGeodesicRadius) continue
            for (offset in CARDINAL_OFFSETS) {
                val worldX = cell.waterPos.x + offset[0]
                val worldZ = cell.waterPos.z + offset[1]
                val key = ModUtilities.horizontalPositionKey(worldX, worldZ)
                if (!scanned.add(key)) continue
                if (!level.hasChunk(worldX shr 4, worldZ shr 4)) continue
                val waterCell = findWaterCell(level, worldX, worldZ, cell.waterPos.y) ?: continue
                if (abs(waterCell.waterPos.y - anchor.y) > WATER_LEVEL_TOLERANCE) continue
                val newIndex = cells.size
                cells.add(waterCell)
                distances.add(distance + 1)
                indexByKey.put(key, newIndex)
                queue.addLast(newIndex)
                if (cells.size >= maxWaterCells) break
            }
        }
        complete = queue.isEmpty() || cells.size >= maxWaterCells
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
        val startY = minOf(level.maxY - 1, referenceY + WATER_LEVEL_TOLERANCE)
        val minY = maxOf(level.minY, referenceY - WATER_LEVEL_TOLERANCE)
        if (startY < minY) return null
        val waterBlock = ModUtilities.findWaterBlockBelow(level, worldX, worldZ, startY, minY)
            ?: return null
        val surfaceBlock = ModUtilities.findTopWaterBlock(level, waterBlock) ?: return null
        if (surfaceBlock.y >= level.maxY - 1) return null
        if (!ModUtilities.isRenderableWaterSurface(level, surfaceBlock)) return null
        return BioluminescentWaterCell(
            surfaceBlock,
            ModUtilities.getFluidSurfaceHeight(level, surfaceBlock),
            scanWaterDepth(level, surfaceBlock)
        )
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
