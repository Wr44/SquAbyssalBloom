package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.util.ModAttachments
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object NautilusDropInventory {

    @SubscribeEvent
    fun onNautilusDeath(event: LivingDeathEvent) {
        val entity = event.entity
        val level = entity.level()
        if (entity is AbstractNautilus && level is ServerLevel) {
            val savedItem = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
            if (!savedItem.isEmpty) {
                entity.spawnAtLocation(level, savedItem)
            }
        }
    }
}