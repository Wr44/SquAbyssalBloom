package fr.heta__h.squ_abyssal_bloom.util.cache

import it.unimi.dsi.fastutil.longs.Long2LongMap
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectIterator
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap


object SurfaceHeightCache {

    private const val MIN_WATER_DEPTH = 2f
    private const val MAX_THIN_OBSTRUCTION_THICKNESS = 4

    private const val TTL_NEAR = 20L
    private const val TTL_FAR = 80L
    private const val NEAR_THRESHOLD_SQ = 64f * 64f

    private val cache = Long2LongOpenHashMap(8192).apply {
        defaultReturnValue(-1L)
    }

    private var cleanupIterator: ObjectIterator<Long2LongMap.Entry>? = null

    private val mutPos = BlockPos.MutableBlockPos()

    private fun key(x: Int, z: Int): Long =
        (x.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL)

    private fun pack(surfaceY: Int, floorY: Int, isValid: Boolean, tick: Long): Long {
        val sy = (surfaceY + 128).toLong() and 0xFFFL
        val fy = (floorY + 128).toLong() and 0xFFFL
        val v = if (isValid) 1L else 0L
        val t = tick and 0x7FFFFFFFFL
        return sy or (fy shl 12) or (v shl 24) or (t shl 25)
    }

    fun unpackSurfaceY(data: Long): Int = (data and 0xFFFL).toInt() - 128
    fun unpackFloorY(data: Long): Int = ((data shr 12) and 0xFFFL).toInt() - 128
    fun unpackIsValidWater(data: Long): Boolean = ((data shr 24) and 1L) == 1L
    private fun unpackTick(data: Long): Long = (data ushr 25)


    fun tick(gameTick: Long, camX: Int, camZ: Int) {
        if (cache.isEmpty()) return

        var steps = 50
        while (steps-- > 0) {
            if (cleanupIterator == null || !cleanupIterator!!.hasNext()) {
                cleanupIterator = cache.long2LongEntrySet().iterator()
                if (!cleanupIterator!!.hasNext()) break
            }

            val entry = cleanupIterator!!.next()
            val key = entry.longKey
            val data = entry.longValue

            val age = gameTick - unpackTick(data)

            val cellX = (key shr 32).toInt()
            val cellZ = key.toInt()
            val dx = (cellX - camX).toFloat()
            val dz = (cellZ - camZ).toFloat()
            val distSq = dx * dx + dz * dz

            val ttl = if (distSq < NEAR_THRESHOLD_SQ) TTL_NEAR else TTL_FAR

            if (age > ttl) {
                cleanupIterator!!.remove()
            }
        }
    }

    fun getOrCompute(level: Level, cx: Int, cz: Int, step: Int, gameTick: Long, camX: Int, camZ: Int): Long {
        val k = key(cx, cz)

        val existing = cache.get(k)
        if (existing != -1L) {
            val age = gameTick - unpackTick(existing)
            val dx = (cx - camX).toFloat()
            val dz = (cz - camZ).toFloat()
            val distSq = dx * dx + dz * dz
            val ttl = if (distSq < NEAR_THRESHOLD_SQ) TTL_NEAR else TTL_FAR
            if (age <= ttl) return existing
        }

        if (!level.hasChunk(cx shr 4, cz shr 4)) {
            return pack(-1, 0, false, gameTick - TTL_FAR - 1)
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
            var floorScanY = surfaceY
            floorScan@ while (floorScanY > level.minY) {
                mutPos.set(cx, floorScanY, cz)
                val state = level.getBlockState(mutPos)

                if (!state.fluidState.`is`(FluidTags.WATER) && state.blocksMotion()) {
                    val peekLimit = maxOf(level.minY, floorScanY - MAX_THIN_OBSTRUCTION_THICKNESS)
                    for (py in (floorScanY - 1) downTo peekLimit) {
                        mutPos.set(cx, py, cz)
                        if (level.getFluidState(mutPos).`is`(FluidTags.WATER)) {
                            floorScanY = py
                            continue@floorScan
                        }
                    }

                    localFloor = floorScanY
                    break
                }

                floorScanY--
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

        val packedData = pack(finalSurfaceY, finalFloorY, isValid, gameTick)
        cache.put(k, packedData)
        return packedData
    }
}