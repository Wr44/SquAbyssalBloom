package fr.heta__h.squ_abyssal_bloom.mixin.render

import fr.heta__h.squ_abyssal_bloom.mixin.`interface`.AddPropertiesToRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique

@Mixin(LivingEntityRenderState::class)
abstract class LivingEntityRenderStateMixin : AddPropertiesToRenderState {

    @Unique
    private var hasGuardianSpikes: Boolean = false

    override fun getHasGuardianSpikes(): Boolean = hasGuardianSpikes

    override fun setHasGuardianSpikes(value: Boolean) {
        hasGuardianSpikes = value
    }
}