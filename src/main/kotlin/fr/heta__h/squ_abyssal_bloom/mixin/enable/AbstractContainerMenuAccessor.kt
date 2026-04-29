package fr.heta__h.squ_abyssal_bloom.mixin.enable

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(AbstractContainerMenu::class)
interface AbstractContainerMenuAccessor {
    @Invoker("addSlot")
    fun invokeAddSlot(slot: Slot) : Slot

    @Invoker("moveItemStackTo")
    fun invokeMoveItemStackTo(stack: ItemStack, startIndex: Int, endIndex: Int, reverseDirection: Boolean): Boolean
}