package fr.heta__h.squ_abyssal_bloom.entity.custom.brine.goal

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.entity.custom.brine.BrineEntity
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.player.Player

class BrinePlayerTargetGoal(brine: BrineEntity) :
    NearestAttackableTargetGoal<Player>(brine, Player::class.java, true) {

    override fun getFollowDistance(): Double = ModServerConfig.BRINE_DETECTION_RANGE.get()
}
