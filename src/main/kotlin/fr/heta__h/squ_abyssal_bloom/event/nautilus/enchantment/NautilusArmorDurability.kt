package fr.heta__h.squ_abyssal_bloom.event.nautilus.enchantment

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object NautilusArmorDurability {

    private val lastDamageTick = ConcurrentHashMap<UUID, Long>()

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        if (event.server.tickCount % 6000 == 0) {
            val now = event.server.overworld().gameTime
            lastDamageTick.entries.removeIf { (_, tick) -> now - tick > 1200L }
        }
    }

    @SubscribeEvent
    fun onLivingDamage(event: LivingDamageEvent.Post) {
        val nautilus = event.entity as? AbstractNautilus ?: return
        if (nautilus.level().isClientSide) return

        val damage = event.healthDamage
        if (damage <= 0) return

        val bodyStack = nautilus.getItemBySlot(EquipmentSlot.BODY)
        if (bodyStack.isEmpty || !bodyStack.isDamageableItem) return

        val now = nautilus.level().gameTime
        val last = lastDamageTick.getOrDefault(nautilus.uuid, -5L)
        if (now - last < 5L) return

        lastDamageTick[nautilus.uuid] = now

        val durabilityDamage = maxOf(1, (damage / 4).toInt())
        bodyStack.hurtAndBreak(durabilityDamage, nautilus, EquipmentSlot.BODY)
    }
}