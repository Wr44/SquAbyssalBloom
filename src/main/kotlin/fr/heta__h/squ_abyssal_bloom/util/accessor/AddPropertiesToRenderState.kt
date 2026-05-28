package fr.heta__h.squ_abyssal_bloom.util.accessor

import net.minecraft.world.item.ItemStack

interface AddPropertiesToRenderState {
    fun getHasGuardianSpikes(): Boolean
    fun setHasGuardianSpikes(value: Boolean)

    fun getEyeHeight(): Float
    fun setEyeHeight(value: Float)

    fun getNautilusExtraItem(): ItemStack
    fun setNautilusExtraItem(stack: ItemStack)
}