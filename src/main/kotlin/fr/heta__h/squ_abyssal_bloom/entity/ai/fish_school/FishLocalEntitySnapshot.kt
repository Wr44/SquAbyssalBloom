package fr.heta__h.squ_abyssal_bloom.entity.ai.fish_school

import net.minecraft.world.entity.LivingEntity

class FishLocalEntitySnapshot(
    val expiresAtTick: Long,
    val searchRadius: Double,
    val entities: List<LivingEntity>
)
