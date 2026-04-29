package fr.heta__h.squ_abyssal_bloom.mixin.enable

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(AbstractContainerScreen::class)
interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    fun getLeftPos(): Int

    @Accessor("topPos")
    fun getTopPos(): Int
}