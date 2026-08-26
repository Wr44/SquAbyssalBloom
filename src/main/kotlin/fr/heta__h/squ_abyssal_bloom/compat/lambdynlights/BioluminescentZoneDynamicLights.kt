package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.bioluminescence_wave.BioluminescentSurfaceLight
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentCompensation.VISIBILITY_EPSILON
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import net.minecraft.client.Minecraft
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.floor

object BioluminescentZoneDynamicLights : AbstractDynamicLightCompat() {

    private const val UPDATE_INTERVAL_TICKS = 4L
    private const val PULSE_UPDATE_INTERVAL_TICKS = 1L
    private const val LIGHT_BUILDS_PER_TICK = 4
    private const val ACTIVE_CHUNK_RADIUS = 3
    private const val ACTIVE_BASE_FACTOR = 0.60
    private const val ACTIVE_WAVE_BOOST_FACTOR = 0.20
    private const val ACTIVE_WAVE_MIN_FACTOR = 0.60
    private const val INACTIVE_WAVE_FACTOR = 0.45
    private const val BLOOM_ACTIVE_FACTOR = 0.85
    private const val BLOOM_INACTIVE_FACTOR = 0.55
    private const val BLOOM_GLOW_RADIUS = 4.0

    private data class PendingChunk(val chunkX: Int, val chunkZ: Int) {
        val key: Long = chunkKey(chunkX, chunkZ)
    }

    private data class ChunkOffset(val x: Int, val z: Int)

    private class ZoneLightState(
        val minChunkX: Int,
        val maxChunkX: Int,
        val minChunkZ: Int,
        val maxChunkZ: Int
    ) {
        val pendingChunks = ArrayDeque<PendingChunk>()
        val desiredChunkKeys = HashSet<Long>()
        val activeLights = HashMap<Long, BioluminescentSurfaceLight>()
        val cachedLights = HashMap<Long, BioluminescentSurfaceLight>()
        val emptyChunkKeys = HashSet<Long>()
        var priorityChunkX = Int.MIN_VALUE
        var priorityChunkZ = Int.MIN_VALUE
    }

    private val activeChunkOffsets = buildList {
        for (offsetX in -ACTIVE_CHUNK_RADIUS..ACTIVE_CHUNK_RADIUS) {
            for (offsetZ in -ACTIVE_CHUNK_RADIUS..ACTIVE_CHUNK_RADIUS) {
                add(ChunkOffset(offsetX, offsetZ))
            }
        }
    }.sortedWith(
        compareBy<ChunkOffset> { offset -> offset.x * offset.x + offset.z * offset.z }
            .thenBy(ChunkOffset::x)
            .thenBy(ChunkOffset::z)
    )

    private val lightsByZone = HashMap<UUID, ZoneLightState>()
    private var tickCounter = 0L

    fun onZoneReady(zone: BioluminescentZone) {
        if (!isInitialized || !ModConfig.enableDynamicLights || lightsByZone.containsKey(zone.eventId)) return
        val data = zone.spatialData ?: return
        val bounds = data.domain.bounds
        val minChunkX = bounds.minX shr 4
        val maxChunkX = bounds.maxX shr 4
        val minChunkZ = bounds.minZ shr 4
        val maxChunkZ = bounds.maxZ shr 4

        val state = ZoneLightState(minChunkX, maxChunkX, minChunkZ, maxChunkZ)
        lightsByZone[zone.eventId] = state
        refreshActiveWindow(zone, state)
    }

    fun onZoneRemoved(zone: BioluminescentZone) {
        val state = lightsByZone.remove(zone.eventId) ?: return
        state.cachedLights.values.forEach(BioluminescentSurfaceLight::markRemoved)
        state.activeLights.values.forEach {
            removeDynamicLight(it)
        }
    }

