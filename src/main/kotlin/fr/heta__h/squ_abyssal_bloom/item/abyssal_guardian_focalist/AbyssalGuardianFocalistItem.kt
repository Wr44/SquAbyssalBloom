package fr.heta__h.squ_abyssal_bloom.item.abyssal_guardian_focalist

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
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
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AbyssalGuardianFocalistItem(properties: Properties) : Item(properties) {

    companion object {
        const val MAX_RANGE = 20.0
        const val MIN_CHARGE_TICKS = 40
        const val MAX_CHARGE_TICKS = 80
        const val COOLDOWN = 50
        const val BASE_DAMAGE = 3.0f
        const val ADD_DAMAGE = 5.0f

        private var lockedTargets = ConcurrentHashMap<UUID, Int>()
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = 72000

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation = ItemUseAnimation.SPYGLASS

    fun getLockedTargetId(player: Player): Int {
        return lockedTargets[player.uuid] ?: -1
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val initialTarget = getTarget(player, MAX_RANGE)

        if (initialTarget is LivingEntity) {
            lockedTargets.put(player.uuid, initialTarget.id)

            player.startUsingItem(hand)

            level.playSound(null, player.x, player.y, player.z,
                SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.0f, 1.0f)

            return InteractionResult.CONSUME
        }
        return InteractionResult.PASS
    }
    override fun onUseTick(level: Level, entity: LivingEntity, stack: ItemStack, timeLeft: Int) {
        if (entity is Player) {
            val targetId = lockedTargets.get(entity.uuid) ?: -1
            val currentTarget = level.getEntity(targetId) as? LivingEntity

            if (currentTarget == null || !currentTarget.isAlive || entity.distanceTo(currentTarget) > MAX_RANGE + 2.0) {
                if (!level.isClientSide) entity.releaseUsingItem()
                return
            }

            val duration = getUseDuration(stack, entity) - timeLeft

            if (duration >= MAX_CHARGE_TICKS) {
                if (!level.isClientSide) entity.releaseUsingItem()
                return
            }
        }
    }

    override fun releaseUsing(stack: ItemStack, level: Level, entity: LivingEntity, timeLeft: Int): Boolean {
        if (entity is Player && !level.isClientSide) {
            val ticksUsed = getUseDuration(stack, entity) - timeLeft

            val targetId = lockedTargets.remove(entity.uuid) ?: -1
            val finalTarget = level.getEntity(targetId) as? LivingEntity
            if (ticksUsed >= MIN_CHARGE_TICKS && finalTarget != null && finalTarget.isAlive) {
                val chargeProgress = Mth.clamp(
                    (ticksUsed - MIN_CHARGE_TICKS).toFloat() / (MAX_CHARGE_TICKS - MIN_CHARGE_TICKS).toFloat(),
                    0.0f, 1.0f
                )

                val finalDamage = BASE_DAMAGE + (chargeProgress * ADD_DAMAGE)

                finalTarget.hurt(level.damageSources().indirectMagic(entity, entity), finalDamage)

                level.playSound(null, finalTarget.x, finalTarget.y, finalTarget.z,
                    SoundEvents.GUARDIAN_FLOP, SoundSource.PLAYERS, 1.0f, 1.5f)

                val durabilityLoss = 1 + (chargeProgress * 4).toInt()
                stack.hurtAndBreak(durabilityLoss, entity, entity.usedItemHand)
            }

            entity.cooldowns.addCooldown(entity.useItem, COOLDOWN)
        } else if (entity is Player && level.isClientSide) {
            lockedTargets.remove(entity.uuid)
        }
        return true
    }

    fun getTarget(player: Player, range: Double): Entity? {
        val startPos = player.eyePosition
        val lookVec = player.lookAngle
        val endPos = startPos.add(lookVec.scale(range))
        val boundingBox = player.boundingBox.expandTowards(lookVec.scale(range)).inflate(1.0)

        var targetEntity: Entity? = null
        var minDistance = range

        for (entity in player.level().getEntities(player, boundingBox) { it is LivingEntity && it != player }) {
            val axisAlignedBB = entity.boundingBox.inflate(entity.pickRadius.toDouble())
            val hitResult = axisAlignedBB.clip(startPos, endPos)

            if (axisAlignedBB.contains(startPos)) return entity
            if (hitResult.isPresent) {
                val dist = startPos.distanceTo(hitResult.get())
                if (dist < minDistance) {
                    targetEntity = entity
                    minDistance = dist
                }
            }
        }
        return targetEntity
    }
}