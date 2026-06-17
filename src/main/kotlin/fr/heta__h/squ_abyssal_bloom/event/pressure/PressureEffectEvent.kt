package fr.heta__h.squ_abyssal_bloom.event.pressure

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.damage_type.ModDamagesTypes
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.DamageTypeTags
import net.minecraft.tags.EntityTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.EntityTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object PressureEffectEvent {

    private const val PRESSURE_BASE_THRESHOLD = 45
    private const val PRESSURE_TIER_DEPTH = 15
    private const val PRESSURE_SEAL_BONUS = 15
    private const val PRESSURE_DURATION = 400
    private const val PRESSURE_DURATION_THRESHOLD = 300

    private const val INTERVAL_TIER_0 = 60L
    private const val INTERVAL_TIER_1 = 40L
    private const val INTERVAL_TIER_2 = 20L

    private const val DROWNING_INTERVAL_TIER_0 = 15L
    private const val DROWNING_INTERVAL_TIER_1 = 10L
    private const val DROWNING_INTERVAL_TIER_2 = 5L

    private val lastDamageTick = ConcurrentHashMap<UUID, Long>()
    private val lastDrownTick = ConcurrentHashMap<UUID, Long>()

    @SubscribeEvent
    fun onEntityTick(event: EntityTickEvent.Post) {
        val entity = event.entity as? LivingEntity ?: return
        if (entity.level().isClientSide) return
        val serverLevel = entity.level() as? ServerLevel ?: return

        if (entity.airSupply < 0) {
            val pressure = entity.getEffect(ModEffects.PRESSURE)
            if (pressure != null
                && entity.isInWater
                && (entity as? Player)?.isCreative != true
                && !entity.isSpectator
                && !entity.isInvulnerable
            ) {
                entity.airSupply = 0

                val last = lastDrownTick[entity.uuid] ?: 0L
                val interval = when (pressure.amplifier) {
                    0 -> DROWNING_INTERVAL_TIER_0
                    1 -> DROWNING_INTERVAL_TIER_1
                    else -> DROWNING_INTERVAL_TIER_2
                }
                if (serverLevel.gameTime - last >= interval) {
                    lastDrownTick[entity.uuid] = serverLevel.gameTime
                    entity.invulnerableTime = 0
                    entity.hurtServer(
                        serverLevel,
                        DamageSource(
                            serverLevel.registryAccess()
                                .lookupOrThrow(Registries.DAMAGE_TYPE)
                                .getOrThrow(ModDamagesTypes.PRESSURE)
                        ),
                        2f
                    )
                }
            }
        }

        if (entity.tickCount % 40 != 0) return

        if (!entity.isInWater
            || entity.isSpectator
            || (entity as? Player)?.isCreative == true
            || entity.isInvulnerable
            || (entity as? Player)?.hasEffect(MobEffects.CONDUIT_POWER) == true)
        {
            entity.removeEffect(ModEffects.PRESSURE)
            return
        }

        if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.type).`is`(EntityTypeTags.CAN_BREATHE_UNDER_WATER)) return

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
            entity.addEffect(MobEffectInstance(ModEffects.PRESSURE, PRESSURE_DURATION, amplifier, false, false, true))
        }

        if (entity.airSupply > 0) {
            val last = lastDamageTick[entity.uuid] ?: 0L
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
    fun onLivingIncomingDamage(event: LivingIncomingDamageEvent) {
        if (!event.source.`is`(DamageTypeTags.IS_DROWNING)) return
        if (event.entity.getEffect(ModEffects.PRESSURE) == null) return
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        lastDamageTick.remove(event.entity.uuid)
        lastDrownTick.remove(event.entity.uuid)
    }

    @SubscribeEvent
    fun onEntityDeath(event: LivingDeathEvent) {
        lastDamageTick.remove(event.entity.uuid)
        lastDrownTick.remove(event.entity.uuid)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        if (event.server.tickCount % 6000 == 0) {
            val now = event.server.overworld().gameTime
            lastDamageTick.entries.removeIf { (_, tick) -> now - tick > 1200L }
            lastDrownTick.entries.removeIf { (_, tick) -> now - tick > 1200L }
        }
    }
}