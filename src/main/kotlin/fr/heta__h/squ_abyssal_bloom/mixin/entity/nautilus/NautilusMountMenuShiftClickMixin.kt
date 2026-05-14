package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import fr.heta__h.squ_abyssal_bloom.mixin.enable.AbstractContainerMenuAccessor
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusEquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractMountInventoryMenu
import net.minecraft.world.inventory.NautilusInventoryMenu
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(AbstractMountInventoryMenu::class)
abstract class NautilusMountMenuShiftClickMixin {

    @Inject(method = ["quickMoveStack"], at = [At("HEAD")], cancellable = true)
    fun handleNautilusShiftClick(player: Player, index: Int, cir: CallbackInfoReturnable<ItemStack>) {
        val menu = this as AbstractMountInventoryMenu
        if (menu !is NautilusInventoryMenu) return

        val slot = menu.slots.getOrNull(index) ?: return
        if (!slot.hasItem()) return

        val stackInSlot = slot.item
        val copyStack = stackInSlot.copy()
        val accessor = menu as AbstractContainerMenuAccessor

        val extraSlotIndex = menu.slots.indexOfFirst { it is NautilusEquipmentSlot }
        if (extraSlotIndex == -1) return

        val PLAYER_START = 2
        val PLAYER_END = extraSlotIndex
        val CHEST_START = extraSlotIndex + 1
        val CHEST_END = menu.slots.size
        val hasChest = CHEST_START < CHEST_END

        when {
            index == extraSlotIndex -> {
                val moved = accessor.invokeMoveItemStackTo(stackInSlot, PLAYER_START, PLAYER_END, true)
                if (moved) {
                    if (stackInSlot.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
                    cir.returnValue = copyStack
                } else {
                    cir.returnValue = ItemStack.EMPTY
                }
                cir.cancel()
            }

            hasChest && index in CHEST_START until CHEST_END -> {
                val moved = accessor.invokeMoveItemStackTo(stackInSlot, PLAYER_START, PLAYER_END, true)
                if (moved) {
                    if (stackInSlot.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
                    cir.returnValue = copyStack
                } else {
                    cir.returnValue = ItemStack.EMPTY
                }
                cir.cancel()
            }

            index in PLAYER_START until PLAYER_END -> {
                var moved = accessor.invokeMoveItemStackTo(stackInSlot, extraSlotIndex, extraSlotIndex + 1, false)
                if (!moved && hasChest) {
                    moved = accessor.invokeMoveItemStackTo(stackInSlot, CHEST_START, CHEST_END, false)
                }
                if (moved) {
                    if (stackInSlot.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
                    cir.returnValue = copyStack
                } else {
                    cir.returnValue = ItemStack.EMPTY
                }
                cir.cancel()
            }
        }
    }
}