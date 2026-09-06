package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity

import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.ModDynamicLightLevels
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.CrystalJellyEntity
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity

class CrystalJellyLuminance : EntityLuminance {

    companion object {
        val INSTANCE = CrystalJellyLuminance()
        val TYPE = EntityLuminance.Type.registerSimple(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "crystal_jelly"),
            INSTANCE
        )
    }

    override fun type(): EntityLuminance.Type = TYPE

    override fun getLuminance(itemLightSourceManager: ItemLightSourceManager, entity: Entity): Int {
        if (!ModConfig.enableDynamicLights) return 0
        if (entity !is CrystalJellyEntity) return 0

        return Math.round(ModDynamicLightLevels.CRYSTAL_JELLY * entity.glowStrength * entity.fadeOutAlpha)
    }
}
