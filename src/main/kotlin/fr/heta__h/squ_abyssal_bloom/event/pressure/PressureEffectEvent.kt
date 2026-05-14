package fr.heta__h.squ_abyssal_bloom.event.pressure

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.damage_type.ModDamagesTypes
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.EntityTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.EntityTickEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object PressureEffectEvent {

    private const val PRESSURE_BASE_THRESHOLD = 35
    private const val PRESSURE_TIER_DEPTH = 15
    private const val PRESSURE_SEAL_BONUS = 15
    private const val PRESSURE_DURATION = 400
    private const val PRESSURE_DURATION_THRESHOLD = 300

    private const val INTERVAL_TIER_0 = 60L
    private const val INTERVAL_TIER_1 = 40L
    private const val INTERVAL_TIER_2 = 20L

    private const val DROWNING_INTERVAL_TIER_0 = 20L
    private const val DROWNING_INTERVAL_TIER_1 = 10L
    private const val DROWNING_INTERVAL_TIER_2 = 7L

    private val lastDamageTick = ConcurrentHashMap<UUID, Long>()

    @SubscribeEvent
    fun onEntityTick(event: EntityTickEvent.Post) {
        val entity = event.entity as? LivingEntity ?: return
        if (entity.level().isClientSide) return
        val serverLevel = entity.level() as? ServerLevel ?: return

        if (!entity.isInWater) {
            entity.removeEffect(ModEffects.PRESSURE)
            return
        }

        if (entity.tickCount % 10 != 0) return

        if (entity.type.`is`(EntityTypeTags.CAN_BREATHE_UNDER_WATER)) return

        val sealLevel = (entity.vehicle as? AbstractNautilus)?.let { nautilus ->
            ModUtilities.getEnchantLevel(nautilus.getItemBySlot(EquipmentSlot.BODY), serverLevel, "pressure_seal")
        } ?: 0

        if (sealLevel >= 4) {
            entity.removeEffect(ModEffects.PRESSURE)
            return
        }

        val depth = ModUtilities.findWaterSurface(entity.level(), entity.blockPosition())
        val effectiveThreshold = PRESSURE_BASE_THRESHOLD + sealLevel * PRESSURE_SEAL_BONUS

        if (depth < effectiveThreshold) {
            entity.removeEffect(ModEffects.PRESSURE)
            return
        }

        val amplifier = ((depth - effectiveThreshold) / PRESSURE_TIER_DEPTH).coerceIn(0, 2)
        val current = entity.getEffect(ModEffects.PRESSURE)

        if (current == null || current.amplifier != amplifier || current.duration < PRESSURE_DURATION_THRESHOLD) {
            entity.addEffect(MobEffectInstance(ModEffects.PRESSURE, PRESSURE_DURATION, amplifier, false, true, true))
        }

        val isDrowning = entity.airSupply <= 0
        val last = lastDamageTick[entity.uuid] ?: 0L

        if (isDrowning) {
            val drowningInterval = when (amplifier) {
                0 -> DROWNING_INTERVAL_TIER_0
                1 -> DROWNING_INTERVAL_TIER_1
                else -> DROWNING_INTERVAL_TIER_2
            }
            if (serverLevel.gameTime - last >= drowningInterval) {
                lastDamageTick[entity.uuid] = serverLevel.gameTime
                entity.hurtServer(serverLevel, entity.damageSources().drown(), 2f)
            }
        } else {
            val interval = when (amplifier) {
                0 -> INTERVAL_TIER_0
                1 -> INTERVAL_TIER_1
                else -> INTERVAL_TIER_2
            }
            if (serverLevel.gameTime - last >= interval) {
                lastDamageTick[entity.uuid] = serverLevel.gameTime
                serverLevel.playSound(
                    null,
                    entity.x, entity.y, entity.z,
                    ModSounds.PRESSURE_DAMAGE.get(),
                    entity.soundSource,
                    1f,
                    0.9f + serverLevel.random.nextFloat() * 0.2f
                )
                entity.hurtServer(
                    serverLevel,
                    DamageSource(
                        serverLevel.registryAccess()
                            .lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(ModDamagesTypes.PRESSURE)
                    ),
                    1f
                )
            }
        }
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        lastDamageTick.remove(event.entity.uuid)
    }
}