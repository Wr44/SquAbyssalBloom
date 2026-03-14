package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object NautilusRiderSurvivalHandler {

    @SubscribeEvent
    fun onEntityTick(event: EntityTickEvent.Post) {
        if (!ModConfig.nautilusLampGivesWaterBreathing) return
        val entity = event.entity as? ServerPlayer ?: return
        if (ModUtilities.getRiderLampInfluence(entity) > 0.5) {
            entity.addEffect(MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, false, false))
        }
    }
}
