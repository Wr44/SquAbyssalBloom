package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.compat.iris.IrisRenderState
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.BioluminescentZoneDynamicLights
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterCell
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomainCollector
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.field.BioluminescentEmissionField
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationStage
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.noise.BioluminescentRegionalNoiseSampler
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPaletteFamily
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPalettes
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.texture.BioluminescentZoneTile
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePresets
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneSize
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
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
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentZoneManager {
    private const val MAX_ACTIVE_ZONES = 2
    private const val MAX_GLOBAL_TILES = 128
    private const val MIN_DISTANCE_BETWEEN_ZONES = 48.0
    private const val REMOVAL_DISTANCE = 96.0
    private const val SPAWN_SCAN_INTERVAL = 40L
    private const val NATURAL_SPAWN_ATTEMPTS = 8
    private const val BASE_SPAWN_CHANCE = 0.018
    private const val NATURAL_INACTIVE_CHANCE = 0.45
    private const val SPAWN_SEARCH_MIN_DISTANCE = 8.0
    private const val SPAWN_SEARCH_MAX_DISTANCE = 48.0
    private const val DEBUG_SEARCH_MIN_DISTANCE = 4.0
    private const val DEBUG_SPAWN_ATTEMPTS = 20
    private const val CANDIDATE_SEARCH_RADIUS = 4
    private const val CANDIDATE_VERTICAL_RADIUS = 8
    private const val CANDIDATE_SAMPLE_RADIUS = 8
    private const val MIN_NEARBY_WATER_CELLS = 16
    private const val WATER_LEVEL_TOLERANCE = 3
    private const val MIN_ZONE_LIFETIME = 600L
    private const val MAX_ZONE_LIFETIME = 1200L
    private const val MAX_RETAINED_FAILURES = 6
    private const val TEMPORAL_RANDOM_SALT = 0x5BE0CD19137E2179L
    private const val ZONE_SEED_SALT = 0x428A2F98D728AE22L
    private const val LIFETIME_SALT = 0x7137449123EF65CDL
    private const val COLOR_PHASE_SALT = 0x3956C25BF348B538L

    sealed interface SpawnResult {
        data class Created(
            val anchor: BlockPos,
            val distance: Double,
            val size: BioluminescentZoneSize,
            val activity: BioluminescentZoneActivity
        ) : SpawnResult

        data object NoCoastalSurface : SpawnResult
        data object ZoneLimitReached : SpawnResult
        data object DimensionNotAllowed : SpawnResult
        data object NoLevel : SpawnResult
    }

    private data class CoastalCandidate(
        val waterSurface: BlockPos,
        val initialCell: BioluminescentWaterCell,
        val nearbyWaterCells: Int
    )

    private val zones = mutableListOf<BioluminescentZone>()
    private val recentFailures = ArrayDeque<String>()

    val activeZones: List<BioluminescentZone>
        get() = zones

    private var currentLevel: ClientLevel? = null
    private var levelIdentitySeed = 0L
    private var temporalRandom: XoroshiroRandomSource? = null
    private var regionalNoise: BioluminescentRegionalNoiseSampler? = null
    private var lastSpawnScanTick: Long? = null
    private var remainingNaturalSpawnAttempts = 0
    private var generationRoundRobin = 0
    private var spawnCounter = 0L
    private var textureCounter = 0L

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
            clearActiveZones("dimension")
            return
        }

        val gameTime = level.gameTime
        removeFinishedAndDistantZones(player.x, player.z, gameTime)
        val generationAdvanced = advanceOneGeneration(level, gameTime)
        zones.forEach { zone ->
            if (zone.isReady) zone.updateMovementWaves(level, gameTime)
        }
        BioluminescentZoneDynamicLights.updateDynamicState(zones, gameTime)

        if (isSpawnScanDue(gameTime)) {
            lastSpawnScanTick = gameTime
            remainingNaturalSpawnAttempts = NATURAL_SPAWN_ATTEMPTS
        }
        if (remainingNaturalSpawnAttempts > 0 && !generationAdvanced) {
            remainingNaturalSpawnAttempts--
            tryNaturalSpawn(level, player.blockPosition(), gameTime)
        }
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
        size: BioluminescentZoneSize,
        paletteFamily: BioluminescentPaletteFamily,
        activity: BioluminescentZoneActivity = BioluminescentZoneActivity.ACTIVE
    ): SpawnResult {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return SpawnResult.NoLevel
        val player = minecraft.player ?: return SpawnResult.NoLevel
        ensureLevel(level)
        if (!isDimensionAllowed(level)) return SpawnResult.DimensionNotAllowed
        if (zones.size >= MAX_ACTIVE_ZONES) return SpawnResult.ZoneLimitReached
        val random = temporalRandom ?: return SpawnResult.NoLevel

        var bestCandidate = findCoastalCandidateNear(
            level,
            player.blockPosition().x,
            player.blockPosition().z
        )?.takeIf(::isFarEnoughFromOtherZones)
        var bestDistanceSqr = bestCandidate?.let { candidate ->
            horizontalDistanceSqr(candidate.waterSurface, player.x, player.z)
        } ?: Double.POSITIVE_INFINITY
        var bestWaterCellCount = bestCandidate?.nearbyWaterCells ?: -1
        repeat(DEBUG_SPAWN_ATTEMPTS) {
            val position = randomRingPosition(
                player.blockPosition(),
                DEBUG_SEARCH_MIN_DISTANCE,
                SPAWN_SEARCH_MAX_DISTANCE,
                random
            )
            val candidate = findCoastalCandidateNear(level, position.x, position.z) ?: return@repeat
            val distanceSqr = horizontalDistanceSqr(candidate.waterSurface, player.x, player.z)
            if (isFarEnoughFromOtherZones(candidate) &&
                (candidate.nearbyWaterCells > bestWaterCellCount ||
                    candidate.nearbyWaterCells == bestWaterCellCount && distanceSqr < bestDistanceSqr)
            ) {
                bestCandidate = candidate
                bestDistanceSqr = distanceSqr
                bestWaterCellCount = candidate.nearbyWaterCells
            }
        }
        val candidate = bestCandidate ?: return SpawnResult.NoCoastalSurface
        createZone(level, candidate, size, paletteFamily, activity, level.gameTime, true)
        return SpawnResult.Created(candidate.waterSurface, sqrt(bestDistanceSqr), size, activity)
    }

    fun clearDebugZones(): Int {
        val count = zones.size
        clearActiveZones("command")
        remainingNaturalSpawnAttempts = 0
        currentLevel?.let { lastSpawnScanTick = it.gameTime }
        return count
    }

    fun debugReport(): List<String> {
        val report = ArrayList<String>()
        val globalTiles = zones.sumOf { it.generationSnapshot().preparedTiles }
        report.add("[Bio Debug] ${zones.size} zone(s), $globalTiles/$MAX_GLOBAL_TILES tuiles")
        report.addAll(IrisRenderState.debugLines())
        report.add(
            "[Bio Debug] lumieres dynamiques: ${BioluminescentZoneDynamicLights.totalRegisteredLightCount()}"
        )
        report.add(
            "[Bio Debug] LOD=${BioluminescentZoneTile.lodDebugText()} px/bloc auto GPU progressif, " +
                "champ=${BioluminescentEmissionField.PIXELS_PER_BLOCK} echantillons/bloc, statique"
        )
        zones.forEachIndexed { index, zone ->
            val snapshot = zone.generationSnapshot()
            val data = zone.spatialData
            val identifier = java.lang.Long.toUnsignedString(zone.zoneSeed, 16)
            val domainText = data?.domain?.let { domain ->
                "${domain.localSize}/${domain.size}"
            } ?: "?/${snapshot.waterCells}"
            val radiusText = data?.domain?.let { domain ->
                "${domain.geodesicRadius}/${domain.analysisGeodesicRadius}"
            } ?: "${snapshot.geodesicRadius}/${snapshot.geodesicRadius + zone.preset.analysisMargin}"
            report.add(
                "[Bio Debug] Z$index seed=$identifier ${zone.preset.size.commandName} " +
                    "activite=${zone.activity.commandName} etat=${snapshot.stage} " +
                    "domaineLocal/analyse=$domainText rayonLocal/analyse=$radiusText " +
                    "cpu=${formatMilliseconds(snapshot.cpuNanos)}ms"
            )
            if (data == null) {
                report.add(
                    "[Bio Debug]   macro=${percent(zone.targetMacroCoverage)} " +
                        "visible=${percent(zone.targetVisibleCoverage)} noyaux=${snapshot.selectedCores} " +
                        "RD=${snapshot.reactionIterations}/${snapshot.targetReactionIterations} " +
                        "tuiles=${snapshot.preparedTiles}, uploads=${snapshot.uploadedTiles}"
                )
                snapshot.failureReason?.let { report.add("[Bio Debug]   echec=$it") }
                return@forEachIndexed
            }
            report.add("[Bio Debug]   limites=${data.domain.bounds.debugText}")
            report.add(
                "[Bio Debug]   macro=${percent(data.macroField.achievedCoverage)}/" +
                    "${percent(data.macroField.targetCoverage)} visible=" +
                    "${percent(data.emissionField.achievedVisibleCoverage)}/" +
                    "${percent(data.emissionField.targetVisibleCoverage)}"
            )
            report.add(
                "[Bio Debug]   noyaux=${data.topology.cores.size} " +
                    data.topology.cores.joinToString(prefix = "[", postfix = "]") { it.shape.name.lowercase() }
            )
            report.add(
                "[Bio Debug]   squelette=${data.topology.skeleton.totalLength} blocs, " +
                    "connexions=${data.topology.skeleton.connectionCount}"
            )
            report.add(
                "[Bio Debug]   RD=${data.reactionDiffusion.width}x${data.reactionDiffusion.length} " +
                    "feed=${formatDecimal(data.reactionDiffusion.feed)} " +
                    "kill=${formatDecimal(data.reactionDiffusion.kill)} " +
                    "iterations=${data.reactionDiffusion.iterations}"
            )
            report.add(
                "[Bio Debug]   tuiles=${data.tiles.size}, pixels=${data.preparedPixelCount}, " +
                    "pixelsTexture=${data.texturePixelCount}, quads=${data.renderQuadCount}, " +
                    "uploads=${data.uploadCount}"
            )
            report.add(
                "[Bio Debug]   alphaVisible=${percent(data.emissionField.averageVisibleAlpha)}, " +
                    "highlights=${percent(data.emissionField.highlightCoverage)}"
            )
            report.add(
                "[Bio Debug]   interactions: sourcesMobiles=${zone.movingEntityCount}, " +
                    "ondes=${zone.activeMovementWaveCount}, " +
                    "lumieres=${BioluminescentZoneDynamicLights.registeredLightCount(zone)}"
            )
        }
        recentFailures.forEach { report.add("[Bio Debug] echec recent: $it") }
        return report
    }

    fun isDimensionAllowed(level: ClientLevel): Boolean {
        val type = level.dimensionType()
        return type.hasSkyLight() &&
            !type.hasCeiling() &&
            type.skybox() == DimensionType.Skybox.OVERWORLD
    }

    private fun advanceOneGeneration(level: ClientLevel, gameTime: Long): Boolean {
        val generating = zones.filterNot { it.isReady || it.isFailed }
        if (generating.isEmpty()) return false
        if (generationRoundRobin >= generating.size) generationRoundRobin = 0
        val zone = generating[generationRoundRobin]
        generationRoundRobin = (generationRoundRobin + 1) % generating.size
        val otherTiles = zones.asSequence()
            .filterNot { it === zone }
            .sumOf { it.generationSnapshot().preparedTiles }
        val previousStage = zone.generationStage
        zone.advanceGeneration(
            level,
            Minecraft.getInstance().textureManager,
            ::nextTextureIdentifier,
            (MAX_GLOBAL_TILES - otherTiles).coerceAtLeast(0),
            gameTime
        )
        if (zone.isFailed) {
            val reason = zone.failureReason ?: "raison inconnue"
            rememberFailure(zone, reason)
            if (zone.requestedByCommand) {
                displayGenerationMessage(
                    "Echec de la generation ${zone.preset.size.commandName}: $reason"
                )
            }
            BioluminescentZoneDynamicLights.onZoneRemoved(zone)
            zones.remove(zone)
            zone.close()
            generationRoundRobin = 0
        } else if (zone.isReady && previousStage != BioluminescentZoneGenerationStage.READY) {
            BioluminescentZoneDynamicLights.onZoneReady(zone)
            val data = checkNotNull(zone.spatialData)
            SquAbyssalBloom.LOGGER.info(
                "[Bioluminescence] Zone {} {} {} prete: {}/{} cellules locales/analysees, " +
                    "macro {}%, visible {}%, {} tuiles",
                java.lang.Long.toUnsignedString(zone.zoneSeed, 16),
                zone.preset.size.commandName,
                zone.activity.commandName,
                data.domain.localSize,
                data.domain.size,
                (data.macroField.achievedCoverage * 100.0).roundToInt(),
                (data.emissionField.achievedVisibleCoverage * 100.0).roundToInt(),
                data.tiles.size
            )
            if (zone.requestedByCommand) {
                displayGenerationMessage(
                    "Zone ${zone.preset.size.commandName} ${zone.activity.commandName} prete: " +
                        "${data.domain.localSize}/" +
                        "${data.domain.size} cellules locales/analysees, " +
                        "${data.tiles.size} tuiles."
                )
            }
        }
        return true
    }

    private fun tryNaturalSpawn(level: ClientLevel, playerPos: BlockPos, gameTime: Long) {
        if (zones.size >= MAX_ACTIVE_ZONES) {
            remainingNaturalSpawnAttempts = 0
            return
        }
        val random = temporalRandom ?: return
        val position = randomRingPosition(
            playerPos,
            SPAWN_SEARCH_MIN_DISTANCE,
            SPAWN_SEARCH_MAX_DISTANCE,
            random
        )
        val candidate = findCoastalCandidateNear(level, position.x, position.z) ?: return
        if (!isFarEnoughFromOtherZones(candidate)) return
        val regional = regionalNoise?.sample(
            candidate.waterSurface.x.toDouble(),
            candidate.waterSurface.z.toDouble()
        ) ?: return
        val regionalFactor = 0.15 + 0.85 * ModUtilities.smooth(0.25, 0.75, regional)
        val capacityFactor = (MAX_ACTIVE_ZONES - zones.size).toDouble() / MAX_ACTIVE_ZONES
        if (random.nextDouble() >= BASE_SPAWN_CHANCE * regionalFactor * sqrt(capacityFactor)) return
        createZone(
            level,
            candidate,
            chooseNaturalSize(random),
            BioluminescentPaletteFamily.RANDOM,
            chooseNaturalActivity(random),
            gameTime,
            false
        )
    }

    private fun createZone(
        level: ClientLevel,
        candidate: CoastalCandidate,
        size: BioluminescentZoneSize,
        paletteFamily: BioluminescentPaletteFamily,
        activity: BioluminescentZoneActivity,
        gameTime: Long,
        requestedByCommand: Boolean
    ): BioluminescentZone {
        spawnCounter++
        val zoneSeed = RandomSupport.mixStafford13(
            levelIdentitySeed xor candidate.waterSurface.asLong() xor gameTime xor
                RandomSupport.mixStafford13(spawnCounter xor ZONE_SEED_SALT)
        )
        val random = XoroshiroRandomSource(RandomSupport.mixStafford13(zoneSeed xor LIFETIME_SALT))
        val lifetime = MIN_ZONE_LIFETIME +
            random.nextInt((MAX_ZONE_LIFETIME - MIN_ZONE_LIFETIME + 1L).toInt())
        val colorPhase = ModUtilities.stableUnitValue(
            RandomSupport.mixStafford13(zoneSeed xor COLOR_PHASE_SALT)
        ) * PI * 2.0
        val zone = BioluminescentZone(
            zoneSeed,
            candidate.waterSurface,
            BioluminescentPalettes.select(zoneSeed, paletteFamily),
            BioluminescentZonePresets.forSize(size),
            activity,
            candidate.initialCell,
            gameTime,
            lifetime,
            colorPhase,
            requestedByCommand
        )
        zones.add(zone)
        SquAbyssalBloom.LOGGER.info(
            "[Bioluminescence] Generation {} {} {} lancee a {} ({} cellules d'eau proches)",
            java.lang.Long.toUnsignedString(zoneSeed, 16),
            size.commandName,
            activity.commandName,
            candidate.waterSurface,
            candidate.nearbyWaterCells
        )
        return zone
    }

    private fun findCoastalCandidateNear(level: ClientLevel, worldX: Int, worldZ: Int): CoastalCandidate? {
        if (!level.hasChunk(worldX shr 4, worldZ shr 4)) return null
        val surfaceHint = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1
        val waterSurface = ModUtilities.findNearbyWaterSurface(
            level,
            BlockPos(worldX, surfaceHint, worldZ),
            CANDIDATE_SEARCH_RADIUS,
            CANDIDATE_VERTICAL_RADIUS
        ) ?: return null
        if (!ModUtilities.isRenderableWaterSurface(level, waterSurface)) return null
        if (!level.getBiome(waterSurface).`is`(BiomeTags.IS_BEACH)) return null
        val initialCell = BioluminescentWaterCell(
            waterSurface,
            ModUtilities.getFluidSurfaceHeight(level, waterSurface),
            BioluminescentWaterDomainCollector.scanWaterDepth(level, waterSurface)
        )
        val nearbyWater = countNearbyWaterCells(level, waterSurface)
        if (nearbyWater < MIN_NEARBY_WATER_CELLS) return null
        return CoastalCandidate(waterSurface, initialCell, nearbyWater)
    }

    private fun countNearbyWaterCells(level: ClientLevel, anchor: BlockPos): Int {
        var count = 0
        for (offsetX in -CANDIDATE_SAMPLE_RADIUS..CANDIDATE_SAMPLE_RADIUS) {
            for (offsetZ in -CANDIDATE_SAMPLE_RADIUS..CANDIDATE_SAMPLE_RADIUS) {
                val worldX = anchor.x + offsetX
                val worldZ = anchor.z + offsetZ
                if (!level.hasChunk(worldX shr 4, worldZ shr 4)) continue
                val water = ModUtilities.findWaterBlockBelow(
                    level,
                    worldX,
                    worldZ,
                    minOf(level.maxY - 1, anchor.y + WATER_LEVEL_TOLERANCE),
                    maxOf(level.minY, anchor.y - WATER_LEVEL_TOLERANCE)
                ) ?: continue
                val top = ModUtilities.findTopWaterBlock(level, water) ?: continue
                if (abs(top.y - anchor.y) <= WATER_LEVEL_TOLERANCE &&
                    ModUtilities.isRenderableWaterSurface(level, top)
                ) count++
            }
        }
        return count
    }

    private fun removeFinishedAndDistantZones(
        playerX: Double,
        playerZ: Double,
        gameTime: Long
    ) {
        val maxDistanceSqr = REMOVAL_DISTANCE * REMOVAL_DISTANCE
        for (index in zones.indices.reversed()) {
            val zone = zones[index]
            if (zone.horizontalDistanceSqr(playerX, playerZ) <= maxDistanceSqr &&
                !zone.isCompleteAt(gameTime)
            ) continue
            BioluminescentZoneDynamicLights.onZoneRemoved(zone)
            zones.removeAt(index)
            zone.close()
        }
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
        generationRoundRobin = 0
        spawnCounter = 0L
        recentFailures.clear()
    }

    private fun clearActiveZones(reason: String) {
        if (zones.isEmpty()) return
        zones.forEach(BioluminescentZone::close)
        zones.clear()
        BioluminescentZoneDynamicLights.clear()
        generationRoundRobin = 0
        SquAbyssalBloom.LOGGER.debug("[Bioluminescence] Toutes les zones supprimees ({})", reason)
    }

    private fun rememberFailure(zone: BioluminescentZone, reason: String) {
        val id = java.lang.Long.toUnsignedString(zone.zoneSeed, 16).takeLast(8)
        val message = "$id ${zone.preset.size.commandName}: $reason"
        recentFailures.addLast(message)
        while (recentFailures.size > MAX_RETAINED_FAILURES) recentFailures.removeFirst()
        SquAbyssalBloom.LOGGER.warn("[Bioluminescence] Echec de generation {}", message)
    }

    private fun displayGenerationMessage(message: String) {
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal(message))
    }

    private fun isSpawnScanDue(gameTime: Long): Boolean {
        val previous = lastSpawnScanTick ?: return false
        return gameTime < previous || gameTime - previous >= SPAWN_SCAN_INTERVAL
    }

    private fun isFarEnoughFromOtherZones(candidate: CoastalCandidate): Boolean {
        val minDistanceSqr = MIN_DISTANCE_BETWEEN_ZONES * MIN_DISTANCE_BETWEEN_ZONES
        return zones.all { zone ->
            horizontalDistanceSqr(candidate.waterSurface, zone.anchor.x + 0.5, zone.anchor.z + 0.5) >=
                minDistanceSqr
        }
    }

    private fun chooseNaturalSize(random: XoroshiroRandomSource): BioluminescentZoneSize {
        val roll = random.nextDouble()
        return when {
            roll < 0.18 -> BioluminescentZoneSize.LARGE
            roll < 0.72 -> BioluminescentZoneSize.MEDIUM
            else -> BioluminescentZoneSize.SMALL
        }
    }

    private fun chooseNaturalActivity(random: XoroshiroRandomSource): BioluminescentZoneActivity {
        return if (random.nextDouble() < NATURAL_INACTIVE_CHANCE) {
            BioluminescentZoneActivity.INACTIVE
        } else {
            BioluminescentZoneActivity.ACTIVE
        }
    }

    private fun randomRingPosition(
        center: BlockPos,
        minDistance: Double,
        maxDistance: Double,
        random: XoroshiroRandomSource
    ): BlockPos {
        val angle = random.nextDouble() * PI * 2.0
        val minSqr = minDistance * minDistance
        val maxSqr = maxDistance * maxDistance
        val distance = sqrt(minSqr + random.nextDouble() * (maxSqr - minSqr))
        return BlockPos(
            (center.x + cos(angle) * distance).roundToInt(),
            center.y,
            (center.z + sin(angle) * distance).roundToInt()
        )
    }

    private fun horizontalDistanceSqr(position: BlockPos, worldX: Double, worldZ: Double): Double {
        return ModUtilities.horizontalDistanceSqr(
            position.x + 0.5,
            position.z + 0.5,
            worldX,
            worldZ
        )
    }

    private fun nextTextureIdentifier(): Identifier {
        textureCounter++
        return Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "dynamic/bioluminescent_zone_${java.lang.Long.toUnsignedString(textureCounter, 16)}"
        )
    }

    private fun percent(value: Double): String = "${(value * 100.0).roundToInt()}%"

    private fun formatMilliseconds(nanos: Long): String = "%.2f".format(nanos / 1_000_000.0)

    private fun formatDecimal(value: Double): String = "%.4f".format(value)

}
