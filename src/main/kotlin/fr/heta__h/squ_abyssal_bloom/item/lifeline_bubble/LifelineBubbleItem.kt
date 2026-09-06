package fr.heta__h.squ_abyssal_bloom.item.lifeline_bubble

import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Leashable
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class LifelineBubbleItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        if (player.isShiftKeyDown) return InteractionResult.PASS
        if (!player.isUnderWater) return InteractionResult.FAIL

        if (level.isClientSide) return InteractionResult.SUCCESS

        val serverLevel = level as ServerLevel

        if (hasAttachedBubble(level, player.id)) return InteractionResult.FAIL
        spawnAndAttachBubble(serverLevel, player, Vec3(player.x, player.eyeY + 1.0, player.z), player.id)

        player.getItemInHand(hand).consume(1, player)
        return InteractionResult.CONSUME
    }

    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        target: LivingEntity,
        hand: InteractionHand
    ): InteractionResult {
        if (!player.isShiftKeyDown) return InteractionResult.PASS
        if (player.level().isClientSide) return InteractionResult.SUCCESS
        if (!player.isUnderWater) return InteractionResult.FAIL
        if (target !is Player) {
            val mob = target as? Mob ?: return InteractionResult.FAIL
            if (!mob.canBeLeashed()) return InteractionResult.FAIL
        }
        if (hasAttachedBubble(player.level() as ServerLevel, target.id)) return InteractionResult.FAIL

        val serverLevel = player.level() as ServerLevel
        spawnAndAttachBubble(serverLevel, player, Vec3(target.x, target.eyeY + 1.0, target.z), target.id)

        stack.consume(1, player)
        return InteractionResult.CONSUME
    }

    private fun spawnAndAttachBubble(serverLevel: ServerLevel, ownerPlayer: Player, targetPos: Vec3, attachedEntityId: Int) {
        val bubble = BubbleProjectile(ModEntities.BUBBLE.get(), serverLevel)
        bubble.apply {
            bubbleStage = 0
            owner = ownerPlayer
            setPos(targetPos.x, targetPos.y, targetPos.z)
            deltaMovement = Vec3(0.0, 0.05, 0.0)
            attachedPlayerId = attachedEntityId
        }
        serverLevel.addFreshEntity(bubble)

        serverLevel.playSound(
            null, targetPos.x, targetPos.y, targetPos.z,
            SoundEvents.LEAD_TIED, SoundSource.NEUTRAL, 1.0f, 1.0f
        )
    }

    private fun hasAttachedBubble(serverLevel: ServerLevel, entityId: Int): Boolean {
        return serverLevel.getEntitiesOfClass(
            BubbleProjectile::class.java,
            net.minecraft.world.phys.AABB.ofSize(
                serverLevel.getEntity(entityId)?.position() ?: return false,
                60.0, 60.0, 60.0
            )
        ) { it.attachedPlayerId == entityId }.isNotEmpty()
    }
}