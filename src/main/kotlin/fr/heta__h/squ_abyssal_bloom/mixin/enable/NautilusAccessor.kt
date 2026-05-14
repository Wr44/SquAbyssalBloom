package fr.heta__h.squ_abyssal_bloom.mixin.enable

import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(AbstractNautilus::class)
interface NautilusAccessor {
    @Accessor("inventory")
    fun getInventory(): SimpleContainer

    @Invoker("createInventory")
    fun invokeCreateInventory()
}