    fun updateDynamicState(zones: Collection<BioluminescentZone>, gameTime: Long) {
        if (!ModConfig.enableDynamicLights) {
            if (lightsByZone.isNotEmpty()) clear()
            return
        }
        if (!isInitialized) return
        zones.forEach { zone ->
            if (zone.eventId !in lightsByZone) onZoneReady(zone)
            lightsByZone[zone.eventId]?.let { state -> refreshActiveWindow(zone, state) }
        }
        if (lightsByZone.isEmpty()) return

        var remainingBuilds = LIGHT_BUILDS_PER_TICK
        for (zone in zones) {
            if (remainingBuilds <= 0) break
            val state = lightsByZone[zone.eventId] ?: continue
            remainingBuilds -= buildLightsStep(zone, state, remainingBuilds)
        }

        tickCounter++
        val renderGameTime = gameTime.toDouble()
        for (zone in zones) {
            val state = lightsByZone[zone.eventId] ?: continue
            val updateInterval = if (zone.activeBloomPulseCount > 0) {
                PULSE_UPDATE_INTERVAL_TICKS
            } else {
                UPDATE_INTERVAL_TICKS
            }
            if (tickCounter % updateInterval != 0L) continue
            updateZoneLights(
                zone,
                state.activeLights.values,
                renderGameTime,
                updateInterval / 20.0
            )
        }
    }

    fun registeredLightCount(zone: BioluminescentZone): Int =
        lightsByZone[zone.eventId]?.activeLights?.size ?: 0

    fun pendingLightBuildCount(zone: BioluminescentZone): Int =
        lightsByZone[zone.eventId]?.pendingChunks?.size ?: 0

    fun totalRegisteredLightCount(): Int = lightsByZone.values.sumOf { it.activeLights.size }

    fun clear() {
        lightsByZone.values.forEach { state ->
            state.cachedLights.values.forEach(BioluminescentSurfaceLight::markRemoved)
            state.activeLights.values.forEach {
                removeDynamicLight(it)
            }
        }
        lightsByZone.clear()
        tickCounter = 0L
    }

    private fun refreshActiveWindow(zone: BioluminescentZone, state: ZoneLightState) {
        val player = Minecraft.getInstance().player
        val priorityChunkX = floor(player?.x ?: (zone.anchor.x + 0.5)).toInt() shr 4
        val priorityChunkZ = floor(player?.z ?: (zone.anchor.z + 0.5)).toInt() shr 4
        if (priorityChunkX == state.priorityChunkX && priorityChunkZ == state.priorityChunkZ) return
        state.priorityChunkX = priorityChunkX
        state.priorityChunkZ = priorityChunkZ

        state.desiredChunkKeys.clear()
        for (offset in activeChunkOffsets) {
            val chunkX = priorityChunkX + offset.x
            val chunkZ = priorityChunkZ + offset.z
            if (chunkX !in state.minChunkX..state.maxChunkX || chunkZ !in state.minChunkZ..state.maxChunkZ) {
                continue
            }
            state.desiredChunkKeys.add(chunkKey(chunkX, chunkZ))
        }

        val activeIterator = state.activeLights.entries.iterator()
        while (activeIterator.hasNext()) {
            val entry = activeIterator.next()
            if (entry.key in state.desiredChunkKeys) continue
            entry.value.markRemoved()
            removeDynamicLight(entry.value)
            activeIterator.remove()
        }

        state.pendingChunks.clear()
        for (offset in activeChunkOffsets) {
            val chunkX = priorityChunkX + offset.x
            val chunkZ = priorityChunkZ + offset.z
            val key = chunkKey(chunkX, chunkZ)
            if (key !in state.desiredChunkKeys || key in state.activeLights || key in state.emptyChunkKeys) continue
            state.pendingChunks.addLast(PendingChunk(chunkX, chunkZ))
        }
    }

    private fun buildLightsStep(
        zone: BioluminescentZone,
        state: ZoneLightState,
        budget: Int
    ): Int {
        val data = zone.spatialData ?: return 0
        var processed = 0
        while (processed < budget && state.pendingChunks.isNotEmpty()) {
            val chunk = state.pendingChunks.removeFirst()
            processed++
            if (chunk.key !in state.desiredChunkKeys || chunk.key in state.activeLights) continue
            val cachedLight = state.cachedLights[chunk.key]
            val light = if (cachedLight != null) {
                cachedLight.markActive()
                cachedLight
            } else {
                BioluminescentSurfaceLight.build(
                    chunk.chunkX shl 4,
                    chunk.chunkZ shl 4,
                    data.domain,
                    data.emissionField
                )?.also { built -> state.cachedLights[chunk.key] = built }
                    ?: run {
                        state.emptyChunkKeys.add(chunk.key)
                        continue
                    }
            }
            state.activeLights[chunk.key] = light
            addDynamicLight(light)
        }
        return processed
    }

