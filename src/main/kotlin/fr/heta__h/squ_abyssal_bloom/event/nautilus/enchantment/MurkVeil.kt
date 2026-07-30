package fr.heta__h.squ_abyssal_bloom.event.nautilus.enchantment

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.ai.stealth.StealthRetargetRegistry
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getEnchantLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.monster.Enemy
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object MurkVeil {

    private val lastAttackTick = ConcurrentHashMap<UUID, Long>()
    private const val STEALTH_BREAK_TICKS = 60L
    private const val STEALTH_MIN_DETECTION_RADIUS = 5.0
    private const val STEALTH_MIN_DETECTION_RADIUS_SQR = STEALTH_MIN_DETECTION_RADIUS * STEALTH_MIN_DETECTION_RADIUS

    private val targetTypeField by lazy {
        runCatching {
            NearestAttackableTargetGoal::class.java
                .getDeclaredField("targetType")
                .also { it.isAccessible = true }
        }.getOrNull()
    }

    private fun isStealthActive(entity: LivingEntity, currentTime: Long): Boolean {
        val nautilus = entity.vehicle as? AbstractNautilus ?: return false
        val bodyStack = nautilus.getItemBySlot(EquipmentSlot.BODY)
        if (getEnchantLevel(bodyStack, nautilus.level(), "murk_veil") == 0) return false

        val lastAttack = lastAttackTick[entity.uuid] ?: 0L
        return currentTime - lastAttack > STEALTH_BREAK_TICKS
    }

    @SubscribeEvent
    fun onLivingChangeTarget(event: LivingChangeTargetEvent) {
        if (event.entity !is Enemy) return

        val target = event.newAboutToBeSetTarget as? ServerPlayer ?: return
        val mob = event.entity as? Mob ?: return
        val currentTime = mob.level().gameTime

        if (!isStealthActive(target, currentTime) || event.entity.distanceToSqr(target.vehicle!!) <= STEALTH_MIN_DETECTION_RADIUS_SQR) return

        val followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE)
        val followRangeSqr = followRange * followRange
        val aabb = mob.boundingBox.inflate(followRange)

        val provider = StealthRetargetRegistry.find(mob)
        val nextTarget = if (provider != null) {
            provider.findStealthRetarget(mob, target) { candidate -> !isStealthActive(candidate, currentTime) }
        } else {
            val field = targetTypeField ?: return
            val targetTypes = mob.targetSelector.availableGoals
                .map { it.goal }
                .filterIsInstance<NearestAttackableTargetGoal<*>>()
                .mapNotNull { goal ->
                    runCatching {
                        @Suppress("UNCHECKED_CAST")
                        field.get(goal) as? Class<out LivingEntity>
                    }.getOrNull()
                }
                .toSet()

            mob.level().getEntitiesOfClass(LivingEntity::class.java, aabb) { candidate ->
                candidate !== mob &&
                        candidate != target &&
                        candidate.isAlive &&
                        !isStealthActive(candidate, currentTime) &&
                        targetTypes.any { it.isInstance(candidate) }
            }
                .map { it to mob.distanceToSqr(it) }
                .filter { (_, dist) -> dist <= followRangeSqr }
                .minByOrNull { (_, dist) -> dist }
                ?.first
        }

        event.newAboutToBeSetTarget = nextTarget
    }

    @SubscribeEvent
    fun onLivingDamage(event: LivingDamageEvent.Post) {
        val attacker = event.source.entity as? ServerPlayer ?: return
        lastAttackTick[attacker.uuid] = attacker.level().gameTime
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        lastAttackTick.remove(event.entity.uuid)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        if (event.server.tickCount % 6000 == 0) {
            val now = event.server.overworld().gameTime
            ModUtilities.sweepStaleUuidTicks(lastAttackTick, now, 50L)
        }
    }
}
