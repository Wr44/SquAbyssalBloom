package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.cache

import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level

object AbyssDepthCache {
    private var lastFrameTime: Long = 0L
    private var cachedRawDepthFactor: Double = 0.0
    private var smoothedDepthFactor: Double = 0.0
    private var cachedPhysicalDepth: Double = 0.0
    private var smoothedPhysicalDepth: Double = 0.0
    private var cachedLampInfluence: Double = 0.0
    private var smoothedLampInfluence: Double = 0.0
    private var isInitialized: Boolean = false

    val displayedDepthFactor: Double
        get() {
            val s = smoothedDepthFactor
            val s2 = s * s
            val s3 = s2 * s
            val inner = 3.0 * s2 - 2.0 * s3
            return inner * inner
        }

    val rawPhysicalDepth: Double get() = cachedPhysicalDepth
    val displayedLampInfluence: Double get() = smoothedLampInfluence

    fun refreshIfNeeded(level: Level, camPos: BlockPos) {
        val now = System.nanoTime()
        val elapsedNanos = now - lastFrameTime
        if (elapsedNanos < 1_000_000L && lastFrameTime != 0L) return
        val dt = if (lastFrameTime == 0L) 0.016 else elapsedNanos / 1_000_000_000.0
        lastFrameTime = now

        val mc = Minecraft.getInstance()

        if (!isInitialized) {
            val physicalDepth = ModUtilities.getDepth(level, camPos)
            cachedPhysicalDepth = physicalDepth
            smoothedPhysicalDepth = physicalDepth

            cachedRawDepthFactor = ModUtilities.getDepthFactor(physicalDepth)
            smoothedDepthFactor = cachedRawDepthFactor

            val entity = mc.cameraEntity as? LivingEntity
            if (entity != null) {
                cachedLampInfluence = maxOf(
                    ModUtilities.getRiderLampInfluence(entity),
                    ModUtilities.getNautilusLampInfluence(level, camPos, 16.0, 1.0)
                )
                smoothedLampInfluence = cachedLampInfluence
            }
            isInitialized = true
            return
        }

        val targetFactor = smoothedDepthFactor + ((cachedRawDepthFactor - smoothedDepthFactor) * (2.0 * dt))
        smoothedDepthFactor = targetFactor.coerceIn(0.0, 1.0)

        smoothedPhysicalDepth += (cachedPhysicalDepth - smoothedPhysicalDepth) * (2.0 * dt)
        smoothedLampInfluence += (cachedLampInfluence - smoothedLampInfluence) * (2.0 * dt)

        val currentTick = mc.level?.gameTime ?: return
        if (currentTick % 4L != 0L) return

        val physicalDepth = ModUtilities.getDepth(level, camPos)
        cachedPhysicalDepth = physicalDepth
        cachedRawDepthFactor = ModUtilities.getDepthFactor(physicalDepth)

        val entity = mc.cameraEntity as? LivingEntity
        if (entity != null) {
            cachedLampInfluence = maxOf(
                ModUtilities.getRiderLampInfluence(entity),
                ModUtilities.getNautilusLampInfluence(level, camPos, 16.0, 1.0)
            )
        } else {
            cachedLampInfluence = 0.0
        }
    }
}
