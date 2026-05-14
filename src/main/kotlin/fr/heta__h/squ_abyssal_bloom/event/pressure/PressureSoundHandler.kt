package fr.heta__h.squ_abyssal_bloom.event.pressure

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.AABB
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object PressureSoundHandler {

    private val DROWNING_HURT_SOUNDS = setOf(
        Identifier.withDefaultNamespace("entity.player.hurt_drown"),
        Identifier.withDefaultNamespace("entity.generic.hurt")
    )

    @SubscribeEvent
    fun onPlaySound(event: PlaySoundEvent) {
        val sound = event.sound ?: return
        if (sound.identifier !in DROWNING_HURT_SOUNDS) return

        val level = Minecraft.getInstance().level ?: return
        val x = sound.x
        val y = sound.y
        val z = sound.z
        val searchBox = AABB(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0)

        val hasEntityWithPressure = level.getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity::class.java, searchBox
        ).any { it.hasEffect(ModEffects.PRESSURE) && it.airSupply > 0 }

        if (hasEntityWithPressure) event.sound = null
    }
}