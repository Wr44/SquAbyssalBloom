package fr.heta__h.squ_abyssal_bloom.event.nautilus.compat

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity.EntityDynamicLightCompat
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer.Companion.NAUTILUS_LAMP
import fr.heta__h.squ_abyssal_bloom.util.ModAttachments
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.item.Items
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object NautilusLightHandler {

    const val NAUTILUS_LAMP_LIGHT_LEVEL: Int = 15
    const val NAUTILUS_LAMP_LIGHT_RADIUS = 0.8f

    private val hasDynLights: Boolean by lazy {
        ModList.get().mods.any { it.modId.contains("lambdynlights", ignoreCase = true) }
    }

    @SubscribeEvent
    fun onEntityTick(event: EntityTickEvent.Post) {
        if (!hasDynLights) return

        val entity = event.entity
        if (!entity.level().isClientSide) return

        if (entity is AbstractNautilus) {
            val extraItem = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)

            if (!extraItem.isEmpty && extraItem.`is`(NAUTILUS_LAMP)) {
                EntityDynamicLightCompat.updateLight(entity, NAUTILUS_LAMP_LIGHT_LEVEL, NAUTILUS_LAMP_LIGHT_RADIUS)
            } else {
                EntityDynamicLightCompat.removeLight(entity.id)
            }
        }
    }
}