package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaterAreaSampler
import net.minecraft.core.BlockPos
import net.minecraft.world.level.levelgen.RandomSupport
import java.util.ArrayDeque
import kotlin.math.sqrt

object PlanktonBloomPlacement {
    private const val DEPTH_SATURATION = 6.0
    private const val SPREAD_SATURATION = 16.0
    private const val DEPTH_WEIGHT = 1.0
    private const val SPREAD_WEIGHT = 1.2
    private const val TIE_WEIGHT = 0.15
    private const val BLOOM_TOPUP_SALT = 0x3C6EF372FE94F82BL
    private val CARDINAL_OFFSETS = arrayOf(
        1 to 0,
        -1 to 0,
        0 to 1,
        0 to -1
    )

    fun selectPositions(
        area: BioluminescenceWaterAreaSampler.BioluminescenceWaterArea,
        maximumGeodesicDistance: Int,
        waveSeed: Long,
        targetCount: Int,
        chosenPositions: MutableList<BlockPos>,
        usedPositionKeys: MutableSet<Long>
    ): List<BlockPos> {
        if (targetCount <= 0 || area.cells.isEmpty()) return emptyList()
        val eligibleCells = area.cells.filter { cell ->
            cell.geodesicDistance <= maximumGeodesicDistance
        }
        if (eligibleCells.isEmpty()) return emptyList()
        val indexByPosition = HashMap<Long, Int>(eligibleCells.size)
        eligibleCells.forEachIndexed { index, cell ->
            indexByPosition[ModUtilities.horizontalPositionKey(cell.waterSurface.x, cell.waterSurface.z)] = index
        }
        val boundaryDepths = boundaryDepths(eligibleCells, indexByPosition)
        val selected = mutableListOf<BlockPos>()

        repeat(targetCount) {
            var bestPosition: BlockPos? = null
            var bestScore = Double.NEGATIVE_INFINITY
            var bestTie = Long.MIN_VALUE

            for (index in eligibleCells.indices) {
                val waterPos = eligibleCells[index].waterSurface
                val positionKey = ModUtilities.horizontalPositionKey(waterPos.x, waterPos.z)
                if (positionKey in usedPositionKeys) continue
                val depthTerm = ModUtilities.smooth(0.0, DEPTH_SATURATION, boundaryDepths[index].toDouble())
                val tie = RandomSupport.mixStafford13(waveSeed xor positionKey xor BLOOM_TOPUP_SALT)
                val score = depthTerm * DEPTH_WEIGHT +
                    spreadTermFor(waterPos, chosenPositions) * SPREAD_WEIGHT +
                    ModUtilities.stableUnitValue(tie) * TIE_WEIGHT
                if (score > bestScore || score == bestScore && tie > bestTie) {
                    bestPosition = waterPos
                    bestScore = score
                    bestTie = tie
                }
            }
            val chosen = bestPosition ?: return selected
            usedPositionKeys.add(ModUtilities.horizontalPositionKey(chosen.x, chosen.z))
            chosenPositions.add(chosen)
            selected.add(chosen)
        }
        return selected
    }

    private fun boundaryDepths(
        cells: List<BioluminescenceWaterAreaSampler.BioluminescenceWaterAreaCell>,
        indexByPosition: Map<Long, Int>
    ): IntArray {
        val depths = IntArray(cells.size) { Int.MAX_VALUE }
        val pending = ArrayDeque<Int>()
        for (index in cells.indices) {
            val position = cells[index].waterSurface
            val boundary = CARDINAL_OFFSETS.any { (offsetX, offsetZ) ->
                ModUtilities.horizontalPositionKey(position.x + offsetX, position.z + offsetZ) !in indexByPosition
            }
            if (boundary) {
                depths[index] = 0
                pending.addLast(index)
            }
        }
        while (pending.isNotEmpty()) {
            val index = pending.removeFirst()
            val position = cells[index].waterSurface
            val nextDepth = depths[index] + 1
            for ((offsetX, offsetZ) in CARDINAL_OFFSETS) {
                val neighbor = indexByPosition[
                    ModUtilities.horizontalPositionKey(position.x + offsetX, position.z + offsetZ)
                ] ?: continue
                if (depths[neighbor] <= nextDepth) continue
                depths[neighbor] = nextDepth
                pending.addLast(neighbor)
            }
        }
        return depths
    }

    private fun spreadTermFor(waterPos: BlockPos, chosenPositions: List<BlockPos>): Double {
        if (chosenPositions.isEmpty()) return 1.0
        var nearestSqr = Double.POSITIVE_INFINITY
        for (chosen in chosenPositions) {
            val distanceSqr = ModUtilities.horizontalDistanceSqr(
                chosen.x.toDouble(),
                chosen.z.toDouble(),
                waterPos.x.toDouble(),
                waterPos.z.toDouble()
            )
            if (distanceSqr < nearestSqr) nearestSqr = distanceSqr
        }
        return ModUtilities.smooth(0.0, SPREAD_SATURATION, sqrt(nearestSqr))
    }
}
