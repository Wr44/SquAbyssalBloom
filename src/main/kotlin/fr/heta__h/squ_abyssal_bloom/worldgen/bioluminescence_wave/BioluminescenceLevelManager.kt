package fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceBounds
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveActivity
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveMode
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveSize
import fr.heta__h.squ_abyssal_bloom.network.bioluminescence.S2CBioluminescenceWaveEndPayload
import fr.heta__h.squ_abyssal_bloom.network.bioluminescence.S2CBioluminescenceWavePayload
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceLevelSavedData
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceServerSettings
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.neoforged.neoforge.network.PacketDistributor
import java.util.UUID
import java.util.WeakHashMap

class BioluminescenceLevelManager private constructor(
    private val level: ServerLevel
) {
    companion object {
        private const val SPATIAL_SECTOR_SIZE = 64
        private const val MAXIMUM_NORMAL_DURATION_TICKS = 72000L
        private val INSTANCES: MutableMap<ServerLevel, BioluminescenceLevelManager> = WeakHashMap()

        @JvmStatic
        @Synchronized
        fun forLevel(level: ServerLevel): BioluminescenceLevelManager {
            return INSTANCES.getOrPut(level) { BioluminescenceLevelManager(level) }
        }

        @JvmStatic
        @Synchronized
        fun releaseLevel(level: ServerLevel) {
            INSTANCES.remove(level)?.close()
        }

        @JvmStatic
        @Synchronized
        fun releaseServerLevels(server: MinecraftServer) {
            val levels = INSTANCES.keys.filter { level -> level.server === server }
            for (level in levels) INSTANCES.remove(level)?.close()
        }
    }

    private val savedData = level.dataStorage.computeIfAbsent(BioluminescenceLevelSavedData.TYPE)
    private val beachResolver = BioluminescenceBeachResolver(level)
    private val previousBeachByPlayer: MutableMap<UUID, BioluminescenceBeachResolver.BeachZone?> = hashMapOf()
    private val visibleEventsByPlayer: MutableMap<UUID, MutableSet<UUID>> = hashMapOf()
    private val waveIdsBySector: MutableMap<Long, MutableSet<UUID>> = hashMapOf()
    private val waveIdsByBeach: MutableMap<UUID, MutableSet<UUID>> = hashMapOf()

    private var lastTickGameTime = Long.MIN_VALUE
    private var nextBeachCheckGameTime = 0L
    private var disabledWasApplied = false
    private var previousNightActive = false
    private var pendingNightStartTrigger = false
    private var pendingTotalNightStartTrigger = false

    var nextNormalWaveEndGameTime: Long = Long.MAX_VALUE
        private set

    init {
        val removedInvalidData = savedData.waves.entries.removeIf { (_, wave) ->
            !isPersistedWaveValid(wave)
        }
        if (removedInvalidData) savedData.setDirty()
        savedData.waves.values.forEach(::indexWave)
        refreshNextNormalWaveEndGameTime()
    }

    val isTotalNightActive: Boolean
        get() = savedData.nightState.totalNightActive

    val lastRolledNightIndex: Long
        get() = savedData.nightState.lastRolledNightIndex

    val sectorIndexCount: Int
        get() = waveIdsBySector.size

    val beachIndexCount: Int
        get() = waveIdsByBeach.size

    val beachCacheZoneCount: Int
        get() = beachResolver.cachedZoneCount

    val beachCacheDetectionCount: Int
        get() = beachResolver.cachedDetectionCount

    fun tick(
        settings: BioluminescenceServerSettings,
        serverManager: BioluminescenceServerManager
    ) {
        val gameTime = level.gameTime
        if (lastTickGameTime == gameTime) return
        lastTickGameTime = gameTime

        if (!settings.enabled) {
            if (!disabledWasApplied) {
                disabledWasApplied = true
                endWaves { true }
                savedData.nightState.totalNightActive = false
                previousBeachByPlayer.clear()
                visibleEventsByPlayer.clear()
                beachResolver.clear()
                savedData.setDirty()
            }
            return
        }
        disabledWasApplied = false
        if (!ModUtilities.isOverworldLikeDimension(level)) {
            if (savedData.waves.isNotEmpty() || savedData.nightState.totalNightActive) {
                endWaves { true }
                savedData.nightState.totalNightActive = false
                savedData.setDirty()
            }
            previousBeachByPlayer.clear()
            visibleEventsByPlayer.clear()
            beachResolver.clear()
            return
        }

        updateNightState(settings)
        val nightNowActive = settings.isNight(level.overworldClockTime)
        if (nightNowActive && !previousNightActive) pendingNightStartTrigger = true
        previousNightActive = nightNowActive

        if (gameTime >= nextNormalWaveEndGameTime) {
            endWaves { wave -> wave.isExpired(gameTime) }
        }

        if (gameTime >= nextBeachCheckGameTime) {
            nextBeachCheckGameTime = gameTime + BioluminescenceServerSettings.BEACH_CHECK_INTERVAL_TICKS
            detectBeachEntries(settings, serverManager)
            synchronizePlayers(settings)
        }
    }

    fun synchronizePlayer(
        player: ServerPlayer,
        settings: BioluminescenceServerSettings
    ) {
        if (player.level() !== level) return
        if (!settings.enabled || !ModUtilities.isOverworldLikeDimension(level)) {
            clearVisibleEvents(player)
            return
        }
        val visible = visibleEventsByPlayer.getOrPut(player.uuid) { hashSetOf() }
        val desired = hashSetOf<UUID>()
        val visibilityRadiusSqr = settings.visibilityRadius * settings.visibilityRadius
        val gameTime = level.gameTime

        val queryRadius = kotlin.math.ceil(settings.visibilityRadius).toInt()
        val queryBounds = BioluminescenceBounds.around(
            player.blockPosition().x,
            player.blockPosition().z,
            queryRadius
        )
        for (wave in wavesIntersecting(queryBounds)) {
            if (!isWaveActive(wave, gameTime, settings)) continue
            if (wave.bounds.horizontalDistanceSqr(player.x, player.z) > visibilityRadiusSqr) continue
            desired.add(wave.eventId)
            if (visible.add(wave.eventId)) sendWave(player, wave)
        }

        val noLongerVisible = visible.filterTo(mutableListOf()) { eventId -> eventId !in desired }
        for (eventId in noLongerVisible) {
            visible.remove(eventId)
            PacketDistributor.sendToPlayer(
                player,
                S2CBioluminescenceWaveEndPayload(eventId, level.dimension().identifier())
            )
        }
        if (visible.isEmpty()) visibleEventsByPlayer.remove(player.uuid)
    }

    fun synchronizeTrackedChunk(
        player: ServerPlayer,
        chunkPos: ChunkPos,
        settings: BioluminescenceServerSettings
    ) {
        if (player.level() !== level || !settings.enabled ||
            !ModUtilities.isOverworldLikeDimension(level)
        ) return
        val chunkBounds = BioluminescenceBounds(
            chunkPos.minBlockX,
            chunkPos.minBlockZ,
            chunkPos.maxBlockX,
            chunkPos.maxBlockZ
        )
        val visibilityRadiusSqr = settings.visibilityRadius * settings.visibilityRadius
        val visible = visibleEventsByPlayer.getOrPut(player.uuid) { hashSetOf() }
        for (wave in wavesIntersecting(chunkBounds)) {
            if (!isWaveActive(wave, level.gameTime, settings)) continue
            if (!wave.bounds.intersects(chunkBounds)) continue
            if (wave.bounds.horizontalDistanceSqr(player.x, player.z) > visibilityRadiusSqr) continue
            if (visible.add(wave.eventId)) sendWave(player, wave)
        }
    }

    fun forgetPlayer(playerId: UUID) {
        previousBeachByPlayer.remove(playerId)
        visibleEventsByPlayer.remove(playerId)
    }

    fun allWaves(): List<ActiveBioluminescenceWave> {
        return savedData.waves.values.toList()
    }

    fun waveById(eventId: UUID): ActiveBioluminescenceWave? {
        return savedData.waves[eventId]
    }

    fun currentBeach(playerId: UUID): BioluminescenceBeachResolver.BeachZone? {
        return previousBeachByPlayer[playerId]
    }

    fun visibleWaveIds(playerId: UUID): Set<UUID> {
        return visibleEventsByPlayer[playerId]?.toSet() ?: emptySet()
    }

    fun overlappingWaveCount(wave: ActiveBioluminescenceWave, settings: BioluminescenceServerSettings): Int {
        return wavesIntersecting(wave.bounds.expanded(settings.overlapMargin))
            .count { other -> other.eventId != wave.eventId }
    }

    fun sectorCount(bounds: BioluminescenceBounds): Int {
        var count = 0
        forEachSector(bounds) { count++ }
        return count
    }

    fun diagnoseBeach(
        playerId: UUID,
        position: BlockPos,
        settings: BioluminescenceServerSettings
    ): BioluminescenceBeachResolver.Diagnostic {
        return beachResolver.diagnose(playerId, position, settings)
    }

    fun verifyBeachIdStability(
        player: ServerPlayer,
        settings: BioluminescenceServerSettings,
        samples: Int = 5
    ): List<UUID?> {
        val ids = mutableListOf<UUID?>()
        repeat(samples) {
            beachResolver.clear()
            ids.add(beachResolver.resolve(player, settings)?.id)
        }
        val stable = ids.all { it == ids.first() }
        SquAbyssalBloom.LOGGER.info(
            "[Bioluminescence] Verification stabilite beachId pour {}: ids={} stable={}",
            player.gameProfile.name,
            ids.map { id -> id?.toString()?.take(8) },
            stable
        )
        return ids
    }

    fun clearBeachCaches() {
        beachResolver.clear()
    }

    fun forceStartTotalNight(settings: BioluminescenceServerSettings): Boolean {
        if (savedData.nightState.totalNightActive) return false
        savedData.nightState.lastRolledNightIndex = settings.nightIndex(level.overworldClockTime)
        savedData.nightState.totalNightActive = true
        pendingTotalNightStartTrigger = true
        nextBeachCheckGameTime = level.gameTime
        savedData.setDirty()
        SquAbyssalBloom.LOGGER.info(
            "[Bioluminescence] Nuit bioluminescente totale forcee dans {} (commande)",
            level.dimension().identifier()
        )
        return true
    }

    fun forceStopTotalNight(): Boolean {
        if (!savedData.nightState.totalNightActive) return false
        savedData.nightState.totalNightActive = false
        endWaves { wave -> wave.mode == BioluminescenceWaveMode.TOTAL_NIGHT }
        savedData.setDirty()
        SquAbyssalBloom.LOGGER.info(
            "[Bioluminescence] Nuit bioluminescente totale arretee dans {} (commande)",
            level.dimension().identifier()
        )
        return true
    }

    fun rollTotalNightChance(settings: BioluminescenceServerSettings): Boolean {
        return level.random.nextDouble() < settings.totalNightChance
    }

    fun endWavesWhere(predicate: (ActiveBioluminescenceWave) -> Boolean): Int {
        val before = savedData.waves.size
        endWaves(predicate)
        return before - savedData.waves.size
    }

    sealed interface WaveCreationOutcome {
        data class Created(val wave: ActiveBioluminescenceWave) : WaveCreationOutcome
        data class Reused(val wave: ActiveBioluminescenceWave) : WaveCreationOutcome
        data object NoValidBeach : WaveCreationOutcome
    }

    fun createWaveForCommand(
        player: ServerPlayer,
        size: BioluminescenceWaveSize,
        mode: BioluminescenceWaveMode,
        settings: BioluminescenceServerSettings,
        seedOverride: Long? = null,
        durationTicksOverride: Long? = null
    ): WaveCreationOutcome {
        val beach = beachResolver.resolve(player, settings) ?: return WaveCreationOutcome.NoValidBeach
        val countBefore = savedData.waves.size
        val wave = createOrReuseWave(
            player,
            beach,
            size,
            mode,
            settings,
            seedOverride,
            durationTicksOverride,
            createdByCommand = true
        )
        return if (savedData.waves.size > countBefore) {
            WaveCreationOutcome.Created(wave)
        } else {
            WaveCreationOutcome.Reused(wave)
        }
    }

    fun triggerNormalWaveIfOnBeach(
        player: ServerPlayer,
        settings: BioluminescenceServerSettings
    ): ActiveBioluminescenceWave? {
        if (savedData.nightState.totalNightActive) return null
        if (!player.isAlive || player.isSpectator) return null
        if (!settings.isNight(level.overworldClockTime)) return null
        val beach = beachResolver.resolve(player, settings) ?: return null
        return createOrReuseWave(
            player,
            beach,
            settings.sampleSize(level.random),
            BioluminescenceWaveMode.NORMAL,
            settings
        )
    }

    private fun updateNightState(settings: BioluminescenceServerSettings) {
        val nightState = savedData.nightState
        val isNight = settings.isNight(level.overworldClockTime)
        if (!isNight) {
            if (nightState.totalNightActive) {
                nightState.totalNightActive = false
                endWaves { wave -> wave.mode == BioluminescenceWaveMode.TOTAL_NIGHT }
                savedData.setDirty()
            }
            return
        }

        val nightIndex = settings.nightIndex(level.overworldClockTime)
        if (nightState.lastRolledNightIndex == nightIndex) return
        if (nightState.totalNightActive) {
            endWaves { wave -> wave.mode == BioluminescenceWaveMode.TOTAL_NIGHT }
        }
        nightState.lastRolledNightIndex = nightIndex
        nightState.totalNightActive = level.random.nextDouble() < settings.totalNightChance
        if (nightState.totalNightActive) {
            endWaves { true }
            pendingTotalNightStartTrigger = true
        }
        savedData.setDirty()
    }

    private fun detectBeachEntries(
        settings: BioluminescenceServerSettings,
        serverManager: BioluminescenceServerManager
    ) {
        val nightJustStarted = pendingNightStartTrigger
        val totalNightJustStarted = pendingTotalNightStartTrigger
        pendingNightStartTrigger = false
        pendingTotalNightStartTrigger = false

        val onlinePlayerIds = hashSetOf<UUID>()
        val night = settings.isNight(level.overworldClockTime)
        for (player in level.players()) {
            onlinePlayerIds.add(player.uuid)
            val beach = if (player.isAlive && !player.isSpectator) {
                beachResolver.resolve(player, settings)
            } else {
                null
            }
            val hadPreviousState = previousBeachByPlayer.containsKey(player.uuid)
            val previousBeach = previousBeachByPlayer.put(player.uuid, beach)
            if (beach == null || !night) continue
            val enteredBeach = !hadPreviousState || previousBeach == null
            if (!enteredBeach && !nightJustStarted && !totalNightJustStarted) continue

            if (savedData.nightState.totalNightActive) {
                createOrReuseWave(
                    player,
                    beach,
                    BioluminescenceWaveSize.LARGE,
                    BioluminescenceWaveMode.TOTAL_NIGHT,
                    settings
                )
            } else if (serverManager.isArmed(player.uuid)) {
                createOrReuseWave(
                    player,
                    beach,
                    settings.sampleSize(level.random),
                    BioluminescenceWaveMode.NORMAL,
                    settings
                )
                serverManager.consumeOpportunity(player, settings)
            }
        }

        previousBeachByPlayer.keys.removeIf { playerId -> playerId !in onlinePlayerIds }
        visibleEventsByPlayer.keys.removeIf { playerId -> playerId !in onlinePlayerIds }
        beachResolver.cleanExpired()
    }

    private fun createOrReuseWave(
        player: ServerPlayer,
        beach: BioluminescenceBeachResolver.BeachZone,
        size: BioluminescenceWaveSize,
        mode: BioluminescenceWaveMode,
        settings: BioluminescenceServerSettings,
        seedOverride: Long? = null,
        durationTicksOverride: Long? = null,
        createdByCommand: Boolean = false
    ): ActiveBioluminescenceWave {
        val candidateBounds = BioluminescenceBounds.around(
            beach.waterSurface.x,
            beach.waterSurface.z,
            size.maximumRadius
        )
        val compatibleWaveIds = linkedSetOf<UUID>()
        compatibleWaveIds.addAll(waveIdsByBeach[beach.id].orEmpty())
        compatibleWaveIds.addAll(waveIdsIntersecting(candidateBounds.expanded(settings.overlapMargin)))
        val existing = compatibleWaveIds.asSequence()
            .mapNotNull(savedData.waves::get)
            .firstOrNull { wave ->
                wave.mode == mode &&
                    (wave.beachId == beach.id ||
                        wave.bounds.expanded(settings.overlapMargin).intersects(candidateBounds))
            }
        if (existing != null) {
            synchronizePlayers(settings)
            if (createdByCommand) {
                SquAbyssalBloom.LOGGER.info(
                    "[Bioluminescence] Reused {} {} {} wave {} for beach {} (commande)",
                    mode.name.lowercase(),
                    existing.size.name.lowercase(),
                    existing.activity.name.lowercase(),
                    existing.eventId,
                    beach.id.toString().take(8)
                )
            }
            return existing
        }

        val startGameTime = level.gameTime + settings.preparationDelayTicks
        val endGameTime = if (mode == BioluminescenceWaveMode.TOTAL_NIGHT) {
            Long.MAX_VALUE
        } else {
            startGameTime + (durationTicksOverride ?: settings.sampleDuration(level.random))
        }
        val activity = if (mode == BioluminescenceWaveMode.TOTAL_NIGHT) {
            BioluminescenceWaveActivity.ACTIVE
        } else {
            settings.sampleActivity(level.random)
        }
        val wave = ActiveBioluminescenceWave(
            eventId = UUID.randomUUID(),
            seed = seedOverride ?: level.random.nextLong(),
            dimension = level.dimension().identifier(),
            beachId = beach.id,
            anchor = beach.waterSurface,
            bounds = candidateBounds,
            startGameTime = startGameTime,
            endGameTime = endGameTime,
            size = size,
            mode = mode,
            activity = activity,
            originPlayerId = player.uuid,
            createdByCommand = createdByCommand
        )
        savedData.waves[wave.eventId] = wave
        indexWave(wave)
        if (mode == BioluminescenceWaveMode.NORMAL) {
            nextNormalWaveEndGameTime = minOf(nextNormalWaveEndGameTime, endGameTime)
        }
        savedData.setDirty()
        synchronizePlayers(settings)
        if (createdByCommand) {
            SquAbyssalBloom.LOGGER.info(
                "[Bioluminescence] Created {} {} {} wave {} at {} in {} for beach {} (commande)",
                mode.name.lowercase(),
                size.name.lowercase(),
                activity.name.lowercase(),
                wave.eventId,
                wave.anchor,
                wave.dimension,
                beach.id.toString().take(8)
            )
        }
        return wave
    }

    private fun synchronizePlayers(settings: BioluminescenceServerSettings) {
        for (player in level.players()) synchronizePlayer(player, settings)
    }

    private fun sendWave(player: ServerPlayer, wave: ActiveBioluminescenceWave) {
        PacketDistributor.sendToPlayer(
            player,
            S2CBioluminescenceWavePayload.from(wave, level.gameTime)
        )
    }

    private fun clearVisibleEvents(player: ServerPlayer) {
        val visible = visibleEventsByPlayer.remove(player.uuid) ?: return
        for (eventId in visible) {
            PacketDistributor.sendToPlayer(
                player,
                S2CBioluminescenceWaveEndPayload(eventId, level.dimension().identifier())
            )
        }
    }

    private fun isWaveActive(
        wave: ActiveBioluminescenceWave,
        gameTime: Long,
        settings: BioluminescenceServerSettings
    ): Boolean {
        return !wave.isExpired(gameTime) &&
            (wave.mode != BioluminescenceWaveMode.TOTAL_NIGHT || settings.isNight(level.overworldClockTime))
    }

    private fun endWaves(predicate: (ActiveBioluminescenceWave) -> Boolean) {
        val removedIds = savedData.waves.values.asSequence()
            .filter(predicate)
            .mapTo(linkedSetOf()) { wave -> wave.eventId }
        if (removedIds.isEmpty()) return
        for (eventId in removedIds) {
            savedData.waves.remove(eventId)?.let(::unindexWave)
            for (player in level.players()) {
                val visible = visibleEventsByPlayer[player.uuid] ?: continue
                if (!visible.remove(eventId)) continue
                PacketDistributor.sendToPlayer(
                    player,
                    S2CBioluminescenceWaveEndPayload(eventId, level.dimension().identifier())
                )
            }
        }
        visibleEventsByPlayer.entries.removeIf { (_, visible) -> visible.isEmpty() }
        refreshNextNormalWaveEndGameTime()
        savedData.setDirty()
    }

    private fun close() {
        previousBeachByPlayer.clear()
        visibleEventsByPlayer.clear()
        beachResolver.clear()
        waveIdsBySector.clear()
        waveIdsByBeach.clear()
    }

    private fun indexWave(wave: ActiveBioluminescenceWave) {
        waveIdsByBeach.getOrPut(wave.beachId) { linkedSetOf() }.add(wave.eventId)
        forEachSector(wave.bounds) { sectorKey ->
            waveIdsBySector.getOrPut(sectorKey) { linkedSetOf() }.add(wave.eventId)
        }
    }

    private fun unindexWave(wave: ActiveBioluminescenceWave) {
        waveIdsByBeach[wave.beachId]?.let { eventIds ->
            eventIds.remove(wave.eventId)
            if (eventIds.isEmpty()) waveIdsByBeach.remove(wave.beachId)
        }
        forEachSector(wave.bounds) { sectorKey ->
            waveIdsBySector[sectorKey]?.let { eventIds ->
                eventIds.remove(wave.eventId)
                if (eventIds.isEmpty()) waveIdsBySector.remove(sectorKey)
            }
        }
    }

    private fun wavesIntersecting(bounds: BioluminescenceBounds): Sequence<ActiveBioluminescenceWave> {
        return waveIdsIntersecting(bounds).asSequence()
            .mapNotNull(savedData.waves::get)
            .filter { wave -> wave.bounds.intersects(bounds) }
    }

    private fun isPersistedWaveValid(wave: ActiveBioluminescenceWave): Boolean {
        if (wave.dimension != level.dimension().identifier() || !wave.bounds.isValid) return false
        val maximumDiameter = wave.size.maximumRadius.toLong() * 2L
        if (wave.bounds.maximumX.toLong() - wave.bounds.minimumX.toLong() > maximumDiameter) return false
        if (wave.bounds.maximumZ.toLong() - wave.bounds.minimumZ.toLong() > maximumDiameter) return false
        if (wave.anchor.x !in wave.bounds.minimumX..wave.bounds.maximumX) return false
        if (wave.anchor.z !in wave.bounds.minimumZ..wave.bounds.maximumZ) return false
        if (wave.endGameTime <= wave.startGameTime) return false
        return when (wave.mode) {
            BioluminescenceWaveMode.NORMAL ->
                wave.endGameTime - wave.startGameTime in 1L..MAXIMUM_NORMAL_DURATION_TICKS
            BioluminescenceWaveMode.TOTAL_NIGHT -> wave.endGameTime == Long.MAX_VALUE
        }
    }

    private fun refreshNextNormalWaveEndGameTime() {
        nextNormalWaveEndGameTime = savedData.waves.values.asSequence()
            .filter { wave -> wave.mode == BioluminescenceWaveMode.NORMAL }
            .minOfOrNull { wave -> wave.endGameTime }
            ?: Long.MAX_VALUE
    }

    private fun waveIdsIntersecting(bounds: BioluminescenceBounds): Set<UUID> {
        val eventIds = linkedSetOf<UUID>()
        forEachSector(bounds) { sectorKey ->
            waveIdsBySector[sectorKey]?.let(eventIds::addAll)
        }
        return eventIds
    }

    private inline fun forEachSector(
        bounds: BioluminescenceBounds,
        action: (Long) -> Unit
    ) {
        val minimumSectorX = Math.floorDiv(bounds.minimumX, SPATIAL_SECTOR_SIZE)
        val minimumSectorZ = Math.floorDiv(bounds.minimumZ, SPATIAL_SECTOR_SIZE)
        val maximumSectorX = Math.floorDiv(bounds.maximumX, SPATIAL_SECTOR_SIZE)
        val maximumSectorZ = Math.floorDiv(bounds.maximumZ, SPATIAL_SECTOR_SIZE)
        for (sectorX in minimumSectorX..maximumSectorX) {
            for (sectorZ in minimumSectorZ..maximumSectorZ) {
                action(ModUtilities.horizontalPositionKey(sectorX, sectorZ))
            }
        }
    }
}
