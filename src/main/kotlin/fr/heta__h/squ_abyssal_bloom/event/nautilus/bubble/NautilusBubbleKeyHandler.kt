package fr.heta__h.squ_abyssal_bloom.event.nautilus.bubble

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.network.bubble.C2SBubbleChargeStartPacket
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.LevelTickEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object NautilusBubbleKeyHandler {

    private var chargePacketSent = false

    @SubscribeEvent
    fun onClientLevelTick(event: LevelTickEvent.Pre) {
        val mc = Minecraft.getInstance()
        if (mc.isPaused) return
        val player = mc.player ?: run { chargePacketSent = false; return }
        val nautilus = player.vehicle as? AbstractNautilus ?: run { chargePacketSent = false; return }

        if (nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT.get()).item != NautilusLayer.BUBBLE) {
            chargePacketSent = false
            return
        }

        val jumpKeyDown = mc.options.keyJump.isDown

        if (jumpKeyDown && !chargePacketSent) {
            val hasHeld = mc.level?.getEntitiesOfClass(
                BubbleProjectile::class.java, nautilus.boundingBox.inflate(3.0)
            ) { it.isHeld }?.isNotEmpty() ?: false

            if (!hasHeld && nautilus.getJumpCooldown() <= 0) {

                mc.connection?.send(ServerboundCustomPayloadPacket(C2SBubbleChargeStartPacket))
                chargePacketSent = true
            }
        } else if (!jumpKeyDown) {
            chargePacketSent = false
        }
    }

}