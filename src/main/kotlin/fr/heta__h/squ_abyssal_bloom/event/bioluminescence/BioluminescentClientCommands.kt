package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.render.bioluminescence_wave.palette.BioluminescentPaletteFamily
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
                    BioluminescentPaletteFamily.RANDOM
                )
            }

        for (size in BioluminescentZoneSize.entries) {
            val sizeNode = Commands.literal(size.commandName)
                .executes { context ->
                    executeSpawn(context.source, size, BioluminescentPaletteFamily.RANDOM)
                }
            for (family in BioluminescentPaletteFamily.entries) {
                sizeNode.then(
                    Commands.literal(family.commandName)
                        .executes { context -> executeSpawn(context.source, size, family) }
                )
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
        event.dispatcher.register(root)
    }

    private fun executeSpawn(
        source: CommandSourceStack,
        size: BioluminescentZoneSize,
        family: BioluminescentPaletteFamily
    ): Int {
        return when (val result = BioluminescentZoneManager.spawnDebugZone(size, family)) {
            is BioluminescentZoneManager.SpawnResult.Created -> {
                source.sendSuccess(
                    {
                        Component.literal(
                            "Generation ${result.size.commandName} lancee a " +
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
