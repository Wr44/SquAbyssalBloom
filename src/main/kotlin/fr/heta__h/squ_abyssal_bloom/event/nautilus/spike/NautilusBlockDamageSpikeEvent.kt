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

    @SubscribeEvent
    fun onNautilusShieldBlock(event: LivingIncomingDamageEvent) {
        val victim = event.entity
        val nautilus = victim as? AbstractNautilus ?: victim.vehicle as? AbstractNautilus ?: return

        val extraItemStack = nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
        if (extraItemStack.item != NautilusLayerItems.SHIELD) return

        if (!nautilus.isDashing && nautilus.level().gameTime >= nautilus.getData(ModAttachments.NAUTILUS_CHARGE_GRACE)) return

        val attacker = event.source.entity ?: return
        if (attacker === nautilus || nautilus.hasPassenger(attacker)) return

        event.isCanceled = true

        val level = nautilus.level()
        if (level is ServerLevel) {
            level.sendParticles(
                ParticleTypes.CRIT,
                nautilus.x, nautilus.y + (nautilus.bbHeight / 2.0), nautilus.z,
                4, 0.2, 0.2, 0.2, 0.05
            )
        }

        if (attacker !is LivingEntity) return

        ModUtilities.playSoundLocal(
            attacker,
            SoundEvents.SHIELD_BLOCK.value(),
            nautilus.soundSource,
            1.0f,
            1.0f
        )
    }
}
