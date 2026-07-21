package fr.heta__h.squ_abyssal_bloom.entity.custom.brine.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet

class BrineFollowBehaviorGoal(private val brine: BrineEntity) : Goal() {
    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        val target = brine.target ?: return false
        return brine.isInWater && brine.xzDistToSqr(target) > brine.attackEnterRadiusSqr
    }

    override fun canContinueToUse(): Boolean = canUse()
    override fun requiresUpdateEveryTick() = true

    override fun tick() {
        val target = brine.target ?: return
        brine.moveToPlayerBlock(target, brine.xzDistTo(target), brine.followSpeed)
    }

    override fun stop() {
        brine.behaviorPathController.stop()
    }
}
