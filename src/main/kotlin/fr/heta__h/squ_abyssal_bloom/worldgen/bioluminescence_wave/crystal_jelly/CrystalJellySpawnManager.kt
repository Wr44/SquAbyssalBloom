package fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.crystal_jelly

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.CrystalJellyEntity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.ActiveBioluminescenceWave
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceBounds
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaterAreaSampler
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveActivity
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveMode
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveSize
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.crystal_jelly.CrystalJellySpawnPlacement
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.crystal_jelly.CrystalJellySpawnPlan
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceLevelManager
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID
import java.util.WeakHashMap
import kotlin.math.cos
import kotlin.math.roundToInt

class CrystalJellySpawnManager(
    val level: ServerLevel
) {

    companion object {
        private val INSTANCES: MutableMap<ServerLevel, CrystalJellySpawnManager> = WeakHashMap()

        @JvmStatic
        @Synchronized
        fun forLevel(level: ServerLevel): CrystalJellySpawnManager {
            return INSTANCES.getOrPut(level) { CrystalJellySpawnManager(level) }
        }

        @JvmStatic
        @Synchronized
        fun releaseLevel(level: ServerLevel) {
            INSTANCES.remove(level)
        }

        private const val SPAWN_ATTEMPT_INTERVAL_TICKS = 40L
        private const val SPAWNS_PER_ATTEMPT = 2
        private const val CANDIDATES_PER_SPAWN = 24

        private const val MINIMUM_PLAYER_DISTANCE = 12.0
        private const val MAXIMUM_PLAYER_DISTANCE = 48.0
        private const val VISIBILITY_CHECK_DISTANCE = 80.0
        private const val POPULATION_SEARCH_MARGIN = 32.0

        private val VIEW_CONE_COSINE = cos(Math.toRadians(75.0))
    }

    private val plans = HashMap<UUID, CrystalJellySpawnPlan>()
    private var lastAttemptGameTime = -SPAWN_ATTEMPT_INTERVAL_TICKS

    fun tick(gameTime: Long, activeWaves: Collection<ActiveBioluminescenceWave>) {
        if (gameTime - lastAttemptGameTime < SPAWN_ATTEMPT_INTERVAL_TICKS) return
        lastAttemptGameTime = gameTime

        plans.keys.retainAll(activeWaves.mapTo(hashSetOf()) { wave -> wave.eventId })

        if (!ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_ENABLED.get()) return
        if (level.players().isEmpty()) return

        for (wave in activeWaves) populateWave(wave)
    }

    fun releaseWave(wave: ActiveBioluminescenceWave) {
        plans.remove(wave.eventId)
        for (jelly in jelliesForWave(wave.eventId, wave.bounds)) jelly.beginFadeOut()
    }

    private fun populateWave(wave: ActiveBioluminescenceWave) {
        if (!hasPlayerInRange(wave)) return

        val plan = plans[wave.eventId]
            ?: buildPlan(wave)?.also { built -> plans[wave.eventId] = built }
            ?: return
        if (plan.columns.isEmpty()) return

        var missing = plan.targetPopulation - countJellies(wave.eventId, wave.bounds)
        var spawned = 0
        while (missing > 0 && spawned < SPAWNS_PER_ATTEMPT) {
            if (!spawnOne(wave, plan)) return
            missing--
            spawned++
        }
    }

    private fun buildPlan(wave: ActiveBioluminescenceWave): CrystalJellySpawnPlan? {
        val geodesicRadius = wave.size.selectGeodesicRadius(wave.seed)
        val placementRadius = (geodesicRadius * BioluminescenceLevelManager.PLACEMENT_RADIUS_RATIO)
            .toInt()
            .coerceAtLeast(1)
        val area = BioluminescenceWaterAreaSampler.collect(
            level,
            wave.anchor,
            geodesicRadius,
            placementRadius,
            wave.size.minimumWaterCells
        ) ?: return null

        val plan = CrystalJellySpawnPlacement.buildPlan(
            level,
            wave.seed,
            area,
            placementRadius,
            targetPopulationFor(wave, area.cells.size)
        )
        SquAbyssalBloom.LOGGER.debug(
            "[Bioluminescence] Wave {}: crystal jelly target={}, {} connected water cell(s), {} spawnable column(s)",
            wave.eventId, plan.targetPopulation, area.cells.size, plan.columns.size
        )
        return plan
    }

    private fun targetPopulationFor(wave: ActiveBioluminescenceWave, waterCellCount: Int): Int {
        val configuredBounds = when (wave.size) {
            BioluminescenceWaveSize.SMALL ->
                ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_SMALL_MIN_COUNT.get() to
                    ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_SMALL_MAX_COUNT.get()
            BioluminescenceWaveSize.LARGE ->
                ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_LARGE_MIN_COUNT.get() to
                    ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_LARGE_MAX_COUNT.get()
        }

        var multiplier = 1.0
        if (wave.activity == BioluminescenceWaveActivity.INACTIVE) {
            multiplier *= ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_INACTIVE_MULTIPLIER.get()
        }
        if (wave.mode == BioluminescenceWaveMode.TOTAL_NIGHT) {
            multiplier *= ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_TOTAL_NIGHT_MULTIPLIER.get()
        }

        val cellsPerJelly = ModServerConfig.BIOLUMINESCENCE_CRYSTAL_JELLY_CELLS_PER_JELLY.get()
        val minimum = (minOf(configuredBounds.first, configuredBounds.second) * multiplier)
            .roundToInt()
            .coerceAtLeast(0)
        val maximum = (maxOf(configuredBounds.first, configuredBounds.second) * multiplier)
            .roundToInt()
            .coerceAtLeast(minimum)
        val scaled = (waterCellCount.toDouble() / cellsPerJelly * multiplier).roundToInt()
        return scaled.coerceIn(minimum, maximum)
    }

    private fun spawnOne(wave: ActiveBioluminescenceWave, plan: CrystalJellySpawnPlan): Boolean {
        repeat(CANDIDATES_PER_SPAWN) {
            val position = CrystalJellySpawnPlacement.pickSpawnPosition(plan, level.random) ?: return false
            if (!isHiddenFromPlayers(position)) return@repeat
            if (spawnAt(wave, position)) return true
        }
        return false
    }

    private fun spawnAt(wave: ActiveBioluminescenceWave, position: Vec3): Boolean {
        val jelly = ModEntities.CRYSTAL_JELLY.get().create(level, EntitySpawnReason.EVENT) ?: return false
        jelly.snapTo(position.x, position.y, position.z, level.random.nextFloat() * 360.0f, 0.0f)
        if (!level.noCollision(jelly)) return false

        jelly.waveEventId = wave.eventId
        return level.addFreshEntity(jelly)
    }

    private fun isHiddenFromPlayers(position: Vec3): Boolean {
        var hasPlayerInRange = false

        for (player in level.players()) {
            if (player.isSpectator) continue

            val eye = player.eyePosition
            val distanceSqr = eye.distanceToSqr(position)
            if (distanceSqr <= MINIMUM_PLAYER_DISTANCE * MINIMUM_PLAYER_DISTANCE) return false
            if (distanceSqr <= MAXIMUM_PLAYER_DISTANCE * MAXIMUM_PLAYER_DISTANCE) hasPlayerInRange = true
            if (distanceSqr > VISIBILITY_CHECK_DISTANCE * VISIBILITY_CHECK_DISTANCE) continue

            if (player.lookAngle.dot(position.subtract(eye).normalize()) < VIEW_CONE_COSINE) continue
            if (ModUtilities.hasClearPath(level, eye, position, player)) return false
        }

        return hasPlayerInRange
    }

    private fun hasPlayerInRange(wave: ActiveBioluminescenceWave): Boolean = level.players().any { player ->
        wave.bounds.horizontalDistanceSqr(player.x, player.z) <=
            MAXIMUM_PLAYER_DISTANCE * MAXIMUM_PLAYER_DISTANCE
    }

    private fun countJellies(eventId: UUID, bounds: BioluminescenceBounds): Int =
        jelliesForWave(eventId, bounds).size

    private fun jelliesForWave(
        eventId: UUID,
        bounds: BioluminescenceBounds
    ): List<CrystalJellyEntity> {
        val searchBox = AABB(
            bounds.minimumX - POPULATION_SEARCH_MARGIN,
            level.minY.toDouble(),
            bounds.minimumZ - POPULATION_SEARCH_MARGIN,
            bounds.maximumX + 1.0 + POPULATION_SEARCH_MARGIN,
            level.maxY.toDouble(),
            bounds.maximumZ + 1.0 + POPULATION_SEARCH_MARGIN
        )
        return level.getEntitiesOfClass(CrystalJellyEntity::class.java, searchBox) { jelly ->
            jelly.waveEventId == eventId
        }
    }
}
