package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentBloom
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentBloomGeometry
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentBloomShape
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentBloomSize
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentBloomState
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentBloomZone
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentPaletteFamily
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentPalettes
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentRegionalNoiseSampler
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentTexture
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentTextureUpdateRequest
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentWaterCell
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.tags.BiomeTags
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.RandomSupport
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent
import net.neoforged.neoforge.event.level.LevelEvent
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentBloomManager {
    const val MAX_ACTIVE_ZONES = 2
    const val MAX_ACTIVE_BLOOMS = 12
    const val SPAWN_SCAN_INTERVAL_TICKS = 40L
    const val SPAWN_ATTEMPTS_PER_SCAN = 8
    const val MIN_DISTANCE_BETWEEN_ZONES = 48.0
    const val REMOVAL_DISTANCE = 96.0
    const val BASE_SPAWN_CHANCE = 0.018

    sealed interface SpawnResult {
        data class Created(
            val anchor: BlockPos,
            val distance: Double,
            val plannedBloomCount: Int
        ) : SpawnResult

        data object NoCoastalSurface : SpawnResult
        data object ZoneLimitReached : SpawnResult
        data object BloomLimitReached : SpawnResult
        data object DimensionNotAllowed : SpawnResult
        data object NoLevel : SpawnResult
    }

    private data class CoastalCandidate(
        val waterSurface: BlockPos,
        val waterCells: List<BioluminescentWaterCell>
    )

    private data class CellClaim(
        val bloom: BioluminescentBloom,
        val cellIndex: Int,
        val score: Double
    )

    private data class FootprintConnectionSample(
        val worldX: Int,
        val worldZ: Int,
        val score: Float
    )

    private data class HorizontalDirection(
        val x: Double,
        val z: Double
    )

    private val zones = mutableListOf<BioluminescentBloomZone>()
    private val blooms = mutableListOf<BioluminescentBloom>()
    private val cellClaims = HashMap<Long, CellClaim>()

    val activeBlooms: List<BioluminescentBloom>
        get() = blooms

    val activeZoneCount: Int
        get() = zones.size

    private var currentLevel: ClientLevel? = null
    private var levelIdentitySeed = 0L
    private var temporalRandom: XoroshiroRandomSource? = null
    private var regionalNoise: BioluminescentRegionalNoiseSampler? = null
    private var lastSpawnScanTick: Long? = null
    private var remainingNaturalSpawnAttempts = 0
    private var maskRoundRobinIndex = 0
    private var spawnCounter = 0L
    private var textureCounter = 0L
    private var cellOwnershipDirty = false

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val minecraft = Minecraft.getInstance()
        if (minecraft.isPaused) return

        val level = minecraft.level ?: run {
            resetLevel()
            return
        }
        val player = minecraft.player ?: return

        ensureLevel(level)
        if (!isDimensionAllowed(level)) {
            if (zones.isNotEmpty()) clearActiveZones("dimension")
            return
        }

        val gameTime = level.gameTime
        removeDistantZones(player.x, player.z)
        removeExpiredOrDistantBlooms(player.x, player.z, gameTime)
        val bloomCountBeforePlacement = blooms.size
        processOneConnectedPlacement(level, gameTime)
        val createdBloomDuringPlacement = blooms.size > bloomCountBeforePlacement
        updateWaterMasks(level, gameTime)

        if (isSpawnScanDue(gameTime)) {
            lastSpawnScanTick = gameTime
            remainingNaturalSpawnAttempts = SPAWN_ATTEMPTS_PER_SCAN
        }
        if (remainingNaturalSpawnAttempts > 0 && !createdBloomDuringPlacement) {
            remainingNaturalSpawnAttempts--
            if (tryNaturalSpawnAttempt(level, player.blockPosition(), gameTime)) {
                remainingNaturalSpawnAttempts = 0
            }
        }
        updateCellOwnership()
        updateTextureUploads(player.x, player.z, gameTime)
        removeExpiredOrDistantBlooms(player.x, player.z, gameTime)
        updateCellOwnership()
        removeEmptyZones()
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level === currentLevel) resetLevel()
    }

    @SubscribeEvent
    fun onClientStopping(event: ClientStoppingEvent) {
        resetLevel()
    }

    fun spawnDebugZone(
        size: BioluminescentBloomSize = BioluminescentBloomSize.MEDIUM,
        paletteFamily: BioluminescentPaletteFamily = BioluminescentPaletteFamily.RANDOM
    ): SpawnResult {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return SpawnResult.NoLevel
        val player = minecraft.player ?: return SpawnResult.NoLevel
        ensureLevel(level)

        if (!isDimensionAllowed(level)) return SpawnResult.DimensionNotAllowed
        if (zones.size >= MAX_ACTIVE_ZONES) return SpawnResult.ZoneLimitReached
        if (reservedBloomCount() >= MAX_ACTIVE_BLOOMS) return SpawnResult.BloomLimitReached

        val random = temporalRandom ?: return SpawnResult.NoLevel
        var bestCandidate: CoastalCandidate? = findCoastalCandidateNear(
            level,
            player.blockPosition().x,
            player.blockPosition().z
        )?.takeIf(::isFarEnoughFromOtherZones)

        repeat(DEBUG_SPAWN_ATTEMPTS) {
            val position = randomRingPosition(
                player.blockPosition(),
                DEBUG_SEARCH_MIN_DISTANCE,
                SPAWN_SEARCH_MAX_DISTANCE,
                random
            )
            val candidate = findCoastalCandidateNear(level, position.x, position.z) ?: return@repeat
            if (!isFarEnoughFromOtherZones(candidate)) return@repeat
            if (candidate.waterCells.size > (bestCandidate?.waterCells?.size ?: -1)) {
                bestCandidate = candidate
            }
        }

        val candidate = bestCandidate ?: return SpawnResult.NoCoastalSurface
        val zone = createZone(candidate, size, paletteFamily, level.gameTime)
            ?: return SpawnResult.BloomLimitReached
        updateCellOwnership(force = true)
        val dx = candidate.waterSurface.x + 0.5 - player.x
        val dz = candidate.waterSurface.z + 0.5 - player.z
        return SpawnResult.Created(candidate.waterSurface, sqrt(dx * dx + dz * dz), zone.targetBloomCount)
    }

    fun clearDebugZones(): Int {
        val count = zones.size
        clearActiveZones("command")
        remainingNaturalSpawnAttempts = 0
        currentLevel?.let { lastSpawnScanTick = it.gameTime }
        return count
    }

    fun isDimensionAllowed(level: ClientLevel): Boolean {
        val type = level.dimensionType()
        return type.hasSkyLight() &&
            !type.hasCeiling() &&
            type.skybox() == DimensionType.Skybox.OVERWORLD
    }

    private fun ensureLevel(level: ClientLevel) {
        if (currentLevel === level) return
        resetLevel()
        currentLevel = level

        val dimensionHash = level.dimension().identifier().toString().hashCode().toLong()
        val identity = System.identityHashCode(level).toLong()
        levelIdentitySeed = RandomSupport.mixStafford13((identity shl 32) xor dimensionHash)
        temporalRandom = XoroshiroRandomSource(
            RandomSupport.mixStafford13(levelIdentitySeed xor TEMPORAL_RANDOM_SALT)
        )
        regionalNoise = BioluminescentRegionalNoiseSampler(levelIdentitySeed)
        lastSpawnScanTick = level.gameTime
    }

    private fun resetLevel() {
        clearActiveZones("level_reset")
        currentLevel = null
        levelIdentitySeed = 0L
        temporalRandom = null
        regionalNoise = null
        lastSpawnScanTick = null
        remainingNaturalSpawnAttempts = 0
        maskRoundRobinIndex = 0
        spawnCounter = 0L
        cellOwnershipDirty = false
    }

    private fun clearActiveZones(reason: String) {
        if (zones.isEmpty() && blooms.isEmpty()) return
        blooms.forEach(BioluminescentBloom::close)
        zones.forEach { zone -> zone.blooms.clear() }
        zones.clear()
        blooms.clear()
        cellClaims.clear()
        maskRoundRobinIndex = 0
        cellOwnershipDirty = false
        SquAbyssalBloom.LOGGER.debug("[Bioluminescence] Cleared all zones ({})", reason)
    }

    private fun isSpawnScanDue(gameTime: Long): Boolean {
        val previousScan = lastSpawnScanTick ?: return false
        return gameTime < previousScan || gameTime - previousScan >= SPAWN_SCAN_INTERVAL_TICKS
    }

    private fun tryNaturalSpawnAttempt(level: ClientLevel, playerPos: BlockPos, gameTime: Long): Boolean {
        if (zones.size >= MAX_ACTIVE_ZONES || reservedBloomCount() >= MAX_ACTIVE_BLOOMS) {
            remainingNaturalSpawnAttempts = 0
            return false
        }

        val random = temporalRandom ?: return false
        val regionalSampler = regionalNoise ?: return false
        val zoneAvailability = (MAX_ACTIVE_ZONES - zones.size).toDouble() / MAX_ACTIVE_ZONES
        val bloomAvailability = (MAX_ACTIVE_BLOOMS - reservedBloomCount()).toDouble() / MAX_ACTIVE_BLOOMS

        val position = randomRingPosition(
            playerPos,
            SPAWN_SEARCH_MIN_DISTANCE,
            SPAWN_SEARCH_MAX_DISTANCE,
            random
        )
        val candidate = findCoastalCandidateNear(level, position.x, position.z) ?: return false
        if (!isFarEnoughFromOtherZones(candidate)) return false

        val regionalValue = regionalSampler.sample(
            candidate.waterSurface.x.toDouble(),
            candidate.waterSurface.z.toDouble()
        )
        val regionalFactor = 0.15 + 0.85 * ModUtilities.smooth(0.25, 0.75, regionalValue)
        val capacityFactor = sqrt(zoneAvailability * bloomAvailability)
        val spawnChance = BASE_SPAWN_CHANCE *
            regionalFactor *
            capacityFactor
        if (random.nextDouble() >= spawnChance) return false

        return createZone(
            candidate,
            chooseNaturalSize(random),
            BioluminescentPaletteFamily.RANDOM,
            gameTime
        ) != null
    }

    private fun createZone(
        candidate: CoastalCandidate,
        requestedSize: BioluminescentBloomSize,
        paletteFamily: BioluminescentPaletteFamily,
        gameTime: Long
    ): BioluminescentBloomZone? {
        val availableSlots = MAX_ACTIVE_BLOOMS - reservedBloomCount()
        if (availableSlots < MIN_BLOOMS_PER_ZONE) return null

        spawnCounter++
        val zoneSeed = RandomSupport.mixStafford13(
            levelIdentitySeed xor
                candidate.waterSurface.asLong() xor
                gameTime xor
                RandomSupport.mixStafford13(spawnCounter xor ZONE_SEED_SALT)
        )
        val zoneRandom = XoroshiroRandomSource(
            RandomSupport.mixStafford13(zoneSeed xor ZONE_GEOMETRY_SALT)
        )
        val lifetime = MINIMUM_ZONE_LIFETIME_TICKS +
            zoneRandom.nextInt((MAXIMUM_ZONE_LIFETIME_TICKS - MINIMUM_ZONE_LIFETIME_TICKS + 1L).toInt())
        val targetBloomCount = plannedBloomCount(requestedSize, zoneRandom).coerceAtMost(availableSlots)
        val coastDirection = estimateCoastDirection(candidate, zoneSeed)
        val zone = BioluminescentBloomZone(
            zoneSeed = zoneSeed,
            anchor = candidate.waterSurface,
            palette = BioluminescentPalettes.select(zoneSeed, paletteFamily),
            createdAt = gameTime,
            lifetime = lifetime,
            colorPhase = BioluminescentBloomState.phaseFromSeed(zoneSeed),
            requestedSize = requestedSize,
            targetBloomCount = targetBloomCount,
            coastDirectionX = coastDirection.x,
            coastDirectionZ = coastDirection.z
        )

        val firstSeed = bloomSeed(zoneSeed, 0)
        val firstGeometry = createGeometry(firstSeed, requestedSize, coastDirection)
        val firstBloom = createBloom(zone, candidate, firstSeed, firstGeometry, gameTime) ?: return null
        registerAcceptedCoverage(zone, candidate, firstBloom)

        zones.add(zone)
        SquAbyssalBloom.LOGGER.debug(
            "[Bioluminescence] Created beach zone {} at {} with {}/{} blooms active/planned " +
                "({} nearby water cells)",
            java.lang.Long.toUnsignedString(zoneSeed, 16),
            candidate.waterSurface,
            zone.blooms.size,
            targetBloomCount,
            candidate.waterCells.size
        )
        return zone
    }

    private fun processOneConnectedPlacement(level: ClientLevel, gameTime: Long) {
        if (blooms.size >= MAX_ACTIVE_BLOOMS) return

        for (zone in zones) {
            if (zone.generationComplete || gameTime < zone.nextPlacementTick) continue
            if (zone.blooms.isEmpty()) {
                zone.generationComplete = true
                continue
            }
            if (zone.blooms.size >= zone.targetBloomCount) {
                zone.generationComplete = true
                continue
            }

            val components = connectedComponents(zone.blooms)
            zone.connectedComponentCount = components.size
            val placed = if (components.size > 1) {
                tryPlaceConnector(level, zone, components)
            } else {
                tryPlaceConnectedBloom(level, zone)
            }

            if (placed) {
                zone.nextBloomIndex++
                zone.placementAttempt = 0
                zone.connectedComponentCount = connectedComponents(zone.blooms).size
                zone.nextPlacementTick = gameTime + connectedPlacementDelay(zone)
                if (shouldFinishZoneGeneration(zone)) zone.generationComplete = true
            } else {
                zone.placementAttempt++
                zone.nextPlacementTick = gameTime + PLACEMENT_RETRY_INTERVAL_TICKS
                if (zone.placementAttempt >= MAX_PLACEMENT_ATTEMPTS_PER_BLOOM) {
                    zone.generationComplete = true
                }
            }
            return
        }
    }

    private fun createBloom(
        zone: BioluminescentBloomZone,
        candidate: CoastalCandidate,
        seed: Long,
        geometry: BioluminescentBloomGeometry,
        startTick: Long
    ): BioluminescentBloom? {
        if (blooms.size >= MAX_ACTIVE_BLOOMS) return null

        val endJitter = Math.floorMod(RandomSupport.mixStafford13(seed xor END_TICK_SALT), 61L) - 20L
        val targetEndTick = zone.createdAt + zone.lifetime + endJitter
        val lifetime = (targetEndTick - startTick).coerceAtLeast(MINIMUM_BLOOM_LIFETIME_TICKS)
        val state = BioluminescentBloomState.create(
            seed = seed,
            createdAt = startTick,
            lifetime = lifetime,
            palette = zone.palette,
            pulseOriginTick = zone.createdAt,
            colorPhase = zone.colorPhase
        )
        val identifier = nextTextureIdentifier()
        val originX = candidate.waterSurface.x - geometry.widthInBlocks / 2
        val originZ = candidate.waterSurface.z - geometry.lengthInBlocks / 2
        val texture = BioluminescentTexture(
            textureManager = Minecraft.getInstance().textureManager,
            identifier = identifier,
            widthInBlocks = geometry.widthInBlocks,
            lengthInBlocks = geometry.lengthInBlocks,
            shape = geometry.shape,
            rotationRadians = geometry.rotationRadians
        )
        texture.prepare(
            state = state,
            zoneSeed = zone.zoneSeed,
            worldOriginX = originX,
            worldOriginZ = originZ
        )

        val bloom = BioluminescentBloom(
            anchor = candidate.waterSurface,
            widthInBlocks = geometry.widthInBlocks,
            lengthInBlocks = geometry.lengthInBlocks,
            shape = geometry.shape,
            rotationRadians = geometry.rotationRadians,
            state = state,
            texture = texture,
            renderType = RenderTypes.eyes(identifier),
            initialWaterCells = candidate.waterCells
        )
        zone.blooms.add(bloom)
        blooms.add(bloom)
        cellOwnershipDirty = true
        return bloom
    }

    private fun updateWaterMasks(level: ClientLevel, gameTime: Long) {
        if (blooms.isEmpty()) return

        var remainingBudget = MASK_COLUMN_BUDGET_PER_TICK
        var visited = 0
        while (remainingBudget > 0 && visited < blooms.size) {
            if (maskRoundRobinIndex >= blooms.size) maskRoundRobinIndex = 0
            val bloom = blooms[maskRoundRobinIndex]
            maskRoundRobinIndex = (maskRoundRobinIndex + 1) % blooms.size
            visited++

            bloom.scheduleMaskRefresh(gameTime)
            val slice = minOf(MASK_COLUMN_SLICE, remainingBudget)
            remainingBudget -= bloom.processMaskRefresh(level, gameTime, slice)
            if (bloom.consumeCellOwnershipInvalidation()) cellOwnershipDirty = true
        }
    }

    private fun updateCellOwnership(force: Boolean = false) {
        if (!force && !cellOwnershipDirty) return

        blooms.forEach(BioluminescentBloom::resetCellOwnership)
        cellClaims.clear()

        for (bloom in blooms) {
            val seedBias = stableUnitValue(
                RandomSupport.mixStafford13(bloom.state.seed xor OWNERSHIP_SEED_BIAS_SALT)
            )
            for (cellIndex in 0 until bloom.cellCount) {
                if (!bloom.hasWaterSurfaceAt(cellIndex)) continue
                val coverage = bloom.texture.coverageAtCell(cellIndex)
                if (coverage < MINIMUM_CELL_COVERAGE) continue

                val cellX = cellIndex % bloom.widthInBlocks
                val cellZ = cellIndex / bloom.widthInBlocks
                val key = worldCellKey(bloom.originX + cellX, bloom.originZ + cellZ)
                val cellBias = stableUnitValue(
                    RandomSupport.mixStafford13(bloom.state.seed xor key xor OWNERSHIP_CELL_BIAS_SALT)
                )
                val score = coverage.toDouble() * OWNERSHIP_DENSITY_WEIGHT +
                    bloom.centerInfluenceAt(cellIndex) * OWNERSHIP_CENTER_WEIGHT +
                    seedBias * OWNERSHIP_SEED_BIAS_WEIGHT +
                    cellBias * OWNERSHIP_CELL_BIAS_WEIGHT
                val current = cellClaims[key]
                if (current == null || score > current.score) {
                    cellClaims[key] = CellClaim(bloom, cellIndex, score)
                }
            }
        }

        cellClaims.values.forEach { claim -> claim.bloom.claimCell(claim.cellIndex) }
        cellOwnershipDirty = false
    }

    private fun updateTextureUploads(playerX: Double, playerZ: Double, gameTime: Long) {
        val updateDistanceSquared = TEXTURE_UPDATE_DISTANCE * TEXTURE_UPDATE_DISTANCE
        val requests = ArrayList<BioluminescentTextureUpdateRequest>(blooms.size)

        for (zone in zones) {
            for (bloom in zone.blooms) {
                val request = bloom.createTextureUpdateRequest(
                    zone = zone,
                    gameTime = gameTime,
                    withinUpdateDistance = bloom.horizontalDistanceSquared(playerX, playerZ) <= updateDistanceSquared
                ) ?: continue
                requests.add(request)
            }
        }

        requests.sortWith(
            compareByDescending<BioluminescentTextureUpdateRequest> { it.priority.rank }
                .thenByDescending { it.overdueTicks }
                .thenByDescending { it.uploadCostUnits }
        )

        var remainingCost = MAX_TEXTURE_UPLOAD_COST_PER_TICK
        var uploadCount = 0
        for (request in requests) {
            if (uploadCount >= MAX_TEXTURE_UPLOADS_PER_TICK) break
            if (request.uploadCostUnits > remainingCost) continue
            if (!request.bloom.applyTextureUpdate(request)) continue
            remainingCost -= request.uploadCostUnits
            uploadCount++
        }
    }

    private fun stableUnitValue(value: Long): Double {
        return ((value ushr 40) and 0xFFFFFFL).toDouble() / 0xFFFFFFL.toDouble()
    }

    private fun worldCellKey(worldX: Int, worldZ: Int): Long {
        return (worldX.toLong() shl 32) xor (worldZ.toLong() and 0xFFFFFFFFL)
    }

    private fun removeExpiredOrDistantBlooms(playerX: Double, playerZ: Double, gameTime: Long) {
        val removalDistanceSquared = REMOVAL_DISTANCE * REMOVAL_DISTANCE
        for (index in blooms.lastIndex downTo 0) {
            val bloom = blooms[index]
            val reason = when {
                bloom.horizontalDistanceSquared(playerX, playerZ) > removalDistanceSquared -> "distance"
                bloom.shouldRemoveAt(gameTime) -> "finished"
                else -> null
            } ?: continue

            blooms.removeAt(index)
            zones.firstOrNull { bloom in it.blooms }?.blooms?.remove(bloom)
            bloom.close()
            cellOwnershipDirty = true
            SquAbyssalBloom.LOGGER.debug(
                "[Bioluminescence] Removed bloom at {} ({}, {} active)",
                bloom.anchor,
                reason,
                blooms.size
            )
        }
        if (maskRoundRobinIndex > blooms.size) maskRoundRobinIndex = 0
    }

    private fun removeDistantZones(playerX: Double, playerZ: Double) {
        val removalDistanceSquared = REMOVAL_DISTANCE * REMOVAL_DISTANCE
        for (index in zones.lastIndex downTo 0) {
            val zone = zones[index]
            val dx = zone.anchor.x + 0.5 - playerX
            val dz = zone.anchor.z + 0.5 - playerZ
            if (dx * dx + dz * dz <= removalDistanceSquared) continue

            zone.blooms.forEach { bloom ->
                blooms.remove(bloom)
                bloom.close()
                cellOwnershipDirty = true
            }
            zone.blooms.clear()
            zone.generationComplete = true
            zones.removeAt(index)
            SquAbyssalBloom.LOGGER.debug(
                "[Bioluminescence] Removed zone {} (distance, {} active)",
                java.lang.Long.toUnsignedString(zone.zoneSeed, 16),
                zones.size
            )
        }
        if (maskRoundRobinIndex > blooms.size) maskRoundRobinIndex = 0
    }

    private fun removeEmptyZones() {
        for (index in zones.lastIndex downTo 0) {
            val zone = zones[index]
            if (zone.blooms.isNotEmpty()) continue
            zones.removeAt(index)
            SquAbyssalBloom.LOGGER.debug(
                "[Bioluminescence] Removed empty zone {} ({} active)",
                java.lang.Long.toUnsignedString(zone.zoneSeed, 16),
                zones.size
            )
        }
    }

    private fun tryPlaceConnectedBloom(
        level: ClientLevel,
        zone: BioluminescentBloomZone
    ): Boolean {
        val bloomIndex = zone.nextBloomIndex
        val seed = bloomSeed(zone.zoneSeed, bloomIndex)
        val sizeRandom = XoroshiroRandomSource(
            RandomSupport.mixStafford13(seed xor CHILD_SIZE_SALT)
        )
        val size = childSize(zone.requestedSize, bloomIndex, sizeRandom)
        val coastDirection = HorizontalDirection(zone.coastDirectionX, zone.coastDirectionZ)
        val geometry = createGeometry(seed, size, coastDirection)
        val placementRandom = XoroshiroRandomSource(
            RandomSupport.mixStafford13(
                seed xor RandomSupport.mixStafford13(zone.placementAttempt.toLong() xor PLACEMENT_ATTEMPT_SALT)
            )
        )
        val directionSign = if (bloomIndex % 2 == 1) 1.0 else -1.0
        val parent = selectConnectionParent(zone, directionSign, placementRandom)
        val angleJitter = (placementRandom.nextDouble() - 0.5) * CONNECTION_DIRECTION_JITTER_RADIANS * 2.0
        val directionCos = cos(angleJitter)
        val directionSin = sin(angleJitter)
        val baseDirectionX = coastDirection.x * directionSign
        val baseDirectionZ = coastDirection.z * directionSign
        val directionX = baseDirectionX * directionCos - baseDirectionZ * directionSin
        val directionZ = baseDirectionX * directionSin + baseDirectionZ * directionCos
        val parentRadius = projectedRadius(parent.widthInBlocks, parent.lengthInBlocks, directionX, directionZ)
        val childRadius = projectedRadius(geometry.widthInBlocks, geometry.lengthInBlocks, directionX, directionZ)
        val minimumOverlap = if (zone.requestedSize == BioluminescentBloomSize.LARGE) {
            LARGE_MINIMUM_OVERLAP_RATIO
        } else {
            MINIMUM_OVERLAP_RATIO
        }
        val maximumOverlap = if (zone.requestedSize == BioluminescentBloomSize.LARGE) {
            LARGE_MAXIMUM_OVERLAP_RATIO
        } else {
            MAXIMUM_OVERLAP_RATIO
        }
        val overlapRatio = minimumOverlap + placementRandom.nextDouble() * (maximumOverlap - minimumOverlap)
        val desiredOverlap = minOf(parentRadius, childRadius) * overlapRatio
        val targetDistance = (parentRadius + childRadius - desiredOverlap).coerceAtLeast(2.0)
        val parentCenter = bloomCenter(parent)
        val approximateAnchor = BlockPos(
            (parentCenter.x + directionX * targetDistance).roundToInt(),
            parent.anchor.y,
            (parentCenter.z + directionZ * targetDistance).roundToInt()
        )
        val candidate = findCoastalCandidateNear(level, approximateAnchor.x, approximateAnchor.z)
            ?: return false
        registerAnalyzedCoverage(zone, candidate)
        if (connectionGap(parent, candidate.waterSurface, geometry) > MAX_CONNECTED_PLACEMENT_GAP) return false
        if (!candidateImprovesCoverage(zone, candidate, geometry, connector = false)) return false

        val bloom = createBloom(zone, candidate, seed, geometry, synchronizedBloomStartTick(zone)) ?: return false
        if (!luminousFootprintsOverlapOnWater(level, parent, bloom)) {
            discardNewBloom(zone, bloom)
            return false
        }
        registerAcceptedCoverage(zone, candidate, bloom)
        return true
    }

    private fun tryPlaceConnector(
        level: ClientLevel,
        zone: BioluminescentBloomZone,
        components: List<List<BioluminescentBloom>>
    ): Boolean {
        val pair = closestBloomsInDifferentComponents(components) ?: return false
        val firstCenter = bloomCenter(pair.first)
        val secondCenter = bloomCenter(pair.second)
        val deltaX = secondCenter.x - firstCenter.x
        val deltaZ = secondCenter.z - firstCenter.z
        val distance = hypot(deltaX, deltaZ)
        val seed = bloomSeed(zone.zoneSeed, zone.nextBloomIndex)
        val connectorRandom = XoroshiroRandomSource(
            RandomSupport.mixStafford13(seed xor CONNECTOR_GEOMETRY_SALT)
        )
        val longSide = (distance.roundToInt() + 4).coerceIn(8, 24)
        val shortSide = 6 + connectorRandom.nextInt(4)
        val geometry = if (abs(deltaX) >= abs(deltaZ)) {
            BioluminescentBloomGeometry(longSide, shortSide, BioluminescentBloomShape.ELONGATED, 0.0)
        } else {
            BioluminescentBloomGeometry(shortSide, longSide, BioluminescentBloomShape.ELONGATED, PI / 2.0)
        }
        val approximateAnchor = BlockPos(
            ((firstCenter.x + secondCenter.x) * 0.5).roundToInt(),
            pair.first.anchor.y,
            ((firstCenter.z + secondCenter.z) * 0.5).roundToInt()
        )
        val candidate = findCoastalCandidateNear(level, approximateAnchor.x, approximateAnchor.z)
            ?: return false
        registerAnalyzedCoverage(zone, candidate)
        if (connectionGap(pair.first, candidate.waterSurface, geometry) > MAX_CONNECTOR_GAP ||
            connectionGap(pair.second, candidate.waterSurface, geometry) > MAX_CONNECTOR_GAP
        ) return false
        if (!candidateImprovesCoverage(zone, candidate, geometry, connector = true)) return false

        val bloom = createBloom(zone, candidate, seed, geometry, synchronizedBloomStartTick(zone)) ?: return false
        if (!luminousFootprintsOverlapOnWater(level, pair.first, bloom) ||
            !luminousFootprintsOverlapOnWater(level, pair.second, bloom)
        ) {
            discardNewBloom(zone, bloom)
            return false
        }
        registerAcceptedCoverage(zone, candidate, bloom)
        return true
    }

    private fun discardNewBloom(zone: BioluminescentBloomZone, bloom: BioluminescentBloom) {
        zone.blooms.remove(bloom)
        blooms.remove(bloom)
        bloom.close()
        cellOwnershipDirty = true
        if (maskRoundRobinIndex > blooms.size) maskRoundRobinIndex = 0
    }

    private fun selectConnectionParent(
        zone: BioluminescentBloomZone,
        directionSign: Double,
        random: XoroshiroRandomSource
    ): BioluminescentBloom {
        if (zone.blooms.size == 1 || random.nextDouble() < CONNECTION_BRANCH_CHANCE) {
            return zone.blooms[random.nextInt(zone.blooms.size)]
        }
        return zone.blooms.maxBy { bloom ->
            val center = bloomCenter(bloom)
            val offsetX = center.x - zone.anchor.x
            val offsetZ = center.z - zone.anchor.z
            (offsetX * zone.coastDirectionX + offsetZ * zone.coastDirectionZ) * directionSign
        }
    }

    private fun candidateImprovesCoverage(
        zone: BioluminescentBloomZone,
        candidate: CoastalCandidate,
        geometry: BioluminescentBloomGeometry,
        connector: Boolean
    ): Boolean {
        val originX = candidate.waterSurface.x - geometry.widthInBlocks / 2
        val originZ = candidate.waterSurface.z - geometry.lengthInBlocks / 2
        var footprintWaterCells = 0
        var newWaterCells = 0
        for (cell in candidate.waterCells) {
            val cellX = cell.waterPos.x - originX
            val cellZ = cell.waterPos.z - originZ
            if (cellX !in 0 until geometry.widthInBlocks || cellZ !in 0 until geometry.lengthInBlocks) continue
            footprintWaterCells++
            if (worldCellKey(cell.waterPos.x, cell.waterPos.z) !in zone.coveredWaterCells) newWaterCells++
        }
        val minimumNewCells = if (connector) {
            MINIMUM_NEW_CONNECTOR_CELLS
        } else {
            max(MINIMUM_NEW_BLOOM_CELLS, (footprintWaterCells * MINIMUM_NEW_COVERAGE_RATIO).roundToInt())
        }
        return newWaterCells >= minimumNewCells
    }

    private fun registerAnalyzedCoverage(zone: BioluminescentBloomZone, candidate: CoastalCandidate) {
        candidate.waterCells.forEach { cell ->
            zone.analyzedWaterCells.add(worldCellKey(cell.waterPos.x, cell.waterPos.z))
        }
    }

    private fun registerAcceptedCoverage(
        zone: BioluminescentBloomZone,
        candidate: CoastalCandidate,
        bloom: BioluminescentBloom
    ) {
        registerAnalyzedCoverage(zone, candidate)
        for (cell in candidate.waterCells) {
            val cellX = cell.waterPos.x - bloom.originX
            val cellZ = cell.waterPos.z - bloom.originZ
            if (cellX !in 0 until bloom.widthInBlocks || cellZ !in 0 until bloom.lengthInBlocks) continue
            val cellIndex = cellZ * bloom.widthInBlocks + cellX
            if (bloom.texture.coverageAtCell(cellIndex) < MINIMUM_CELL_COVERAGE) continue
            zone.coveredWaterCells.add(worldCellKey(cell.waterPos.x, cell.waterPos.z))
        }
    }

    private fun shouldFinishZoneGeneration(zone: BioluminescentBloomZone): Boolean {
        if (zone.blooms.size >= zone.targetBloomCount) return true
        return zone.requestedSize == BioluminescentBloomSize.LARGE &&
            zone.blooms.size >= MINIMUM_CONNECTED_LARGE_BLOOMS &&
            zone.connectedComponentCount <= 1 &&
            zone.coverageRatio() >= LARGE_ZONE_COVERAGE_TARGET
    }

    private fun connectedPlacementDelay(zone: BioluminescentBloomZone): Long {
        val mixed = RandomSupport.mixStafford13(
            zone.zoneSeed xor zone.nextBloomIndex.toLong() xor PLACEMENT_DELAY_SALT
        )
        return MIN_CONNECTED_PLACEMENT_DELAY_TICKS +
            Math.floorMod(mixed, MAX_CONNECTED_PLACEMENT_DELAY_TICKS - MIN_CONNECTED_PLACEMENT_DELAY_TICKS + 1L)
    }

    private fun synchronizedBloomStartTick(zone: BioluminescentBloomZone): Long {
        val stagger = minOf(
            zone.nextBloomIndex * SYNCHRONIZED_START_STAGGER_TICKS,
            MAX_SYNCHRONIZED_START_STAGGER_TICKS
        )
        return zone.createdAt + stagger
    }

    private fun connectedComponents(blooms: List<BioluminescentBloom>): List<List<BioluminescentBloom>> {
        if (blooms.isEmpty()) return emptyList()
        val visited = BooleanArray(blooms.size)
        val result = ArrayList<List<BioluminescentBloom>>()
        val queue = java.util.ArrayDeque<Int>()

        for (startIndex in blooms.indices) {
            if (visited[startIndex]) continue
            val component = ArrayList<BioluminescentBloom>()
            visited[startIndex] = true
            queue.addLast(startIndex)
            while (queue.isNotEmpty()) {
                val index = queue.removeFirst()
                val bloom = blooms[index]
                component.add(bloom)
                for (otherIndex in blooms.indices) {
                    if (visited[otherIndex]) continue
                    if (!luminousFootprintsOverlap(bloom, blooms[otherIndex])) continue
                    visited[otherIndex] = true
                    queue.addLast(otherIndex)
                }
            }
            result.add(component)
        }
        return result
    }

    private fun closestBloomsInDifferentComponents(
        components: List<List<BioluminescentBloom>>
    ): Pair<BioluminescentBloom, BioluminescentBloom>? {
        var closestPair: Pair<BioluminescentBloom, BioluminescentBloom>? = null
        var closestDistanceSquared = Double.POSITIVE_INFINITY
        for (firstComponentIndex in 0 until components.lastIndex) {
            for (secondComponentIndex in firstComponentIndex + 1 until components.size) {
                for (first in components[firstComponentIndex]) {
                    val firstCenter = bloomCenter(first)
                    for (second in components[secondComponentIndex]) {
                        val secondCenter = bloomCenter(second)
                        val dx = firstCenter.x - secondCenter.x
                        val dz = firstCenter.z - secondCenter.z
                        val distanceSquared = dx * dx + dz * dz
                        if (distanceSquared < closestDistanceSquared) {
                            closestDistanceSquared = distanceSquared
                            closestPair = first to second
                        }
                    }
                }
            }
        }
        return closestPair
    }

    private fun luminousFootprintsOverlap(
        first: BioluminescentBloom,
        second: BioluminescentBloom
    ): Boolean {
        val minimumX = maxOf(first.originX, second.originX)
        val maximumX = minOf(
            first.originX + first.widthInBlocks - 1,
            second.originX + second.widthInBlocks - 1
        )
        val minimumZ = maxOf(first.originZ, second.originZ)
        val maximumZ = minOf(
            first.originZ + first.lengthInBlocks - 1,
            second.originZ + second.lengthInBlocks - 1
        )
        if (minimumX > maximumX || minimumZ > maximumZ) return false

        for (worldZ in minimumZ..maximumZ) {
            for (worldX in minimumX..maximumX) {
                val firstIndex = (worldZ - first.originZ) * first.widthInBlocks + worldX - first.originX
                if (first.texture.coverageAtCell(firstIndex) < MINIMUM_CONNECTIVITY_COVERAGE) continue
                val secondIndex = (worldZ - second.originZ) * second.widthInBlocks + worldX - second.originX
                if (second.texture.coverageAtCell(secondIndex) >= MINIMUM_CONNECTIVITY_COVERAGE) return true
            }
        }
        return false
    }

    private fun luminousFootprintsOverlapOnWater(
        level: ClientLevel,
        first: BioluminescentBloom,
        second: BioluminescentBloom
    ): Boolean {
        val minimumX = maxOf(first.originX, second.originX)
        val maximumX = minOf(
            first.originX + first.widthInBlocks - 1,
            second.originX + second.widthInBlocks - 1
        )
        val minimumZ = maxOf(first.originZ, second.originZ)
        val maximumZ = minOf(
            first.originZ + first.lengthInBlocks - 1,
            second.originZ + second.lengthInBlocks - 1
        )
        if (minimumX > maximumX || minimumZ > maximumZ) return false

        val samples = ArrayList<FootprintConnectionSample>()
        for (worldZ in minimumZ..maximumZ) {
            for (worldX in minimumX..maximumX) {
                val firstIndex = (worldZ - first.originZ) * first.widthInBlocks + worldX - first.originX
                val firstCoverage = first.texture.coverageAtCell(firstIndex)
                if (firstCoverage < MINIMUM_CONNECTIVITY_COVERAGE) continue
                val secondIndex = (worldZ - second.originZ) * second.widthInBlocks + worldX - second.originX
                val secondCoverage = second.texture.coverageAtCell(secondIndex)
                if (secondCoverage < MINIMUM_CONNECTIVITY_COVERAGE) continue
                if (first.hasWaterSurfaceAt(firstIndex) || second.hasWaterSurfaceAt(secondIndex)) return true
                samples.add(
                    FootprintConnectionSample(
                        worldX = worldX,
                        worldZ = worldZ,
                        score = minOf(firstCoverage, secondCoverage)
                    )
                )
            }
        }

        samples.sortByDescending(FootprintConnectionSample::score)
        val referenceY = (first.anchor.y + second.anchor.y) / 2
        for (index in 0 until minOf(samples.size, MAX_CONNECTION_WATER_PROBES)) {
            val sample = samples[index]
            if (isRenderableConnectionWater(level, sample.worldX, sample.worldZ, referenceY)) return true
        }
        return false
    }

    private fun isRenderableConnectionWater(
        level: ClientLevel,
        worldX: Int,
        worldZ: Int,
        referenceY: Int
    ): Boolean {
        if (!level.hasChunk(worldX shr 4, worldZ shr 4)) return false
        val waterBlock = ModUtilities.findWaterBlockBelow(
            level = level,
            x = worldX,
            z = worldZ,
            startY = minOf(level.maxY - 1, referenceY + CANDIDATE_WATER_VERTICAL_RADIUS),
            minimumY = maxOf(level.minY, referenceY - CANDIDATE_WATER_VERTICAL_RADIUS)
        ) ?: return false
        val topWaterBlock = ModUtilities.findTopWaterBlock(level, waterBlock) ?: return false
        return abs(topWaterBlock.y - referenceY) <= CONNECTION_WATER_LEVEL_TOLERANCE &&
            ModUtilities.isRenderableWaterSurface(level, topWaterBlock)
    }

    private fun connectionGap(
        first: BioluminescentBloom,
        secondAnchor: BlockPos,
        secondGeometry: BioluminescentBloomGeometry
    ): Double {
        val firstCenter = bloomCenter(first)
        val secondCenter = geometryCenter(secondAnchor, secondGeometry)
        return scaledBoundsGap(
            firstCenter.x,
            firstCenter.z,
            first.widthInBlocks,
            first.lengthInBlocks,
            secondCenter.x,
            secondCenter.z,
            secondGeometry.widthInBlocks,
            secondGeometry.lengthInBlocks
        )
    }

    private fun scaledBoundsGap(
        firstX: Double,
        firstZ: Double,
        firstWidth: Int,
        firstLength: Int,
        secondX: Double,
        secondZ: Double,
        secondWidth: Int,
        secondLength: Int
    ): Double {
        val gapX = (
            abs(firstX - secondX) -
                (firstWidth + secondWidth) * 0.5 * VISUAL_EXTENT_SCALE
            ).coerceAtLeast(0.0)
        val gapZ = (
            abs(firstZ - secondZ) -
                (firstLength + secondLength) * 0.5 * VISUAL_EXTENT_SCALE
            ).coerceAtLeast(0.0)
        return hypot(gapX, gapZ)
    }

    private fun projectedRadius(width: Int, length: Int, directionX: Double, directionZ: Double): Double {
        return (
            abs(directionX) * width * 0.5 +
                abs(directionZ) * length * 0.5
            ) * VISUAL_EXTENT_SCALE
    }

    private fun bloomCenter(bloom: BioluminescentBloom): HorizontalDirection {
        return HorizontalDirection(
            bloom.originX + bloom.widthInBlocks * 0.5,
            bloom.originZ + bloom.lengthInBlocks * 0.5
        )
    }

    private fun geometryCenter(
        anchor: BlockPos,
        geometry: BioluminescentBloomGeometry
    ): HorizontalDirection {
        val originX = anchor.x - geometry.widthInBlocks / 2
        val originZ = anchor.z - geometry.lengthInBlocks / 2
        return HorizontalDirection(
            originX + geometry.widthInBlocks * 0.5,
            originZ + geometry.lengthInBlocks * 0.5
        )
    }

    private fun estimateCoastDirection(candidate: CoastalCandidate, seed: Long): HorizontalDirection {
        if (candidate.waterCells.size < 2) return fallbackDirection(seed)
        val meanX = candidate.waterCells.sumOf { it.waterPos.x.toDouble() } / candidate.waterCells.size
        val meanZ = candidate.waterCells.sumOf { it.waterPos.z.toDouble() } / candidate.waterCells.size
        var covarianceX = 0.0
        var covarianceZ = 0.0
        var covarianceXZ = 0.0
        for (cell in candidate.waterCells) {
            val dx = cell.waterPos.x - meanX
            val dz = cell.waterPos.z - meanZ
            covarianceX += dx * dx
            covarianceZ += dz * dz
            covarianceXZ += dx * dz
        }
        val covarianceTotal = covarianceX + covarianceZ
        val anisotropy = if (covarianceTotal <= 0.0) {
            0.0
        } else {
            hypot(covarianceX - covarianceZ, 2.0 * covarianceXZ) / covarianceTotal
        }
        if (anisotropy < MINIMUM_COAST_DIRECTION_ANISOTROPY) return fallbackDirection(seed)
        val angle = 0.5 * atan2(2.0 * covarianceXZ, covarianceX - covarianceZ)
        return HorizontalDirection(cos(angle), sin(angle))
    }

    private fun fallbackDirection(seed: Long): HorizontalDirection {
        val mixed = RandomSupport.mixStafford13(seed xor COAST_DIRECTION_SALT)
        val angle = stableUnitValue(mixed) * PI * 2.0
        return HorizontalDirection(cos(angle), sin(angle))
    }

    private fun findCoastalCandidateNear(level: ClientLevel, worldX: Int, worldZ: Int): CoastalCandidate? {
        if (!level.hasChunk(worldX shr 4, worldZ shr 4)) return null
        val surfaceHintY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1
        val waterSurface = ModUtilities.findNearbyWaterSurface(
            level = level,
            center = BlockPos(worldX, surfaceHintY, worldZ),
            horizontalRadius = CANDIDATE_WATER_SEARCH_RADIUS,
            verticalRadius = CANDIDATE_WATER_VERTICAL_RADIUS
        ) ?: return null
        if (!ModUtilities.isRenderableWaterSurface(level, waterSurface)) return null

        if (!level.getBiome(waterSurface).`is`(BiomeTags.IS_BEACH)) return null

        val waterCells = ArrayList<BioluminescentWaterCell>(COASTAL_SAMPLE_DIAMETER * COASTAL_SAMPLE_DIAMETER)
        val startY = minOf(level.maxY - 1, waterSurface.y + WATER_LEVEL_TOLERANCE)
        val minimumY = maxOf(level.minY, waterSurface.y - WATER_LEVEL_TOLERANCE)

        for (offsetX in -COASTAL_SAMPLE_RADIUS..COASTAL_SAMPLE_RADIUS) {
            for (offsetZ in -COASTAL_SAMPLE_RADIUS..COASTAL_SAMPLE_RADIUS) {
                val x = waterSurface.x + offsetX
                val z = waterSurface.z + offsetZ
                if (!level.hasChunk(x shr 4, z shr 4)) continue

                val waterBlock = ModUtilities.findWaterBlockBelow(
                    level = level,
                    x = x,
                    z = z,
                    startY = startY,
                    minimumY = minimumY
                )
                val topWaterBlock = waterBlock?.let { ModUtilities.findTopWaterBlock(level, it) }
                if (topWaterBlock != null &&
                    kotlin.math.abs(topWaterBlock.y - waterSurface.y) <= WATER_LEVEL_TOLERANCE &&
                    ModUtilities.isRenderableWaterSurface(level, topWaterBlock)
                ) {
                    waterCells.add(
                        BioluminescentWaterCell(
                            topWaterBlock,
                            ModUtilities.getFluidSurfaceHeight(level, topWaterBlock)
                        )
                    )
                }
            }
        }

        if (waterCells.size < MINIMUM_CANDIDATE_WATER_CELLS) return null

        return CoastalCandidate(
            waterSurface = waterSurface,
            waterCells = waterCells
        )
    }

    private fun isFarEnoughFromOtherZones(candidate: CoastalCandidate): Boolean {
        val minimumDistanceSquared = MIN_DISTANCE_BETWEEN_ZONES * MIN_DISTANCE_BETWEEN_ZONES
        return zones.all { zone ->
            val dx = zone.anchor.x - candidate.waterSurface.x
            val dz = zone.anchor.z - candidate.waterSurface.z
            dx.toDouble() * dx.toDouble() + dz.toDouble() * dz.toDouble() >= minimumDistanceSquared
        }
    }

    private fun createGeometry(
        seed: Long,
        size: BioluminescentBloomSize,
        preferredDirection: HorizontalDirection
    ): BioluminescentBloomGeometry {
        val random = XoroshiroRandomSource(RandomSupport.mixStafford13(seed xor BLOOM_GEOMETRY_SALT))
        val shape = when (random.nextInt(100)) {
            in 0..9 -> BioluminescentBloomShape.ELLIPTICAL
            in 10..19 -> BioluminescentBloomShape.RECTANGULAR
            in 20..44 -> BioluminescentBloomShape.ELONGATED
            else -> BioluminescentBloomShape.CLUSTERED
        }
        val primary = randomInRange(random, size.minimumBlocks, size.maximumBlocks)
        val secondary = randomInRange(random, size.minimumBlocks, size.maximumBlocks)

        val initialDimensions = if (shape == BioluminescentBloomShape.ELONGATED) {
            val shortMaximum = when (size) {
                BioluminescentBloomSize.SMALL -> 9
                BioluminescentBloomSize.MEDIUM -> 14
                BioluminescentBloomSize.LARGE -> 22
            }
            val shortSide = randomInRange(random, BioluminescentBloomSize.SMALL.minimumBlocks, shortMaximum)
            if (random.nextBoolean()) primary to shortSide else shortSide to primary
        } else {
            primary to secondary
        }

        val dimensions = if (
            shape == BioluminescentBloomShape.ELONGATED ||
            shape == BioluminescentBloomShape.RECTANGULAR
        ) {
            val longSide = max(initialDimensions.first, initialDimensions.second)
            val shortSide = minOf(initialDimensions.first, initialDimensions.second)
            if (abs(preferredDirection.x) >= abs(preferredDirection.z)) {
                longSide to shortSide
            } else {
                shortSide to longSide
            }
        } else {
            initialDimensions
        }
        val rotationRadians = if (
            shape == BioluminescentBloomShape.ELONGATED ||
            shape == BioluminescentBloomShape.RECTANGULAR
        ) {
            val baseRotation = if (dimensions.first >= dimensions.second) 0.0 else PI / 2.0
            baseRotation + (random.nextDouble() - 0.5) * MAXIMUM_AXIS_ROTATION_JITTER * 2.0
        } else {
            null
        }

        return BioluminescentBloomGeometry(
            widthInBlocks = dimensions.first.coerceIn(MINIMUM_BLOOM_SIZE, MAXIMUM_BLOOM_SIZE),
            lengthInBlocks = dimensions.second.coerceIn(MINIMUM_BLOOM_SIZE, MAXIMUM_BLOOM_SIZE),
            shape = shape,
            rotationRadians = rotationRadians
        )
    }

    private fun plannedBloomCount(
        size: BioluminescentBloomSize,
        random: XoroshiroRandomSource
    ): Int {
        val base = when (size) {
            BioluminescentBloomSize.SMALL -> 2 + random.nextInt(2)
            BioluminescentBloomSize.MEDIUM -> 3 + random.nextInt(3)
            BioluminescentBloomSize.LARGE -> 5 + random.nextInt(4)
        }
        return base.coerceIn(MIN_BLOOMS_PER_ZONE, MAX_BLOOMS_PER_ZONE)
    }

    private fun chooseNaturalSize(random: XoroshiroRandomSource): BioluminescentBloomSize {
        val roll = random.nextDouble()
        return when {
            roll < 0.18 -> BioluminescentBloomSize.LARGE
            roll < 0.72 -> BioluminescentBloomSize.MEDIUM
            else -> BioluminescentBloomSize.SMALL
        }
    }

    private fun childSize(
        requestedSize: BioluminescentBloomSize,
        index: Int,
        random: XoroshiroRandomSource
    ): BioluminescentBloomSize {
        return when (requestedSize) {
            BioluminescentBloomSize.SMALL -> BioluminescentBloomSize.SMALL
            BioluminescentBloomSize.MEDIUM -> if (index % 3 == 0) {
                BioluminescentBloomSize.SMALL
            } else {
                BioluminescentBloomSize.MEDIUM
            }
            BioluminescentBloomSize.LARGE -> when {
                index % 4 == 0 -> BioluminescentBloomSize.SMALL
                random.nextBoolean() -> BioluminescentBloomSize.MEDIUM
                else -> BioluminescentBloomSize.LARGE
            }
        }
    }

    private fun randomRingPosition(
        center: BlockPos,
        minimumDistance: Double,
        maximumDistance: Double,
        random: XoroshiroRandomSource
    ): BlockPos {
        val angle = random.nextDouble() * PI * 2.0
        val minimumSquared = minimumDistance * minimumDistance
        val maximumSquared = maximumDistance * maximumDistance
        val distance = sqrt(minimumSquared + random.nextDouble() * (maximumSquared - minimumSquared))
        return BlockPos(
            (center.x + cos(angle) * distance).roundToInt(),
            center.y,
            (center.z + sin(angle) * distance).roundToInt()
        )
    }

    private fun randomInRange(random: XoroshiroRandomSource, minimum: Int, maximum: Int): Int {
        return minimum + random.nextInt(maximum - minimum + 1)
    }

    private fun bloomSeed(zoneSeed: Long, index: Int): Long {
        return RandomSupport.mixStafford13(
            zoneSeed xor RandomSupport.mixStafford13(index.toLong() xor BLOOM_SEED_SALT)
        )
    }

    private fun nextTextureIdentifier(): Identifier {
        textureCounter++
        val suffix = java.lang.Long.toUnsignedString(textureCounter, 16)
        return Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "dynamic/bioluminescent_water_$suffix"
        )
    }

    private fun reservedBloomCount(): Int {
        return zones.sumOf { zone ->
            if (zone.generationComplete) zone.blooms.size else max(zone.blooms.size, zone.targetBloomCount)
        }
    }

    private const val SPAWN_SEARCH_MIN_DISTANCE = 8.0
    private const val SPAWN_SEARCH_MAX_DISTANCE = 48.0
    private const val DEBUG_SEARCH_MIN_DISTANCE = 4.0
    private const val DEBUG_SPAWN_ATTEMPTS = 20
    private const val CANDIDATE_WATER_SEARCH_RADIUS = 4
    private const val CANDIDATE_WATER_VERTICAL_RADIUS = 8
    private const val COASTAL_SAMPLE_RADIUS = 8
    private const val COASTAL_SAMPLE_DIAMETER = COASTAL_SAMPLE_RADIUS * 2 + 1
    private const val MINIMUM_CANDIDATE_WATER_CELLS = 16
    private const val WATER_LEVEL_TOLERANCE = 3
    private const val MASK_COLUMN_BUDGET_PER_TICK = 768
    private const val MASK_COLUMN_SLICE = 192
    private const val MINIMUM_CELL_COVERAGE = 0.002f
    private const val OWNERSHIP_DENSITY_WEIGHT = 0.82
    private const val OWNERSHIP_CENTER_WEIGHT = 0.175
    private const val OWNERSHIP_SEED_BIAS_WEIGHT = 0.004
    private const val OWNERSHIP_CELL_BIAS_WEIGHT = 0.001
    private const val TEXTURE_UPDATE_DISTANCE = REMOVAL_DISTANCE
    private const val MAX_TEXTURE_UPLOAD_COST_PER_TICK = 32
    private const val MAX_TEXTURE_UPLOADS_PER_TICK = 3
    private const val MINIMUM_BLOOM_SIZE = 6
    private const val MAXIMUM_BLOOM_SIZE = 48
    private const val MIN_BLOOMS_PER_ZONE = 2
    private const val MAX_BLOOMS_PER_ZONE = 8
    private const val MINIMUM_ZONE_LIFETIME_TICKS = 600L
    private const val MAXIMUM_ZONE_LIFETIME_TICKS = 1200L
    private const val MINIMUM_BLOOM_LIFETIME_TICKS = 600L
    private const val MIN_CONNECTED_PLACEMENT_DELAY_TICKS = 4L
    private const val MAX_CONNECTED_PLACEMENT_DELAY_TICKS = 8L
    private const val SYNCHRONIZED_START_STAGGER_TICKS = 3L
    private const val MAX_SYNCHRONIZED_START_STAGGER_TICKS = 18L
    private const val PLACEMENT_RETRY_INTERVAL_TICKS = 2L
    private const val MAX_PLACEMENT_ATTEMPTS_PER_BLOOM = 10
    private const val MINIMUM_CONNECTED_LARGE_BLOOMS = 5
    private const val LARGE_ZONE_COVERAGE_TARGET = 0.70
    private const val MINIMUM_NEW_COVERAGE_RATIO = 0.08
    private const val MINIMUM_NEW_BLOOM_CELLS = 8
    private const val MINIMUM_NEW_CONNECTOR_CELLS = 4
    private const val MINIMUM_OVERLAP_RATIO = 0.32
    private const val MAXIMUM_OVERLAP_RATIO = 0.50
    private const val LARGE_MINIMUM_OVERLAP_RATIO = 0.48
    private const val LARGE_MAXIMUM_OVERLAP_RATIO = 0.68
    private const val VISUAL_EXTENT_SCALE = 0.76
    private const val MAX_CONNECTED_PLACEMENT_GAP = 0.75
    private const val MAX_CONNECTOR_GAP = 0.75
    private const val MINIMUM_CONNECTIVITY_COVERAGE = 0.03f
    private const val MAX_CONNECTION_WATER_PROBES = 12
    private const val CONNECTION_WATER_LEVEL_TOLERANCE = 4
    private const val CONNECTION_DIRECTION_JITTER_RADIANS = 0.30
    private const val CONNECTION_BRANCH_CHANCE = 0.12
    private const val MAXIMUM_AXIS_ROTATION_JITTER = 0.20
    private const val MINIMUM_COAST_DIRECTION_ANISOTROPY = 0.08
    private const val TEMPORAL_RANDOM_SALT = 0x5BE0CD19137E2179L
    private const val ZONE_SEED_SALT = 0x428A2F98D728AE22L
    private const val ZONE_GEOMETRY_SALT = 0x7137449123EF65CDL
    private const val BLOOM_SEED_SALT = 0x3956C25BF348B538L
    private const val BLOOM_GEOMETRY_SALT = 0x59F111F1B605D019L
    private const val END_TICK_SALT = 0x106AA07032BBD1B8L
    private const val CHILD_SIZE_SALT = 0x2B7E151628AED2A6L
    private const val PLACEMENT_ATTEMPT_SALT = 0x3243F6A8885A308DL
    private const val PLACEMENT_DELAY_SALT = 0x6C8E9CF570932BD5L
    private const val CONNECTOR_GEOMETRY_SALT = 0x1D8E4E27C47D124FL
    private const val COAST_DIRECTION_SALT = 0x4A7484AA6EA6E483L
    private const val OWNERSHIP_SEED_BIAS_SALT = 0x452821E638D01377L
    private const val OWNERSHIP_CELL_BIAS_SALT = 0x5BE0CD19137E2179L
}
