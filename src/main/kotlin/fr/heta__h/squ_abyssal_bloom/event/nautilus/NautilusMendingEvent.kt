package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.neoforged.fml.common.EventBusSubscriber
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent
import kotlin.math.ceil
import kotlin.math.min

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object NautilusMendingEvent {

    @SubscribeEvent
    fun onXpPickup(event: PlayerXpEvent.PickupXp) {
        val player = event.entity
        val orb = event.orb
        val vehicle = player.vehicle

        if (vehicle !is AbstractNautilus) return

        val nautilusItem = vehicle.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)

        if (nautilusItem == ItemStack.EMPTY || !nautilusItem.isDamaged) return

        val mendingLevel = ModUtilities.getEnchantLevel(
            nautilusItem,
            player.level(),
            "mending",
            "minecraft"
        )

        if (mendingLevel > 0) {
            val orbXp = orb.value
            val repairAmount = min(orbXp * 2, nautilusItem.damageValue)

            val consumedXp = ceil(repairAmount / 2.0).toInt()
            val remainingXp = orbXp - consumedXp

            if (consumedXp > 0) {
                nautilusItem.damageValue -= repairAmount

                event.isCanceled = true
                orb.discard()

                if (remainingXp > 0 && !player.level().isClientSide) {
                    val newOrb = ExperienceOrb(player.level(), player.x, player.y, player.z, remainingXp)
                    player.level().addFreshEntity(newOrb)
                }
            }
        }
    }
}