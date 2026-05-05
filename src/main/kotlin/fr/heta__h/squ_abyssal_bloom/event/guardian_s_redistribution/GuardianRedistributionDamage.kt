package fr.heta__h.squ_abyssal_bloom.event.guardian_s_redistribution

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import kotlin.math.pow

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object GuardianRedistributionDamage {

    @SubscribeEvent
    fun onDamageTaken(event: LivingDamageEvent.Pre) {
        val victim = event.entity
        val attacker = event.source.entity as? LivingEntity ?: return
        if (attacker is Player) {
            val gamemode = attacker.gameMode()
            if (gamemode != null && gamemode != GameType.SURVIVAL && gamemode != GameType.ADVENTURE) {
                return
            }
        }
        if (victim.level().isClientSide) return
        if (!victim.getData(ModAttachments.HAS_GUARDIAN_SPIKES)) return

        val effectInstance = victim.getEffect(ModEffects.GUARDIAN_S_REDISTRIBUTION) ?: return

        val level = effectInstance.amplifier + 1

        val finalDamageForVictim = redistributeDamage(
            attacker,
            victim,
            event.originalDamage,
            level.toFloat()
        )

        event.newDamage = finalDamageForVictim
    }

    fun redistributeDamage(attacker: LivingEntity, victim: LivingEntity, damage: Float, level: Float): Float {
        val ratio = 1.0 - 1.5.pow(-level.toDouble())
        val damageToApply = (damage * ratio).toFloat()

        if (damageToApply > 0.1f) {
            attacker.hurtServer(level as ServerLevel,
                victim.damageSources().thorns(victim),
                damageToApply
            )

            attacker.level().playSound(
                null,
                attacker.blockPosition(),
                SoundEvents.THORNS_HIT,
                SoundSource.HOSTILE,
                1.0f,
                1.0f
            )
        }

        return damage - damageToApply
    }

}
