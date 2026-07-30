package fr.heta__h.squ_abyssal_bloom.entity.custom.brine.goal

import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

class BrineHoverBehaviorGoal(private val brine: BrineEntity) : Goal() {
    init {
        flags = EnumSet.noneOf(Flag::class.java)
    }

    override fun canUse() = brine.isInWater
    override fun requiresUpdateEveryTick() = true

    override fun tick() {
        brine.deltaMovement = Vec3(
            brine.deltaMovement.x,
            brine.deltaMovement.y + brine.computeHoverSpringForce(),
            brine.deltaMovement.z
        )
    }
}
