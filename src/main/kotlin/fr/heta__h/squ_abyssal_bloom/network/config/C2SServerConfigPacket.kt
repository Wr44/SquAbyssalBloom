package fr.heta__h.squ_abyssal_bloom.network.config

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ServerConfigData
import net.minecraft.ChatFormatting
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext

data class C2SServerConfigPacket(val data: ServerConfigData) : CustomPacketPayload {

    override fun type() = ID

    companion object {
        val ID = CustomPacketPayload.Type<C2SServerConfigPacket>(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "server_config_update")
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, C2SServerConfigPacket> = StreamCodec.composite(
            ServerConfigData.STREAM_CODEC, C2SServerConfigPacket::data,
            ::C2SServerConfigPacket
        )

        fun handle(payload: C2SServerConfigPacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player() as? ServerPlayer ?: return@enqueueWork
                val css = player.createCommandSourceStack()

                if (!css.server.playerList.isOp(player.nameAndId())) {
                    player.sendSystemMessage(
                        Component.translatable("config.squ_abyssal_bloom.server.no_permission")
                            .withStyle(ChatFormatting.RED)
                    )
                    return@enqueueWork
                }

                payload.data.applyToSpec()
                PacketDistributor.sendToAllPlayers(S2CServerConfigPacket(ServerConfigData.fromSpec()))
            }
        }
    }
}