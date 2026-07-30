package fr.heta__h.squ_abyssal_bloom.mixin.block.brine_bubble_column

import fr.heta__h.squ_abyssal_bloom.block.brine_bubble_column.BrineBubbleColumnBlock
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.BubbleColumnBlock
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(BubbleColumnAmbientSoundHandler::class)
abstract class BrineBubbleColumnAmbientSoundMixin {

    @Shadow
    @Final
    private lateinit var player: LocalPlayer

    @Shadow
    private var wasInBubbleColumn = false

    @Unique
    private var wasInAnyBubbleColumn = false

    @Unique
    private var isFirstBubbleColumnTick = true

    @Inject(
        method = ["tick"],
        at = [At("TAIL")]
    )
    private fun includeBrineBubbleColumnSound(callbackInfo: CallbackInfo) {
        val brineColumnState = player.level()
            .getBlockStatesIfLoaded(
                player.boundingBox
                    .inflate(0.0, -0.4000000059604645, 0.0)
                    .deflate(1.0e-6)
            )
            .filter { state -> state.block is BrineBubbleColumnBlock }
            .findFirst()
            .orElse(null)

        val isInVanillaBubbleColumn = wasInBubbleColumn
        val isInBrineBubbleColumn = brineColumnState != null

        if (
            isInBrineBubbleColumn &&
            !isInVanillaBubbleColumn &&
            !wasInAnyBubbleColumn &&
            !isFirstBubbleColumnTick &&
            !player.isSpectator
        ) {
            val sound = if (brineColumnState.getValue(BubbleColumnBlock.DRAG_DOWN)) {
                SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE
            } else {
                SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE
            }
            player.playSound(sound, 1.0f, 1.0f)
        }

        val isInAnyBubbleColumn = isInVanillaBubbleColumn || isInBrineBubbleColumn
        wasInAnyBubbleColumn = isInAnyBubbleColumn
        wasInBubbleColumn = isInAnyBubbleColumn
        isFirstBubbleColumnTick = false
    }
}
