package fr.heta__h.squ_abyssal_bloom.item.lifeline_bubble

import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class LifelineBubbleItem(properties: Item.Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        if (!player.isUnderWater) return InteractionResult.FAIL

        val serverLevel = level as ServerLevel
        val bubble = BubbleProjectile(ModEntities.BUBBLE.get(), serverLevel)
        bubble.apply {
            bubbleStage = 0
            owner = player
            bubble.setPos(player.x, player.eyeY + 1.0, player.z)
            deltaMovement = Vec3(0.0, 0.05, 0.0)
            attachedPlayerId = player.id
        }
        serverLevel.addFreshEntity(bubble)

        if (!player.abilities.instabuild) player.getItemInHand(hand).shrink(1)
        return InteractionResult.CONSUME
    }

    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        target: LivingEntity,
        hand: InteractionHand
    ): InteractionResult {
        if (player.level().isClientSide) return InteractionResult.SUCCESS
        if (!player.isUnderWater) return InteractionResult.FAIL

        val serverLevel = player.level() as ServerLevel
        val bubble = BubbleProjectile(ModEntities.BUBBLE.get(), serverLevel)
        bubble.apply {
            bubbleStage = 0
            owner = player
            setPos(target.x, target.eyeY + 1.0, target.z)
            deltaMovement = Vec3(0.0, 0.05, 0.0)
            attachedPlayerId = target.id
        }
        serverLevel.addFreshEntity(bubble)

        if (!player.abilities.instabuild) stack.shrink(1)
        return InteractionResult.CONSUME
    }
}