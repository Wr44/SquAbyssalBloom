package fr.heta__h.squ_abyssal_bloom.event.abyssal_guardian_focalist

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist.ClientBeamData
import fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist.FocalistBeamSyncPayload
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object GuardianBeamNetworkHandler {
    @SubscribeEvent
    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar(Squ_abyssal_bloom.ID).versioned("1.0")

        registrar.playToClient(
            FocalistBeamSyncPayload.ID,
            FocalistBeamSyncPayload.CODEC
        ) { payload, context ->
            context.enqueueWork {
                if (payload.isShooting) {
                    ClientBeamData.activeBeams[payload.shooterId] = payload.targetId
                } else {
                    ClientBeamData.activeBeams.remove(payload.shooterId)
                }
            }
        }
    }
}