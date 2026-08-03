package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.server

import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveMode
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveSize
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.GameProfileArgument
import net.minecraft.network.chat.Component
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.ActiveBioluminescenceWave
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceLevelManager
import fr.heta__h.squ_abyssal_bloom.worldgen.bioluminescence_wave.BioluminescenceServerManager
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceServerSettings
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.PlayerBioluminescenceStatus
import net.minecraft.server.MinecraftServer
import net.minecraft.server.players.NameAndId
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterCommandsEvent
import java.util.UUID

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
            "=== Bioluminescence : statut (${level.dimension().identifier()}) ===",
            "Systeme serveur: ${if (settings.enabled) "active" else "desactive"}",
            "Temps de jeu (gameTime): ${level.gameTime}",
            "Temps d'horloge overworld: $dayTime (tick du jour ${Math.floorMod(dayTime, 24000L)})",
            "Etat: ${if (isNight) "nuit" else "jour"}",
            "Indice de nuit courant: ${settings.nightIndex(dayTime)} " +
                "(dernier tirage memorise: ${levelManager.lastRolledNightIndex})",
            "Nuit totale active: ${if (levelManager.isTotalNightActive) "oui" else "non"}",
            "Joueurs WAITING: ${serverManager.countByStatus(PlayerBioluminescenceStatus.WAITING)}",
            "Joueurs ARMED: ${serverManager.countByStatus(PlayerBioluminescenceStatus.ARMED)}",
            "Joueurs connus du systeme: ${serverManager.knownPlayerIds().size}",
            "Vagues actives: ${waves.size} (normal=$normalCount, nuit_totale=$totalNightCount)",
            "Prochaine echeance d'expiration connue: " + if (nextDeadline == Long.MAX_VALUE) {
                "aucune"
            } else {
                "$nextDeadline (dans ${nextDeadline - level.gameTime} ticks)"
            },
            "Index spatial: ${levelManager.sectorIndexCount} secteur(s), " +
                "${levelManager.beachIndexCount} plage(s) indexee(s)",
            "Cache plages: ${levelManager.beachCacheZoneCount} zone(s), " +
                "${levelManager.beachCacheDetectionCount} detection(s) joueur"
        )
        lines.forEach { line -> source.sendSuccess({ Component.literal(line) }, false) }
        return 1
    }

    private fun executeNightStart(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val settings = BioluminescenceServerSettings.current()
        if (!settings.enabled) {
            source.sendFailure(Component.literal("Le systeme de bioluminescence est desactive dans la configuration serveur."))
            return 0
        }
        if (!ModUtilities.isOverworldLikeDimension(level)) {
            source.sendFailure(Component.literal("Cette dimension n'est pas compatible avec la bioluminescence."))
            return 0
        }
        val started = BioluminescenceLevelManager.forLevel(level).forceStartTotalNight(settings)
        if (!started) {
            source.sendFailure(Component.literal("Une nuit totale est deja active dans ce niveau."))
            return 0
        }
        source.sendSuccess(
            { Component.literal("Nuit bioluminescente totale demarree dans ${level.dimension().identifier()}.") },
            true
        )
        return 1
    }

    private fun executeNightStop(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val level = source.level
        val stopped = BioluminescenceLevelManager.forLevel(level).forceStopTotalNight()
        if (!stopped) {
            source.sendFailure(Component.literal("Aucune nuit totale active dans ce niveau."))
            return 0
        }
        source.sendSuccess(
            { Component.literal("Nuit bioluminescente totale arretee dans ${level.dimension().identifier()}.") },
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
                    "Tirage nuit totale (probabilite configuree=${settings.totalNightChance}) : " +
                        if (result) "SUCCES (aurait declenche une nuit totale)" else "ECHEC"
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
            { Component.literal("$removed vague(s) supprimee(s) dans ${level.dimension().identifier()}.") },
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
                source.sendSuccess({ Component.literal("Vague ${resolved.eventId} supprimee.") }, true)
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
            source.sendFailure(Component.literal("Cette dimension n'est pas compatible avec la bioluminescence."))
            return 0
        }
        val settings = BioluminescenceServerSettings.current()
        if (!settings.enabled) {
            source.sendFailure(Component.literal("Le systeme de bioluminescence est desactive dans la configuration serveur."))
            return 0
        }
        val manager = BioluminescenceLevelManager.forLevel(level)
        return when (val outcome = manager.createWaveForCommand(player, size, mode, settings, seed, durationTicks)) {
            is BioluminescenceLevelManager.WaveCreationOutcome.Created -> {
                source.sendSuccess(
                    {
                        Component.literal(
                            "Vague creee: ${outcome.wave.eventId.toString().take(8)} " +
                                "(${mode.name.lowercase()}, ${size.name.lowercase()}) a ${outcome.wave.anchor}."
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
                            "Une vague compatible existe deja: ${outcome.wave.eventId.toString().take(8)} " +
                                "(reutilisee par deduplication spatiale, aucun conflit cree)."
                        )
                    },
                    true
                )
                1
            }
            BioluminescenceLevelManager.WaveCreationOutcome.NoValidBeach -> {
                source.sendFailure(Component.literal("Aucune plage ou surface d'eau valide detectee a votre position."))
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
            source.sendSuccess({ Component.literal("Aucune vague active dans ${level.dimension().identifier()}.") }, false)
            return 0
        }
        source.sendSuccess(
            { Component.literal("=== Vagues actives dans ${level.dimension().identifier()} (${waves.size}) ===") },
            false
        )
        val gameTime = level.gameTime
        for (wave in waves.take(MAX_LISTED_WAVES)) {
            val age = gameTime - wave.startGameTime
            val remaining = if (wave.mode == BioluminescenceWaveMode.TOTAL_NIGHT) {
                "illimite"
            } else {
                "${wave.endGameTime - gameTime}"
            }
            source.sendSuccess(
                {
                    Component.literal(
                        "${wave.eventId.toString().take(8)} ${wave.mode.name.lowercase()} ${wave.size.name.lowercase()} " +
                            "${wave.activity.name.lowercase()} anchor=${wave.anchor} age=$age reste=$remaining " +
                            "chevauchements=${manager.overlappingWaveCount(wave, settings)}"
                    )
                },
                false
            )
        }
        if (waves.size > MAX_LISTED_WAVES) {
            source.sendSuccess(
                {
                    Component.literal(
                        "... et ${waves.size - MAX_LISTED_WAVES} de plus (utilisez 'wave info <id>' pour le detail)."
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
            "=== Vague ${wave.eventId} ===",
            "Court: ${wave.eventId.toString().take(8)}",
            "Seed: ${wave.seed}",
            "Dimension: ${wave.dimension}",
            "BeachId: ${wave.beachId}",
            "Mode: ${wave.mode.name.lowercase()}  Taille: ${wave.size.name.lowercase()}  Activite: ${wave.activity.name.lowercase()}",
            "Anchor: ${wave.anchor}",
            "Bounds: [${wave.bounds.minimumX},${wave.bounds.minimumZ}] a [${wave.bounds.maximumX},${wave.bounds.maximumZ}]",
            "startGameTime=${wave.startGameTime}  endGameTime=${wave.endGameTime}",
            "Age: $age ticks  Temps restant: $remaining",
            "Origine: ${if (wave.createdByCommand) "commande" else "naturelle"}" +
                (wave.originPlayerId?.let { originId -> "  (joueur d'origine: $originId)" } ?: ""),
            "Joueurs dans la zone de synchronisation: $viewers",
            "Secteurs spatiaux couverts: ${manager.sectorCount(wave.bounds)}",
            "Chevauchements detectes: ${manager.overlappingWaveCount(wave, settings)}",
            "Etat persistant: oui (sauvegarde du niveau)"
        )
        lines.forEach { line -> source.sendSuccess({ Component.literal(line) }, false) }
        return 1
    }

    private fun executePlayerList(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val server = source.server
        val serverManager = BioluminescenceServerManager.forServer(server)
        val ids = serverManager.knownPlayerIds()
        if (ids.isEmpty()) {
            source.sendSuccess({ Component.literal("Aucun joueur connu du systeme de bioluminescence.") }, false)
            return 0
        }
        source.sendSuccess({ Component.literal("=== Joueurs connus (${ids.size}) ===") }, false)
        for (playerId in ids.take(MAX_LISTED_PLAYERS)) {
            source.sendSuccess({ Component.literal(formatPlayerSummary(server, playerId)) }, false)
        }
        if (ids.size > MAX_LISTED_PLAYERS) {
            source.sendSuccess(
                {
                    Component.literal(
                        "... et ${ids.size - MAX_LISTED_PLAYERS} de plus (utilisez 'player info <joueur>' pour le detail)."
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
                { Component.literal("Aucun etat de bioluminescence enregistre pour ${profile.name()} ($playerId).") },
                false
            )
            return 0
        }
        val online = server.playerList.getPlayer(playerId)
        val visibleWaves = levelManager.visibleWaveIds(playerId)
        val beach = levelManager.currentBeach(playerId)
        val gameTime = server.overworld().gameTime

        val lines = mutableListOf(
            "=== Joueur ${profile.name()} ($playerId) ===",
            "Statut: ${state.status.name.lowercase()}",
            "Connecte: ${if (online != null) "oui" else "non"}"
        )
        if (online != null) {
            lines.add("Dimension: ${online.level().dimension().identifier()}")
            lines.add("Position: ${online.blockPosition()}")
        }
        lines.add("Plage detectee: ${beach?.id?.toString()?.take(8) ?: "aucune"}")
        lines.add(
            if (state.status == PlayerBioluminescenceStatus.WAITING) {
                "Ticks nocturnes restants avant la prochaine opportunite: ${state.remainingNightTicksUntilOpportunity}"
            } else {
                "Arme: opportunite disponible a la prochaine entree sur une plage."
            }
        )
        lines.add(
            "Derniere activite (gameTime): ${state.lastActivityGameTime} " +
                "(il y a ${gameTime - state.lastActivityGameTime} ticks)"
        )
        lines.add(
            if (visibleWaves.isEmpty()) {
                "Vague(s) visible(s)/rejointe(s): aucune"
            } else {
                "Vague(s) visible(s)/rejointe(s): " + visibleWaves.joinToString(", ") { id -> id.toString().take(8) }
            }
        )
        lines.add("Etat de prechargement client: information indisponible cote serveur (donnee purement client).")
        lines.forEach { line -> source.sendSuccess({ Component.literal(line) }, false) }
        return 1
    }

    private fun executePlayerArm(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val profile = resolveSingleProfile(context, source) ?: return 0
        val settings = BioluminescenceServerSettings.current()
        val triggeredWave = BioluminescenceServerManager.forServer(source.server).forcePlayerArmed(profile.id(), settings)
        val message = if (triggeredWave != null) {
            "${profile.name()} est ARME : une vague ${triggeredWave.activity.name.lowercase()} vient d'etre " +
                "declenchee immediatement (deja sur une plage valide)."
        } else {
            "${profile.name()} est ARME, en attente de la prochaine entree sur une plage valide (aucune plage detectee a sa position actuelle)."
        }
        source.sendSuccess({ Component.literal(message) }, true)
        return 1
    }

    private fun executePlayerDisarm(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val profile = resolveSingleProfile(context, source) ?: return 0
        val settings = BioluminescenceServerSettings.current()
        BioluminescenceServerManager.forServer(source.server).forcePlayerDisarmed(profile.id(), settings)
        source.sendSuccess({ Component.literal("${profile.name()} est desarme.") }, true)
        return 1
    }

    private fun executePlayerSchedule(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val profile = resolveSingleProfile(context, source) ?: return 0
        val nightTicks = LongArgumentType.getLong(context, "nightTicks")
        BioluminescenceServerManager.forServer(source.server)
            .schedulePlayerOpportunity(profile.id(), nightTicks, BioluminescenceServerSettings.current())
        source.sendSuccess(
            { Component.literal("Delai avant la prochaine opportunite de ${profile.name()} fixe a $nightTicks ticks.") },
            true
        )
        return 1
    }

    private fun executePlayerReset(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val profile = resolveSingleProfile(context, source) ?: return 0
        BioluminescenceServerManager.forServer(source.server)
            .resetPlayerState(profile.id(), BioluminescenceServerSettings.current())
        source.sendSuccess({ Component.literal("Etat de bioluminescence de ${profile.name()} reinitialise.") }, true)
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
            "=== Diagnostic de plage a ${diagnostic.position} ===",
            "Dimension compatible: ${if (diagnostic.dimensionSupported) "oui" else "non"}",
            "Biome: ${diagnostic.biomeId ?: "inconnu"} (tag plage: ${if (diagnostic.isBeachBiome) "oui" else "non"})",
            "Surface d'eau trouvee: ${diagnostic.waterSurface ?: "aucune"}",
            "Assez d'eau proche: ${diagnostic.hasEnoughNearbyWater?.let { if (it) "oui" else "non" } ?: "non evalue"}",
            "Resultat: ${if (diagnostic.zone != null) "PLAGE VALIDE" else "REFUSE"}"
        )
        diagnostic.zone?.let { zone ->
            lines.add("beachId: ${zone.id}")
            lines.add(
                "Bounds estimes: [${zone.bounds.minimumX},${zone.bounds.minimumZ}] a " +
                    "[${zone.bounds.maximumX},${zone.bounds.maximumZ}]"
            )
        }
        diagnostic.failureReason?.let { reason -> lines.add("Raison precise du refus: $reason") }
        lines.add("Cache detection joueur: ${if (diagnostic.playerDetectionCacheHit) "hit" else "miss"}")
        lines.add("Cache zone cellule: ${if (diagnostic.cellZoneCacheHit) "hit" else "miss"}")
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
                    "Verification stabilite beachId (${ids.size} appels): " +
                        "${ids.map { id -> id?.toString()?.take(8) ?: "aucune plage" }} -> " +
                        if (stable) "STABLE" else "INSTABLE"
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
            { Component.literal("Caches de detection de plage vides pour ${level.dimension().identifier()}.") },
            true
        )
        return 1
    }

    private fun resolveSingleProfile(
        context: CommandContext<CommandSourceStack>,
        source: CommandSourceStack
    ): NameAndId? {
        val profiles = GameProfileArgument.getGameProfiles(context, "player")
        return when {
            profiles.isEmpty() -> {
                source.sendFailure(Component.literal("Joueur introuvable."))
                null
            }
            profiles.size > 1 -> {
                source.sendFailure(
                    Component.literal(
                        "Le selecteur correspond a plusieurs joueurs (${profiles.size}), une seule cible est attendue."
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
                source.sendFailure(Component.literal("Aucune vague ne correspond a '$input'."))
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
            ?: return "${playerId.toString().take(8)} : aucun etat enregistre"
        val online = server.playerList.getPlayer(playerId)
        val label = online?.gameProfile?.name ?: playerId.toString().take(8)
        return "$label (${playerId.toString().take(8)}) statut=${state.status.name.lowercase()} " +
            (if (online != null) "connecte" else "hors_ligne")
    }
}
