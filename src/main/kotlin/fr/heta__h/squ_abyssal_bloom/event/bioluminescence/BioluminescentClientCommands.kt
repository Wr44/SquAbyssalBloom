package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPaletteFamily
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneActivity
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.zone.BioluminescentZoneSize
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent
import kotlin.math.roundToInt

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentClientCommands {
    @SubscribeEvent
    fun registerCommands(event: RegisterClientCommandsEvent) {
        val root = Commands.literal("squ_bioluminescence")
        val spawn = Commands.literal("spawn")
            .executes { context ->
                executeSpawn(
                    context.source,
                    BioluminescentZoneSize.MEDIUM,
                    BioluminescentPaletteFamily.RANDOM,
                    BioluminescentZoneActivity.ACTIVE
                )
            }

        for (size in BioluminescentZoneSize.entries) {
            val sizeNode = Commands.literal(size.commandName)
                .executes { context ->
                    executeSpawn(
                        context.source,
                        size,
                        BioluminescentPaletteFamily.RANDOM,
                        BioluminescentZoneActivity.ACTIVE
                    )
                }
            for (activity in BioluminescentZoneActivity.entries) {
                val activityNode = Commands.literal(activity.commandName)
                    .executes { context ->
                        executeSpawn(context.source, size, BioluminescentPaletteFamily.RANDOM, activity)
                    }
                for (family in BioluminescentPaletteFamily.entries) {
                    activityNode.then(
                        Commands.literal(family.commandName)
                            .executes { context -> executeSpawn(context.source, size, family, activity) }
                    )
                }
                sizeNode.then(activityNode)
            }
            for (family in BioluminescentPaletteFamily.entries) {
                val familyNode = Commands.literal(family.commandName)
                    .executes { context ->
                        executeSpawn(context.source, size, family, BioluminescentZoneActivity.ACTIVE)
                    }
                for (activity in BioluminescentZoneActivity.entries) {
                    familyNode.then(
                        Commands.literal(activity.commandName)
                            .executes { context -> executeSpawn(context.source, size, family, activity) }
                    )
                }
                sizeNode.then(familyNode)
            }
            spawn.then(sizeNode)
        }

        root.then(spawn)
        root.then(
            Commands.literal("debug")
                .executes { context ->
                    val report = BioluminescentZoneManager.debugReport()
                    report.forEach { line ->
                        context.source.sendSuccess({ Component.literal(line) }, false)
                    }
                    report.size
                }
        )
        root.then(
            Commands.literal("clear")
                .executes { context ->
                    val count = BioluminescentZoneManager.clearDebugZones()
                    context.source.sendSuccess(
                        { Component.literal("$count zone(s) de bioluminescence supprimee(s).") },
                        false
                    )
                    1
                }
        )
        root.then(
            Commands.literal("irisdebug")
                .then(
                    Commands.literal("mode")
                        .executes { context ->
                            context.source.sendSuccess(
                                { Component.literal("Mode debug Iris actuel: ${BioluminescentIrisDebugState.mode}") },
                                false
                            )
                            1
                        }
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .executes { context -> executeIrisDebugMode(context.source, StringArgumentType.getString(context, "name")) }
                        )
                )
                .then(
                    Commands.literal("offset")
                        .then(
                            Commands.literal("reset")
                                .executes { context ->
                                    BioluminescentIrisDebugState.compensationOffsetOverride = null
                                    context.source.sendSuccess(
                                        { Component.literal("Offset de compensation reinitialise.") },
                                        false
                                    )
                                    1
                                }
                        )
                        .then(
                            Commands.argument("value", DoubleArgumentType.doubleArg())
                                .executes { context ->
                                    val value = DoubleArgumentType.getDouble(context, "value")
                                    BioluminescentIrisDebugState.compensationOffsetOverride = value
                                    context.source.sendSuccess(
                                        { Component.literal("Offset de compensation regle sur $value") },
                                        false
                                    )
                                    1
                                }
                        )
                )
                .then(
                    Commands.literal("reset")
                        .executes { context ->
                            BioluminescentIrisDebugState.reset()
                            context.source.sendSuccess(
                                { Component.literal("Debug Iris reinitialise (mode NORMAL, offset par defaut).") },
                                false
                            )
                            1
                        }
                )
        )
        event.dispatcher.register(root)
    }

    private fun executeIrisDebugMode(source: CommandSourceStack, name: String): Int {
        val mode = BioluminescentIrisDebugMode.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: return failure(
                source,
                "Mode inconnu. Modes valides: ${BioluminescentIrisDebugMode.entries.joinToString { it.name }}"
            )
        BioluminescentIrisDebugState.mode = mode
        source.sendSuccess({ Component.literal("Mode debug Iris regle sur $mode") }, false)
        return 1
    }

    private fun executeSpawn(
        source: CommandSourceStack,
        size: BioluminescentZoneSize,
        family: BioluminescentPaletteFamily,
        activity: BioluminescentZoneActivity
    ): Int {
        return when (val result = BioluminescentZoneManager.spawnDebugZone(size, family, activity)) {
            is BioluminescentZoneManager.SpawnResult.Created -> {
                source.sendSuccess(
                    {
                        Component.literal(
                            "Generation ${result.size.commandName} ${result.activity.commandName} lancee a " +
                                "${result.distance.roundToInt()} blocs."
                        )
                    },
                    false
                )
                1
            }
            BioluminescentZoneManager.SpawnResult.NoCoastalSurface -> failure(
                source,
                "Aucune surface d'eau cotiere valide trouvee a proximite."
            )
            BioluminescentZoneManager.SpawnResult.ZoneLimitReached -> failure(
                source,
                "La limite de zones actives est deja atteinte."
            )
            BioluminescentZoneManager.SpawnResult.DimensionNotAllowed -> failure(
                source,
                "La bioluminescence cotiere n'est pas autorisee dans cette dimension."
            )
            BioluminescentZoneManager.SpawnResult.NoLevel -> failure(
                source,
                "Aucun niveau client actif."
            )
        }
    }

    private fun failure(source: CommandSourceStack, message: String): Int {
        source.sendFailure(Component.literal(message))
        return 0
    }
}
