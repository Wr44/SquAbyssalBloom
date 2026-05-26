package fr.heta__h.squ_abyssal_bloom.mixin.block.conduit;

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks;
import fr.heta__h.squ_abyssal_bloom.block.astral_prismarine.AstralPrismarineBlock;
import fr.heta__h.squ_abyssal_bloom.util.conduit.AstralPrismarineTracker;
import fr.heta__h.squ_abyssal_bloom.util.conduit.ConduitHuntingTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.ArrayList;
import java.util.List;

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
    private static double modifyHuntingSearchRange(double original) {
        return 32.0;
    }

    @ModifyConstant(method = "updateDestroyTarget", constant = @Constant(doubleValue = 8.0))
    private static double modifyHuntingCheckRange(double original) {
        return 32.0;
    }

    @ModifyConstant(method = "updateAndAttackTarget", constant = @Constant(floatValue = 4.0f))
    private static float modifyHuntingDamage(float original) {
        return 0.75f;
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private static void enforceAstralPrismarineArches(
            Level level, BlockPos pos, List<BlockPos> positions,
            CallbackInfoReturnable<Boolean> cir
    ) {
        BlockPos up = pos.offset(0, 2, 0);
        BlockPos down = pos.offset(0, -2, 0);
        BlockPos north = pos.offset(0, 0, -2);
        BlockPos south = pos.offset(0, 0, 2);
        BlockPos east = pos.offset(2, 0, 0);
        BlockPos west = pos.offset(-2, 0, 0);

        boolean hasNorthOrSouth = !level.getBlockState(north).isAir() || !level.getBlockState(south).isAir();
        boolean hasEastOrWest = !level.getBlockState(east).isAir() || !level.getBlockState(west).isAir();

        boolean meridionalValid = true;
        boolean equatorialValid = true;

        if (hasNorthOrSouth) {
            BlockPos[] cardinals = {up, down, north, south};
            for (BlockPos p : cardinals) {
                if (!level.getBlockState(p).is(ModBlocks.ASTRAL_PRISMARINE)) {
                    meridionalValid = false;
                    break;
                }
            }
            if (meridionalValid) {
                for (BlockPos p : cardinals) {
                    if (!positions.contains(p)) positions.add(p);
                }
            }
        }

        if (hasEastOrWest) {
            BlockPos[] cardinals = {up, down, east, west};
            for (BlockPos p : cardinals) {
                if (!level.getBlockState(p).is(ModBlocks.ASTRAL_PRISMARINE)) {
                    equatorialValid = false;
                    break;
                }
            }
            if (equatorialValid) {
                for (BlockPos p : cardinals) {
                    if (!positions.contains(p)) positions.add(p);
                }
            }
        }

        int validArchCount = 0;
        if (hasNorthOrSouth && meridionalValid) validArchCount++;
        if (hasEastOrWest && equatorialValid) validArchCount++;

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

        BlockPos up = pos.offset(0, 2, 0);
        BlockPos down = pos.offset(0, -2, 0);
        BlockPos north = pos.offset(0, 0, -2);
        BlockPos south = pos.offset(0, 0, 2);
        BlockPos east = pos.offset(2, 0, 0);
        BlockPos west = pos.offset(-2, 0, 0);

        boolean hasNorthOrSouth = !level.getBlockState(north).isAir() || !level.getBlockState(south).isAir();
        boolean hasEastOrWest = !level.getBlockState(east).isAir() || !level.getBlockState(west).isAir();

        List<BlockPos> shouldBeActive = new ArrayList<>();

        if (hasNorthOrSouth && isActive) {
            BlockPos[] cardinals = {up, down, north, south};
            boolean allAstral = true;

            for (BlockPos p : cardinals) {
                if (!level.getBlockState(p).is(ModBlocks.ASTRAL_PRISMARINE)) {
                    allAstral = false;
                    break;
                }
            }

            if (allAstral) {
                shouldBeActive.addAll(List.of(cardinals));
            }
        }

        if (hasEastOrWest && isActive) {
            BlockPos[] cardinals = {up, down, east, west};
            boolean allAstral = true;

            for (BlockPos p : cardinals) {
                if (!level.getBlockState(p).is(ModBlocks.ASTRAL_PRISMARINE)) {
                    allAstral = false;
                    break;
                }
            }

            if (allAstral) {
                shouldBeActive.addAll(List.of(cardinals));
            }
        }

        List<BlockPos> allPossiblePositions = List.of(
                pos.offset(0, 2, 0), pos.offset(0, -2, 0), pos.offset(0, 0, 2), pos.offset(0, 0, -2),
                pos.offset(0, 2, 2), pos.offset(0, 2, -2), pos.offset(0, -2, 2), pos.offset(0, -2, -2),
                pos.offset(2, 0, 0), pos.offset(-2, 0, 0),
                pos.offset(2, 0, 2), pos.offset(2, 0, -2), pos.offset(-2, 0, 2), pos.offset(-2, 0, -2),
                pos.offset(2, 2, 0), pos.offset(2, -2, 0), pos.offset(-2, 2, 0), pos.offset(-2, -2, 0)
        );

        for (BlockPos checkPos : allPossiblePositions) {
            BlockState blockState = level.getBlockState(checkPos);

            if (blockState.is(ModBlocks.ASTRAL_PRISMARINE)) {
                boolean shouldActivate = shouldBeActive.contains(checkPos);
                boolean currentlyActive = blockState.getValue(AstralPrismarineBlock.ACTIVE);

                if (currentlyActive != shouldActivate) {
                    level.setBlock(
                            checkPos,
                            blockState.setValue(AstralPrismarineBlock.ACTIVE, shouldActivate),
                            3
                    );

                    if (shouldActivate) {
                        AstralPrismarineTracker.INSTANCE.markActive(checkPos);
                    } else {
                        AstralPrismarineTracker.INSTANCE.markInactive(checkPos);
                    }
                }
            }
        }
    }
}