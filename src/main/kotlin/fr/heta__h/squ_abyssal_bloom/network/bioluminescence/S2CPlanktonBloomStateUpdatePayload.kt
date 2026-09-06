package fr.heta__h.squ_abyssal_bloom.network.bioluminescence

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.Optional
import java.util.UUID

data class S2CPlanktonBloomStateUpdatePayload(
    val waveEventId: UUID,
    val bloomId: UUID,
    val lifecycle: PlanktonBloomLifecycle,
    val remainingHarvests: Int,
    val activatedAtGameTime: Long?
) : CustomPacketPayload {
    companion object {
        val ID = CustomPacketPayload.Type<S2CPlanktonBloomStateUpdatePayload>(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "plankton_bloom_state_update")
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, S2CPlanktonBloomStateUpdatePayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeUUID(payload.waveEventId)
                buffer.writeUUID(payload.bloomId)
                buffer.writeEnum(payload.lifecycle)
                buffer.writeVarInt(payload.remainingHarvests)
                buffer.writeOptional(Optional.ofNullable(payload.activatedAtGameTime)) { buf, value ->
                    buf.writeLong(value)
                }
            },
            { buffer ->
                S2CPlanktonBloomStateUpdatePayload(
                    waveEventId = buffer.readUUID(),
                    bloomId = buffer.readUUID(),
                    lifecycle = buffer.readEnum(PlanktonBloomLifecycle::class.java),
                    remainingHarvests = buffer.readVarInt(),
                    activatedAtGameTime = buffer.readOptional { buf -> buf.readLong() }.orElse(null)
                )
            }
        )

        fun handle(payload: S2CPlanktonBloomStateUpdatePayload, context: IPayloadContext) {
            context.enqueueWork {
                BioluminescenceClientPayloadHandler.handleBloomStateUpdate(payload)
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<S2CPlanktonBloomStateUpdatePayload> = ID
}
