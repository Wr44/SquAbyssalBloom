package fr.heta__h.squ_abyssal_bloom.network

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist.FocalistBeamSyncPayload
import fr.heta__h.squ_abyssal_bloom.network.bubble.C2SBubbleChargeStartPacket
import fr.heta__h.squ_abyssal_bloom.network.bioluminescence.S2CBioluminescenceArmedPayload
import fr.heta__h.squ_abyssal_bloom.network.bioluminescence.S2CBioluminescenceWaveEndPayload
import fr.heta__h.squ_abyssal_bloom.network.bioluminescence.S2CBioluminescenceWavePayload
import fr.heta__h.squ_abyssal_bloom.network.config.C2SServerConfigPacket
import fr.heta__h.squ_abyssal_bloom.network.config.S2CServerConfigPacket
import fr.heta__h.squ_abyssal_bloom.network.nautilus_chest.SyncNautilusExtraSlotPayload
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object ModNetworking {

    @SubscribeEvent
    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar(SquAbyssalBloom.ID).versioned("2.0")

        // Register clientbound payloads
        registrar.playToClient(
            FocalistBeamSyncPayload.ID,
            FocalistBeamSyncPayload.CODEC,
            FocalistBeamSyncPayload::handle
        )

        registrar.playToClient(
            SyncNautilusExtraSlotPayload.ID,
            SyncNautilusExtraSlotPayload.CODEC,
            SyncNautilusExtraSlotPayload::handle
        )

        registrar.playToClient(
            S2CServerConfigPacket.ID,
            S2CServerConfigPacket.STREAM_CODEC,
            S2CServerConfigPacket::handle
        )

        registrar.playToClient(
            S2CBioluminescenceWavePayload.ID,
            S2CBioluminescenceWavePayload.STREAM_CODEC,
            S2CBioluminescenceWavePayload::handle
        )

        registrar.playToClient(
            S2CBioluminescenceWaveEndPayload.ID,
            S2CBioluminescenceWaveEndPayload.STREAM_CODEC,
            S2CBioluminescenceWaveEndPayload::handle
        )

        registrar.playToClient(
            S2CBioluminescenceArmedPayload.ID,
            S2CBioluminescenceArmedPayload.STREAM_CODEC,
            S2CBioluminescenceArmedPayload::handle
        )


        // Register serverbound payloads

        registrar.playToServer(
            C2SBubbleChargeStartPacket.ID,
            C2SBubbleChargeStartPacket.STREAM_CODEC,
            C2SBubbleChargeStartPacket::handle
        )

        registrar.playToServer(
            C2SServerConfigPacket.ID,
            C2SServerConfigPacket.STREAM_CODEC,
            C2SServerConfigPacket::handle
        )
    }
}
