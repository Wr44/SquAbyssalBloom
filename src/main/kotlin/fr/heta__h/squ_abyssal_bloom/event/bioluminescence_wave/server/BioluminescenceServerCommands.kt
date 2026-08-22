package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.server

import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveMode
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveSize
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.GameProfileArgument
import net.minecraft.network.chat.Component
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.ActiveBioluminescenceWave
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceLevelManager
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceServerManager
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceServerSettings
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.PlayerBioluminescenceStatus
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.bloom.PlanktonBloomManager
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomState
import net.minecraft.server.MinecraftServer
import net.minecraft.server.players.NameAndId
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterCommandsEvent
import java.util.UUID
import kotlin.math.sqrt

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object BioluminescenceServerCommands {
    private const val MAX_LISTED_WAVES = 20
    private const val MAX_LISTED_PLAYERS = 20

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        val root = Commands.literal("squ_bioluminescence")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

        root.then(Commands.literal("status").executes(::executeStatus))

        root.then(
            Commands.literal("night")
                .then(Commands.literal("start").executes(::executeNightStart))
                .then(Commands.literal("stop").executes(::executeNightStop))
                .then(Commands.literal("roll").executes(::executeNightRoll))
        )

        root.then(
            Commands.literal("wave")
                .then(buildWaveClear())
                .then(buildWaveCreate())
                .then(buildWaveCreateHere())
                .then(Commands.literal("list").executes(::executeWaveList))
                .then(
                    Commands.literal("info")
                        .then(Commands.argument("eventId", StringArgumentType.word()).executes(::executeWaveInfo))
                )
        )

        root.then(
            Commands.literal("player")
                .then(Commands.literal("list").executes(::executePlayerList))
                .then(
                    Commands.literal("info")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(::executePlayerInfo))
                )
                .then(
                    Commands.literal("arm")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(::executePlayerArm))
                )
                .then(
                    Commands.literal("disarm")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(::executePlayerDisarm))
                )
                .then(
                    Commands.literal("schedule")
                        .then(
                            Commands.argument("player", GameProfileArgument.gameProfile())
                                .then(
                                    Commands.argument("nightTicks", LongArgumentType.longArg(0))
                                        .executes(::executePlayerSchedule)
                                )
                        )
                )
                .then(
                    Commands.literal("reset")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(::executePlayerReset))
                )
        )

        root.then(
            Commands.literal("beach")
                .then(Commands.literal("here").executes(::executeBeachHere))
                .then(Commands.literal("clearcache").executes(::executeBeachClearCache))
                .then(Commands.literal("verify").executes(::executeBeachVerify))
        )

        root.then(
            Commands.literal("bloom")
                .then(Commands.literal("list").executes(::executeBloomList))
                .then(
                    Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word()).executes(::executeBloomInfo))
                )
                .then(Commands.literal("here").executes(::executeBloomHere))
                .then(
                    Commands.literal("forceactivate")
                        .then(Commands.argument("id", StringArgumentType.word()).executes(::executeBloomForceActivate))
                )
        )

        event.dispatcher.register(root)
    }

    private fun buildWaveClear() = Commands.literal("clear")
        .then(Commands.literal("all").executes { context -> executeWaveClear(context) { true } })
        .then(
            Commands.literal("normal")
                .executes { context -> executeWaveClear(context) { wave -> wave.mode == BioluminescenceWaveMode.NORMAL } }
        )
        .then(
            Commands.literal("total_night")
                .executes { context -> executeWaveClear(context) { wave -> wave.mode == BioluminescenceWaveMode.TOTAL_NIGHT } }
        )
        .then(Commands.argument("eventId", StringArgumentType.word()).executes(::executeWaveClearById))

    private fun buildWaveCreate(): com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> {
        val createNode = Commands.literal("create")
        for (size in BioluminescenceWaveSize.entries) {
            val sizeNode = Commands.literal(size.name.lowercase())
                .executes { context -> executeWaveCreate(context, size, BioluminescenceWaveMode.NORMAL, null, null) }
            for (mode in BioluminescenceWaveMode.entries) {
                val modeNode = Commands.literal(mode.name.lowercase())
                    .executes { context -> executeWaveCreate(context, size, mode, null, null) }
                    .then(
                        Commands.argument("seed", LongArgumentType.longArg())
                            .executes { context ->
                                executeWaveCreate(context, size, mode, LongArgumentType.getLong(context, "seed"), null)
                            }
                            .then(
                                Commands.argument("durationTicks", LongArgumentType.longArg(1))
                                    .executes { context ->
                                        executeWaveCreate(
                                            context,
                                            size,
                                            mode,
                                            LongArgumentType.getLong(context, "seed"),
                                            LongArgumentType.getLong(context, "durationTicks")
                                        )
                                    }
                            )
                    )
                sizeNode.then(modeNode)
            }
            createNode.then(sizeNode)
        }
        return createNode
    }

    private fun buildWaveCreateHere(): com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> {
        val createHereNode = Commands.literal("createhere")
        for (size in BioluminescenceWaveSize.entries) {
            createHereNode.then(
                Commands.literal(size.name.lowercase())
                    .executes { context -> executeWaveCreate(context, size, BioluminescenceWaveMode.NORMAL, null, null) }
            )
        }
        return createHereNode
    }

    private fun executeStatus(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val server = source.server
        val serverManager = BioluminescenceServerManager.forServer(server)
        val levelManager = BioluminescenceLevelManager.forLevel(level)
        val settings = BioluminescenceServerSettings.current()
        val waves = levelManager.allWaves()
        val normalCount = waves.count { wave -> wave.mode == BioluminescenceWaveMode.NORMAL }
        val totalNightCount = waves.count { wave -> wave.mode == BioluminescenceWaveMode.TOTAL_NIGHT }
        val dayTime = level.overworldClockTime
        val isNight = settings.isNight(dayTime)
        val nextDeadline = levelManager.nextNormalWaveEndGameTime

        val lines = listOf(
            "${level.dimension().identifier()} enabled=${settings.enabled} " +
                "gameTime=${level.gameTime} dayTime=$dayTime(${Math.floorMod(dayTime, 24000L)}) " +
                "${if (isNight) "night" else "day"}",
            "nightIndex=${settings.nightIndex(dayTime)} lastRolled=${levelManager.lastRolledNightIndex} " +
                "totalNight=${levelManager.isTotalNightActive} " +
                "nextExpiry=" + if (nextDeadline == Long.MAX_VALUE) {
                "none"
            } else {
                "$nextDeadline(in ${nextDeadline - level.gameTime} ticks)"
            },
            "players: waiting=${serverManager.countByStatus(PlayerBioluminescenceStatus.WAITING)} " +
                "armed=${serverManager.countByStatus(PlayerBioluminescenceStatus.ARMED)} " +
                "known=${serverManager.knownPlayerIds().size}",
            "waves=${waves.size} (normal=$normalCount, totalNight=$totalNightCount) " +
                "sectors=${levelManager.sectorIndexCount} beaches=${levelManager.beachIndexCount} " +
                "beachCache=${levelManager.beachCacheZoneCount}/${levelManager.beachCacheDetectionCount}"
        )
        lines.forEach { line -> source.sendSuccess({ Component.literal(line) }, false) }
        return 1
    }

    private fun executeNightStart(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val settings = BioluminescenceServerSettings.current()
        if (!settings.enabled) {
            source.sendFailure(Component.literal("Bioluminescence is disabled in the server config."))
            return 0
        }
        if (!ModUtilities.isOverworldLikeDimension(level)) {
            source.sendFailure(Component.literal("This dimension does not support bioluminescence."))
            return 0
        }
        val started = BioluminescenceLevelManager.forLevel(level).forceStartTotalNight(settings)
        if (!started) {
            source.sendFailure(Component.literal("A total night is already active in this level."))
            return 0
        }
        source.sendSuccess(
            { Component.literal("Total bioluminescent night started in ${level.dimension().identifier()}.") },
            true
        )
        return 1
    }

    private fun executeNightStop(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val stopped = BioluminescenceLevelManager.forLevel(level).forceStopTotalNight()
        if (!stopped) {
            source.sendFailure(Component.literal("No total night active in this level."))
            return 0
        }
        source.sendSuccess(
            { Component.literal("Total bioluminescent night stopped in ${level.dimension().identifier()}.") },
            true
        )
        return 1
    }

    private fun executeNightRoll(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val settings = BioluminescenceServerSettings.current()
        val result = BioluminescenceLevelManager.forLevel(source.level).rollTotalNightChance(settings)
        source.sendSuccess(
            {
                Component.literal(
                    "Total night roll (configured chance=${settings.totalNightChance}) : " +
                        if (result) "SUCCESS (would have triggered a total night)" else "FAILURE"
                )
            },
            false
        )
        return 1
    }

    private fun executeWaveClear(
        context: CommandContext<CommandSourceStack>,
        predicate: (ActiveBioluminescenceWave) -> Boolean
    ): Int {
        val source = context.source
        val level = source.level
        val removed = BioluminescenceLevelManager.forLevel(level).endWavesWhere(predicate)
        source.sendSuccess(
            { Component.literal("$removed wave(s) cleared in ${level.dimension().identifier()}.") },
            true
        )
        return removed
    }

    private fun executeWaveClearById(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val manager = BioluminescenceLevelManager.forLevel(level)
        val input = StringArgumentType.getString(context, "eventId")
        return when (val resolved = resolveWaveMatch(manager, input, source)) {
            null -> 0
            else -> {
                val removed = manager.endWavesWhere { candidate -> candidate.eventId == resolved.eventId }
                source.sendSuccess({ Component.literal("Wave ${resolved.eventId} cleared.") }, true)
                removed
            }
        }
    }

    private fun executeWaveCreate(
        context: CommandContext<CommandSourceStack>,
        size: BioluminescenceWaveSize,
        mode: BioluminescenceWaveMode,
        seed: Long?,
        durationTicks: Long?
    ): Int {
        val source = context.source
        val player = source.playerOrException
        val level = source.level
        if (!ModUtilities.isOverworldLikeDimension(level)) {
            source.sendFailure(Component.literal("This dimension does not support bioluminescence."))
            return 0
        }
        val settings = BioluminescenceServerSettings.current()
        if (!settings.enabled) {
            source.sendFailure(Component.literal("Bioluminescence is disabled in the server config."))
            return 0
        }
        val manager = BioluminescenceLevelManager.forLevel(level)
        return when (val outcome = manager.createWaveForCommand(player, size, mode, settings, seed, durationTicks)) {
            is BioluminescenceLevelManager.WaveCreationOutcome.Created -> {
                source.sendSuccess(
                    {
                        Component.literal(
                            "Wave created: ${outcome.wave.eventId.toString().take(8)} " +
                                "(${mode.name.lowercase()}, ${size.name.lowercase()}) at ${outcome.wave.anchor}."
                        )
                    },
                    true
                )
                1
            }
            is BioluminescenceLevelManager.WaveCreationOutcome.Reused -> {
                source.sendSuccess(
                    {
                        Component.literal(
                            "Compatible wave already exists: ${outcome.wave.eventId.toString().take(8)} " +
                                "(reused by spatial deduplication, no conflict created)."
                        )
                    },
                    true
                )
                1
            }
            BioluminescenceLevelManager.WaveCreationOutcome.NoValidBeach -> {
                source.sendFailure(Component.literal("No valid beach or water surface at your position."))
                0
            }
        }
    }

    private fun executeWaveList(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val manager = BioluminescenceLevelManager.forLevel(level)
        val settings = BioluminescenceServerSettings.current()
        val waves = manager.allWaves()
        if (waves.isEmpty()) {
            source.sendSuccess({ Component.literal("No active wave in ${level.dimension().identifier()}.") }, false)
            return 0
        }
        source.sendSuccess(
            { Component.literal("Active waves in ${level.dimension().identifier()} (${waves.size}):") },
            false
        )
        val gameTime = level.gameTime
        val bloomManager = PlanktonBloomManager.forLevel(level)
        for (wave in waves.take(MAX_LISTED_WAVES)) {
            val age = gameTime - wave.startGameTime
            val remaining = if (wave.mode == BioluminescenceWaveMode.TOTAL_NIGHT) {
                "illimite"
            } else {
                "${wave.endGameTime - gameTime}"
            }
            val blooms = bloomManager.bloomsForWave(wave.eventId)
            source.sendSuccess(
                {
                    Component.literal(
                        "${wave.eventId.toString().take(8)} ${wave.mode.name.lowercase()} ${wave.size.name.lowercase()} " +
                            "${wave.activity.name.lowercase()} anchor=${wave.anchor} " +
                            "visibleRadius=${wave.size.selectGeodesicRadius(wave.seed)} " +
                            "age=$age remaining=$remaining " +
                            "overlaps=${manager.overlappingWaveCount(wave, settings)} " +
                            "blooms=${blooms.size}"
                    )
                },
                false
            )
        }
        if (waves.size > MAX_LISTED_WAVES) {
            source.sendSuccess(
                {
                    Component.literal(
                        "... and ${waves.size - MAX_LISTED_WAVES} more (use 'wave info <id>' for details)."
                    )
                },
                false
            )
        }
        return waves.size
    }

    private fun executeWaveInfo(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val manager = BioluminescenceLevelManager.forLevel(level)
        val settings = BioluminescenceServerSettings.current()
        val input = StringArgumentType.getString(context, "eventId")
        val wave = resolveWaveMatch(manager, input, source) ?: return 0

        val gameTime = level.gameTime
        val age = gameTime - wave.startGameTime
        val remaining = if (wave.mode == BioluminescenceWaveMode.TOTAL_NIGHT) "illimite" else "${wave.endGameTime - gameTime}"
        val viewers = level.players().count { player -> wave.eventId in manager.visibleWaveIds(player.uuid) }
        val lines = listOf(
            "${wave.eventId} seed=${wave.seed} dim=${wave.dimension} beach=${wave.beachId}",
            "${wave.mode.name.lowercase()} ${wave.size.name.lowercase()} ${wave.activity.name.lowercase()} " +
                "anchor=${wave.anchor} " +
                "visibleRadius=${wave.size.selectGeodesicRadius(wave.seed)} " +
                "bounds=[${wave.bounds.minimumX},${wave.bounds.minimumZ}]..[${wave.bounds.maximumX},${wave.bounds.maximumZ}]",
            "start=${wave.startGameTime} end=${wave.endGameTime} age=$age remaining=$remaining",
            "origin=${if (wave.createdByCommand) "command" else "natural"}" +
                (wave.originPlayerId?.let { originId -> "($originId)" } ?: "") +
                " viewers=$viewers sectors=${manager.sectorCount(wave.bounds)} " +
                "overlaps=${manager.overlappingWaveCount(wave, settings)}"
        )
        lines.forEach { line -> source.sendSuccess({ Component.literal(line) }, false) }

        for (bloom in PlanktonBloomManager.forLevel(level).bloomsForWave(wave.eventId)) {
            source.sendSuccess({ Component.literal(describeBloom(bloom)) }, false)
        }
        return 1
    }

    private fun describeBloom(bloom: PlanktonBloomState): String {
        return "bloom=${bloom.id.toString().take(8)} ${bloom.lifecycle.name.lowercase()} pos=${bloom.position} " +
            "harvests=${bloom.remainingHarvests}/${bloom.maxHarvests} " +
            "activated=${bloom.activatedAtGameTime ?: "never"}"
    }

    private fun executePlayerList(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val server = source.server
        val serverManager = BioluminescenceServerManager.forServer(server)
        val ids = serverManager.knownPlayerIds()
        if (ids.isEmpty()) {
            source.sendSuccess({ Component.literal("No player known to the bioluminescence system.") }, false)
            return 0
        }
        source.sendSuccess({ Component.literal("Known players (${ids.size}):") }, false)
        for (playerId in ids.take(MAX_LISTED_PLAYERS)) {
            source.sendSuccess({ Component.literal(formatPlayerSummary(server, playerId)) }, false)
        }
        if (ids.size > MAX_LISTED_PLAYERS) {
            source.sendSuccess(
                {
                    Component.literal(
                        "... and ${ids.size - MAX_LISTED_PLAYERS} more (use 'player info <player>' for details)."
                    )
                },
                false
            )
        }
        return ids.size
    }

    private fun executePlayerInfo(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val server = source.server
        val profile = resolveSingleProfile(context, source) ?: return 0
        val playerId = profile.id()
        val serverManager = BioluminescenceServerManager.forServer(server)
        val levelManager = BioluminescenceLevelManager.forLevel(source.level)
        val state = serverManager.snapshotState(playerId)
        if (state == null) {
            source.sendSuccess(
                { Component.literal("No bioluminescence state recorded for ${profile.name()} ($playerId).") },
                false
            )
            return 0
        }
        val online = server.playerList.getPlayer(playerId)
        val visibleWaves = levelManager.visibleWaveIds(playerId)
        val beach = levelManager.currentBeach(playerId)
        val gameTime = server.overworld().gameTime

        val lines = listOf(
            "${profile.name()} ($playerId) ${state.status.name.lowercase()} " +
                "online=${online != null}" +
                (online?.let { " dim=${it.level().dimension().identifier()} pos=${it.blockPosition()}" } ?: ""),
            "beach=${beach?.id?.toString()?.take(8) ?: "none"} " +
                (if (state.status == PlayerBioluminescenceStatus.WAITING) {
                    "nightTicksUntilOpportunity=${state.remainingNightTicksUntilOpportunity}"
                } else {
                    "armed=next beach entry"
                }) +
                " lastActivity=${state.lastActivityGameTime} (${gameTime - state.lastActivityGameTime} ticks ago)",
            "visibleWaves=" + (
                if (visibleWaves.isEmpty()) "none"
                else visibleWaves.joinToString(",") { id -> id.toString().take(8) }
                )
        )
        lines.forEach { line -> source.sendSuccess({ Component.literal(line) }, false) }
        return 1
    }

    private fun executePlayerArm(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val profile = resolveSingleProfile(context, source) ?: return 0
        val settings = BioluminescenceServerSettings.current()
        val triggeredWave = BioluminescenceServerManager.forServer(source.server).forcePlayerArmed(profile.id(), settings)
        val message = if (triggeredWave != null) {
            "${profile.name()} armed: a ${triggeredWave.activity.name.lowercase()} wave was triggered " +
                "immediately (already on a valid beach)."
        } else {
            "${profile.name()} armed, waiting for next valid beach entry (no beach at current position)."
        }
        source.sendSuccess({ Component.literal(message) }, true)
        return 1
    }

    private fun executePlayerDisarm(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val profile = resolveSingleProfile(context, source) ?: return 0
        val settings = BioluminescenceServerSettings.current()
        BioluminescenceServerManager.forServer(source.server).forcePlayerDisarmed(profile.id(), settings)
        source.sendSuccess({ Component.literal("${profile.name()} disarmed.") }, true)
        return 1
    }

    private fun executePlayerSchedule(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val profile = resolveSingleProfile(context, source) ?: return 0
        val nightTicks = LongArgumentType.getLong(context, "nightTicks")
        BioluminescenceServerManager.forServer(source.server)
            .schedulePlayerOpportunity(profile.id(), nightTicks, BioluminescenceServerSettings.current())
        source.sendSuccess(
            { Component.literal("Next opportunity for ${profile.name()} set to $nightTicks night ticks.") },
            true
        )
        return 1
    }

    private fun executePlayerReset(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val profile = resolveSingleProfile(context, source) ?: return 0
        BioluminescenceServerManager.forServer(source.server)
            .resetPlayerState(profile.id(), BioluminescenceServerSettings.current())
        source.sendSuccess({ Component.literal("Bioluminescence state for ${profile.name()} reset.") }, true)
        return 1
    }

    private fun executeBeachHere(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        val level = source.level
        val settings = BioluminescenceServerSettings.current()
        val manager = BioluminescenceLevelManager.forLevel(level)
        val diagnostic = manager.diagnoseBeach(player.uuid, player.blockPosition(), settings)

        val lines = mutableListOf(
            "beach diagnostic at ${diagnostic.position}",
            "dimensionSupported=${diagnostic.dimensionSupported}",
            "biome=${diagnostic.biomeId ?: "unknown"} beachTag=${diagnostic.isBeachBiome}",
            "waterSurface=${diagnostic.waterSurface ?: "none"}",
            "enoughNearbyWater=${diagnostic.hasEnoughNearbyWater ?: "not evaluated"}",
            "result=${if (diagnostic.zone != null) "VALID BEACH" else "REJECTED"}"
        )
        diagnostic.zone?.let { zone ->
            lines.add("beachId: ${zone.id}")
            lines.add(
                "bounds=[${zone.bounds.minimumX},${zone.bounds.minimumZ}].." +
                    "[${zone.bounds.maximumX},${zone.bounds.maximumZ}]"
            )
        }
        diagnostic.failureReason?.let { reason -> lines.add("reason=$reason") }
        lines.add("playerDetectionCache=${if (diagnostic.playerDetectionCacheHit) "hit" else "miss"}")
        lines.add("cellZoneCache=${if (diagnostic.cellZoneCacheHit) "hit" else "miss"}")
        lines.forEach { line -> source.sendSuccess({ Component.literal(line) }, false) }
        return 1
    }

    private fun executeBeachVerify(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        val level = source.level
        val settings = BioluminescenceServerSettings.current()
        val manager = BioluminescenceLevelManager.forLevel(level)
        val ids = manager.verifyBeachIdStability(player, settings)
        val stable = ids.all { id -> id == ids.first() }
        source.sendSuccess(
            {
                Component.literal(
                    "beachId stability (${ids.size} calls): " +
                        "${ids.map { id -> id?.toString()?.take(8) ?: "no beach" }} -> " +
                        if (stable) "STABLE" else "UNSTABLE"
                )
            },
            false
        )
        return 1
    }

    private fun executeBeachClearCache(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        BioluminescenceLevelManager.forLevel(level).clearBeachCaches()
        source.sendSuccess(
            { Component.literal("Beach detection caches cleared for ${level.dimension().identifier()}.") },
            true
        )
        return 1
    }

    private fun executeBloomList(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val manager = PlanktonBloomManager.forLevel(level)
        val blooms = manager.allBlooms()
        if (blooms.isEmpty()) {
            source.sendSuccess({ Component.literal("No bloom in ${level.dimension().identifier()}.") }, false)
            return 0
        }
        source.sendSuccess(
            { Component.literal("Blooms in ${level.dimension().identifier()} (${blooms.size}):") },
            false
        )
        val gameTime = level.gameTime
        for (bloom in blooms.take(MAX_LISTED_WAVES)) {
            source.sendSuccess(
                {
                    Component.literal(
                        describeBloom(bloom) + " wave=${bloom.waveEventId.toString().take(8)}"
                    )
                },
                false
            )
        }
        if (blooms.size > MAX_LISTED_WAVES) {
            source.sendSuccess(
                {
                    Component.literal(
                        "... and ${blooms.size - MAX_LISTED_WAVES} more (use 'bloom info <id>' for details)."
                    )
                },
                false
            )
        }
        return blooms.size
    }

    private fun executeBloomInfo(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val manager = PlanktonBloomManager.forLevel(level)
        val input = StringArgumentType.getString(context, "id")
        val bloom = resolveBloomMatch(manager, input, source) ?: return 0
        val gameTime = level.gameTime
        val waterValid = manager.diagnoseWater(bloom.position)
        val movement = manager.movementDiagnostic(bloom.position)

        val lines = listOf(
            "${bloom.id} wave=${bloom.waveEventId} seed=${bloom.visualSeed}",
            describeBloom(bloom) +
                (bloom.activatedAtGameTime?.let { activatedAt -> "(${gameTime - activatedAt} ticks ago)" } ?: ""),
            "water=${if (waterValid) "valid" else "invalid (will be invalidated next pass)"} " +
                "movement=${if (movement.detected) "detected" else "none"} " +
                "(radius=${movement.radius}, candidates=${movement.candidateCount}, " +
                "maxDisplacement=${"%.4f".format(movement.fastestSpeed)}, threshold=${movement.threshold})"
        )
        lines.forEach { line -> source.sendSuccess({ Component.literal(line) }, false) }
        return 1
    }

    private fun executeBloomHere(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        val level = source.level
        val manager = PlanktonBloomManager.forLevel(level)
        val blooms = manager.allBlooms()
        if (blooms.isEmpty()) {
            source.sendFailure(Component.literal("No bloom in ${level.dimension().identifier()}."))
            return 0
        }
        val nearest = blooms.minBy { bloom -> bloom.position.distSqr(player.blockPosition()) }
        val distance = sqrt(nearest.position.distSqr(player.blockPosition()))
        val activationRadius = ModServerConfig.BIOLUMINESCENCE_BLOOM_ACTIVATION_RADIUS.get()
        val harvestRadius = ModServerConfig.BIOLUMINESCENCE_BLOOM_HARVEST_RADIUS.get()

        source.sendSuccess(
            {
                Component.literal(
                    "nearest ${describeBloom(nearest)} wave=${nearest.waveEventId.toString().take(8)} " +
                        "distance=${"%.1f".format(distance)} " +
                        "inActivationRadius($activationRadius)=${distance <= activationRadius} " +
                        "inHarvestRadius($harvestRadius)=${distance <= harvestRadius}"
                )
            },
            false
        )
        return 1
    }

    private fun executeBloomForceActivate(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val manager = PlanktonBloomManager.forLevel(level)
        val input = StringArgumentType.getString(context, "id")
        val bloom = resolveBloomMatch(manager, input, source) ?: return 0
        val success = manager.forceActivate(bloom.id, level.gameTime)
        if (success) {
            source.sendSuccess({ Component.literal("Bloom ${bloom.id.toString().take(8)} force-activated.") }, true)
        } else {
            source.sendFailure(Component.literal("Cannot activate ${bloom.id.toString().take(8)} (state=${bloom.lifecycle}, requires DORMANT)."))
        }
        return if (success) 1 else 0
    }

    private fun resolveBloomMatch(
        manager: PlanktonBloomManager,
        input: String,
        source: CommandSourceStack
    ): PlanktonBloomState? {
        val exact = runCatching { UUID.fromString(input) }.getOrNull()
        val matches = if (exact != null) {
            manager.allBlooms().filter { bloom -> bloom.id == exact }
        } else {
            manager.allBlooms().filter { bloom -> bloom.id.toString().startsWith(input, ignoreCase = true) }
        }
        return when {
            matches.isEmpty() -> {
                source.sendFailure(Component.literal("No bloom matches '$input'."))
                null
            }
            matches.size > 1 -> {
                source.sendFailure(
                    Component.literal(
                        "Identifiant ambigu '$input' (${matches.size} correspondances) : " +
                            matches.joinToString(", ") { bloom -> bloom.id.toString().take(8) }
                    )
                )
                null
            }
            else -> matches.single()
        }
    }

    private fun resolveSingleProfile(
        context: CommandContext<CommandSourceStack>,
        source: CommandSourceStack
    ): NameAndId? {
        val profiles = GameProfileArgument.getGameProfiles(context, "player")
        return when {
            profiles.isEmpty() -> {
                source.sendFailure(Component.literal("Player not found."))
                null
            }
            profiles.size > 1 -> {
                source.sendFailure(
                    Component.literal(
                        "Selector matched multiple players (${profiles.size}), expected exactly one."
                    )
                )
                null
            }
            else -> profiles.first()
        }
    }

    private fun resolveWaveMatch(
        manager: BioluminescenceLevelManager,
        input: String,
        source: CommandSourceStack
    ): ActiveBioluminescenceWave? {
        val exact = runCatching { UUID.fromString(input) }.getOrNull()
        val matches = if (exact != null) {
            manager.allWaves().filter { wave -> wave.eventId == exact }
        } else {
            manager.allWaves().filter { wave -> wave.eventId.toString().startsWith(input, ignoreCase = true) }
        }
        return when {
            matches.isEmpty() -> {
                source.sendFailure(Component.literal("No wave matches '$input'."))
                null
            }
            matches.size > 1 -> {
                source.sendFailure(
                    Component.literal(
                        "Identifiant ambigu '$input' (${matches.size} correspondances) : " +
                            matches.joinToString(", ") { wave -> wave.eventId.toString().take(8) }
                    )
                )
                null
            }
            else -> matches.single()
        }
    }

    private fun formatPlayerSummary(server: MinecraftServer, playerId: UUID): String {
        val serverManager = BioluminescenceServerManager.forServer(server)
        val state = serverManager.snapshotState(playerId)
            ?: return "${playerId.toString().take(8)} : no recorded state"
        val online = server.playerList.getPlayer(playerId)
        val label = online?.gameProfile?.name ?: playerId.toString().take(8)
        return "$label (${playerId.toString().take(8)}) status=${state.status.name.lowercase()} " +
            (if (online != null) "online" else "offline")
    }
}
