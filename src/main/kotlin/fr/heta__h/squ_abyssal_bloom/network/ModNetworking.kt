package fr.heta__h.squ_abyssal_bloom.network

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist.FocalistBeamSyncPayload
import fr.heta__h.squ_abyssal_bloom.network.bubble.C2SBubbleChargeStartPacket
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.network.nautilus_chest.SyncNautilusExtraSlotPayload
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import kotlin.math.cos
import kotlin.math.sin

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object ModNetworking {

    @SubscribeEvent
    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar(Squ_abyssal_bloom.ID).versioned("1.0")

        
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

        
        registrar.playToServer(
            C2SBubbleChargeStartPacket.ID,
            C2SBubbleChargeStartPacket.STREAM_CODEC,
            C2SBubbleChargeStartPacket::handle
        )
    }
}