package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity

import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer.Companion.NAUTILUS_LAMP
import fr.heta__h.squ_abyssal_bloom.util.ModAttachments
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus

class NautilusLampLuminance : EntityLuminance {

    companion object {
        val INSTANCE = NautilusLampLuminance()
        val TYPE = EntityLuminance.Type.registerSimple(
            Identifier.fromNamespaceAndPath("squ_abyssal_bloom", "nautilus_lamp"),
            INSTANCE
        )
    }

    override fun type(): EntityLuminance.Type = TYPE

    override fun getLuminance(itemLightSourceManager: ItemLightSourceManager, entity: Entity): Int {
        if (entity is AbstractNautilus) {
            val extraItem = entity.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)

            if (!extraItem.isEmpty && extraItem.`is`(NAUTILUS_LAMP)) {
                return 15
            }
        }
        return 0
    }
}