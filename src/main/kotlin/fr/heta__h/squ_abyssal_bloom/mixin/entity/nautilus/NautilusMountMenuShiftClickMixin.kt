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

        val playerStart = 2
        val playerMainEnd = playerStart + 27
        val playerHotbarStart = playerMainEnd
        val playerEnd = extraSlotIndex
        val chestStart = extraSlotIndex + 1
        val chestEnd = menu.slots.size
        val hasChest = chestStart < chestEnd

        val isSaddleOrArmor = index == 0 || index == 1
        val isExtraSlot = index == extraSlotIndex
        val isChestSlot = hasChest && index in chestStart until chestEnd

        when {
            isSaddleOrArmor || isExtraSlot || isChestSlot -> {
                if (!slot.mayPickup(player)) {
                    cir.returnValue = ItemStack.EMPTY
                    cir.cancel()
                    return
                }

                val moved = accessor.invokeMoveItemStackTo(stackInSlot, playerStart, playerEnd, true)
                if (moved) {
                    if (stackInSlot.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
                    slot.onTake(player, copyStack)
                    cir.returnValue = copyStack
                } else {
                    cir.returnValue = ItemStack.EMPTY
                }
                cir.cancel()
            }

            index in playerStart until playerEnd -> {
                var moved = false

                val armorSlot = menu.slots[1]
                if (armorSlot.mayPlace(stackInSlot) && !armorSlot.hasItem()) {
                    moved = accessor.invokeMoveItemStackTo(stackInSlot, 1, 2, false)
                }

                if (!stackInSlot.isEmpty) {
                    val saddleSlot = menu.slots[0]
                    if (saddleSlot.mayPlace(stackInSlot) && !saddleSlot.hasItem()) {
                        moved = accessor.invokeMoveItemStackTo(stackInSlot, 0, 1, false) || moved
                    }
                }

                if (!stackInSlot.isEmpty) {
                    val extraSlot = menu.slots[extraSlotIndex]
                    if (extraSlot.mayPlace(stackInSlot) && !extraSlot.hasItem()) {
                        moved = accessor.invokeMoveItemStackTo(
                            stackInSlot,
                            extraSlotIndex,
                            extraSlotIndex + 1,
                            false,
                        ) || moved
                    }
                }

                if (!stackInSlot.isEmpty && hasChest) {
                    moved = accessor.invokeMoveItemStackTo(stackInSlot, chestStart, chestEnd, false) || moved
                }

                if (!stackInSlot.isEmpty) {
                    val movedWithinPlayerInventory = if (index in playerStart until playerMainEnd) {
                        accessor.invokeMoveItemStackTo(stackInSlot, playerHotbarStart, playerEnd, false)
                    } else {
                        accessor.invokeMoveItemStackTo(stackInSlot, playerStart, playerMainEnd, false)
                    }
                    moved = movedWithinPlayerInventory || moved
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
