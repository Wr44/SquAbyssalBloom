package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

object AbyssDepthCache {
    private var lastTickCount: Int = -1
    private var cachedRawDepthFactor: Double = 0.0
    private var cachedIsLargeBody: Boolean = false
    private var smoothedDepthFactor: Double = 0.0

    val rawDepthFactor: Double get() = cachedRawDepthFactor
    val isLargeBody: Boolean get() = cachedIsLargeBody
    val displayedDepthFactor: Double get() = smoothedDepthFactor

    fun refreshIfNeeded(level: Level, camPos: BlockPos) {
        val currentTick = Minecraft.getInstance().level?.gameTime?.toInt() ?: return
        if (currentTick == lastTickCount) return
        lastTickCount = currentTick

        cachedIsLargeBody = ModUtilities.isLargeBodyWater(level, camPos, 5)
        if (cachedIsLargeBody) {
            cachedRawDepthFactor = ModUtilities.smoothDepthGaussian(level, camPos, sigma = 1.5).coerceAtLeast(0.0)
        } else {
            cachedRawDepthFactor = 0.0
        }
        smoothedDepthFactor += (cachedRawDepthFactor - smoothedDepthFactor) * 0.05
    }
}
