package fr.heta__h.squ_abyssal_bloom.network.bioluminescence

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext

data class S2CBioluminescenceArmedPayload(val armed: Boolean) : CustomPacketPayload {
    companion object {
        val ID = CustomPacketPayload.Type<S2CBioluminescenceArmedPayload>(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "bioluminescence_armed")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, S2CBioluminescenceArmedPayload> = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            S2CBioluminescenceArmedPayload::armed,
            ::S2CBioluminescenceArmedPayload
        )

        fun handle(payload: S2CBioluminescenceArmedPayload, context: IPayloadContext) {
            context.enqueueWork {
                BioluminescenceClientPayloadHandler.handleArmed(payload)
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<S2CBioluminescenceArmedPayload> = ID
}
