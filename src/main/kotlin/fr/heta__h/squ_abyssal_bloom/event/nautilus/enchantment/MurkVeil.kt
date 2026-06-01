package fr.heta__h.squ_abyssal_bloom.event.nautilus.enchantment

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getEnchantLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent
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

    @SubscribeEvent
    fun onLivingChangeTarget(event: LivingChangeTargetEvent) {
        if (event.entity !is Enemy) return

        val target = event.newAboutToBeSetTarget as? ServerPlayer ?: return
        val nautilus = target.vehicle as? AbstractNautilus ?: return
        val bodyStack = nautilus.getItemBySlot(EquipmentSlot.BODY)
        if (getEnchantLevel(bodyStack, nautilus.level(), "murk_veil") == 0) return

        val lastAttack = lastAttackTick[target.uuid] ?: 0L
        val inStealth = nautilus.level().gameTime - lastAttack > STEALTH_BREAK_TICKS
        if (!inStealth || event.entity.distanceToSqr(nautilus) <= STEALTH_MIN_DETECTION_RADIUS_SQR) return

        val mob = event.entity as? Mob ?: return
        val followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE)
        val followRangeSqr = followRange * followRange
        val aabb = mob.boundingBox.inflate(followRange)

        val nextTarget = when (mob) {
            is BarnacleEntity -> {
                mob.level().getEntitiesOfClass(LivingEntity::class.java, aabb) { candidate ->
                    candidate != target &&
                            candidate.isAlive &&
                            mob.distanceToSqr(candidate) <= followRangeSqr &&
                            when (candidate) {
                                is Player -> (candidate.gameMode() == net.minecraft.world.level.GameType.SURVIVAL ||
                                        candidate.gameMode() == net.minecraft.world.level.GameType.ADVENTURE) &&
                                        !candidate.hasEffect(MobEffects.INVISIBILITY)
                                is net.minecraft.world.entity.monster.Guardian -> true
                                else -> false
                            }
                }.minByOrNull { mob.distanceToSqr(it) }
            }
            else -> {
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
                    candidate != target &&
                            candidate.isAlive &&
                            targetTypes.any { it.isInstance(candidate) }
                }
                    .map { it to mob.distanceToSqr(it) }
                    .filter { (_, dist) -> dist <= followRangeSqr }
                    .minByOrNull { (_, dist) -> dist }
                    ?.first
            }
        }

        event.newAboutToBeSetTarget = nextTarget
    }

    @SubscribeEvent
    fun onPlayerAttack(event: AttackEntityEvent) {
        lastAttackTick[event.entity.uuid] = event.entity.level().gameTime
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        lastAttackTick.remove(event.entity.uuid)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        if (event.server.tickCount % 6000 == 0) {
            val now = event.server.overworld().gameTime
            lastAttackTick.entries.removeIf { (_, tick) -> now - tick > 50L }
        }
    }
}