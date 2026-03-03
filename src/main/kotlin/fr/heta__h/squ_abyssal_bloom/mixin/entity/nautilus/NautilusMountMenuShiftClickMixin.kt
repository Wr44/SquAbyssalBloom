package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import fr.heta__h.squ_abyssal_bloom.mixin.`interface`.AbstractContainerMenuAccessor
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

        if (menu is NautilusInventoryMenu) {
            val slot = menu.slots[index]

            if (slot != null && slot.hasItem()) {
                val stackInSlot = slot.item
                val copyStack = stackInSlot.copy()

                val EXTRA_SLOT = 38
                val PLAYER_START = 2
                val PLAYER_END = 38

                if (index == EXTRA_SLOT) {
                    if (!(menu as AbstractContainerMenuAccessor).invokeMoveItemStackTo(stackInSlot, PLAYER_START, PLAYER_END, true)) {
                        cir.returnValue = ItemStack.EMPTY
                    } else {
                        if (stackInSlot.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
                        cir.returnValue = copyStack
                    }
                    return
                }

                if (index in PLAYER_START until PLAYER_END) {
                    if ((menu as AbstractContainerMenuAccessor).invokeMoveItemStackTo(stackInSlot, EXTRA_SLOT, EXTRA_SLOT + 1, false)) {
                        if (stackInSlot.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
                        cir.returnValue = copyStack
                        return
                    }
                }
            }
        }
    }
}