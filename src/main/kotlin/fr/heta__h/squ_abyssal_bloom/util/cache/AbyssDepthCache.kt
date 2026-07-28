package fr.heta__h.squ_abyssal_bloom.util.cache

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.level.LevelEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object AbyssDepthCache {
    private var cachedLevel: Level? = null
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
            val inner = ModUtilities.smoothstep(smoothedDepthFactor)
            return inner * inner
        }

    val rawPhysicalDepth: Double get() = cachedPhysicalDepth

    fun clear() {
        cachedLevel = null
        lastFrameTime = 0L
        cachedRawDepthFactor = 0.0
        smoothedDepthFactor = 0.0
        cachedPhysicalDepth = 0.0
        smoothedPhysicalDepth = 0.0
        cachedLampInfluence = 0.0
        smoothedLampInfluence = 0.0
        isInitialized = false
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level is ClientLevel) clear()
    }

    fun refreshIfNeeded(level: Level, camPos: BlockPos) {
        if (cachedLevel !== level) {
            clear()
            cachedLevel = level
        }

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
            cachedLampInfluence = ModUtilities.getCombinedLampInfluence(entity, level, camPos, 16.0, 1.0)
            smoothedLampInfluence = cachedLampInfluence
            isInitialized = true
            return
        }

        val targetFactor = smoothedDepthFactor + ((cachedRawDepthFactor - smoothedDepthFactor) * (2.0 * dt))
        smoothedDepthFactor = targetFactor.coerceIn(0.0, 1.0)

        smoothedPhysicalDepth += (cachedPhysicalDepth - smoothedPhysicalDepth) * (2.0 * dt)
        smoothedLampInfluence += (cachedLampInfluence - smoothedLampInfluence) * (2.0 * dt)

        val currentTick = level.gameTime
        if (currentTick % 4L != 0L) return

        val physicalDepth = ModUtilities.getDepth(level, camPos)
        cachedPhysicalDepth = physicalDepth
        cachedRawDepthFactor = ModUtilities.getDepthFactor(physicalDepth)

        val entity = mc.cameraEntity as? LivingEntity
        cachedLampInfluence = ModUtilities.getCombinedLampInfluence(entity, level, camPos, 16.0, 1.0)
    }
}
