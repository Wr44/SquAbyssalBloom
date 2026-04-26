package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.cache

import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap


object SurfaceHeightCache {

    private const val MIN_WATER_DEPTH = 2f

    
    private const val TTL_NEAR = 20L    
    private const val TTL_FAR = 80L     
    private const val NEAR_THRESHOLD_SQ = 64f * 64f

    private const val CLEANUP_INTERVAL = 60L

    class CellData(
        val waterSurfaceY: Int,
        val floorY: Int,
        val isValidWater: Boolean,
        val createdTick: Long
    )

    private val cache = HashMap<Long, CellData>(8192)
    private var lastCleanupTick = 0L

    
    private val mutPos = BlockPos.MutableBlockPos()

    private fun key(x: Int, z: Int): Long =
        (x.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL)

    
    fun tick(gameTick: Long, camX: Int, camZ: Int) {
        if (gameTick - lastCleanupTick < CLEANUP_INTERVAL) return
        lastCleanupTick = gameTick

        val iter = cache.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val cell = entry.value
            val age = gameTick - cell.createdTick

            
            val cellX = (entry.key shr 32).toInt()
            val cellZ = entry.key.toInt()
            val dx = (cellX - camX).toFloat()
            val dz = (cellZ - camZ).toFloat()
            val distSq = dx * dx + dz * dz

            val ttl = if (distSq < NEAR_THRESHOLD_SQ) TTL_NEAR else TTL_FAR

            if (age > ttl) {
                iter.remove()
            }
        }
    }


    fun getOrCompute(level: Level, cx: Int, cz: Int, step: Int, gameTick: Long, camX: Int, camZ: Int): CellData {
        val k = key(cx, cz)
        val existing = cache[k]
        if (existing != null) {
            val age = gameTick - existing.createdTick
            val dx = (cx - camX).toFloat()
            val dz = (cz - camZ).toFloat()
            val distSq = dx * dx + dz * dz
            val ttl = if (distSq < NEAR_THRESHOLD_SQ) TTL_NEAR else TTL_FAR
            if (age <= ttl) return existing
        }

        var finalSurfaceY = -1
        var finalFloorY = 0
        var isValid = false

        var searchY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz)

        while (searchY > level.minY) {
            mutPos.set(cx, searchY - 1, cz)
            val isWater = level.getFluidState(mutPos).`is`(FluidTags.WATER)

            if (!isWater) {
                var foundWater = false
                for (y in searchY - 2 downTo level.minY) {
                    mutPos.set(cx, y, cz)
                    if (level.getFluidState(mutPos).`is`(FluidTags.WATER)) {
                        searchY = y + 1 
                        foundWater = true
                        break
                    }
                }
                if (!foundWater) break
                continue
            }

            val surfaceY = searchY - 1

            var localFloor = surfaceY
            for (y in surfaceY downTo level.minY) {
                mutPos.set(cx, y, cz)
                val state = level.getBlockState(mutPos)
                if (!state.fluidState.`is`(FluidTags.WATER) && state.blocksMotion()) {
                    localFloor = y
                    break
                }
            }

            val deepEnough = (surfaceY - localFloor) >= MIN_WATER_DEPTH

            if (deepEnough) {
                finalSurfaceY = surfaceY
                finalFloorY = localFloor
                isValid = true
                break
            } else {
                searchY = localFloor
            }
        }

        val data = CellData(finalSurfaceY, finalFloorY, isValid, gameTick)
        cache[k] = data
        return data
    }
}
