package fr.heta__h.squ_abyssal_bloom.render.abyssal_depth

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
    private const val REFRESH_CYCLE_TICKS = 5L

    private var cachedLevel: Level? = null
    private var lastFrameTime: Long = 0L
    private var cachedRawDepthFactor: Double = 0.0
    private var smoothedDepthFactor: Double = 0.0
    private var cachedPhysicalDepth: Double = 0.0
    private var smoothedPhysicalDepth: Double = 0.0
    private var cachedAmbientFogRepellerInfluence: Double = 0.0
    private var smoothedAmbientFogRepellerInfluence: Double = 0.0
    private var cachedFogPlaneRepellerInfluence: Double = 0.0
    private var smoothedFogPlaneRepellerInfluence: Double = 0.0
    private var isInitialized: Boolean = false

    val displayedDepthFactor: Double
        get() {
            val inner = ModUtilities.smooth(smoothedDepthFactor)
            return inner * inner
        }

    val rawPhysicalDepth: Double get() = cachedPhysicalDepth

    val displayedAmbientFogRepellerInfluence: Double get() = smoothedAmbientFogRepellerInfluence
    val displayedFogPlaneRepellerInfluence: Double get() = smoothedFogPlaneRepellerInfluence

    fun clear() {
        cachedLevel = null
        lastFrameTime = 0L
        cachedRawDepthFactor = 0.0
        smoothedDepthFactor = 0.0
        cachedPhysicalDepth = 0.0
        smoothedPhysicalDepth = 0.0
        cachedAmbientFogRepellerInfluence = 0.0
        smoothedAmbientFogRepellerInfluence = 0.0
        cachedFogPlaneRepellerInfluence = 0.0
        smoothedFogPlaneRepellerInfluence = 0.0
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
            cachedAmbientFogRepellerInfluence = ModUtilities.getFogRepellerInfluence(entity, level, camPos, 16.0, 1.0)
            smoothedAmbientFogRepellerInfluence = cachedAmbientFogRepellerInfluence
            cachedFogPlaneRepellerInfluence = ModUtilities.getFogRepellerInfluence(entity, level, camPos, 32.0, 1.5)
            smoothedFogPlaneRepellerInfluence = cachedFogPlaneRepellerInfluence
            isInitialized = true
            return
        }

        smoothedDepthFactor = ModUtilities.smoothTowards(
            smoothedDepthFactor,
            cachedRawDepthFactor,
            dt
        ).coerceIn(0.0, 1.0)
        smoothedPhysicalDepth = ModUtilities.smoothTowards(
            smoothedPhysicalDepth,
            cachedPhysicalDepth,
            dt
        )
        smoothedAmbientFogRepellerInfluence = ModUtilities.smoothTowards(smoothedAmbientFogRepellerInfluence, cachedAmbientFogRepellerInfluence, dt)
        smoothedFogPlaneRepellerInfluence = ModUtilities.smoothTowards(smoothedFogPlaneRepellerInfluence, cachedFogPlaneRepellerInfluence, dt)

        val currentTick = level.gameTime

        when ((currentTick % REFRESH_CYCLE_TICKS).toInt()) {
            0 -> {
                val physicalDepth = ModUtilities.getDepth(level, camPos)
                cachedPhysicalDepth = physicalDepth
                cachedRawDepthFactor = ModUtilities.getDepthFactor(physicalDepth)
            }
            2 -> {
                val entity = mc.cameraEntity as? LivingEntity
                cachedAmbientFogRepellerInfluence = ModUtilities.getFogRepellerInfluence(entity, level, camPos, 16.0, 1.0)
            }
            4 -> {
                val entity = mc.cameraEntity as? LivingEntity
                cachedFogPlaneRepellerInfluence = ModUtilities.getFogRepellerInfluence(entity, level, camPos, 32.0, 1.5)
            }
        }
    }
}
