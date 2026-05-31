package fr.heta__h.squ_abyssal_bloom.network.config

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache
import fr.heta__h.squ_abyssal_bloom.config.ServerConfigData
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext

data class S2CServerConfigPacket(val data: ServerConfigData) : CustomPacketPayload {

    override fun type() = ID

    companion object {
        val ID = CustomPacketPayload.Type<S2CServerConfigPacket>(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "server_config_sync")
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, S2CServerConfigPacket> = StreamCodec.composite(
            ServerConfigData.STREAM_CODEC, S2CServerConfigPacket::data,
            ::S2CServerConfigPacket
        )

        fun handle(payload: S2CServerConfigPacket, context: IPayloadContext) {
            context.enqueueWork {
                ServerConfigCache.update(payload.data)
            }
        }
    }
}