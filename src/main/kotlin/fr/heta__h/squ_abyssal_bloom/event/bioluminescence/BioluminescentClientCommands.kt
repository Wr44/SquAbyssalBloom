package fr.heta__h.squ_abyssal_bloom.event.bioluminescence

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentBloomSize
import fr.heta__h.squ_abyssal_bloom.util.bioluminescence.BioluminescentPaletteFamily
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
                    BioluminescentBloomSize.MEDIUM,
                    BioluminescentPaletteFamily.RANDOM
                )
            }

        for (size in BioluminescentBloomSize.entries) {
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
            Commands.literal("clear")
                .executes { context ->
                    val removedZones = BioluminescentBloomManager.clearDebugZones()
                    context.source.sendSuccess(
                        { Component.literal("$removedZones zone(s) de bioluminescence supprimée(s).") },
                        false
                    )
                    1
                }
        )

        event.dispatcher.register(root)
    }

    private fun executeSpawn(
        source: CommandSourceStack,
        size: BioluminescentBloomSize,
        family: BioluminescentPaletteFamily
    ): Int {
        return when (val result = BioluminescentBloomManager.spawnDebugZone(size, family)) {
            is BioluminescentBloomManager.SpawnResult.Created -> {
                source.sendSuccess(
                    {
                        Component.literal(
                            "Zone de bioluminescence créée à ${result.distance.roundToInt()} blocs " +
                                "(${result.plannedBloomCount} nappes prévues)."
                        )
                    },
                    false
                )
                1
            }
            BioluminescentBloomManager.SpawnResult.NoCoastalSurface -> failure(
                source,
                "Aucune surface côtière valide trouvée à proximité."
            )
            BioluminescentBloomManager.SpawnResult.ZoneLimitReached -> failure(
                source,
                "Limite maximale de zones déjà atteinte."
            )
            BioluminescentBloomManager.SpawnResult.BloomLimitReached -> failure(
                source,
                "Limite maximale de nappes déjà atteinte."
            )
            BioluminescentBloomManager.SpawnResult.DimensionNotAllowed -> failure(
                source,
                "La bioluminescence côtière n'est pas autorisée dans cette dimension."
            )
            BioluminescentBloomManager.SpawnResult.NoLevel -> failure(
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
