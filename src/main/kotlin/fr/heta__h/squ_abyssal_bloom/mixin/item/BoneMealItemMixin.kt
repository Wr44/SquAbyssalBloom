package fr.heta__h.squ_abyssal_bloom.mixin.item

import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.gameevent.GameEvent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(BoneMealItem::class)
abstract class BoneMealItemMixin {

    @Inject(method = ["useOn"], at = [At("HEAD")], cancellable = true)
    private fun bonemealBloodSeagrass(context: UseOnContext, cir: CallbackInfoReturnable<InteractionResult>) {
        val level = context.level
        val pos = context.clickedPos
        val clickedFace = context.clickedFace
        val relative = pos.relative(clickedFace)
        val boneMealStack = context.itemInHand

        if (!level.getBiome(relative).`is`(ModBiomes.BLOOD_VALLEY)) {
            return
        }

        if (BoneMealItem.applyBonemeal(boneMealStack, level, pos, context.player)) {
            if (level is ServerLevel) {
                context.player?.let { player ->
                    boneMealStack.causeUseVibration(player, GameEvent.ITEM_INTERACT_FINISH)
                }
                level.levelEvent(1505, pos, 15)
                cir.returnValue = InteractionResult.SUCCESS_SERVER
            } else {
                cir.returnValue = InteractionResult.PASS
            }
            return
        }

        val clickedState = level.getBlockState(pos)
        val solidBlockFace = clickedState.isFaceSturdy(level, pos, clickedFace)

        if (!solidBlockFace || !level.getBlockState(relative).`is`(Blocks.WATER) || !level.getFluidState(relative).isFull) {
            return
        }

        if (level !is ServerLevel) {
            cir.setReturnValue(InteractionResult.SUCCESS)
            return
        }

        val random = level.random
        val bloodSeagrass = ModBlocks.BLOOD_SEAGRASS.get().defaultBlockState()

        outer@ for (j in 0 until 128) {
            var testPos = relative

            for (i in 0 until j / 16) {
                testPos = testPos.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1)
                if (level.getBlockState(testPos).isCollisionShapeFullBlock(level, testPos)) {
                    continue@outer
                }
            }

            if (bloodSeagrass.canSurvive(level, testPos)) {
                val testState = level.getBlockState(testPos)
                if (testState.`is`(Blocks.WATER) && level.getFluidState(testPos).isFull) {
                    level.setBlock(testPos, bloodSeagrass, 3)
                } else if (testState.`is`(ModBlocks.BLOOD_SEAGRASS.get()) && (testState.block as BonemealableBlock).isValidBonemealTarget(level, testPos, testState) && random.nextInt(10) == 0) {
                    (testState.block as BonemealableBlock).performBonemeal(level, random, testPos, testState)
                }
            }
        }

        boneMealStack.shrink(1)
        context.player?.let { player ->
            boneMealStack.causeUseVibration(player, GameEvent.ITEM_INTERACT_FINISH)
        }
        level.levelEvent(1505, relative, 15)

        cir.returnValue = InteractionResult.SUCCESS
    }
}