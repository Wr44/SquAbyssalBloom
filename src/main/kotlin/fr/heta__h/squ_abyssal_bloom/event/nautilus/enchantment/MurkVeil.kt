package fr.heta__h.squ_abyssal_bloom.event.nautilus.enchantment

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getEnchantLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object MurkVeil {

    private val lastAttackTick = ConcurrentHashMap<UUID, Long>()
    private const val STEALTH_BREAK_TICKS = 60L
    private const val STEALTH_MIN_DETECTION_RADIUS = 5.0
    private const val STEALTH_MIN_DETECTION_RADIUS_SQR = STEALTH_MIN_DETECTION_RADIUS * STEALTH_MIN_DETECTION_RADIUS


    @SubscribeEvent
    fun onLivingChangeTarget(event: LivingChangeTargetEvent) {
        val target = event.newAboutToBeSetTarget as? ServerPlayer ?: return
        val nautilus = target.vehicle as? AbstractNautilus ?: return
        val bodyStack = nautilus.getItemBySlot(EquipmentSlot.BODY)
        if (getEnchantLevel(bodyStack, nautilus.level(), "murk_veil") == 0) return

        val lastAttack = lastAttackTick[target.uuid] ?: 0L
        val inStealth = nautilus.level().gameTime - lastAttack > STEALTH_BREAK_TICKS
        if (inStealth && event.entity.distanceToSqr(nautilus) > STEALTH_MIN_DETECTION_RADIUS_SQR) {
            event.newAboutToBeSetTarget = null
        }
    }


    @SubscribeEvent
    fun onPlayerAttack(event: AttackEntityEvent) {
        lastAttackTick[event.entity.uuid] = event.entity.level().gameTime
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        lastAttackTick.remove(event.entity.uuid)
    }

}