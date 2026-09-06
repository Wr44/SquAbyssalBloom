package fr.heta__h.squ_abyssal_bloom.item.plankton_bottle

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.network.chat.Component
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
import java.util.function.Consumer

class PartialPlanktonBottleItem(properties: Properties) : Item(properties) {

    companion object {
        fun applyFill(stack: ItemStack, fill: Int, required: Int) {
            stack.set(ModDataComponents.PLANKTON_FILL.get(), fill)
            stack.set(
                DataComponents.CUSTOM_MODEL_DATA,
                CustomModelData(
                    listOf(fill.toFloat() / required.coerceAtLeast(1)),
                    emptyList(),
                    emptyList(),
                    emptyList()
                )
            )
        }

        fun create(fill: Int, required: Int): ItemStack =
            ItemStack(ModItems.PARTIAL_PLANKTON_BOTTLE.get()).also { applyFill(it, fill, required) }

        private const val POUR_VOLUME = 0.7f
        private const val POUR_BASE_PITCH = 1.0f
        private const val POUR_PITCH_PER_FILL = 0.15f
        private const val POUR_MAX_PITCH = 2.0f
    }

    private fun fillOf(stack: ItemStack): Int = stack.getOrDefault(ModDataComponents.PLANKTON_FILL.get(), 0)

    override fun overrideOtherStackedOnMe(
        slotStack: ItemStack,
        carried: ItemStack,
        slot: Slot,
        action: ClickAction,
        player: Player,
        access: SlotAccess
    ): Boolean {
        if (action != ClickAction.SECONDARY) return false
        if (!carried.`is`(ModItems.PARTIAL_PLANKTON_BOTTLE.get())) return false

        val required = ModServerConfig.CRYSTAL_JELLY_BOTTLE_FILLS_REQUIRED.get()
        val target = fillOf(slotStack)
        val source = fillOf(carried)
        val poured = minOf(required - target, source)
        if (poured <= 0) return false

        val merged = target + poured
        if (merged >= required) {
            slot.set(ItemStack(ModItems.PLANKTON_BOTTLE.get()))
        } else {
            applyFill(slotStack, merged, required)
            slot.setChanged()
        }

        val left = source - poured
        if (left <= 0) {
            access.set(ItemStack(Items.GLASS_BOTTLE))
        } else {
            applyFill(carried, left, required)
        }

        player.level().playSound(
            player, player.blockPosition(),
            ModSounds.BOTTLE_FILL_BIOLUMINESCENT.get(), SoundSource.PLAYERS,
            POUR_VOLUME,
            (POUR_BASE_PITCH + POUR_PITCH_PER_FILL * merged).coerceAtMost(POUR_MAX_PITCH)
        )
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

        val required = ModServerConfig.CRYSTAL_JELLY_BOTTLE_FILLS_REQUIRED.get()
        tooltip.accept(
            Component.translatable("item.squ_abyssal_bloom.partial_plankton_bottle.fill", fillOf(stack), required)
                .withStyle(ChatFormatting.AQUA)
        )
    }
}
