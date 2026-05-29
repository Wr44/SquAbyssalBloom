package fr.heta__h.squ_abyssal_bloom.event.nautilus.enchantment

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object NautilusArmorDurability {

    @SubscribeEvent
    fun onLivingDamage(event: LivingDamageEvent.Post) {
        val nautilus = event.entity as? AbstractNautilus ?: return
        if (nautilus.level().isClientSide) return

        val damage = event.newDamage
        if (damage <= 0) return

        val bodyStack = nautilus.getItemBySlot(EquipmentSlot.BODY)
        if (bodyStack.isEmpty || !bodyStack.isDamageableItem) return

        val durabilityDamage = maxOf(1, (damage / 4).toInt())
        bodyStack.hurtAndBreak(durabilityDamage, nautilus, EquipmentSlot.BODY)
    }
}