package fr.heta__h.squ_abyssal_bloom.item.abyssal_guardian_focalist

import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist.FocalistBeamSyncPayload
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getEnchantLevel
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.playSoundLocal
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.hasClearPath
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.level.Level
import net.neoforged.neoforge.network.PacketDistributor
import java.lang.Math.clamp
import java.lang.Math.random
import java.lang.Math.toRadians
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max

class AbyssalGuardianFocalistItem(properties: Properties) : Item(properties) {

    companion object {
        const val MAX_RANGE = 27.5
        const val MIN_CHARGE_TICKS = 30
        const val MAX_CHARGE_TICKS = 80
        const val COOLDOWN = 50

        const val BASE_DAMAGE = 7.5f
        const val ADD_DAMAGE = 8.5f

        const val MAX_LOCK_ANGLE = 45.0

        const val TARGETING_SEARCH_INFLATION = 5.0
        const val TARGETING_FORGIVENESS_ANGLE = 15.0
        const val TARGETING_ANGLE_WEIGHT = 5.0

        const val VISION_MIN = 2
        const val VISION_MAX = 8
        const val LENS_BASE = 1
        const val LENS_ADD = 3
        const val SYPHON_HEAL_FACTOR = 0.33f
        const val SYPHON_ABSORB_CAP = 4.0f
        const val SINGULARITY_PULL = 10.0
        const val SINGULARITY_PULL_FACTOR = 0.5
        const val SINGULARITY_STUN_DURATION = 100

        private val LOCK_DOT_THRESHOLD = cos(toRadians(MAX_LOCK_ANGLE))

        private val lockedTargets = ConcurrentHashMap<UUID, Int>()
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = 72000

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation =
        ItemUseAnimation.SPYGLASS

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val target = getTarget(player, MAX_RANGE)

        if (target is LivingEntity) {
            lockedTargets[player.uuid] = target.id

            player.startUsingItem(hand)

            if (!level.isClientSide) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    player,
                    FocalistBeamSyncPayload(player.id, target.id, true)
                )
            }

            level.playSound(
                null,
                player.x, player.y, player.z,
                SoundEvents.GUARDIAN_ATTACK,
                SoundSource.PLAYERS,
                1f,
                1f
            )

            playSoundLocal(
                target,
                SoundEvents.GUARDIAN_ATTACK,
                SoundSource.PLAYERS,
                1f,
                1f
            )

