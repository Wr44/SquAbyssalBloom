package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.bioluminescence_wave.BioluminescentSurfaceLight
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence_wave.BioluminescentCompensation.VISIBILITY_EPSILON
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle

object BioluminescentZoneDynamicLights : AbstractDynamicLightCompat() {

    private const val UPDATE_INTERVAL_TICKS = 1L
    private const val SMOOTHING_DELTA = UPDATE_INTERVAL_TICKS / 20.0
    private const val ACTIVE_BASE_FACTOR = 0.60
    private const val ACTIVE_WAVE_BOOST_FACTOR = 0.20
    private const val ACTIVE_WAVE_MIN_FACTOR = 0.60
    private const val INACTIVE_WAVE_FACTOR = 0.45
    private const val BLOOM_ACTIVE_FACTOR = 0.85
    private const val BLOOM_INACTIVE_FACTOR = 0.55
    private const val BLOOM_GLOW_RADIUS = 4.0

    private val lightsByZone = HashMap<java.util.UUID, List<BioluminescentSurfaceLight>>()
    private var tickCounter = 0L

    fun onZoneReady(zone: BioluminescentZone) {
        if (!isInitialized) return
        val data = zone.spatialData ?: return
        val bounds = data.domain.bounds
        val minChunkX = bounds.minX shr 4
        val maxChunkX = bounds.maxX shr 4
        val minChunkZ = bounds.minZ shr 4
        val maxChunkZ = bounds.maxZ shr 4

        val lights = ArrayList<BioluminescentSurfaceLight>()
        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                val light = BioluminescentSurfaceLight.build(
                    chunkX shl 4,
                    chunkZ shl 4,
                    data.domain,
                    data.emissionField
                ) ?: continue
                lights.add(light)
                addDynamicLight(light)
            }
        }
        lightsByZone[zone.eventId] = lights
    }

    fun onZoneRemoved(zone: BioluminescentZone) {
        val lights = lightsByZone.remove(zone.eventId) ?: return
        lights.forEach {
            it.markRemoved()
            removeDynamicLight(it)
        }
    }

    fun updateDynamicState(zones: Collection<BioluminescentZone>, gameTime: Long) {
        if (!isInitialized || lightsByZone.isEmpty()) return
        tickCounter++
        if (tickCounter % UPDATE_INTERVAL_TICKS != 0L) return

        val renderGameTime = gameTime.toDouble()
        for (zone in zones) {
            val lights = lightsByZone[zone.eventId] ?: continue
            updateZoneLights(zone, lights, renderGameTime)
        }
    }

    fun registeredLightCount(zone: BioluminescentZone): Int = lightsByZone[zone.eventId]?.size ?: 0

    fun totalRegisteredLightCount(): Int = lightsByZone.values.sumOf { it.size }

    fun clear() {
        lightsByZone.values.forEach { lights ->
            lights.forEach {
                it.markRemoved()
                removeDynamicLight(it)
            }
        }
        lightsByZone.clear()
    }

    private fun updateZoneLights(
        zone: BioluminescentZone,
        lights: List<BioluminescentSurfaceLight>,
        renderGameTime: Double
    ) {
        val lifecycle = zone.lifecycleIntensityAt(renderGameTime)
        val isActive = zone.activity == BioluminescentZoneActivity.ACTIVE
        val dormant = lifecycle <= 0.0f || (
            !isActive &&
                zone.movementWaveVisibilityStrength() <= VISIBILITY_EPSILON &&
                zone.bloomPulseVisibilityStrength() <= VISIBILITY_EPSILON
            )
        if (dormant) {
            lights.forEach { it.setIntensityFactor(0.0, SMOOTHING_DELTA) }
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
            light.setIntensityFactor(factor, SMOOTHING_DELTA)
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
}
