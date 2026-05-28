package fr.heta__h.squ_abyssal_bloom.event.nautilus.spike

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object NautilusBlockDamageSpikeEvent {

    private const val FRONT_SHIELD_DOT_THRESHOLD = 0.5

    @SubscribeEvent
    fun onNautilusShieldBlock(event: LivingIncomingDamageEvent) {
        val entity = event.entity

        if (entity !is AbstractNautilus) return
        if (!entity.isDashing) return

        val extraItemStack = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
        if (extraItemStack.item != NautilusLayerItems.SHIELD) return

        val attacker = event.source.entity ?: return

        val toAttacker = attacker.position()
            .subtract(entity.position())
            .normalize()

        val lookVec = entity.deltaMovement.normalize()

        val dot = lookVec.dot(toAttacker)

        if (dot > FRONT_SHIELD_DOT_THRESHOLD) {
            event.isCanceled = true

            val level = entity.level()
            if (level is ServerLevel) {
                level.sendParticles(
                    ParticleTypes.CRIT,
                    entity.x, entity.y + (entity.bbHeight / 2.0), entity.z,
                    4, 0.2, 0.2, 0.2, 0.05
                )
            }

            if (attacker !is LivingEntity) return

            ModUtilities.playSoundLocal(
                attacker,
                SoundEvents.SHIELD_BLOCK.value(),
                entity.soundSource,
                1.0f,
                1.0f
            )
        }
    }
}