package fr.heta__h.squ_abyssal_bloom.event.guardian_s_redistribution

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import net.minecraft.core.Holder
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.living.MobEffectEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object GuardianRedistributionListener {

    @SubscribeEvent
    fun onEffectAdded(event: MobEffectEvent.Added) {
        val entity = event.entity
        if (entity.level().isClientSide) return

        val effectValue = event.effectInstance.effect.value()

        when {
            effectValue === ModEffects.GUARDIAN_S_REDISTRIBUTION.value() -> {
                if (!entity.hasEffect(MobEffects.INVISIBILITY)) {
                    entity.setData(ModAttachments.HAS_GUARDIAN_SPIKES, true)
                }
            }
            effectValue === MobEffects.INVISIBILITY.value() -> {
                if (entity.hasEffect(ModEffects.GUARDIAN_S_REDISTRIBUTION)) {
                    entity.setData(ModAttachments.HAS_GUARDIAN_SPIKES, false)
                }
            }
        }
    }

    @SubscribeEvent
    fun onEffectRemoved(event: MobEffectEvent.Remove) {
        handleEffectEnd(event.entity, event.effect)
    }

    @SubscribeEvent
    fun onEffectExpired(event: MobEffectEvent.Expired) {
        handleEffectEnd(event.entity, event.effectInstance?.effect ?: return)
    }

    @SubscribeEvent
    fun onEntityJoinLevel(event: EntityJoinLevelEvent) {
        val entity = event.entity as? Player ?: return
        if (entity.level().isClientSide) return
        resync(entity)
    }

    @SubscribeEvent
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        resync(event.entity)
    }

    @SubscribeEvent
    fun onPlayerDeath(event: LivingDeathEvent) {
        val entity = event.entity as? LivingEntity ?: return
        if (entity.level().isClientSide) return
        entity.setData(ModAttachments.HAS_GUARDIAN_SPIKES, false)
    }

    private fun resync(entity: LivingEntity) {
        if (entity.level().isClientSide) return
        val shouldHaveSpikes = entity.hasEffect(ModEffects.GUARDIAN_S_REDISTRIBUTION) &&
                !entity.hasEffect(MobEffects.INVISIBILITY)
        if (entity.getData(ModAttachments.HAS_GUARDIAN_SPIKES) != shouldHaveSpikes) {
            entity.setData(ModAttachments.HAS_GUARDIAN_SPIKES, shouldHaveSpikes)
        }
    }

    private fun handleEffectEnd(entity: LivingEntity, effect: Holder<MobEffect>) {
        if (entity.level().isClientSide) return

        val effectValue = effect.value()

        when {
            effectValue === ModEffects.GUARDIAN_S_REDISTRIBUTION.value() -> {
                entity.setData(ModAttachments.HAS_GUARDIAN_SPIKES, false)
            }
            effectValue === MobEffects.INVISIBILITY.value() -> {
                if (entity.hasEffect(ModEffects.GUARDIAN_S_REDISTRIBUTION)) {
                    entity.setData(ModAttachments.HAS_GUARDIAN_SPIKES, true)
                }
            }
        }
    }
}