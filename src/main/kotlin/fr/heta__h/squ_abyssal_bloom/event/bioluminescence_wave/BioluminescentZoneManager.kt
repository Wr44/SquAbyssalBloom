package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import fr.heta__h.squ_abyssal_bloom.compat.iris.IrisRenderState
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.BioluminescentZoneDynamicLights
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveActivity
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveMode
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveSize
import fr.heta__h.squ_abyssal_bloom.network.bioluminescence.S2CBioluminescenceWavePayload
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterCell
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.domain.BioluminescentWaterDomainCollector
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerationStage
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.generation.BioluminescentZoneGenerator
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPaletteFamily
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPalettes
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZone
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZonePresets
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.RandomSupport
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent
import net.neoforged.neoforge.event.level.LevelEvent
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.PI
import kotlin.math.sqrt

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentZoneManager {
    private const val MAX_SYNCHRONIZED_EVENTS = 64
    private const val MAX_RETAINED_FAILURES = 6
    private const val MAX_GENERATION_ATTEMPTS = 3
    private const val RETRY_DELAY_TICKS = 200L
    private const val MAX_GPU_TILES = 128
    private const val COLOR_PHASE_SALT = 0x3956C25BF348B538L
    private const val WATER_REVALIDATION_BUDGET_PER_ZONE = 4

    private data class ClientWave(
        val payload: S2CBioluminescenceWavePayload,
        val serverGameTimeOffset: Long
    ) {
        fun sameEventData(other: ClientWave): Boolean {
            return payload.eventId == other.payload.eventId &&
                payload.seed == other.payload.seed &&
                payload.dimension == other.payload.dimension &&
                payload.beachId == other.payload.beachId &&
                payload.anchor == other.payload.anchor &&
                payload.bounds == other.payload.bounds &&
                payload.startGameTime == other.payload.startGameTime &&
                payload.endGameTime == other.payload.endGameTime &&
                payload.size == other.payload.size &&
                payload.mode == other.payload.mode
        }
    }

    private val synchronizedWaves: LinkedHashMap<UUID, ClientWave> = linkedMapOf()
    private val zonesByEventId: LinkedHashMap<UUID, BioluminescentZone> = linkedMapOf()
    private val retryAtGameTime: MutableMap<UUID, Long> = hashMapOf()
    private val generationAttemptsByEventId: MutableMap<UUID, Int> = hashMapOf()
    private val endingZoneIds: MutableSet<UUID> = linkedSetOf()
    private val recentFailures = ArrayDeque<String>()

    val activeZones: Collection<BioluminescentZone>
        get() = zonesByEventId.values

    private var currentLevel: ClientLevel? = null
    private var generationRoundRobin = 0
    private var textureCounter = 0L
    private var opportunityArmed = false
    private var nextNormalWaveEndClientGameTime = Long.MAX_VALUE

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val minecraft = Minecraft.getInstance()
        if (minecraft.isPaused) return
        val level = minecraft.level ?: run {
            resetLevel()
            return
        }
        if (minecraft.player == null) return
        ensureLevel(level)
        if (!ModUtilities.isOverworldLikeDimension(level)) {
            clearZones()
            synchronizedWaves.clear()
            retryAtGameTime.clear()
            generationAttemptsByEventId.clear()
            nextNormalWaveEndClientGameTime = Long.MAX_VALUE
            return
        }

        if (level.gameTime >= nextNormalWaveEndClientGameTime) {
            removeExpiredWaves(level.gameTime)
        }
        if (!ModConfig.enableBioluminescenceRendering) {
            clearZones()
            return
        }

        createPendingZones(level)
        advanceOneGeneration(level)
        advanceEndingZones(level.gameTime)
        for (zone in zonesByEventId.values) {
            if (!zone.isReady) continue
            zone.updateMovementWaves(level, level.gameTime)
            if (zone.activity == BioluminescentZoneActivity.ACTIVE) {
                zone.spawnAmbientParticles(level, level.gameTime)
            }
            zone.revalidateWaterStep(level, WATER_REVALIDATION_BUDGET_PER_ZONE)
        }
        BioluminescentZoneDynamicLights.updateDynamicState(zonesByEventId.values, level.gameTime)
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level === currentLevel) resetLevel()
    }

    @SubscribeEvent
    fun onClientStopping(event: ClientStoppingEvent) {
        resetLevel()
    }

    fun synchronizeWave(level: ClientLevel, payload: S2CBioluminescenceWavePayload) {
        ensureLevel(level)
        if (payload.dimension != level.dimension().identifier()) return
        val synchronized = ClientWave(
            payload,
            payload.serverGameTimeAtSend - level.gameTime
        )
        val previous = synchronizedWaves[payload.eventId]
        if (previous != null && previous.sameEventData(synchronized)) {
            zonesByEventId[payload.eventId]?.serverGameTimeOffset = synchronized.serverGameTimeOffset
            synchronizedWaves[payload.eventId] = synchronized
            refreshNextNormalWaveEndClientGameTime()
            return
        }
        if (previous != null) removeZone(payload.eventId)
        synchronizedWaves[payload.eventId] = synchronized
        retryAtGameTime.remove(payload.eventId)
        generationAttemptsByEventId.remove(payload.eventId)
        while (synchronizedWaves.size > MAX_SYNCHRONIZED_EVENTS) {
            val oldestEventId = synchronizedWaves.entries.first().key
            synchronizedWaves.remove(oldestEventId)
            retryAtGameTime.remove(oldestEventId)
            generationAttemptsByEventId.remove(oldestEventId)
            removeZone(oldestEventId)
        }
        refreshNextNormalWaveEndClientGameTime()
    }

    fun removeWave(eventId: UUID, gameTime: Long) {
        if (synchronizedWaves.remove(eventId) == null) return
        retryAtGameTime.remove(eventId)
        generationAttemptsByEventId.remove(eventId)
        val zone = zonesByEventId[eventId]
        if (zone != null && zone.mode == BioluminescenceWaveMode.TOTAL_NIGHT && zone.hasRenderableTiles) {
            zone.beginEnding(gameTime + zone.serverGameTimeOffset)
            endingZoneIds.add(eventId)
        } else {
            removeZone(eventId)
        }
        refreshNextNormalWaveEndClientGameTime()
    }

    fun setOpportunityArmed(armed: Boolean) {
        opportunityArmed = armed
    }

    fun debugReport(): List<String> {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level
        val playerPosition = minecraft.player?.let { player -> player.x to player.z }
        val renderGameTime = level?.gameTime?.toDouble() ?: 0.0
        val report = mutableListOf<String>()

        report.add(
            "[Bio Debug] -- informations recues du serveur --"
        )
        report.add(
            "[Bio Debug] dimension=${level?.dimension()?.identifier()} armed=$opportunityArmed " +
                "events=${synchronizedWaves.size} zones=${zonesByEventId.size} " +
                "renderingEnabled=${ModConfig.enableBioluminescenceRendering}"
        )
        for ((eventId, synchronized) in synchronizedWaves) {
            val zone = zonesByEventId[eventId]
            val serverNow = (level?.gameTime ?: 0L) + synchronized.serverGameTimeOffset
            val snapshot = zone?.generationSnapshot()
            val distance = if (playerPosition != null && zone != null) {
                sqrt(zone.horizontalDistanceSqr(playerPosition.first, playerPosition.second))
            } else {
                null
            }
            report.add(
                "[Bio Debug] -- generation client : ${eventId.toString().take(8)} --"
            )
            report.add(
                "[Bio Debug] ${eventId.toString().take(8)} ${synchronized.payload.mode.name.lowercase()} " +
                    "${synchronized.payload.size.name.lowercase()} ${synchronized.payload.activity.name.lowercase()} " +
                    "age=${serverNow - synchronized.payload.startGameTime} " +
                    "stage=${snapshot?.stage ?: "waiting_surface"} cellules=${snapshot?.waterCells ?: 0} " +
                    "noyaux=${snapshot?.selectedCores ?: 0}"
            )
            report.add(
                "[Bio Debug] ${eventId.toString().take(8)} reaction_diffusion=${snapshot?.reactionIterations ?: 0}" +
                    "/${snapshot?.targetReactionIterations ?: 0} " +
                    "tiles_preparees=${snapshot?.preparedTiles ?: 0} tiles_uploadees=${snapshot?.uploadedTiles ?: 0} " +
                    "tiles_visibles=${zone?.tiles?.size ?: 0} cpu_cumule_ms=${(snapshot?.cpuNanos ?: 0L) / 1_000_000.0}"
            )
            report.add(
                "[Bio Debug] ${eventId.toString().take(8)} tentatives=${generationAttemptsByEventId[eventId] ?: 0} " +
                    "prochain_retry=${retryAtGameTime[eventId]?.toString() ?: "aucun"} " +
                    "distance_joueur=${distance?.let { d -> "%.1f".format(d) } ?: "inconnue"} " +
                    "intensite_temporelle=${zone?.temporalIntensityAt(renderGameTime) ?: 0.0f}"
            )
        }
        recentFailures.forEach { failure -> report.add("[Bio Debug] echec recent: $failure") }

        report.add("[Bio Debug] -- generation GPU locale --")
        report.add(
            "[Bio Debug] budgets: generation_budget_micros=" +
                "${BioluminescentZoneGenerator.GENERATION_TIME_SLICE_NANOS / 1000L} " +
                "gpu_upload_steps_par_tick=${BioluminescentZoneGenerator.UPLOAD_STEPS_PER_ADVANCE} " +
                "max_tiles_gpu=$MAX_GPU_TILES"
        )
        report.add(
            "[Bio Debug] renderer: iris_installe=${ModCompat.hasIris} " +
                "shader_pack_actif=${ModCompat.hasIris && IrisRenderState.shaderPackInUse} " +
                "chemin=${if (ModCompat.hasIris && IrisRenderState.shaderPackInUse) "iris" else "vanilla"} " +
                "distance_rendu=${ModConfig.bioluminescenceRenderDistance}"
        )
        if (ModCompat.hasIris) report.addAll(IrisRenderState.debugLines())
        return report
    }

    private fun createPendingZones(level: ClientLevel) {
        for ((eventId, synchronized) in synchronizedWaves) {
            if (eventId in zonesByEventId) continue
            if (level.gameTime < (retryAtGameTime[eventId] ?: Long.MIN_VALUE)) continue
            val initialCell = findInitialCell(level, synchronized.payload.anchor) ?: continue
            val lifetime = if (synchronized.payload.mode == BioluminescenceWaveMode.TOTAL_NIGHT) {
                Long.MAX_VALUE
            } else {
                (synchronized.payload.endGameTime - synchronized.payload.startGameTime).coerceAtLeast(1L)
            }
            val colorPhase = ModUtilities.stableUnitValue(
                RandomSupport.mixStafford13(synchronized.payload.seed xor COLOR_PHASE_SALT)
            ) * PI * 2.0
            zonesByEventId[eventId] = BioluminescentZone(
                zoneSeed = synchronized.payload.seed,
                anchor = initialCell.waterPos,
                palette = BioluminescentPalettes.select(
                    synchronized.payload.seed,
                    BioluminescentPaletteFamily.RANDOM
                ),
                preset = when (synchronized.payload.size) {
                    BioluminescenceWaveSize.SMALL -> BioluminescentZonePresets.SMALL
                    BioluminescenceWaveSize.LARGE -> BioluminescentZonePresets.LARGE
                },
                activity = when (synchronized.payload.activity) {
                    BioluminescenceWaveActivity.ACTIVE -> BioluminescentZoneActivity.ACTIVE
                    BioluminescenceWaveActivity.INACTIVE -> BioluminescentZoneActivity.INACTIVE
                },
                initialCell = initialCell,
                createdAt = synchronized.payload.startGameTime,
                lifetime = lifetime,
                colorPhase = colorPhase,
                requestedByCommand = false,
                eventId = eventId,
                mode = synchronized.payload.mode,
                endGameTime = synchronized.payload.endGameTime,
                serverGameTimeOffset = synchronized.serverGameTimeOffset
            )
        }
    }

    private fun findInitialCell(level: ClientLevel, anchor: BlockPos): BioluminescentWaterCell? {
        if (!ModUtilities.hasLoadedChunk(level, anchor.x shr 4, anchor.z shr 4)) return null
        BioluminescentWaterDomainCollector.sampleWaterCell(level, anchor.x, anchor.z, anchor.y)?.let { return it }
        for (radius in 1..4) {
            for (offsetX in -radius..radius) {
                for (offsetZ in -radius..radius) {
                    if (maxOf(kotlin.math.abs(offsetX), kotlin.math.abs(offsetZ)) != radius) continue
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

    private fun advanceOneGeneration(level: ClientLevel) {
        val priorityPosition = Minecraft.getInstance().player?.blockPosition() ?: return
        var totalReservedTiles = 0
        val generating = ArrayList<BioluminescentZone>()
        for (zone in zonesByEventId.values) {
            totalReservedTiles += zone.reservedTileCount
            if (!zone.isReady && !zone.isFailed) generating.add(zone)
        }
        if (generating.isEmpty()) return
        if (generating.size > 1) {
            generating.sortBy { zone ->
                zone.horizontalDistanceSqr(priorityPosition.x + 0.5, priorityPosition.z + 0.5)
            }
        }
        if (generationRoundRobin >= generating.size) generationRoundRobin = 0
        val zone = generating[generationRoundRobin]
        generationRoundRobin = (generationRoundRobin + 1) % generating.size
        val otherTiles = (totalReservedTiles - zone.reservedTileCount).coerceAtLeast(0)
        val previousStage = zone.generationStage
        zone.advanceGeneration(
            level,
            Minecraft.getInstance().textureManager,
            ::nextTextureIdentifier,
            (MAX_GPU_TILES - otherTiles).coerceAtLeast(0),
            level.gameTime,
            priorityPosition
        )
        if (zone.isFailed) {
            rememberFailure(zone, zone.failureReason ?: "raison inconnue")
            val attempts = (generationAttemptsByEventId[zone.eventId] ?: 0) + 1
            generationAttemptsByEventId[zone.eventId] = attempts
            retryAtGameTime[zone.eventId] = if (attempts >= MAX_GENERATION_ATTEMPTS) {
                Long.MAX_VALUE
            } else {
                level.gameTime + RETRY_DELAY_TICKS
            }
            removeZone(zone.eventId)
        } else if (zone.isReady && previousStage != BioluminescentZoneGenerationStage.READY) {
            retryAtGameTime.remove(zone.eventId)
            generationAttemptsByEventId.remove(zone.eventId)
            BioluminescentZoneDynamicLights.onZoneReady(zone)
        }
    }

    private fun advanceEndingZones(gameTime: Long) {
        if (endingZoneIds.isEmpty()) return
        val renderGameTime = gameTime.toDouble()
        val completed = endingZoneIds.filter { eventId ->
            val zone = zonesByEventId[eventId]
            zone == null || zone.endingFadeComplete(renderGameTime)
        }
        for (eventId in completed) {
            endingZoneIds.remove(eventId)
            removeZone(eventId)
        }
    }

    private fun removeExpiredWaves(clientGameTime: Long) {
        val expired = synchronizedWaves.values.asSequence()
            .filter { wave ->
                wave.payload.mode == BioluminescenceWaveMode.NORMAL &&
                    clientGameTime + wave.serverGameTimeOffset >= wave.payload.endGameTime
            }
            .mapTo(mutableListOf()) { wave -> wave.payload.eventId }
        for (eventId in expired) {
            synchronizedWaves.remove(eventId)
            retryAtGameTime.remove(eventId)
            generationAttemptsByEventId.remove(eventId)
            removeZone(eventId)
        }
        refreshNextNormalWaveEndClientGameTime()
    }

    private fun refreshNextNormalWaveEndClientGameTime() {
        nextNormalWaveEndClientGameTime = synchronizedWaves.values.asSequence()
            .filter { wave -> wave.payload.mode == BioluminescenceWaveMode.NORMAL }
            .minOfOrNull { wave -> wave.payload.endGameTime - wave.serverGameTimeOffset }
            ?: Long.MAX_VALUE
    }

    private fun removeZone(eventId: UUID) {
        val zone = zonesByEventId.remove(eventId) ?: return
        BioluminescentZoneDynamicLights.onZoneRemoved(zone)
        zone.close()
        generationRoundRobin = 0
    }

    private fun ensureLevel(level: ClientLevel) {
        if (currentLevel === level) return
        resetLevel(clearOpportunity = false)
        currentLevel = level
    }

    private fun resetLevel(clearOpportunity: Boolean = true) {
        clearZones()
        synchronizedWaves.clear()
        retryAtGameTime.clear()
        generationAttemptsByEventId.clear()
        nextNormalWaveEndClientGameTime = Long.MAX_VALUE
        currentLevel = null
        if (clearOpportunity) opportunityArmed = false
        generationRoundRobin = 0
        recentFailures.clear()
    }

    private fun clearZones() {
        zonesByEventId.values.forEach(BioluminescentZone::close)
        zonesByEventId.clear()
        endingZoneIds.clear()
        BioluminescentZoneDynamicLights.clear()
        generationRoundRobin = 0
    }

    private fun rememberFailure(zone: BioluminescentZone, reason: String) {
        val message = "${zone.eventId.toString().take(8)} ${zone.preset.size.commandName}: $reason"
        recentFailures.addLast(message)
        while (recentFailures.size > MAX_RETAINED_FAILURES) recentFailures.removeFirst()
    }

    private fun nextTextureIdentifier(): Identifier {
        textureCounter++
        return Identifier.fromNamespaceAndPath(
            SquAbyssalBloom.ID,
            "dynamic/bioluminescent_zone_${java.lang.Long.toUnsignedString(textureCounter, 16)}"
        )
    }
}
