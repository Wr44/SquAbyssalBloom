package fr.heta__h.squ_abyssal_bloom.event.nautilus

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.util.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent
import java.util.WeakHashMap

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID)
object NautilusDashSpikeEvent {

    private const val HITBOX_EXPANSION = 0.5

    private const val BASE_DAMAGE = 4.0f
    private const val SPEED_DAMAGE_MULTIPLIER = 4.0f
    private const val BASE_KNOCKBACK = 0.5
    private const val SPEED_KNOCKBACK_MULTIPLIER = 1.2
    private const val MAX_KNOCKBACK = 3.0
    private const val DURABILITY_LOSS_PER_HIT = 4
    private const val SOUND_VOLUME = 1.0f
    private const val DASH_IFRAMES = 10L
    private const val RECOIL_Y = 0.3
    private const val VAMPIRISM_RATIO = 0.30f

    private val hitTracker = WeakHashMap<LivingEntity, Long>()

    @SubscribeEvent
    fun onNautilusDash(event: EntityTickEvent.Pre) {
        val entity = event.entity
        val level = entity.level()

        if (entity !is AbstractNautilus || level !is ServerLevel) return
        if (!entity.isDashing) return

        val extraItemStack = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
        if (extraItemStack.item != NautilusLayer.SHIELD) return

        val attackBox = entity.boundingBox
            .expandTowards(entity.deltaMovement)
            .inflate(HITBOX_EXPANSION)

        val targets = level.getEntities(entity, attackBox)
        val currentTick = level.gameTime

        val dashSpeed = entity.deltaMovement.length()

        val calculatedDamage = BASE_DAMAGE + (dashSpeed * SPEED_DAMAGE_MULTIPLIER).toFloat()
        val calculatedKnockback = (BASE_KNOCKBACK + (dashSpeed * SPEED_KNOCKBACK_MULTIPLIER)).coerceAtMost(MAX_KNOCKBACK)

        val lungeLevel = ModUtilities.getEnchantLevel(
            extraItemStack,
            level,
            "lunge",
            "minecraft"
        )

        for (target in targets) {
            if (target is LivingEntity && !entity.hasPassenger(target) && target.isAlive) {

                val lastHitTick = hitTracker.getOrDefault(target, 0L)
                if (currentTick - lastHitTick < DASH_IFRAMES) {
                    continue
                }

                val didHurt = target.hurtServer(level, entity.damageSources().mobAttack(entity), calculatedDamage)

                if (didHurt) {
                    hitTracker[target] = currentTick

                    val healAmount = calculatedDamage * VAMPIRISM_RATIO
                    if (entity.health < entity.maxHealth) {
                        entity.heal(healAmount)

                        level.sendParticles(
                            ParticleTypes.HEART,
                            entity.x, entity.y + (entity.bbHeight / 1.5), entity.z,
                            3,
                            0.3, 0.2, 0.3,
                            0.0
                        )

                        level.sendParticles(
                            ParticleTypes.DAMAGE_INDICATOR,
                            target.x, target.y + (target.bbHeight / 2.0), target.z,
                            5, 0.2, 0.2, 0.2, 0.1
                        )
                    }

                    val deltaX = entity.x - target.x
                    val deltaZ = entity.z - target.z

                    target.knockback(calculatedKnockback, deltaX, deltaZ)

                    if (lungeLevel > 0) {
                        val reboundStrength = calculatedKnockback * (lungeLevel + 1f) * (1f/3f)

                        entity.isDashing = false

                        val lookVec = entity.lookAngle

                        val recoilX = -lookVec.x * reboundStrength
                        val recoilZ = -lookVec.z * reboundStrength
                        val recoilY = RECOIL_Y

                        entity.deltaMovement = net.minecraft.world.phys.Vec3(recoilX, recoilY, recoilZ)

                        entity.hurtMarked = true

                        val rider = entity.controllingPassenger
                        if (rider is ServerPlayer) {
                            rider.connection.send(ClientboundSetEntityMotionPacket(entity))
                        }
                    }

                    val dynamicPitch = 0.8f + (dashSpeed * 0.2f).toFloat()
                    entity.playSound(SoundEvents.SPEAR_HIT.value(), SOUND_VOLUME, dynamicPitch)

                    extraItemStack.hurtAndBreak(DURABILITY_LOSS_PER_HIT, entity, EquipmentSlot.BODY)
                }
            }
        }
    }
}