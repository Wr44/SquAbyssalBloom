package fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext

data class FocalistBeamSyncPayload(val shooterId: Int, val targetId: Int, val isShooting: Boolean) : CustomPacketPayload {

    companion object {
        val ID = CustomPacketPayload.Type<FocalistBeamSyncPayload>(Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "focalist_beam_sync"))

        val CODEC: StreamCodec<ByteBuf, FocalistBeamSyncPayload> = StreamCodec.composite(
            ByteBufCodecs.INT, FocalistBeamSyncPayload::shooterId,
            ByteBufCodecs.INT, FocalistBeamSyncPayload::targetId,
            ByteBufCodecs.BOOL, FocalistBeamSyncPayload::isShooting,
            ::FocalistBeamSyncPayload
        )

        fun handle(payload: FocalistBeamSyncPayload, context: IPayloadContext) {
            context.enqueueWork {
                ClientBeamHandler.handleBeamSync(payload)
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}