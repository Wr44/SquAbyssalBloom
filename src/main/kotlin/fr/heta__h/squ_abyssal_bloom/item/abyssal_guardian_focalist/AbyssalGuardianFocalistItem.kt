package fr.heta__h.squ_abyssal_bloom.item.abyssal_guardian_focalist

import fr.heta__h.squ_abyssal_bloom.network.abyssal_guardian_focalist.FocalistBeamSyncPayload
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.level.Level
import net.neoforged.neoforge.network.PacketDistributor
import java.lang.Math.toRadians
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos

class AbyssalGuardianFocalistItem(properties: Properties) : Item(properties) {

    companion object {
        const val MAX_RANGE = 27.5
        const val MIN_CHARGE_TICKS = 20
        const val MAX_CHARGE_TICKS = 80
        const val COOLDOWN = 50
        const val BASE_DAMAGE = 7.5f
        const val ADD_DAMAGE = 10f
        const val MAX_LOCK_ANGLE = 45.0

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

            level.playSound(
                target,
                target.x, target.y, target.z,
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

        if (!target.isAlive) {
            if (!level.isClientSide) entity.releaseUsingItem()
            return
        }

        if (entity.distanceTo(target) > MAX_RANGE + 2.0) {
            if (!level.isClientSide) entity.releaseUsingItem()
            return
        }

        if (!isLookingAtTarget(entity, target)) {
            if (!level.isClientSide) entity.releaseUsingItem()
            return
        }

        val duration = getUseDuration(stack, entity) - timeLeft

        if (duration >= MAX_CHARGE_TICKS) {
            if (!level.isClientSide) entity.releaseUsingItem()
        }

        if (duration % 20 == 0) {
            level.playSound(
                null,
                entity.x, entity.y, entity.z,
                SoundEvents.GUARDIAN_ATTACK,
                SoundSource.PLAYERS,
                1f,
                1f + (duration / MAX_CHARGE_TICKS.toFloat()) * 0.5f
            )

            level.playSound(
                target,
                target.x, target.y, target.z,
                SoundEvents.GUARDIAN_ATTACK,
                SoundSource.PLAYERS,
                1f,
                1f + (duration / MAX_CHARGE_TICKS.toFloat()) * 0.5f
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
            val chargeProgress = Mth.clamp(
                (ticksUsed - MIN_CHARGE_TICKS).toFloat() /
                        (MAX_CHARGE_TICKS - MIN_CHARGE_TICKS).toFloat(),
                0.0f,
                1.0f
            )

            val damage = BASE_DAMAGE + (chargeProgress * ADD_DAMAGE)

            target.hurt(
                level.damageSources().indirectMagic(entity, entity),
                damage
            )

            level.playSound(
                null,
                target.x, target.y, target.z,
                SoundEvents.GUARDIAN_FLOP,
                SoundSource.PLAYERS,
                1.0f,
                1.5f
            )

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
        val endPos = startPos.add(lookVec.scale(range))

        val boundingBox =
            player.boundingBox.expandTowards(lookVec.scale(range)).inflate(1.0)

        var targetEntity: Entity? = null
        var minDistance = range

        for (entity in player.level().getEntities(player, boundingBox) {
            it is LivingEntity && it != player
        }) {
            val aabb = entity.boundingBox.inflate(entity.pickRadius.toDouble())
            val hit = aabb.clip(startPos, endPos)

            if (aabb.contains(startPos)) return entity

            if (hit.isPresent) {
                val dist = startPos.distanceTo(hit.get())
                if (dist < minDistance) {
                    targetEntity = entity
                    minDistance = dist
                }
            }
        }

        return targetEntity
    }
}