    private fun updateZoneLights(
        zone: BioluminescentZone,
        lights: Collection<BioluminescentSurfaceLight>,
        renderGameTime: Double,
        smoothingDelta: Double
    ) {
        val lifecycle = zone.lifecycleIntensityAt(renderGameTime)
        val isActive = zone.activity == BioluminescentZoneActivity.ACTIVE
        val dormant = lifecycle <= 0.0f || (
            !isActive &&
                zone.movementWaveVisibilityStrength() <= VISIBILITY_EPSILON &&
                zone.bloomPulseVisibilityStrength() <= VISIBILITY_EPSILON
            )
        if (dormant) {
            lights.forEach { it.setIntensityFactor(0.0, smoothingDelta) }
            return
        }

        for (light in lights) {
            val minX = light.chunkOriginX.toDouble()
            val minZ = light.chunkOriginZ.toDouble()
            val maxX = minX + BioluminescentSurfaceLight.CHUNK_SIZE
            val maxZ = minZ + BioluminescentSurfaceLight.CHUNK_SIZE
            val waveIntensity = if (zone.movementWavesAffect(minX, minZ, maxX, maxZ, renderGameTime)) {
                zone.movementWaveIntensityAt((minX + maxX) * 0.5, (minZ + maxZ) * 0.5, renderGameTime)
            } else {
                0.0f
            }
            val bloomIntensity = bloomIntensityIn(zone, minX, minZ, maxX, maxZ, renderGameTime)

            val factor = if (isActive) {
                val base = lifecycle * ACTIVE_BASE_FACTOR
                val boosted = base + lifecycle * waveIntensity * ACTIVE_WAVE_BOOST_FACTOR
                val waveFloor = lifecycle * waveIntensity * ACTIVE_WAVE_MIN_FACTOR
                val bloomFloor = lifecycle * bloomIntensity * BLOOM_ACTIVE_FACTOR
                maxOf(boosted, waveFloor, bloomFloor) * ModConfig.dynamicLightsBioluminescenceActiveIntensity
            } else {
                val waveFactor = lifecycle * waveIntensity * INACTIVE_WAVE_FACTOR
                val bloomFactor = lifecycle * bloomIntensity * BLOOM_INACTIVE_FACTOR
                maxOf(waveFactor, bloomFactor) * ModConfig.dynamicLightsBioluminescenceInactiveIntensity
            }
            light.setIntensityFactor(factor, smoothingDelta)
        }
    }

    private fun bloomIntensityIn(
        zone: BioluminescentZone,
        minX: Double,
        minZ: Double,
        maxX: Double,
        maxZ: Double,
        renderGameTime: Double
    ): Float {
        if (!ModConfig.enableBioluminescenceBloomRendering) return 0.0f
        val pulseIntensity = zone.bloomPulseMaxIntensityIn(minX, minZ, maxX, maxZ, renderGameTime)

        var glowIntensity = 0.0f
        for (bloom in zone.blooms.values) {
            if (bloom.lifecycle != PlanktonBloomLifecycle.ACTIVE) continue
            val geometry = bloom.geometry ?: continue
            if (geometry.centerX < minX - BLOOM_GLOW_RADIUS || geometry.centerX > maxX + BLOOM_GLOW_RADIUS) continue
            if (geometry.centerZ < minZ - BLOOM_GLOW_RADIUS || geometry.centerZ > maxZ + BLOOM_GLOW_RADIUS) continue
            val fade = bloom.appearanceFadeAt(renderGameTime, zone.serverGameTimeOffset) *
                bloom.terminalFadeAt(renderGameTime) *
                bloom.pulseIntensityAt(renderGameTime, zone.serverGameTimeOffset)
            glowIntensity = maxOf(glowIntensity, fade)
        }

        return maxOf(pulseIntensity, glowIntensity)
    }

    private fun chunkKey(chunkX: Int, chunkZ: Int): Long =
        (chunkX.toLong() shl 32) xor (chunkZ.toLong() and 0xFFFFFFFFL)
}
