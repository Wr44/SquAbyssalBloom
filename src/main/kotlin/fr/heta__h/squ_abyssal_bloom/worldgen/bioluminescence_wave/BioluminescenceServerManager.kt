package fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.ActiveBioluminescenceWave
import fr.heta__h.squ_abyssal_bloom.network.bioluminescence.S2CBioluminescenceArmedPayload
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescencePlayerSavedData
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceServerSettings
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.PlayerBioluminescenceState
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.PlayerBioluminescenceStatus
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.RandomSource
import net.neoforged.neoforge.network.PacketDistributor
import java.util.UUID
import java.util.WeakHashMap

class BioluminescenceServerManager private constructor(
    private val server: MinecraftServer
) {
    companion object {
        private const val PLAYER_CLEANUP_INTERVAL_TICKS = 6000L
        private const val ACTIVITY_UPDATE_INTERVAL_TICKS = 1200L
        private const val SETTINGS_REFRESH_INTERVAL_TICKS = 20L
        private const val PLAYER_RETENTION_TICKS = 720000L

        private val INSTANCES: MutableMap<MinecraftServer, BioluminescenceServerManager> = WeakHashMap()

        @JvmStatic
        @Synchronized
        fun forServer(server: MinecraftServer): BioluminescenceServerManager {
            return INSTANCES.getOrPut(server) { BioluminescenceServerManager(server) }
        }

        @JvmStatic
        @Synchronized
        fun releaseServer(server: MinecraftServer) {
            INSTANCES.remove(server)
        }
    }

    private val savedData = server.overworld().dataStorage.computeIfAbsent(BioluminescencePlayerSavedData.TYPE)
    private var lastTickGameTime = Long.MIN_VALUE
    private var nextPlayerCleanupGameTime = 0L
    private var nextSettingsRefreshGameTime = 0L
    private var cachedSettings = BioluminescenceServerSettings.current()
    private var lastEnabledState: Boolean? = null

    fun tick() {
        val gameTime = server.overworld().gameTime
        if (lastTickGameTime == gameTime) return
        lastTickGameTime = gameTime
        if (gameTime >= nextSettingsRefreshGameTime) {
            cachedSettings = BioluminescenceServerSettings.current()
            nextSettingsRefreshGameTime = gameTime + SETTINGS_REFRESH_INTERVAL_TICKS
        }
        val settings = cachedSettings
        synchronizeEnabledState(settings)

        for (level in server.allLevels) {
            BioluminescenceLevelManager.forLevel(level).tick(settings, this)
        }
        if (settings.enabled) tickConnectedPlayers(settings, gameTime)
        cleanInactivePlayerStates(gameTime)
    }

    fun isArmed(playerId: UUID): Boolean {
        return savedData.states[playerId]?.status == PlayerBioluminescenceStatus.ARMED
    }

    fun knownPlayerIds(): Set<UUID> {
        return savedData.states.keys.toSet()
    }

    fun snapshotState(playerId: UUID): PlayerBioluminescenceState? {
        return savedData.states[playerId]?.copy()
    }

    fun countByStatus(status: PlayerBioluminescenceStatus): Int {
        return savedData.states.values.count { state -> state.status == status }
    }

    fun forcePlayerArmed(playerId: UUID, settings: BioluminescenceServerSettings): ActiveBioluminescenceWave? {
        val random = randomFor(playerId)
        val state = stateForId(playerId, settings, random)
        state.status = PlayerBioluminescenceStatus.ARMED
        state.remainingNightTicksUntilOpportunity = 0L
        state.lastActivityGameTime = server.overworld().gameTime
        savedData.setDirty()
        val player = server.playerList.getPlayer(playerId) ?: return null
        PacketDistributor.sendToPlayer(player, S2CBioluminescenceArmedPayload(settings.enabled))
        return triggerIfAlreadyOnBeach(player, settings)
    }

    fun forcePlayerDisarmed(playerId: UUID, settings: BioluminescenceServerSettings) {
        val random = randomFor(playerId)
        val state = stateForId(playerId, settings, random)
        state.status = PlayerBioluminescenceStatus.WAITING
        state.remainingNightTicksUntilOpportunity = settings.sampleOpportunityDelay(random)
        state.lastActivityGameTime = server.overworld().gameTime
        savedData.setDirty()
        server.playerList.getPlayer(playerId)?.let { player ->
            PacketDistributor.sendToPlayer(player, S2CBioluminescenceArmedPayload(false))
        }
    }

    fun schedulePlayerOpportunity(playerId: UUID, nightTicks: Long, settings: BioluminescenceServerSettings) {
        val random = randomFor(playerId)
        val state = stateForId(playerId, settings, random)
        state.status = PlayerBioluminescenceStatus.WAITING
        state.remainingNightTicksUntilOpportunity = nightTicks.coerceAtLeast(0L)
        state.lastActivityGameTime = server.overworld().gameTime
        savedData.setDirty()
        server.playerList.getPlayer(playerId)?.let { player ->
            PacketDistributor.sendToPlayer(player, S2CBioluminescenceArmedPayload(false))
        }
    }

    fun resetPlayerState(playerId: UUID, settings: BioluminescenceServerSettings) {
        savedData.states.remove(playerId)
        stateForId(playerId, settings, randomFor(playerId))
        savedData.setDirty()
        server.playerList.getPlayer(playerId)?.let { player ->
            PacketDistributor.sendToPlayer(player, S2CBioluminescenceArmedPayload(false))
        }
    }

    private fun randomFor(playerId: UUID): RandomSource {
        return server.playerList.getPlayer(playerId)?.random ?: server.overworld().random
    }

    fun consumeOpportunity(player: ServerPlayer, settings: BioluminescenceServerSettings) {
        val state = stateFor(player, settings)
        state.status = PlayerBioluminescenceStatus.WAITING
        state.remainingNightTicksUntilOpportunity = settings.sampleOpportunityDelay(player.random)
        state.lastActivityGameTime = server.overworld().gameTime
        savedData.setDirty()
        PacketDistributor.sendToPlayer(player, S2CBioluminescenceArmedPayload(false))
    }

    fun onPlayerLogin(player: ServerPlayer) {
        val settings = BioluminescenceServerSettings.current()
        val state = stateFor(player, settings)
        state.lastActivityGameTime = server.overworld().gameTime
        savedData.setDirty()
        PacketDistributor.sendToPlayer(
            player,
            S2CBioluminescenceArmedPayload(
                settings.enabled && state.status == PlayerBioluminescenceStatus.ARMED
            )
        )
        BioluminescenceLevelManager.forLevel(player.level()).synchronizePlayer(player, settings)
    }

    fun onPlayerLogout(player: ServerPlayer) {
        savedData.states[player.uuid]?.let { state ->
            state.lastActivityGameTime = server.overworld().gameTime
            savedData.setDirty()
        }
        BioluminescenceLevelManager.forLevel(player.level()).forgetPlayer(player.uuid)
    }

    fun onPlayerChangedDimension(player: ServerPlayer, previousLevel: ServerLevel?) {
        previousLevel?.let { level ->
            BioluminescenceLevelManager.forLevel(level).forgetPlayer(player.uuid)
        }
        val settings = BioluminescenceServerSettings.current()
        PacketDistributor.sendToPlayer(
            player,
            S2CBioluminescenceArmedPayload(settings.enabled && isArmed(player.uuid))
        )
        BioluminescenceLevelManager.forLevel(player.level()).synchronizePlayer(player, settings)
    }

    private fun synchronizeEnabledState(settings: BioluminescenceServerSettings) {
        if (lastEnabledState == settings.enabled) return
        lastEnabledState = settings.enabled
        for (player in server.playerList.players) {
            val armed = settings.enabled &&
                savedData.states[player.uuid]?.status == PlayerBioluminescenceStatus.ARMED
            PacketDistributor.sendToPlayer(player, S2CBioluminescenceArmedPayload(armed))
        }
    }

    private fun tickConnectedPlayers(settings: BioluminescenceServerSettings, gameTime: Long) {
        var changed = false
        for (player in server.playerList.players) {
            val state = stateFor(player, settings)
            if (gameTime - state.lastActivityGameTime >= ACTIVITY_UPDATE_INTERVAL_TICKS) {
                state.lastActivityGameTime = gameTime
                changed = true
            }
            if (state.status != PlayerBioluminescenceStatus.WAITING) continue
            val level = player.level()
            if (!ModUtilities.isOverworldLikeDimension(level)) continue
            if (!settings.isNight(level.overworldClockTime)) continue
            if (BioluminescenceLevelManager.forLevel(level).isTotalNightActive) continue

            state.remainingNightTicksUntilOpportunity--
            changed = true
            if (state.remainingNightTicksUntilOpportunity > 0L) continue

            state.status = PlayerBioluminescenceStatus.ARMED
            state.remainingNightTicksUntilOpportunity = 0L
            PacketDistributor.sendToPlayer(player, S2CBioluminescenceArmedPayload(true))
            triggerIfAlreadyOnBeach(player, settings)
        }
        if (changed) savedData.setDirty()
    }

    private fun triggerIfAlreadyOnBeach(
        player: ServerPlayer,
        settings: BioluminescenceServerSettings
    ): ActiveBioluminescenceWave? {
        val level = player.level()
        if (!ModUtilities.isOverworldLikeDimension(level)) return null
        val outcome = BioluminescenceLevelManager.forLevel(level).triggerNormalWaveIfOnBeach(player, settings)
        if (outcome is BioluminescenceLevelManager.WaveCreationOutcome.Created) {
            consumeOpportunity(player, settings)
        }
        return when (outcome) {
            is BioluminescenceLevelManager.WaveCreationOutcome.Created -> outcome.wave
            is BioluminescenceLevelManager.WaveCreationOutcome.Reused -> outcome.wave
            BioluminescenceLevelManager.WaveCreationOutcome.NoValidBeach, null -> null
        }
    }

    private fun stateFor(
        player: ServerPlayer,
        settings: BioluminescenceServerSettings
    ): PlayerBioluminescenceState = stateForId(player.uuid, settings, player.random)

    private fun stateForId(
        playerId: UUID,
        settings: BioluminescenceServerSettings,
        random: RandomSource
    ): PlayerBioluminescenceState {
        val existing = savedData.states[playerId]
        if (existing != null) {
            if (existing.status == PlayerBioluminescenceStatus.WAITING &&
                existing.remainingNightTicksUntilOpportunity <= 0L
            ) {
                existing.remainingNightTicksUntilOpportunity = settings.sampleOpportunityDelay(random)
                savedData.setDirty()
            }
            return existing
        }

        return PlayerBioluminescenceState(
            playerId = playerId,
            remainingNightTicksUntilOpportunity = settings.sampleOpportunityDelay(random),
            lastActivityGameTime = server.overworld().gameTime
        ).also { state ->
            savedData.states[playerId] = state
            savedData.setDirty()
        }
    }

    private fun cleanInactivePlayerStates(gameTime: Long) {
        if (gameTime < nextPlayerCleanupGameTime) return
        nextPlayerCleanupGameTime = gameTime + PLAYER_CLEANUP_INTERVAL_TICKS
        val connectedPlayerIds = server.playerList.players.mapTo(hashSetOf()) { player -> player.uuid }
        val removed = savedData.states.entries.removeIf { (playerId, state) ->
            playerId !in connectedPlayerIds &&
                gameTime - state.lastActivityGameTime > PLAYER_RETENTION_TICKS
        }
        if (removed) savedData.setDirty()
    }
}
