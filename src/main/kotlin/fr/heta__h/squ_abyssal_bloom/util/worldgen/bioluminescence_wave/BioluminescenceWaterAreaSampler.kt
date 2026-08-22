package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelReader
import java.util.ArrayDeque
import kotlin.math.abs

object BioluminescenceWaterAreaSampler {

    data class BioluminescenceWaterAreaCell(
        val waterSurface: BlockPos,
        val geodesicDistance: Int
    )

    data class BioluminescenceWaterArea(
        val cells: List<BioluminescenceWaterAreaCell>,
        val encounteredUnloadedChunks: Boolean
    )

    private const val WATER_LEVEL_TOLERANCE = 3
    private val CARDINAL_OFFSETS = arrayOf(
        1 to 0,
        -1 to 0,
        0 to 1,
        0 to -1
    )

    fun collect(
        level: LevelReader,
        anchor: BlockPos,
        maximumGeodesicDistance: Int,
        fullyCollectedDistance: Int,
        minimumCellCount: Int
    ): BioluminescenceWaterArea? {
        require(maximumGeodesicDistance >= 0)
        require(fullyCollectedDistance in 0..maximumGeodesicDistance)
        require(minimumCellCount > 0)
        val initialSurface = sampleSurface(level, anchor.x, anchor.z, anchor.y)
            ?: ModUtilities.findNearbyRenderableWaterSurface(level, anchor, 4, WATER_LEVEL_TOLERANCE)
            ?: return null
        val cells = ArrayList<BioluminescenceWaterAreaCell>()
        val pending = ArrayDeque<Int>()
        val visited = hashSetOf<Long>()
        var encounteredUnloadedChunks = false

        cells.add(BioluminescenceWaterAreaCell(initialSurface, 0))
        pending.addLast(0)
        visited.add(ModUtilities.horizontalPositionKey(initialSurface.x, initialSurface.z))

        while (pending.isNotEmpty()) {
            val index = pending.removeFirst()
            val cell = cells[index]
            if (cell.geodesicDistance >= fullyCollectedDistance && cells.size >= minimumCellCount) break
            if (cell.geodesicDistance >= maximumGeodesicDistance) continue

            for ((offsetX, offsetZ) in CARDINAL_OFFSETS) {
                val worldX = cell.waterSurface.x + offsetX
                val worldZ = cell.waterSurface.z + offsetZ
                val key = ModUtilities.horizontalPositionKey(worldX, worldZ)
                if (!visited.add(key)) continue
                if (!ModUtilities.hasLoadedChunk(level, worldX shr 4, worldZ shr 4)) {
                    encounteredUnloadedChunks = true
                    continue
                }
                val surface = sampleSurface(level, worldX, worldZ, cell.waterSurface.y) ?: continue
                if (abs(surface.y - initialSurface.y) > WATER_LEVEL_TOLERANCE) continue
                cells.add(BioluminescenceWaterAreaCell(surface, cell.geodesicDistance + 1))
                pending.addLast(cells.lastIndex)
            }
        }

        return BioluminescenceWaterArea(cells, encounteredUnloadedChunks)
    }

    private fun sampleSurface(level: LevelReader, worldX: Int, worldZ: Int, referenceY: Int): BlockPos? {
        if (!ModUtilities.hasLoadedChunk(level, worldX shr 4, worldZ shr 4)) return null
        val water = ModUtilities.findWaterBlockBelow(
            level,
            worldX,
            worldZ,
            minOf(level.maxY - 1, referenceY + WATER_LEVEL_TOLERANCE),
            maxOf(level.minY, referenceY - WATER_LEVEL_TOLERANCE)
        ) ?: return null
        val surface = ModUtilities.findTopWaterBlock(level, water) ?: return null
        return surface.takeIf { ModUtilities.isRenderableWaterSurface(level, it) }
    }
}
