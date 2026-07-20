package fr.heta__h.squ_abyssal_bloom.entity.custom.brine.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet

class BrineDirectAttackBehaviorGoal(private val brine: BrineEntity) : Goal() {
    private var bubbleCooldown = 0
    private var exitTimer = 0
    private var verticalTransitionTimer = 0

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        val target = brine.target ?: return false
        return brine.isInWater &&
            brine.xzDistToSqr(target) <= BrineEntity.ATTACK_ENTER_RADIUS_SQR &&
            target.y - brine.y <= BrineEntity.COLUMN_VS_DIRECT_Y_THRESHOLD
    }

    override fun canContinueToUse(): Boolean {
        val target = brine.target ?: return false
        if (!brine.isInWater) return false
        if (brine.xzDistToSqr(target) > BrineEntity.ATTACK_EXIT_RADIUS_SQR &&
            exitTimer >= BrineEntity.DIRECT_EXIT_GRACE_TICKS
        ) return false
        if (target.y - brine.y > BrineEntity.COLUMN_VS_DIRECT_Y_THRESHOLD &&
            verticalTransitionTimer >= BrineEntity.DIRECT_Y_GRACE_TICKS
        ) return false
        return true
    }

    override fun requiresUpdateEveryTick() = true

    override fun start() {
        bubbleCooldown = BrineEntity.DIRECT_BUBBLE_COOLDOWN_TICKS
        exitTimer = 0
        verticalTransitionTimer = 0
    }

    override fun tick() {
        if (bubbleCooldown > 0) bubbleCooldown--
        val target = brine.target ?: return
        val horizontalDistance = brine.xzDistTo(target)
        val verticalDistance = target.y - brine.y

        if (horizontalDistance > BrineEntity.ATTACK_EXIT_RADIUS) {
            exitTimer++
            verticalTransitionTimer = 0
            brine.behaviorPathController.stop()
            return
        }
        exitTimer = 0

        if (verticalDistance > BrineEntity.COLUMN_VS_DIRECT_Y_THRESHOLD) {
            verticalTransitionTimer++
            brine.behaviorPathController.stop()
            return
        }
        verticalTransitionTimer = 0

        brine.moveToPlayerBlock(target, horizontalDistance, BrineEntity.ATTACK_MOVE_SPEED)
        if (bubbleCooldown <= 0) {
            bubbleCooldown = BrineEntity.DIRECT_BUBBLE_COOLDOWN_TICKS
            brine.fireDirectBubble(target)
        }
    }

    override fun stop() {
        brine.behaviorPathController.stop()
        exitTimer = 0
        verticalTransitionTimer = 0
    }
}
