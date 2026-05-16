package fr.heta__h.squ_abyssal_bloom.event.conduit

import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.conduit.ConduitHuntingTracker
import net.minecraft.client.player.LocalPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.MobEffectEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

@EventBusSubscriber(value = [Dist.CLIENT])
object ClientConduitHandler {

    var isInDomain = false
        private set

    @SubscribeEvent
    fun onEffectAdded(event: MobEffectEvent.Added) {
        if (!isLocalPlayer(event.entity)) return
        if (event.effectInstance.effect != MobEffects.CONDUIT_POWER) return
        isInDomain = true
    }

    @SubscribeEvent
    fun onEffectRemoved(event: MobEffectEvent.Remove) {
        if (!isLocalPlayer(event.entity)) return
        if (event.effect != MobEffects.CONDUIT_POWER) return
        isInDomain = false
        playPressureTransition(event.entity)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        ConduitHuntingTracker.clearHits()
    }

    private fun isLocalPlayer(entity: Entity): Boolean {
        if (!entity.level().isClientSide) return false
        return entity is LocalPlayer
    }

    private fun playPressureTransition(entity: Entity) {
        entity.level().playLocalSound(
            entity.x, entity.y, entity.z, ModSounds.BUBBLE_PROJECTILE_LAUNCH.get(),
            SoundSource.PLAYERS,
            1.0f, 1.0f, false
        )
    }

}