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

        val isSaddleOrArmor = index == 0 || index == 1
        val isExtraSlot = index == extraSlotIndex
        val isChestSlot = hasChest && index in CHEST_START until CHEST_END

        when {
            isSaddleOrArmor || isExtraSlot || isChestSlot -> {
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
                var moved = false

                val saddleSlot = menu.slots[0]
                if (saddleSlot.mayPlace(stackInSlot) && !saddleSlot.hasItem()) {
                    moved = accessor.invokeMoveItemStackTo(stackInSlot, 0, 1, false)
                }

                if (!stackInSlot.isEmpty) {
                    val armorSlot = menu.slots[1]
                    if (armorSlot.mayPlace(stackInSlot) && !armorSlot.hasItem()) {
                        val movedArmor = accessor.invokeMoveItemStackTo(stackInSlot, 1, 2, false)
                        moved = moved || movedArmor
                    }
                }

                if (!stackInSlot.isEmpty) {
                    val extraSlot = menu.slots[extraSlotIndex]
                    if (extraSlot.mayPlace(stackInSlot) && !extraSlot.hasItem()) {
                        val movedExtra = accessor.invokeMoveItemStackTo(stackInSlot, extraSlotIndex, extraSlotIndex + 1, false)
                        moved = moved || movedExtra
                    }
                }

                if (!stackInSlot.isEmpty && hasChest) {
                    val movedChest = accessor.invokeMoveItemStackTo(stackInSlot, CHEST_START, CHEST_END, false)
                    moved = moved || movedChest
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