package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusMouseHelper
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusReopenQueue
import net.minecraft.client.gui.screens.inventory.NautilusInventoryScreen
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ScreenEvent
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object NautilusDropInventory {

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onNautilusDrops(event: LivingDropsEvent) {
        val entity = event.entity
        val level = entity.level()

        if (entity is AbstractNautilus && level is ServerLevel) {
            val savedItem = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)

            if (!savedItem.isEmpty) {
                val itemEntity = ItemEntity(level, entity.x, entity.y, entity.z, savedItem.copy())
                itemEntity.setDefaultPickUpDelay()
                event.drops.add(itemEntity)

                if (savedItem.`is`(Items.CHEST)) {
                    val chestItems = entity.getData(ModAttachments.NAUTILUS_CHEST_ITEMS)
                    chestItems.forEach { stack ->
                        if (!stack.isEmpty) {
                            val chestItemEntity = ItemEntity(level, entity.x, entity.y, entity.z, stack.copy())
                            chestItemEntity.setDefaultPickUpDelay()
                            event.drops.add(chestItemEntity)
                        }
                    }
                    entity.setData(ModAttachments.NAUTILUS_CHEST_ITEMS, emptyList())
                }

                entity.setData(ModAttachments.NAUTILUS_EXTRA_SLOT, ItemStack.EMPTY)
            }
        }
    }


    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        NautilusReopenQueue.flush()
    }


}