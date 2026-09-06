package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
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
        val root = Commands.literal("bioluminescence")
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
        event.dispatcher.register(root)
    }
}
