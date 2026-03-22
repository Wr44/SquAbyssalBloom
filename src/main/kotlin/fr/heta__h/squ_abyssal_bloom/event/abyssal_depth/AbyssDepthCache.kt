package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import kotlin.math.pow

object AbyssDepthCache {
    private var lastFrameTime: Long = 0L

    private var cachedRawDepthFactor: Double = 0.0
    private var smoothedDepthFactor: Double = 0.0


    private var cachedPhysicalDepth: Double = 0.0
    private var smoothedPhysicalDepth: Double = 0.0

    val displayedDepthFactor: Double
        get() = (3*smoothedDepthFactor.pow(2)-2*smoothedDepthFactor.pow(3)).pow(2)
    val rawPhysicalDepth: Double
        get() = cachedPhysicalDepth

    fun refreshIfNeeded(level: Level, camPos: BlockPos) {
        val now = System.currentTimeMillis()
        val dt = if (lastFrameTime == 0L) 0.016 else (now - lastFrameTime) / 1000.0
        lastFrameTime = now

        val targetFactor = smoothedDepthFactor + ((cachedRawDepthFactor - smoothedDepthFactor) * (2.0 * dt))
        smoothedDepthFactor = targetFactor.coerceIn(0.0, 1.0)

        smoothedPhysicalDepth += (cachedPhysicalDepth - smoothedPhysicalDepth) * (2.0 * dt)

        val currentTick = Minecraft.getInstance().level?.gameTime?.toInt() ?: return
        if (currentTick % 4 != 0) return

        val physicalDepth = ModUtilities.findWaterSurface(level, camPos).toDouble()
        cachedPhysicalDepth = physicalDepth
        cachedRawDepthFactor = ModUtilities.getDepthFactor(physicalDepth)
    }

    fun reset() {
        cachedRawDepthFactor = 0.0
        smoothedDepthFactor = 0.0
        cachedPhysicalDepth = 0.0
        smoothedPhysicalDepth = 0.0
        lastFrameTime = 0L
    }
}