            return InteractionResult.CONSUME
        }

        return InteractionResult.PASS
    }

    override fun onUseTick(level: Level, entity: LivingEntity, stack: ItemStack, timeLeft: Int) {
        if (entity !is Player) return

        val targetId = lockedTargets[entity.uuid] ?: return
        val target = level.getEntity(targetId) as? LivingEntity ?: run {
            if (!level.isClientSide) entity.releaseUsingItem()
            return
        }

        if (!target.isAlive
            || entity.distanceTo(target) > MAX_RANGE + 2.0
            || !isLookingAtTarget(entity, target)
            || !hasClearPath(level, entity, target)) {
            if (!level.isClientSide) entity.releaseUsingItem()
            return
        }

        val visionLevel = getEnchantLevel(stack, level, "focused_vision")
        val aegisLevel = getEnchantLevel(stack, level, "prismarine_aegis")

        if (!level.isClientSide && aegisLevel > 0) {
            entity.addEffect(MobEffectInstance(ModEffects.GUARDIAN_S_REDISTRIBUTION, 5, 5, false, false))
            target.addEffect(MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false))
        }

        val actualMaxCharge = MAX_CHARGE_TICKS - (visionLevel * VISION_MAX)
        val actualMinCharge = MIN_CHARGE_TICKS - (visionLevel * VISION_MIN)

        val duration = getUseDuration(stack, entity) - timeLeft

        if (duration == actualMinCharge) {
            playSoundLocal(
                entity,
                ModSounds.ABYSSAL_GUARDIAN_FOCALIST_READY.value(),
                SoundSource.PLAYERS,
                0.75f,
                1.5f + 0.5f*random().toFloat()
            )
        }

        if (duration >= actualMaxCharge) {
            if (!level.isClientSide) entity.releaseUsingItem()
        }

        if (duration % 30 == 0) {
            level.playSound(
                null,
                entity.x, entity.y, entity.z,
                SoundEvents.GUARDIAN_ATTACK,
                SoundSource.PLAYERS,
                0.8f - (duration / actualMaxCharge)*0.5f,
                1f + (duration / actualMaxCharge.toFloat()) * 0.5f
            )

            playSoundLocal(
                target,
                SoundEvents.GUARDIAN_ATTACK,
                SoundSource.PLAYERS,
                0.8f - (duration / actualMaxCharge)*0.5f,
                1f + (duration / actualMaxCharge.toFloat()) * 0.5f
            )
        }
    }

    override fun releaseUsing(
        stack: ItemStack,
        level: Level,
        entity: LivingEntity,
        timeLeft: Int
    ): Boolean {
        if (level.isClientSide) return true
        if (entity !is Player) return true

        val targetId = lockedTargets.remove(entity.uuid) ?: return true
        val target = level.getEntity(targetId) as? LivingEntity ?: return true

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            entity,
            FocalistBeamSyncPayload(entity.id, -1, false)
        )

        val ticksUsed = getUseDuration(stack, entity) - timeLeft

        if (ticksUsed >= MIN_CHARGE_TICKS && target.isAlive) {
            val visionLevel = getEnchantLevel(stack, level, "focused_vision")
            val lensLevel = getEnchantLevel(stack, level, "converging_lens")
            val siphonLevel = getEnchantLevel(stack, level, "siphon")
            val singularityLevel = getEnchantLevel(stack, level, "singularity")

            val actualMinCharge = MIN_CHARGE_TICKS - (visionLevel * VISION_MIN)
            val actualMaxCharge = MAX_CHARGE_TICKS - (visionLevel * VISION_MAX)


            val chargeProgress = clamp(
                (ticksUsed - actualMinCharge).toFloat() / (actualMaxCharge - actualMinCharge).toFloat(),
                0.0f,
                1.0f
            )

            val actualBaseDamage = BASE_DAMAGE + lensLevel * LENS_BASE
            val actualAddDamage = ADD_DAMAGE + lensLevel * LENS_ADD

            val damage = actualBaseDamage + (chargeProgress * actualAddDamage)

            target.hurtServer(
                level as ServerLevel,
                level.damageSources().indirectMagic(entity, entity),
                damage
            )

            if (siphonLevel > 0) {
                val healAmount = damage * SYPHON_HEAL_FACTOR
                val missingHealth = entity.maxHealth - entity.health
                if (healAmount > missingHealth) {
                    if (missingHealth > 0f) entity.heal(missingHealth)

                    val excess = healAmount - missingHealth

                    val targetAbsorption = (entity.absorptionAmount + excess).coerceAtMost(max(SYPHON_ABSORB_CAP, entity.absorptionAmount))

                    entity.addEffect(MobEffectInstance(
                        MobEffects.ABSORPTION,
                        6000,
                        2,
                        false,
                        false,
                        false
                    ))
                    entity.absorptionAmount = targetAbsorption
                } else {
                    entity.heal(healAmount)
                }

                playSoundLocal(
                    entity,
                    SoundEvents.CONDUIT_ACTIVATE,
                    SoundSource.PLAYERS,
                    1.2f,
                    1.5f
                )

            } else if (singularityLevel > 0) {
                target.addEffect(
                    MobEffectInstance(MobEffects.SLOWNESS, SINGULARITY_STUN_DURATION, 25, false, false)
                )
                val pullBox = target.boundingBox.inflate(SINGULARITY_PULL)
                val nearbyEntities = level.getEntitiesOfClass(LivingEntity::class.java, pullBox) {
                    it != target &&
                            it != entity &&
                            it.rootVehicle != entity.rootVehicle &&
                            it.rootVehicle != target.rootVehicle
                }
                for (pulled in nearbyEntities) {
                    val pullVector = target.position().subtract(pulled.position()).scale(SINGULARITY_PULL_FACTOR)
                    pulled.deltaMovement = pullVector
                    pulled.hurtMarked = true
                }

                level.playSound(
                    null,
                    target.x,
                    target.y,
                    target.z,
                    SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.PLAYERS,
                    1.3f,
                    0.5f)
            }


            val durabilityLoss = 1 + (chargeProgress * 4).toInt()
            stack.hurtAndBreak(durabilityLoss, entity, entity.usedItemHand)
        }

        entity.cooldowns.addCooldown(entity.useItem, COOLDOWN)

        return true
    }

    private fun isLookingAtTarget(player: Player, target: LivingEntity): Boolean {
        val look = player.lookAngle.normalize()
        val toTarget = target.eyePosition.subtract(player.eyePosition).normalize()
        val dot = look.dot(toTarget)
        return dot >= LOCK_DOT_THRESHOLD
    }

    fun getTarget(player: Player, range: Double): Entity? {
        val startPos = player.eyePosition
        val lookVec = player.lookAngle

        val searchBox = player.boundingBox.expandTowards(lookVec.scale(range)).inflate(TARGETING_SEARCH_INFLATION)

        var bestTarget: Entity? = null
        var bestScore = Double.MAX_VALUE

        for (entity in player.level().getEntities(player, searchBox) { it is LivingEntity && it != player }) {
            val dist = startPos.distanceTo(entity.eyePosition)
            if (dist > range) continue

            val toEntity = entity.eyePosition.subtract(startPos).normalize()
            val dotProduct = lookVec.dot(toEntity).coerceIn(-1.0, 1.0)
            val angle = Math.toDegrees(acos(dotProduct))

            if (angle <= TARGETING_FORGIVENESS_ANGLE) {
                if (hasClearPath(player.level(), player, entity as LivingEntity)) {

                    val score = (angle * TARGETING_ANGLE_WEIGHT) + dist

                    if (score < bestScore) {
                        bestTarget = entity
                        bestScore = score
                    }
                }
            }
        }

        return bestTarget
    }
}