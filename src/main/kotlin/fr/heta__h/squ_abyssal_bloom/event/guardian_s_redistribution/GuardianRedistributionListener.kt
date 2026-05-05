package fr.heta__h.squ_abyssal_bloom.event.guardian_s_redistribution

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object GuardianRedistributionListener {

    @SubscribeEvent
    fun onLivingTick(event: EntityTickEvent.Post) {
        val entity = event.entity
        if (entity !is LivingEntity) return
        if (!entity.level().isClientSide) {
            val hasPotion = entity.hasEffect(ModEffects.GUARDIAN_S_REDISTRIBUTION)
            val currentState = entity.getData(ModAttachments.HAS_GUARDIAN_SPIKES)
            if (hasPotion != currentState && !entity.hasEffect(MobEffects.INVISIBILITY)) {
                entity.setData(ModAttachments.HAS_GUARDIAN_SPIKES, hasPotion)
            }
        }
    }
}