package fr.heta__h.squ_abyssal_bloom.event.nautilus.bubble

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object NautilusBubbleSlowHandler {

    private val MODIFIER_ID = Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "bubble_slowdown")
    private val nautilusWithHeldBubble = ConcurrentHashMap.newKeySet<UUID>()

    fun onBubbleHeld(nautilusUUID: UUID) {
        nautilusWithHeldBubble.add(nautilusUUID)
    }

    fun onBubbleReleased(nautilusUUID: UUID) {
        nautilusWithHeldBubble.remove(nautilusUUID)
    }

    @SubscribeEvent
    fun onEntityTickPost(event: EntityTickEvent.Post) {
        val nautilus = event.entity as? AbstractNautilus ?: return

        val speedAttribute = nautilus.getAttribute(Attributes.MOVEMENT_SPEED) ?: return

        if (nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT.get()).item != NautilusLayerItems.BUBBLE) {
            if (speedAttribute.getModifier(MODIFIER_ID) != null) speedAttribute.removeModifier(MODIFIER_ID)
            return
        }

        if (nautilusWithHeldBubble.contains(nautilus.uuid)) {
            if (speedAttribute.getModifier(MODIFIER_ID) == null) {
                speedAttribute.addTransientModifier(
                    AttributeModifier(MODIFIER_ID, -0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                )
            }
        } else {
            if (speedAttribute.getModifier(MODIFIER_ID) != null) speedAttribute.removeModifier(MODIFIER_ID)
        }
    }
}