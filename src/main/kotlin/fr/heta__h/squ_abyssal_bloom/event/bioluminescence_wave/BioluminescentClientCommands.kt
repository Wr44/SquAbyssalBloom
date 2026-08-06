package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent

@EventBusSubscriber(
    modid = SquAbyssalBloom.ID,
    value = [Dist.CLIENT]
)
object BioluminescentClientCommands {
    @SubscribeEvent
    fun registerCommands(event: RegisterClientCommandsEvent) {
        val root = Commands.literal("squ_bioluminescence")
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
            Commands.literal("irisdebug")
                .then(
                    Commands.literal("mode")
                        .executes { context ->
                            context.source.sendSuccess(
                                { Component.literal("Iris debug mode: ${BioluminescentIrisDebugState.mode}") },
                                false
                            )
                            1
                        }
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    executeIrisDebugMode(
                                        context.source,
                                        StringArgumentType.getString(context, "name")
                                    )
                                }
                        )
                )
                .then(
                    Commands.literal("offset")
                        .then(
                            Commands.literal("reset")
                                .executes { context ->
                                    BioluminescentIrisDebugState.compensationOffsetOverride = null
                                    context.source.sendSuccess(
                                        { Component.literal("Compensation offset reset.") },
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
                                        { Component.literal("Compensation offset set to $value") },
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
                                { Component.literal("Iris debug reset (NORMAL mode, default offset).") },
                                false
                            )
                            1
                        }
                )
        )
        event.dispatcher.register(root)
    }

    private fun executeIrisDebugMode(source: CommandSourceStack, name: String): Int {
        val mode = BioluminescentIrisDebugMode.entries.firstOrNull {
            mode -> mode.name.equals(name, ignoreCase = true)
        } ?: run {
            source.sendFailure(
                Component.literal(
                    "Unknown mode. Valid modes: ${BioluminescentIrisDebugMode.entries.joinToString { it.name }}"
                )
            )
            return 0
        }
        BioluminescentIrisDebugState.mode = mode
        source.sendSuccess({ Component.literal("Iris debug mode set to $mode") }, false)
        return 1
    }
}
