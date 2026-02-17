package fr.heta__h.squ_abyssal_bloom.event

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.ElderGuardian
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object BarnacleCatchDrop {

    @SubscribeEvent
    fun onTargetDrops(event: LivingDropsEvent) {
        val source = event.source.entity
        val target = event.entity

        if (source is BarnacleEntity) {
            val drops = event.drops.map { it.item.copy() }
            for (stack in drops) {
                source.addToBeExpelled(stack)
            }

            EquipmentSlot.entries.forEach { slot ->
                val stack = target.getItemBySlot(slot)
                if (!stack.isEmpty) {
                    source.addToBeExpelled(stack.copy())
                    target.setItemSlot(slot, ItemStack.EMPTY)
                }
            }

            if (target is Guardian && target !is ElderGuardian) {
                val eye = ItemStack(ModItems.GUARDIAN_EYE.get())
                source.addToBeExpelled(eye)
            }

            event.isCanceled = true
        }
    }
}