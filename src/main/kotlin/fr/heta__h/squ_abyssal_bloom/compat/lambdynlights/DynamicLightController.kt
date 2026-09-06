package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights

import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.abyssal_guardian_focalist.GuardianBeamDynamicLightCompat
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity.EntityDynamicLightCompat
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.minecraft.client.Minecraft

object DynamicLightController {

    fun setEnabled(enabled: Boolean) {
        if (ModConfig.enableDynamicLights == enabled) return

        ModConfig.enableDynamicLights = enabled
        if (!enabled) {
            BioluminescentZoneDynamicLights.clear()
            GuardianBeamDynamicLightCompat.clearBeamLights()
            EntityDynamicLightCompat.clearLights()
        }
        Minecraft.getInstance().reloadResourcePacks()
    }
}
