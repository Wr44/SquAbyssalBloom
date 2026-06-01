package fr.heta__h.squ_abyssal_bloom.item.bubble_spitter

import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.data_component.bubble.SplatterData
import fr.heta__h.squ_abyssal_bloom.data_component.bubble.SplatterEntry
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ClickAction
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import java.lang.Math.random
import java.util.function.Consumer

class BubbleSpitterItem(properties: Properties) : Item(properties) {

    override fun overrideOtherStackedOnMe(
        slotStack: ItemStack,
        carried: ItemStack,
        slot: Slot,
        action: ClickAction,
        player: Player,
        access: SlotAccess
    ): Boolean {
        if (action != ClickAction.SECONDARY) return false
        if (!isPotion(carried)) return false
        if (!hasSplatter(slotStack, player)) return false

        val potionContents = carried.get(DataComponents.POTION_CONTENTS) ?: return false
        val newEffects = potionContents.allEffects.toList()

        if (newEffects.isEmpty()) {
            slotStack.remove(ModDataComponents.SPLATTER_DATA.get())
            replaceWithBottle(carried, access, player)
            player.level().playSound(player, player.blockPosition(), ModSounds.CLEAN_BUBBLE_SPITTER.get(), SoundSource.PLAYERS, 0.5f, 0.8f + 0.4f*random().toFloat())
            return true
        }

        val existing = slotStack.get(ModDataComponents.SPLATTER_DATA.get())
        val (merged, changed) = mergeEntries(existing?.entries ?: emptyList(), newEffects.map { instance ->
            SplatterEntry(instance.effect, instance.duration, instance.amplifier)
        })

        if (!changed) return false

        val allColors = merged.map { it.effect.value().color }
        val avgColor = SplatterData.averageColor(allColors)

        slotStack.set(ModDataComponents.SPLATTER_DATA.get(), SplatterData(merged, avgColor))
        replaceWithBottle(carried, access, player)
        player.level().playSound(player, player.blockPosition(), ModSounds.POTION_BUBBLE_SPITTER.get(), SoundSource.PLAYERS, 0.7f, 0.8f + 0.4f*random().toFloat())
        return true
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        tooltip: Consumer<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, tooltip, flag)
        val data = stack.get(ModDataComponents.SPLATTER_DATA.get()) ?: return

        tooltip.accept(
            Component.translatable("item.squ_abyssal_bloom.bubble_spitter.splatter_effects")
                .withStyle(ChatFormatting.GRAY)
        )

        for (entry in data.entries) {
            val effect = entry.effect.value()
            val seconds = entry.duration / 20
            val level = if (entry.amplifier > 0) " ${entry.amplifier + 1}" else ""
            tooltip.accept(
                Component.literal("  ")
                    .append(Component.translatable(effect.descriptionId))
                    .append(level)
                    .append(" (${seconds}s)")
                    .withStyle { it.withColor(effect.color) }
            )
        }
    }

    private fun mergeEntries(existing: List<SplatterEntry>, incoming: List<SplatterEntry>): Pair<List<SplatterEntry>, Boolean> {
        val map = LinkedHashMap<String, SplatterEntry>()
        var changed = false
        for (entry in existing) {
            val key = entry.effect.unwrapKey().map { it.identifier().toString() }.orElse("")
            map[key] = entry
        }
        for (entry in incoming) {
            val key = entry.effect.unwrapKey().map { it.identifier().toString() }.orElse("")
            val prev = map[key]
            if (prev == null) {
                map[key] = entry
                changed = true
            } else if (entry.amplifier > prev.amplifier) {
                map[key] = entry
                changed = true
            } else if (entry.amplifier == prev.amplifier && entry.duration > prev.duration) {
                map[key] = entry
                changed = true
            }
        }
        return map.values.toList() to changed
    }

    private fun replaceWithBottle(carried: ItemStack, access: SlotAccess, player: Player) {
        if (carried.count == 1) {
            access.set(ItemStack(Items.GLASS_BOTTLE))
        } else {
            carried.shrink(1)
            val bottle = ItemStack(Items.GLASS_BOTTLE)
            if (!player.inventory.add(bottle)) {
                player.drop(bottle, false)
            }
        }
    }

    private fun isPotion(stack: ItemStack): Boolean {
        val item = stack.item
        return item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION
    }

    private fun hasSplatter(stack: ItemStack, player: Player): Boolean {
        return ModUtilities.getEnchantLevel(
            stack,
            player.level(),
            "splatter"
        ) != 0
    }


}