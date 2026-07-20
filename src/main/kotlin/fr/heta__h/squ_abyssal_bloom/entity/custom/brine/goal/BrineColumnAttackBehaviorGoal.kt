package fr.heta__h.squ_abyssal_bloom.entity.custom.brine.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet

class BrineColumnAttackBehaviorGoal(private val brine: BrineEntity) : Goal() {
    private var bubbleCooldown = 0

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        val target = brine.target ?: return false
        return brine.isInWater &&
            brine.xzDistToSqr(target) <= BrineEntity.ATTACK_ENTER_RADIUS_SQR &&
            target.y - brine.y > BrineEntity.COLUMN_VS_DIRECT_Y_THRESHOLD
    }

    override fun canContinueToUse(): Boolean {
        val target = brine.target ?: return false
        return brine.isInWater &&
            brine.xzDistToSqr(target) <= BrineEntity.ATTACK_EXIT_RADIUS_SQR &&
            target.y - brine.y > BrineEntity.COLUMN_VS_DIRECT_Y_THRESHOLD
    }

    override fun requiresUpdateEveryTick() = true

    override fun start() {
        bubbleCooldown = BrineEntity.COLUMN_BUBBLE_COOLDOWN_TICKS
    }

    override fun tick() {
        if (bubbleCooldown > 0) bubbleCooldown--
        val target = brine.target ?: return
        brine.moveToPlayerBlock(target, brine.xzDistTo(target), BrineEntity.ATTACK_MOVE_SPEED)
        brine.placeOrUpdateBubbleColumn()
        if (bubbleCooldown <= 0) {
            bubbleCooldown = BrineEntity.COLUMN_BUBBLE_COOLDOWN_TICKS
            brine.fireColumnBubble()
        }
    }

    override fun stop() {
        brine.behaviorPathController.stop()
        brine.clearBubbleColumn()
    }
}
