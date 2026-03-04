package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.util.ModAttachments
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent 

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object NautilusDropInventory {

    @SubscribeEvent
    fun onNautilusDrops(event: LivingDropsEvent) {
        val entity = event.entity
        val level = entity.level()

        if (entity is AbstractNautilus && level is ServerLevel) {
            val savedItem = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)

            if (!savedItem.isEmpty) {
                val itemEntity = ItemEntity(level, entity.x, entity.y, entity.z, savedItem)

                itemEntity.setDefaultPickUpDelay()

                event.drops.add(itemEntity)

                entity.setData(ModAttachments.NAUTILUS_EXTRA_SLOT, ItemStack.EMPTY)
            }
        }
    }
}