package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity

import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.entity.custom.bubble.BubbleProjectile
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity

class LuminescentBubbleLuminance : EntityLuminance {

    companion object {
        private const val LUMINANCE = 12

        val INSTANCE = LuminescentBubbleLuminance()
        val TYPE = EntityLuminance.Type.registerSimple(
            Identifier.fromNamespaceAndPath("squ_abyssal_bloom", "luminescent_bubble"),
            INSTANCE
        )
    }

    override fun type(): EntityLuminance.Type = TYPE

    override fun getLuminance(itemLightSourceManager: ItemLightSourceManager, entity: Entity): Int {
        if (!ModConfig.enableDynamicLights) return 0
        if (entity !is BubbleProjectile || !entity.isLuminescent) return 0
        return (LUMINANCE * ModConfig.dynamicLightsNautilusIntensity).toInt().coerceIn(0, 15)
    }
}
