package fr.heta__h.squ_abyssal_bloom.mixin.block.conduit

import fr.heta__h.squ_abyssal_bloom.mixin.enable.LivingEntityFluidAccessor
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.material.FluidState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

@Mixin(LivingEntity::class)
abstract class PlayerConduitMixin {

    @Unique
    private fun isPlayerInConduitDomain(): Boolean {
        if (this !is Player) return false
        return (this as Player).hasEffect(MobEffects.CONDUIT_POWER)
    }

    @Redirect(
        method = ["travel"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;shouldTravelInFluid(Lnet/minecraft/world/level/material/FluidState;)Z"
        )
    )
    private fun redirectFluidPhysicsForDomain(instance: LivingEntity, fluidState: FluidState): Boolean {
        if (!isPlayerInConduitDomain()) return (instance as LivingEntityFluidAccessor).callShouldTravelInFluid(fluidState)
        return false
    }

    @Redirect(
        method = ["aiStep"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getFluidJumpThreshold()D"
        )
    )
    private fun redirectFluidJumpThreshold(instance: LivingEntity): Double {
        if (!isPlayerInConduitDomain()) return instance.fluidJumpThreshold
        return Double.MAX_VALUE
    }

    @Redirect(
        method = ["aiStep"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z"
        )
    )
    private fun redirectWaterFlagForJump(instance: LivingEntity): Boolean {
        if (!isPlayerInConduitDomain()) return instance.isInWater
        return false
    }
}