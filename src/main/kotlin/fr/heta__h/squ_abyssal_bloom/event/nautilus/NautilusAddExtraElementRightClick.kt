package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object NautilusAddExtraElementRightClick {

    @SubscribeEvent
    fun onNautilusInteract(event: PlayerInteractEvent.EntityInteract) {
        val target = event.target
        val player = event.entity
        val hand = event.hand
        val itemInHand = player.getItemInHand(hand)

        if (target !is AbstractNautilus) return

        if (!ModUtilities.isNautilusExtraEquipment(itemInHand)) return

        if (target.getItemBySlot(EquipmentSlot.SADDLE).isEmpty) return
        val currentEquipped = target.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)

        if (currentEquipped.isEmpty) {

            val level = target.level()

            if (!level.isClientSide) {

                val itemToEquip = itemInHand.copyWithCount(1)

                target.setData(ModAttachments.NAUTILUS_EXTRA_SLOT, itemToEquip)

                ModUtilities.playSoundLocal(
                    player,
                    SoundEvents.ARMOR_EQUIP_NAUTILUS.value(),
                    SoundSource.PLAYERS,
                    1.0f,
                    1.5f
                )

                if (!player.abilities.instabuild) {
                    itemInHand.shrink(1)
                }
            }

            event.isCanceled = true
            event.cancellationResult = InteractionResult.SUCCESS

        }
    }
}