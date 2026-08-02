package fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.world.level.levelgen.RandomSupport
import kotlin.math.floor
import kotlin.math.sqrt

class BioluminescentCellularNoise(
    private val seed: Long,
    private val cellSize: Double
) {
    companion object {
        const val X_SALT = 0x6A09E667F3BCC909L
        const val Z_SALT = 0x3C6EF372FE94F82AL
    }

    init {
        require(cellSize > 0.0)
    }

    fun sample(worldX: Double, worldZ: Double): Double {
        val gridX = floor(worldX / cellSize).toInt()
        val gridZ = floor(worldZ / cellSize).toInt()
        var firstDistanceSqr = Double.POSITIVE_INFINITY
        for (offsetZ in -1..1) {
            for (offsetX in -1..1) {
                val cellX = gridX + offsetX
                val cellZ = gridZ + offsetZ
                val featureX = (cellX + featureOffset(cellX, cellZ, X_SALT)) * cellSize
                val featureZ = (cellZ + featureOffset(cellX, cellZ, Z_SALT)) * cellSize
                val deltaX = worldX - featureX
                val deltaZ = worldZ - featureZ
                val distanceSqr = deltaX * deltaX + deltaZ * deltaZ
                if (distanceSqr < firstDistanceSqr) {
                    firstDistanceSqr = distanceSqr
                }
            }
        }
        val first = sqrt(firstDistanceSqr) / cellSize
        return 1.0 - ModUtilities.smooth(0.08, 0.82, first)
    }

    private fun featureOffset(cellX: Int, cellZ: Int, salt: Long): Double {
        val mixed = RandomSupport.mixStafford13(
            seed xor salt xor ModUtilities.horizontalPositionKey(cellX, cellZ)
        )
        return 0.14 + ModUtilities.stableUnitValue(mixed) * 0.72
    }

}
