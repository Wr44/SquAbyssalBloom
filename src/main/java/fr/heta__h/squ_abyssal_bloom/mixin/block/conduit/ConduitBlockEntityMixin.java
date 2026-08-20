package fr.heta__h.squ_abyssal_bloom.mixin.block.conduit;

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks;
import fr.heta__h.squ_abyssal_bloom.block.astral_prismarine.AstralPrismarineBlock;
import fr.heta__h.squ_abyssal_bloom.util.conduit.AstralPrismarineTracker;
import fr.heta__h.squ_abyssal_bloom.util.conduit.ConduitHuntingTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ConduitBlockEntity.class)
public abstract class ConduitBlockEntityMixin {

    @Unique
    private static final int[][] ASTRAL_OFFSETS = {
            {0, 2, 0}, {0, -2, 0}, {0, 0, 2}, {0, 0, -2},
            {0, 2, 2}, {0, 2, -2}, {0, -2, 2}, {0, -2, -2},
            {2, 0, 0}, {-2, 0, 0},
            {2, 0, 2}, {2, 0, -2}, {-2, 0, 2}, {-2, 0, -2},
            {2, 2, 0}, {2, -2, 0}, {-2, 2, 0}, {-2, -2, 0}
    };

    @Unique
    private static final int[][] MERIDIONAL_OFFSETS = {{0, 2, 0}, {0, -2, 0}, {0, 0, -2}, {0, 0, 2}};

    @Unique
    private static final int[][] EQUATORIAL_OFFSETS = {{0, 2, 0}, {0, -2, 0}, {2, 0, 0}, {-2, 0, 0}};

    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean suppressVanillaConduitPower(Player player, MobEffectInstance effect) { return false; }

    @Redirect(method = "updateAndAttackTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
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

    @Inject(method = "updateShape", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
    private static void enforceAstralPrismarineArches(
            Level level, BlockPos pos, List<BlockPos> positions,
            CallbackInfoReturnable<Boolean> cir
    ) {
        boolean meridionalValid = true;
        boolean equatorialValid = true;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int[] off : MERIDIONAL_OFFSETS) {
            mutablePos.set(pos.getX() + off[0], pos.getY() + off[1], pos.getZ() + off[2]);
            if (!level.getBlockState(mutablePos).is(ModBlocks.ASTRAL_PRISMARINE)) {
                meridionalValid = false;
                break;
            }
        }
        if (meridionalValid) {
            for (int[] off : MERIDIONAL_OFFSETS) {
                BlockPos p = pos.offset(off[0], off[1], off[2]);
                if (!positions.contains(p)) positions.add(p);
            }
        }

        for (int[] off : EQUATORIAL_OFFSETS) {
            mutablePos.set(pos.getX() + off[0], pos.getY() + off[1], pos.getZ() + off[2]);
            if (!level.getBlockState(mutablePos).is(ModBlocks.ASTRAL_PRISMARINE)) {
                equatorialValid = false;
                break;
            }
        }
        if (equatorialValid) {
            for (int[] off : EQUATORIAL_OFFSETS) {
                BlockPos p = pos.offset(off[0], off[1], off[2]);
                if (!positions.contains(p)) positions.add(p);
            }
        }

        int validArchCount = (meridionalValid ? 1 : 0) + (equatorialValid ? 1 : 0);
        if (validArchCount == 0) {
            cir.setReturnValue(false);
        } else {
            cir.setReturnValue(positions.size() >= 16);
        }
    }

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void updateAstralBlocks(
            Level level, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity,
            CallbackInfo ci
    ) {
        boolean isActive = blockEntity.isActive();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        boolean meridionalValid = isActive;
        boolean equatorialValid = isActive;

        if (isActive) {
            for (int[] off : MERIDIONAL_OFFSETS) {
                mutablePos.set(pos.getX() + off[0], pos.getY() + off[1], pos.getZ() + off[2]);
                if (!level.getBlockState(mutablePos).is(ModBlocks.ASTRAL_PRISMARINE)) {
                    meridionalValid = false;
                    break;
                }
            }
            for (int[] off : EQUATORIAL_OFFSETS) {
                mutablePos.set(pos.getX() + off[0], pos.getY() + off[1], pos.getZ() + off[2]);
                if (!level.getBlockState(mutablePos).is(ModBlocks.ASTRAL_PRISMARINE)) {
                    equatorialValid = false;
                    break;
                }
            }
        }

        for (int[] offset : ASTRAL_OFFSETS) {
            mutablePos.set(pos.getX() + offset[0], pos.getY() + offset[1], pos.getZ() + offset[2]);
            BlockState blockState = level.getBlockState(mutablePos);

            if (blockState.is(ModBlocks.ASTRAL_PRISMARINE)) {
                boolean shouldActivate = false;

                if (meridionalValid && offset[0] == 0 && (Math.abs(offset[1]) + Math.abs(offset[2]) == 2)) {
                    shouldActivate = true;
                } else if (equatorialValid && offset[2] == 0 && (Math.abs(offset[0]) + Math.abs(offset[1]) == 2)) {
                    shouldActivate = true;
                }

                boolean currentlyActive = blockState.getValue(AstralPrismarineBlock.ACTIVE);

                if (currentlyActive != shouldActivate) {
                    BlockPos immutablePos = mutablePos.immutable();
                    level.setBlock(
                            immutablePos,
                            blockState.setValue(AstralPrismarineBlock.ACTIVE, shouldActivate),
                            3
                    );

                    if (level instanceof ServerLevel serverLevel) {
                        if (shouldActivate) {
                            AstralPrismarineTracker.INSTANCE.markActive(serverLevel, immutablePos);
                        } else {
                            AstralPrismarineTracker.INSTANCE.markInactive(serverLevel, immutablePos);
                        }
                    }
                }
            }
        }
    }
}
