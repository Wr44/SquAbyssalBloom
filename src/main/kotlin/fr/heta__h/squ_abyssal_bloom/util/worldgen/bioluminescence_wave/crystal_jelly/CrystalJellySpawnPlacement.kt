package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.crystal_jelly

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaterAreaSampler
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

object CrystalJellySpawnPlacement {

    private const val MINIMUM_COLUMN_DEPTH = 3
    private const val SURFACE_CLEARANCE = 1
    private const val COLUMN_ORDER_SALT = 0x2545F4914F6CDD1DL
    private const val CANDIDATE_COLUMNS_PER_JELLY = 6
    private const val MAXIMUM_CANDIDATE_COLUMNS = 256
    private const val JELLIES_PER_SWARM = 5
    private const val SWARM_RADIUS = 10.0

    fun buildPlan(
        level: LevelReader,
        waveSeed: Long,
        area: BioluminescenceWaterAreaSampler.BioluminescenceWaterArea,
        placementRadius: Int,
        targetPopulation: Int
    ): CrystalJellySpawnPlan {
        if (targetPopulation <= 0) return CrystalJellySpawnPlan(0, emptyList(), emptyList())

        val wantedColumns = (targetPopulation * CANDIDATE_COLUMNS_PER_JELLY)
            .coerceAtMost(MAXIMUM_CANDIDATE_COLUMNS)
        val columns = ArrayList<CrystalJellySpawnColumn>(wantedColumns)

        for (cell in orderedCells(area, placementRadius, waveSeed)) {
            if (columns.size >= wantedColumns) break
            columns.add(columnAt(level, cell.waterSurface) ?: continue)
        }

        return CrystalJellySpawnPlan(targetPopulation, columns, selectSwarmCentres(columns, targetPopulation))
    }

    fun pickSpawnPosition(plan: CrystalJellySpawnPlan, random: RandomSource): Vec3? {
        if (plan.columns.isEmpty()) return null

        val column = pickColumn(plan, random)
        val lowestY = column.floorY + 1
        val highestY = column.surfaceY - SURFACE_CLEARANCE
        if (highestY < lowestY) return null

        val spawnY = lowestY + random.nextInt(highestY - lowestY + 1)
        return Vec3(column.x + 0.5, spawnY.toDouble(), column.z + 0.5)
    }

    private fun orderedCells(
        area: BioluminescenceWaterAreaSampler.BioluminescenceWaterArea,
        placementRadius: Int,
        waveSeed: Long
    ): List<BioluminescenceWaterAreaSampler.BioluminescenceWaterAreaCell> {
        return area.cells
            .filter { cell -> cell.geodesicDistance <= placementRadius }
            .sortedBy { cell ->
                RandomSupport.mixStafford13(
                    waveSeed xor
                        ModUtilities.horizontalPositionKey(cell.waterSurface.x, cell.waterSurface.z) xor
                        COLUMN_ORDER_SALT
                )
            }
    }

    private fun columnAt(level: LevelReader, waterSurface: BlockPos): CrystalJellySpawnColumn? {
        val floor = ModUtilities.findLocalWaterFloor(level, waterSurface) ?: return null
        if (waterSurface.y - floor.y + 1 < MINIMUM_COLUMN_DEPTH) return null
        return CrystalJellySpawnColumn(waterSurface.x, waterSurface.z, waterSurface.y, floor.y)
    }

    private fun selectSwarmCentres(
        columns: List<CrystalJellySpawnColumn>,
        targetPopulation: Int
    ): List<CrystalJellySpawnColumn> {
        if (columns.isEmpty()) return emptyList()

        val centreCount = ceil(targetPopulation.toDouble() / JELLIES_PER_SWARM)
            .toInt()
            .coerceIn(1, columns.size)
        val centres = ArrayList<CrystalJellySpawnColumn>(centreCount)
        centres.add(columns.first())

        while (centres.size < centreCount) {
            var farthest: CrystalJellySpawnColumn? = null
            var farthestDistanceSqr = 0.0
            for (column in columns) {
                val nearestSqr = centres.minOf { centre -> horizontalDistanceSqr(centre, column) }
                if (nearestSqr > farthestDistanceSqr) {
                    farthestDistanceSqr = nearestSqr
                    farthest = column
                }
            }
            centres.add(farthest ?: break)
        }

        return centres
    }

    private fun pickColumn(plan: CrystalJellySpawnPlan, random: RandomSource): CrystalJellySpawnColumn {
        if (plan.swarmCentres.isEmpty()) return plan.columns[random.nextInt(plan.columns.size)]

        val centre = plan.swarmCentres[random.nextInt(plan.swarmCentres.size)]
        val swarm = plan.columns.filter { column ->
            horizontalDistanceSqr(centre, column) <= SWARM_RADIUS * SWARM_RADIUS
        }
        if (swarm.isEmpty()) return plan.columns[random.nextInt(plan.columns.size)]
        return swarm[random.nextInt(swarm.size)]
    }

    private fun horizontalDistanceSqr(
        first: CrystalJellySpawnColumn,
        second: CrystalJellySpawnColumn
    ): Double = ModUtilities.horizontalDistanceSqr(
        first.x.toDouble(),
        first.z.toDouble(),
        second.x.toDouble(),
        second.z.toDouble()
    )
}
