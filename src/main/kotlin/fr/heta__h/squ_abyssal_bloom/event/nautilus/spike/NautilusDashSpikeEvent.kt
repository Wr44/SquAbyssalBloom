package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent
import java.util.WeakHashMap

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object NautilusDashSpikeEvent {

    private const val HITBOX_EXPANSION = 0.5
    private const val BASE_DAMAGE = 4.0f
    private const val SPEED_DAMAGE_MULTIPLIER = 4.0f
    private const val BASE_KNOCKBACK = 0.5
    private const val SPEED_KNOCKBACK_MULTIPLIER = 1.2
    private const val MAX_KNOCKBACK = 3.0
    private const val SHARPNESS_BASE = 1.0f
    private const val SHARPNESS_ADD = 0.75f
    private const val DURABILITY_LOSS_PER_HIT = 4
    private const val SOUND_VOLUME = 1.0f
    private const val DASH_IFRAMES = 10L
    private const val CHARGE_GRACE_TICKS = 20L
    private const val RECOIL_Y = 0.3

    private val hitTracker = WeakHashMap<LivingEntity, Long>()

    @SubscribeEvent
    fun onNautilusDash(event: EntityTickEvent.Pre) {
        val entity = event.entity
        val level = entity.level()

        if (entity !is AbstractNautilus || level !is ServerLevel) return

        val extraItemStack = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
        if (extraItemStack.item != NautilusLayerItems.SHIELD) return

        val recoil = entity.getData(ModAttachments.NAUTILUS_RECOIL)
        if (recoil.lengthSqr() > 0.001) {
            entity.deltaMovement = entity.deltaMovement.add(recoil.scale(0.25))
            val next = recoil.scale(0.6)
            entity.setData(
                ModAttachments.NAUTILUS_RECOIL,
                if (next.lengthSqr() < 0.001) Vec3.ZERO else next
            )
            entity.hurtMarked = true
            val rider = entity.controllingPassenger
            if (rider is ServerPlayer) {
                rider.connection.send(ClientboundSetEntityMotionPacket(entity))
            }
        }

        if (!entity.isDashing) return

        val attackBox = entity.boundingBox
            .expandTowards(entity.deltaMovement)
            .inflate(HITBOX_EXPANSION)

        val targets = level.getEntities(entity, attackBox)
        val currentTick = level.gameTime
        entity.setData(ModAttachments.NAUTILUS_CHARGE_GRACE, currentTick + CHARGE_GRACE_TICKS)
        val dashSpeed = entity.deltaMovement.length()
        val calculatedKnockback = (BASE_KNOCKBACK + (dashSpeed * SPEED_KNOCKBACK_MULTIPLIER)).coerceAtMost(MAX_KNOCKBACK)

        val lungeLevel = ModUtilities.getEnchantLevel(extraItemStack, level, "lunge", "minecraft")
        val sharpnessLevel = ModUtilities.getEnchantLevel(extraItemStack, level, "sharpness", "minecraft")
        val sharpnessBonus = if (sharpnessLevel > 0) SHARPNESS_BASE + (sharpnessLevel - 1) * SHARPNESS_ADD else 0f

        val calculatedDamage = BASE_DAMAGE + (dashSpeed * SPEED_DAMAGE_MULTIPLIER).toFloat() + sharpnessBonus

        for (target in targets) {
            if (target !is LivingEntity || entity.hasPassenger(target) || !target.isAlive) continue

            val lastHitTick = hitTracker.getOrDefault(target, 0L)
            if (currentTick - lastHitTick < DASH_IFRAMES) continue

            val didHurt = target.hurtServer(
                level,
                entity.damageSources().mobAttack(entity.controllingPassenger ?: entity),
                calculatedDamage
            )

            if (!didHurt) continue

            hitTracker[target] = currentTick

            val dir = target.position().subtract(entity.position()).normalize()
            target.deltaMovement = target.deltaMovement.add(
                dir.x * calculatedKnockback,
                dir.y * calculatedKnockback * 0.5,
                dir.z * calculatedKnockback
            )
            target.hurtMarked = true

            if (target is ServerPlayer) {
                target.connection.send(ClientboundSetEntityMotionPacket(target))
            }

            if (lungeLevel > 0) {
                val reboundStrength = calculatedKnockback * (lungeLevel + 1f) * 0.8f
                val lookVec = entity.lookAngle
                entity.isDashing = false
                entity.setData(
                    ModAttachments.NAUTILUS_RECOIL,
                    net.minecraft.world.phys.Vec3(
                        -lookVec.x * reboundStrength,
                        RECOIL_Y,
                        -lookVec.z * reboundStrength
                    )
                )
            }

            val dynamicPitch = 0.8f + (dashSpeed * 0.2f).toFloat()
            entity.playSound(SoundEvents.SPEAR_HIT.value(), SOUND_VOLUME, dynamicPitch)
            extraItemStack.hurtAndBreak(DURABILITY_LOSS_PER_HIT, entity, EquipmentSlot.BODY)
            entity.setData(ModAttachments.NAUTILUS_EXTRA_SLOT.get(), extraItemStack)
        }
    }
}