package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights

import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.lighting.BioluminescentSurfaceLight
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity

object BioluminescentZoneDynamicLights : AbstractDynamicLightCompat() {
    private const val UPDATE_INTERVAL_TICKS = 4L
    private const val VISIBILITY_EPSILON = 0.01
    private const val ACTIVE_BASE_FACTOR = 0.60
    private const val ACTIVE_WAVE_BOOST_FACTOR = 0.20
    private const val ACTIVE_WAVE_MIN_FACTOR = 0.60
    private const val INACTIVE_WAVE_FACTOR = 0.45

    private val lightsByZone = HashMap<Long, List<BioluminescentSurfaceLight>>()
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
        lightsByZone[zone.zoneSeed] = lights
    }

    fun onZoneRemoved(zone: BioluminescentZone) {
        val lights = lightsByZone.remove(zone.zoneSeed) ?: return
        lights.forEach {
            it.markRemoved()
            removeDynamicLight(it)
        }
    }

    fun updateDynamicState(zones: List<BioluminescentZone>, gameTime: Long) {
        if (!isInitialized || lightsByZone.isEmpty()) return
        tickCounter++
        if (tickCounter % UPDATE_INTERVAL_TICKS != 0L) return

        val renderGameTime = gameTime.toDouble()
        for (zone in zones) {
            val lights = lightsByZone[zone.zoneSeed] ?: continue
            updateZoneLights(zone, lights, renderGameTime)
        }
    }

    fun registeredLightCount(zone: BioluminescentZone): Int = lightsByZone[zone.zoneSeed]?.size ?: 0

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
        if (lifecycle <= 0.0f) {
            lights.forEach { it.setIntensityFactor(0.0) }
            return
        }
        if (!isActive && zone.movementWaveVisibilityStrength() <= VISIBILITY_EPSILON) {
            lights.forEach { it.setIntensityFactor(0.0) }
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

            val factor = if (isActive) {
                val base = lifecycle * ACTIVE_BASE_FACTOR
                val boosted = base + lifecycle * waveIntensity * ACTIVE_WAVE_BOOST_FACTOR
                val waveFloor = lifecycle * waveIntensity * ACTIVE_WAVE_MIN_FACTOR
                maxOf(boosted, waveFloor)
            } else {
                lifecycle * waveIntensity * INACTIVE_WAVE_FACTOR
            }
            light.setIntensityFactor(factor)
        }
    }
}
