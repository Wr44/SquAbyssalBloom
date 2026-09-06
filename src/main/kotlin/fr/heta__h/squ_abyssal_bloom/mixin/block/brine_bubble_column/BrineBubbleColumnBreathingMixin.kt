package fr.heta__h.squ_abyssal_bloom.mixin.block.brine_bubble_column

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LivingEntity::class)
abstract class BrineBubbleColumnBreathingMixin {

    @Unique
    private var shouldRefillAirFromBrineColumn = false

    @Unique
    private var hadBrineColumnDrowningRisk = false

    @Unique
    private var brineColumnTargetAirSupply = 0

    @Inject(method = ["baseTick"], at = [At("HEAD")])
    private fun prepareBrineBubbleColumnAirRefill(callbackInfo: CallbackInfo) {
        shouldRefillAirFromBrineColumn = false
        hadBrineColumnDrowningRisk = false

        val entity = this as LivingEntity
        if (entity.level().isClientSide) return

        val eyePosition = BlockPos.containing(entity.x, entity.eyeY, entity.z)
        if (!entity.level().getBlockState(eyePosition).`is`(ModBlocks.BRINE_BUBBLE_COLUMN.get())) return

        shouldRefillAirFromBrineColumn = true
        brineColumnTargetAirSupply = (entity.airSupply + 4).coerceAtMost(entity.maxAirSupply)

        if (entity.airSupply <= -19) {
            hadBrineColumnDrowningRisk = true
            entity.airSupply = brineColumnTargetAirSupply.coerceAtLeast(0)
        }
    }

    @Inject(method = ["baseTick"], at = [At("TAIL")])
    private fun applyBrineBubbleColumnAirRefill(callbackInfo: CallbackInfo) {
        if (!shouldRefillAirFromBrineColumn) return

        val entity = this as LivingEntity
        if (hadBrineColumnDrowningRisk || entity.airSupply < brineColumnTargetAirSupply) {
            entity.airSupply = brineColumnTargetAirSupply
        }

        shouldRefillAirFromBrineColumn = false
        hadBrineColumnDrowningRisk = false
    }
}
