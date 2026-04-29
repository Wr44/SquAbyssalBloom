package fr.heta__h.squ_abyssal_bloom.mixin.render

import fr.heta__h.squ_abyssal_bloom.accesor.AddPropertiesToRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique

@Mixin(LivingEntityRenderState::class)
abstract class LivingEntityRenderStateMixin : AddPropertiesToRenderState {

    @Unique
    private var hasGuardianSpikes: Boolean = false

    @Unique
    private var nautilusExtraItem : ItemStack = ItemStack.EMPTY

    override fun getHasGuardianSpikes(): Boolean = hasGuardianSpikes

    override fun setHasGuardianSpikes(value: Boolean) {
        hasGuardianSpikes = value
    }

    override fun getNautilusExtraItem(): ItemStack = nautilusExtraItem

    override fun setNautilusExtraItem(stack: ItemStack) {
        nautilusExtraItem = stack
    }
}