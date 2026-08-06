package fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.bloom

import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomState
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomSavedData
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveSize
import fr.heta__h.squ_abyssal_bloom.network.bioluminescence.S2CPlanktonBloomStateUpdatePayload
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterCell
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomainCollector
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.skeleton.BioluminescentTopologyBuilder
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePreset
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePresets
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.ActiveBioluminescenceWave
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceLevelManager
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.PacketDistributor
import java.util.UUID
import java.util.WeakHashMap
import kotlin.math.abs

class PlanktonBloomManager private constructor(
    private val level: ServerLevel
) {
    companion object {
        private val INSTANCES: MutableMap<ServerLevel, PlanktonBloomManager> = WeakHashMap()

        @JvmStatic
        @Synchronized
        fun forLevel(level: ServerLevel): PlanktonBloomManager {
            return INSTANCES.getOrPut(level) { PlanktonBloomManager(level) }
        }

        @JvmStatic
        @Synchronized
        fun releaseLevel(level: ServerLevel) {
            INSTANCES.remove(level)
        }

        private const val BLOOM_HARVEST_COUNT_SALT = 0x650A73548BAF63DEL
        private const val BLOOM_VISUAL_SEED_SALT = 0x1D373AE2A0FF3F31L
        private const val GEODESIC_RADIUS_SALT = 0x7137449123EF65CDL

        private const val MIN_CORE_BOUNDARY_DEPTH = 1
        private const val MAX_ANCHOR_DISTANCE_RATIO = 0.55
        private const val BOUNDARY_DEPTH_PROMINENCE_WEIGHT = 1.5
        private const val DOMAIN_COLLECTION_BUDGET_MULTIPLIER = 6

        private const val BLOOM_COUNT_SALT = 0x136E5CBFCA9AC0B4L
        private const val BLOOM_ORDER_SALT = 0x4F1BBCDCE5DE3FDEL

        private const val MOVEMENT_VERTICAL_MARGIN = 4.0
        private const val ACTIVATION_SCAN_INTERVAL_TICKS = 5L
        private const val WATER_REVALIDATION_INTERVAL_TICKS = 100L
    }

    sealed interface HarvestOutcome {
        data class Granted(val bloom: PlanktonBloomState) : HarvestOutcome
        data object Invalidated : HarvestOutcome
        data object Unavailable : HarvestOutcome
    }

    private val savedData = level.dataStorage.computeIfAbsent(PlanktonBloomSavedData.TYPE)

    private var lastActivationScanGameTime = -1L
    private var lastWaterRevalidationGameTime = -1L
    private val lastKnownPositions = HashMap<UUID, Vec3>()
    private var displacementCacheGameTime = -1L
    private val displacementCache = HashMap<UUID, Double>()

    fun createBloomsForWave(wave: ActiveBioluminescenceWave): List<PlanktonBloomState> {
        if (!ModServerConfig.BIOLUMINESCENCE_BLOOM_ENABLED.get()) return emptyList()

        val preset = when (wave.size) {
            BioluminescenceWaveSize.SMALL -> BioluminescentZonePresets.SMALL
            BioluminescenceWaveSize.LARGE -> BioluminescentZonePresets.LARGE
        }
        val initialCell = findInitialCell(wave.anchor) ?: run {
            SquAbyssalBloom.LOGGER.warn(
                "[Bioluminescence] No initial water cell found for wave {} (anchor={}), skipping blooms",
                wave.eventId, wave.anchor
            )
            return emptyList()
        }
        val geodesicRadius = selectGeodesicRadius(wave.seed, preset)
        val collector = BioluminescentWaterDomainCollector(
            initialCell.waterPos,
            initialCell,
            geodesicRadius,
            preset.analysisMargin,
            preset.maxWaterCells
        )
        collector.advance(level, preset.maxWaterCells * DOMAIN_COLLECTION_BUDGET_MULTIPLIER)
        if (!collector.complete) {
            SquAbyssalBloom.LOGGER.warn(
                "[Bioluminescence] Incomplete water domain for wave {}, skipping blooms (unloaded chunks)",
                wave.eventId
            )
            return emptyList()
        }

        val topology = try {
            val domain = collector.build()
            val topologyBuilder = BioluminescentTopologyBuilder(domain, preset, wave.seed)
            while (topologyBuilder.stage != BioluminescentTopologyBuilder.Stage.COMPLETE) topologyBuilder.advance()
            domain to topologyBuilder.build()
        } catch (exception: IllegalStateException) {
            SquAbyssalBloom.LOGGER.warn(
                "[Bioluminescence] Topology generation failed for wave {}, skipping blooms: {}",
                wave.eventId,
                exception.message
            )
            return emptyList()
        }
        val (domain, topologyResult) = topology

        data class Candidate(
            val coreIndex: Int,
            val positionKey: Long,
            val tier: Int,
            val prominence: Double
        )

        val maxAnchorDistance = geodesicRadius * MAX_ANCHOR_DISTANCE_RATIO
        val candidates = topologyResult.cores.withIndex().map { (coreIndex, core) ->
            val boundaryDepth = domain.boundaryDepth[core.cellIndex]
            val withinAnchorRange = domain.geodesicDistanceFromAnchor[core.cellIndex] <= maxAnchorDistance
            val connectedToSkeleton = topologyResult.skeleton.paths.any { path ->
                path.sourceCore == coreIndex || path.destinationCore == coreIndex
            }
            val tier = when {
                boundaryDepth >= MIN_CORE_BOUNDARY_DEPTH && withinAnchorRange && connectedToSkeleton -> 0
                boundaryDepth >= MIN_CORE_BOUNDARY_DEPTH && connectedToSkeleton -> 1
                boundaryDepth >= MIN_CORE_BOUNDARY_DEPTH -> 2
                else -> 3
            }
            val luminousArea = core.radiusX * core.radiusZ * core.weight
            val prominence = luminousArea + boundaryDepth * BOUNDARY_DEPTH_PROMINENCE_WEIGHT
            Candidate(
                coreIndex,
                ModUtilities.horizontalPositionKey(core.worldX.toInt(), core.worldZ.toInt()),
                tier,
                prominence
            )
        }

        val countRange = when (wave.size) {
            BioluminescenceWaveSize.SMALL -> configuredRange(
                ModServerConfig.BIOLUMINESCENCE_BLOOM_SMALL_MIN_COUNT.get(),
                ModServerConfig.BIOLUMINESCENCE_BLOOM_SMALL_MAX_COUNT.get()
            )
            BioluminescenceWaveSize.LARGE -> configuredRange(
                ModServerConfig.BIOLUMINESCENCE_BLOOM_LARGE_MIN_COUNT.get(),
                ModServerConfig.BIOLUMINESCENCE_BLOOM_LARGE_MAX_COUNT.get()
            )
        }
        val targetCount = pickInRange(countRange, wave.seed xor BLOOM_COUNT_SALT)
        if (targetCount <= 0) return emptyList()
        val selected = candidates.sortedWith(
            compareBy<Candidate> { it.tier }
                .thenByDescending { it.prominence }
                .thenBy { candidate ->
                    ModUtilities.stableUnitValue(
                        RandomSupport.mixStafford13(wave.seed xor candidate.positionKey xor BLOOM_ORDER_SALT)
                    )
                }
        ).take(targetCount.coerceAtMost(candidates.size))

        val harvestRange = configuredRange(
            ModServerConfig.BIOLUMINESCENCE_BLOOM_MIN_HARVESTS.get(),
            ModServerConfig.BIOLUMINESCENCE_BLOOM_MAX_HARVESTS.get()
        )
        val created = selected.map { candidate ->
            val core = topologyResult.cores[candidate.coreIndex]
            val maxHarvests = pickInRange(
                harvestRange,
                wave.seed xor candidate.positionKey xor BLOOM_HARVEST_COUNT_SALT
            ).coerceAtLeast(1)
            val visualSeed = RandomSupport.mixStafford13(wave.seed xor candidate.positionKey xor BLOOM_VISUAL_SEED_SALT)
            PlanktonBloomState(
                id = UUID.randomUUID(),
                waveEventId = wave.eventId,
                position = domain.cells[core.cellIndex].waterPos,
                visualSeed = visualSeed,
                maxHarvests = maxHarvests,
                remainingHarvests = maxHarvests
            )
        }

        SquAbyssalBloom.LOGGER.debug(
            "[Bioluminescence] Wave {}: {} core(s), target={}, {} bloom(s) created",
            wave.eventId, topologyResult.cores.size, targetCount, created.size
        )

        if (created.isNotEmpty()) {
            created.forEach { bloom -> savedData.blooms[bloom.id] = bloom }
            savedData.setDirty()
        }
        return created
    }

    fun bloomsForWave(waveEventId: UUID): List<PlanktonBloomState> {
        return savedData.blooms.values.filter { bloom -> bloom.waveEventId == waveEventId }
    }

    fun allBlooms(): List<PlanktonBloomState> {
        return savedData.blooms.values.toList()
    }

    fun diagnoseWater(position: BlockPos): Boolean = hasValidWater(position)

    fun diagnoseMovement(position: BlockPos): Boolean = hasNearbyMovement(position)

    fun removeBloomsForWave(waveEventId: UUID) {
        val removedAny = savedData.blooms.values.removeIf { bloom -> bloom.waveEventId == waveEventId }
        if (removedAny) savedData.setDirty()
    }

    fun forceActivate(bloomId: UUID, gameTime: Long): Boolean {
        val bloom = savedData.blooms[bloomId] ?: return false
        if (bloom.lifecycle != PlanktonBloomLifecycle.DORMANT) return false
        activate(bloom, gameTime)
        return true
    }

    fun tick(gameTime: Long) {
        if (savedData.blooms.isEmpty()) return
        val scanActivation = gameTime - lastActivationScanGameTime >= ACTIVATION_SCAN_INTERVAL_TICKS
        val revalidateWater = gameTime - lastWaterRevalidationGameTime >= WATER_REVALIDATION_INTERVAL_TICKS
        if (!scanActivation && !revalidateWater) return
        if (scanActivation) lastActivationScanGameTime = gameTime
        if (revalidateWater) lastWaterRevalidationGameTime = gameTime

        for (bloom in savedData.blooms.values.toList()) {
            if (
                bloom.lifecycle == PlanktonBloomLifecycle.DEPLETED ||
                bloom.lifecycle == PlanktonBloomLifecycle.INVALIDATED
            ) continue

            if (revalidateWater && !hasValidWater(bloom.position)) {
                invalidate(bloom)
                continue
            }

            if (scanActivation &&
                bloom.lifecycle == PlanktonBloomLifecycle.DORMANT &&
                hasNearbyMovement(bloom.position)
            ) {
                activate(bloom, gameTime)
            }
        }
    }

    fun findHarvestable(hitPos: Vec3, radius: Double): PlanktonBloomState? {
        val radiusSqr = radius * radius
        return savedData.blooms.values.firstOrNull { bloom ->
            bloom.lifecycle == PlanktonBloomLifecycle.ACTIVE &&
                Vec3.atCenterOf(bloom.position).distanceToSqr(hitPos) <= radiusSqr
        }
    }

    fun harvestNear(hitPos: Vec3, radius: Double): HarvestOutcome {
        val bloom = findHarvestable(hitPos, radius) ?: return HarvestOutcome.Unavailable
        return harvest(bloom.id)
    }

    fun harvest(bloomId: UUID): HarvestOutcome {
        val bloom = savedData.blooms[bloomId] ?: return HarvestOutcome.Unavailable
        if (bloom.lifecycle != PlanktonBloomLifecycle.ACTIVE) return HarvestOutcome.Unavailable
        if (!hasValidWater(bloom.position)) {
            invalidate(bloom)
            return HarvestOutcome.Invalidated
        }
        if (bloom.remainingHarvests <= 0) return HarvestOutcome.Unavailable

        bloom.remainingHarvests--
        if (bloom.remainingHarvests <= 0) bloom.lifecycle = PlanktonBloomLifecycle.DEPLETED
        savedData.setDirty()
        broadcastBloomUpdate(bloom)
        return HarvestOutcome.Granted(bloom)
    }

    private fun activate(bloom: PlanktonBloomState, gameTime: Long) {
        bloom.lifecycle = PlanktonBloomLifecycle.ACTIVE
        bloom.activatedAtGameTime = gameTime
        savedData.setDirty()
        broadcastBloomUpdate(bloom)
    }

    private fun invalidate(bloom: PlanktonBloomState) {
        bloom.lifecycle = PlanktonBloomLifecycle.INVALIDATED
        savedData.setDirty()
        broadcastBloomUpdate(bloom)
    }

    private fun broadcastBloomUpdate(bloom: PlanktonBloomState) {
        val levelManager = BioluminescenceLevelManager.forLevel(level)
        val payload = S2CPlanktonBloomStateUpdatePayload(
            bloom.waveEventId,
            bloom.id,
            bloom.lifecycle,
            bloom.remainingHarvests,
            bloom.activatedAtGameTime
        )
        for (player in level.players()) {
            if (bloom.waveEventId in levelManager.visibleWaveIds(player.uuid)) {
                PacketDistributor.sendToPlayer(player, payload)
            }
        }
    }

    private fun hasValidWater(position: BlockPos): Boolean {
        return ModUtilities.isRenderableWaterSurface(level, position)
    }

    data class MovementDiagnostic(
        val detected: Boolean,
        val radius: Double,
        val candidateCount: Int,
        val fastestSpeed: Double,
        val threshold: Double,
        val entityDetails: List<String> = emptyList()
    )

    private fun hasNearbyMovement(position: BlockPos): Boolean {
        return movementDiagnostic(position).detected
    }

    fun movementDiagnostic(position: BlockPos): MovementDiagnostic {
        val radius = ModServerConfig.BIOLUMINESCENCE_BLOOM_ACTIVATION_RADIUS.get()
        val threshold = ModServerConfig.BIOLUMINESCENCE_BLOOM_ACTIVATION_MIN_DISPLACEMENT.get()
        val center = Vec3.atCenterOf(position)
        val searchBounds = AABB(
            center.x - radius, center.y - MOVEMENT_VERTICAL_MARGIN, center.z - radius,
            center.x + radius, center.y + MOVEMENT_VERTICAL_MARGIN, center.z + radius
        )
        val candidates = level.getEntitiesOfClass(
            ServerPlayer::class.java,
            searchBounds,
            ModUtilities::isQualifyingWaterMovingPlayer
        )
        val largestDisplacement = candidates.maxOfOrNull(::trackedDisplacement) ?: 0.0
        return MovementDiagnostic(
            detected = largestDisplacement >= threshold,
            radius = radius,
            candidateCount = candidates.size,
            fastestSpeed = largestDisplacement,
            threshold = threshold,
            entityDetails = candidates.map { player ->
                "${player.gameProfile.name} pos=${player.blockPosition()} " +
                    "distance=${"%.1f".format(center.distanceTo(player.position()))} " +
                    "deplacement=${"%.4f".format(trackedDisplacement(player))}"
            }
        )
    }

    private fun trackedDisplacement(entity: Entity): Double {
        val gameTime = level.gameTime
        if (displacementCacheGameTime != gameTime) {
            displacementCacheGameTime = gameTime
            displacementCache.clear()
        }
        return displacementCache.getOrPut(entity.uuid) {
            val current = entity.position()
            val previous = lastKnownPositions.put(entity.uuid, current)
            if (previous == null) 0.0 else current.distanceTo(previous)
        }
    }

    private fun findInitialCell(anchor: BlockPos): BioluminescentWaterCell? {
        if (!ModUtilities.hasLoadedChunk(level, anchor.x shr 4, anchor.z shr 4)) return null
        BioluminescentWaterDomainCollector.sampleWaterCell(level, anchor.x, anchor.z, anchor.y)?.let { return it }
        for (radius in 1..4) {
            for (offsetX in -radius..radius) {
                for (offsetZ in -radius..radius) {
                    if (maxOf(abs(offsetX), abs(offsetZ)) != radius) continue
                    val worldX = anchor.x + offsetX
                    val worldZ = anchor.z + offsetZ
                    if (!ModUtilities.hasLoadedChunk(level, worldX shr 4, worldZ shr 4)) continue
                    BioluminescentWaterDomainCollector.sampleWaterCell(level, worldX, worldZ, anchor.y)
                        ?.let { return it }
                }
            }
        }
        return null
    }

    private fun selectGeodesicRadius(zoneSeed: Long, preset: BioluminescentZonePreset): Int {
        return pickInRange(preset.geodesicRadiusRange, zoneSeed xor GEODESIC_RADIUS_SALT)
    }

    private fun configuredRange(minimum: Int, maximum: Int): IntRange {
        return minimum..maxOf(minimum, maximum)
    }

    private fun pickInRange(range: IntRange, seed: Long): Int {
        val span = (range.last - range.first + 1).toLong()
        if (span <= 1L) return range.first
        return range.first + Math.floorMod(RandomSupport.mixStafford13(seed), span).toInt()
    }
}
