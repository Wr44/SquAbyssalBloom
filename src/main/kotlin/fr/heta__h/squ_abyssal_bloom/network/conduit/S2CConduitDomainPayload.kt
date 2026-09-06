package fr.heta__h.squ_abyssal_bloom.network.conduit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext

data class S2CConduitDomainPayload(
    val kind: ConduitDomainKind,
    val blockPos: BlockPos,
    val entityId: Int,
    val radius: Double
) : CustomPacketPayload {

    override fun type() = ID

    companion object {
        val NONE = S2CConduitDomainPayload(ConduitDomainKind.NONE, BlockPos.ZERO, -1, 0.0)

        fun ofBlock(pos: BlockPos, radius: Double) =
            S2CConduitDomainPayload(ConduitDomainKind.BLOCK, pos, -1, radius)

        fun ofPortable(entityId: Int, radius: Double) =
            S2CConduitDomainPayload(ConduitDomainKind.PORTABLE, BlockPos.ZERO, entityId, radius)

        val ID = CustomPacketPayload.Type<S2CConduitDomainPayload>(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "conduit_domain")
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, S2CConduitDomainPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeEnum(payload.kind)
                when (payload.kind) {
                    ConduitDomainKind.NONE -> Unit
                    ConduitDomainKind.BLOCK -> {
                        buffer.writeBlockPos(payload.blockPos)
                        buffer.writeDouble(payload.radius)
                    }
                    ConduitDomainKind.PORTABLE -> {
                        buffer.writeVarInt(payload.entityId)
                        buffer.writeDouble(payload.radius)
                    }
                }
            },
            { buffer ->
                when (val kind = buffer.readEnum(ConduitDomainKind::class.java)) {
                    ConduitDomainKind.NONE -> NONE
                    ConduitDomainKind.BLOCK -> S2CConduitDomainPayload(
                        kind, buffer.readBlockPos(), -1, buffer.readDouble()
                    )
                    ConduitDomainKind.PORTABLE -> S2CConduitDomainPayload(
                        kind, BlockPos.ZERO, buffer.readVarInt(), buffer.readDouble()
                    )
                }
            }
        )

        fun handle(payload: S2CConduitDomainPayload, context: IPayloadContext) {
            context.enqueueWork {
                ConduitClientPayloadHandler.handleDomain(payload)
            }
        }
    }
}
