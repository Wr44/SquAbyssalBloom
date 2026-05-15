package fr.heta__h.squ_abyssal_bloom.mixin.enable

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.material.FluidState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(LivingEntity::class)
interface LivingEntityFluidAccessor {
    @Invoker("shouldTravelInFluid")
    fun callShouldTravelInFluid(fluidState: FluidState): Boolean
}