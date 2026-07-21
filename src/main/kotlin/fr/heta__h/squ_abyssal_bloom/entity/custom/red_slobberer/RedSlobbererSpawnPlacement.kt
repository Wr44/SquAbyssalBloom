package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer

import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.SpawnPlacementType
import net.minecraft.world.level.LevelReader

object RedSlobbererSpawnPlacement : SpawnPlacementType {

    override fun adjustSpawnPosition(level: LevelReader, originalPos: BlockPos): BlockPos {
        for (dx in FOOTPRINT_OFFSETS) {
            for (dz in FOOTPRINT_OFFSETS) {
                val sample = originalPos.offset(dx, 0, dz)
                if (!level.hasChunkAt(sample)) return originalPos
                val state = level.getBlockState(sample)
                if (!state.getCollisionShape(level, sample).isEmpty) {
                    return originalPos.above()
                }
            }
        }

        return originalPos
    }

    override fun isSpawnPositionOk(
        level: LevelReader,
        pos: BlockPos,
        entityType: EntityType<*>?
    ): Boolean {
        if (!level.worldBorder.isWithinBounds(pos)) return false
        if (!level.getFluidState(pos).`is`(FluidTags.WATER)) return false
        if (!level.getFluidState(pos.above()).`is`(FluidTags.WATER)) return false
        if (!level.getFluidState(pos.above(2)).`is`(FluidTags.WATER)) return false

        var supportedSamples = 0
        var waterColumns = 0

        for (dx in FOOTPRINT_OFFSETS) {
            for (dz in FOOTPRINT_OFFSETS) {
                val column = pos.offset(dx, 0, dz)
                if (!level.hasChunkAt(column)) return false

                if (
                    level.getFluidState(column).`is`(FluidTags.WATER) &&
                    level.getFluidState(column.above()).`is`(FluidTags.WATER) &&
                    level.getFluidState(column.above(2)).`is`(FluidTags.WATER)
                ) {
                    waterColumns++
                }

                for (depth in 1..MAX_SUPPORT_DEPTH) {
                    val supportPos = column.below(depth)
                    val supportState = level.getBlockState(supportPos)
                    if (!supportState.getCollisionShape(level, supportPos).isEmpty) {
                        supportedSamples++
                        break
                    }
                }
            }
        }

        return waterColumns >= MINIMUM_WATER_COLUMNS && supportedSamples >= MINIMUM_SUPPORT_SAMPLES
    }

    private val FOOTPRINT_OFFSETS = intArrayOf(-2, 0, 2)

    private const val MAX_SUPPORT_DEPTH = 2
    private const val MINIMUM_WATER_COLUMNS = 7
    private const val MINIMUM_SUPPORT_SAMPLES = 3
}
