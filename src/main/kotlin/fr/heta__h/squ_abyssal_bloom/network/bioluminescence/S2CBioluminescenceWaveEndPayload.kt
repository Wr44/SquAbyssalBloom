package fr.heta__h.squ_abyssal_bloom.network.bioluminescence

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.UUID

data class S2CBioluminescenceWaveEndPayload(
    val eventId: UUID,
    val dimension: Identifier
) : CustomPacketPayload {
    companion object {
        val ID = CustomPacketPayload.Type<S2CBioluminescenceWaveEndPayload>(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "bioluminescence_wave_end")
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, S2CBioluminescenceWaveEndPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeUUID(payload.eventId)
                buffer.writeIdentifier(payload.dimension)
            },
            { buffer ->
                S2CBioluminescenceWaveEndPayload(
                    buffer.readUUID(),
                    buffer.readIdentifier()
                )
            }
        )

        fun handle(payload: S2CBioluminescenceWaveEndPayload, context: IPayloadContext) {
            context.enqueueWork {
                BioluminescenceClientPayloadHandler.handleWaveEnd(payload)
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<S2CBioluminescenceWaveEndPayload> = ID
}
