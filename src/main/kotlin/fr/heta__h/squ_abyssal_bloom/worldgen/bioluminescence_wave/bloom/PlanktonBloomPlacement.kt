package fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.bloom

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomain
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.sqrt

object PlanktonBloomPlacement {
    private const val DEPTH_SATURATION = 6.0
    private const val SPREAD_SATURATION = 16.0
    private const val DEPTH_WEIGHT = 1.0
    private const val SPREAD_WEIGHT = 1.2
    private const val TIE_WEIGHT = 0.15
    private const val BLOOM_TOPUP_SALT = 0x3C6EF372FE94F82BL

    fun topUpCells(
        domain: BioluminescentWaterDomain,
        maxGeodesicDistance: Double,
        waveSeed: Long,
        missingCount: Int,
        chosenPositions: MutableList<BlockPos>,
        usedPositionKeys: MutableSet<Long>
    ): List<Int> {
        if (missingCount <= 0) return emptyList()
        val added = mutableListOf<Int>()
        repeat(missingCount) {
            val next = pickCell(domain, maxGeodesicDistance, waveSeed, chosenPositions, usedPositionKeys)
            if (next < 0) return added
            val waterPos = domain.cells[next].waterPos
            usedPositionKeys.add(ModUtilities.horizontalPositionKey(waterPos.x, waterPos.z))
            chosenPositions.add(waterPos)
            added.add(next)
        }
        return added
    }

    private fun pickCell(
        domain: BioluminescentWaterDomain,
        maxGeodesicDistance: Double,
        waveSeed: Long,
        chosenPositions: List<BlockPos>,
        usedPositionKeys: Set<Long>
    ): Int {
        var bestIndex = -1
        var bestScore = Double.NEGATIVE_INFINITY
        var bestTie = Long.MIN_VALUE

        for (index in domain.localCellIndices) {
            if (domain.geodesicDistanceFromAnchor[index] > maxGeodesicDistance) continue
            val waterPos = domain.cells[index].waterPos
            val positionKey = ModUtilities.horizontalPositionKey(waterPos.x, waterPos.z)
            if (positionKey in usedPositionKeys) continue

            val depthTerm = ModUtilities.smooth(
                0.0,
                DEPTH_SATURATION,
                domain.boundaryDepth[index].toDouble()
            )
            val tie = RandomSupport.mixStafford13(waveSeed xor positionKey xor BLOOM_TOPUP_SALT)
            val score = depthTerm * DEPTH_WEIGHT +
                spreadTermFor(waterPos, chosenPositions) * SPREAD_WEIGHT +
                ModUtilities.stableUnitValue(tie) * TIE_WEIGHT
            if (score > bestScore || score == bestScore && tie > bestTie) {
                bestIndex = index
                bestScore = score
                bestTie = tie
            }
        }


        return bestIndex
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
