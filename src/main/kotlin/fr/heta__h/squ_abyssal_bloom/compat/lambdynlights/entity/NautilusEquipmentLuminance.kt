package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity

import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.ModDynamicLightLevels
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus

class NautilusEquipmentLuminance : EntityLuminance {

    companion object {
        val INSTANCE = NautilusEquipmentLuminance()

        val TYPE = EntityLuminance.Type.registerSimple(
            Identifier.fromNamespaceAndPath("squ_abyssal_bloom", "nautilus_lamp"),
            INSTANCE
        )
    }

    override fun type(): EntityLuminance.Type = TYPE

    override fun getLuminance(itemLightSourceManager: ItemLightSourceManager, entity: Entity): Int {
        if (!ModConfig.enableDynamicLights) return 0
        if (entity !is AbstractNautilus) return 0

        val extraItem = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
        if (extraItem.isEmpty) return 0

        if (extraItem.`is`(NautilusLayerItems.LAMP) || extraItem.`is`(NautilusLayerItems.CONDUIT)) {
            return scaled(ModDynamicLightLevels.NAUTILUS_LAMP)
        }

        if (extraItem.getOrDefault(ModDataComponents.PLANKTON_LUMINESCENCE.get(), false)) {
            return scaled(ModDynamicLightLevels.PLANKTON_LUMINESCENCE)
        }

        return 0
    }

    private fun scaled(luminance: Int): Int =
        (luminance * ModConfig.dynamicLightsNautilusIntensity).toInt().coerceIn(0, 15)
}
