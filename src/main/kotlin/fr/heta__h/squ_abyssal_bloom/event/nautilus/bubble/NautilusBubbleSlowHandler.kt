package fr.heta__h.squ_abyssal_bloom.event.nautilus.bubble

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object NautilusBubbleSlowHandler {

    @SubscribeEvent
    fun onEntityTickPost(event: EntityTickEvent.Post) {
        val nautilus = event.entity as? AbstractNautilus ?: return

        if (nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT.get()).item != NautilusLayer.BUBBLE) return

        val hasHeldBubble = nautilus.level().getEntitiesOfClass(
            BubbleProjectile::class.java,
            nautilus.boundingBox.inflate(4.0)
        ) { it.isHeld && it.owner == nautilus }.isNotEmpty()

        val speedAttribute = nautilus.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
        val modifierId = Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "bubble_slowdown")

        if (hasHeldBubble) {
            if (speedAttribute.getModifier(modifierId) == null) {
                speedAttribute.addTransientModifier(
                    AttributeModifier(
                        modifierId,
                        -0.8,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
                )
            }
        } else {
            if (speedAttribute.getModifier(modifierId) != null) {
                speedAttribute.removeModifier(modifierId)
            }
        }
    }
}