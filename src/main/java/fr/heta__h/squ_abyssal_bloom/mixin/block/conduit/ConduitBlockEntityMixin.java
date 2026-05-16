package fr.heta__h.squ_abyssal_bloom.mixin.block.conduit;

import fr.heta__h.squ_abyssal_bloom.util.conduit.ConduitHuntingTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConduitBlockEntity.class)
public abstract class ConduitBlockEntityMixin {

    @Redirect(
            method = "applyEffects",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z")
    )
    private static boolean suppressVanillaConduitPower(Player player, MobEffectInstance effect) {
        return false;
    }

    @Redirect(
            method = "updateAndAttackTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private static boolean redirectHuntingAttack(LivingEntity entity, ServerLevel level, DamageSource source, float amount) {
        if (!ConduitHuntingTracker.tryMarkHit(entity.getUUID())) return false;
        return entity.hurtServer(level, source, amount);
    }

    @ModifyConstant(method = "getDestroyRangeAABB", constant = @Constant(doubleValue = 8.0))
    private static double modifyHuntingSearchRange(double original) { return 32.0; }

    @ModifyConstant(method = "updateDestroyTarget", constant = @Constant(doubleValue = 8.0))
    private static double modifyHuntingCheckRange(double original) { return 32.0; }

    @ModifyConstant(method = "updateAndAttackTarget", constant = @Constant(floatValue = 4.0f))
    private static float modifyHuntingDamage(float original) { return 0.75f; }
}