package fr.heta__h.squ_abyssal_bloom.mixin.render

import fr.heta__h.squ_abyssal_bloom.mixin.render.`interface`.IGuardianSpikeState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique

@Mixin(LivingEntityRenderState::class)
abstract class LivingEntityRenderStateMixin : IGuardianSpikeState {

    @Unique
    private var hasGuardianSpikes: Boolean = false

    override fun getHasGuardianSpikes(): Boolean {
        return this.hasGuardianSpikes
    }

    override fun setHasGuardianSpikes(value: Boolean) {
        this.hasGuardianSpikes = value
    }
}