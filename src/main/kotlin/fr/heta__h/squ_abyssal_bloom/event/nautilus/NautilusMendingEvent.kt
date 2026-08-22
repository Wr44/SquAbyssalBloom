package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.mixin.enable.ExperienceOrbAccessor
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent
import kotlin.math.min

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object NautilusMendingEvent {

    private data class MendingTarget(
        val stack: ItemStack,
        val writeBack: () -> Unit = {},
    )

    @SubscribeEvent
    fun onXpPickup(event: PlayerXpEvent.PickupXp) {
        if (event.isCanceled) return

        val player = event.entity as? ServerPlayer ?: return
        val nautilus = player.vehicle as? AbstractNautilus ?: return
        if (collectNautilusTargets(nautilus).isEmpty()) return

        val orb = event.orb
        val orbAccessor = orb as ExperienceOrbAccessor
        val orbCount = orbAccessor.getCount()
        if (orbCount <= 0) return

        event.isCanceled = true
        player.takeXpDelay = 2
        player.take(orb, 1)

        val remainingXp = repairItems(player, nautilus, orb.value)
        if (remainingXp > 0) player.giveExperiencePoints(remainingXp)

        if (orbCount == 1) {
            orb.discard()
        } else {
            orbAccessor.setCount(orbCount - 1)
        }
    }

    private fun repairItems(player: ServerPlayer, nautilus: AbstractNautilus, initialXp: Int): Int {
        var remainingXp = initialXp

        while (remainingXp > 0) {
            val candidates = collectPlayerTargets(player) + collectNautilusTargets(nautilus)
            if (candidates.isEmpty()) break

            val target = candidates[player.random.nextInt(candidates.size)]
            val repairCapacity = EnchantmentHelper.modifyDurabilityToRepairFromXp(
                player.level(),
                target.stack,
                (remainingXp * target.stack.xpRepairRatio).toInt(),
            )
            if (repairCapacity <= 0) break

            val repairedDurability = min(repairCapacity, target.stack.damageValue)
            if (repairedDurability <= 0) break

            target.stack.damageValue -= repairedDurability
            target.writeBack()
            remainingXp -= repairedDurability * remainingXp / repairCapacity
        }

        return remainingXp
    }

    private fun collectPlayerTargets(player: ServerPlayer): List<MendingTarget> {
        val candidates = mutableListOf<MendingTarget>()

        for (slot in EquipmentSlot.VALUES) {
            val stack = player.getItemBySlot(slot)
            if (!stack.isDamaged) continue

            val enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
            for (entry in enchantments.entrySet()) {
                val enchantment = entry.key.value()
                if (enchantment.effects().has(EnchantmentEffectComponents.REPAIR_WITH_XP) &&
                    enchantment.matchingSlot(slot)
                ) {
                    candidates.add(MendingTarget(stack))
                }
            }
        }

        return candidates
    }

    private fun collectNautilusTargets(nautilus: AbstractNautilus): List<MendingTarget> {
        val candidates = mutableListOf<MendingTarget>()

        val extraSlot = nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
        if (extraSlot.isDamaged && EnchantmentHelper.has(extraSlot, EnchantmentEffectComponents.REPAIR_WITH_XP)) {
            candidates.add(MendingTarget(extraSlot) {
                nautilus.setData(ModAttachments.NAUTILUS_EXTRA_SLOT, extraSlot)
            })
        }

        val bodyArmor = nautilus.getItemBySlot(EquipmentSlot.BODY)
        if (bodyArmor.isDamaged && EnchantmentHelper.has(bodyArmor, EnchantmentEffectComponents.REPAIR_WITH_XP)) {
            candidates.add(MendingTarget(bodyArmor) {
                nautilus.setItemSlot(EquipmentSlot.BODY, bodyArmor)
            })
        }

        return candidates
    }
}
