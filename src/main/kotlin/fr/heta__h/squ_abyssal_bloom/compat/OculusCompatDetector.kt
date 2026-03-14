package fr.heta__h.squ_abyssal_bloom.compat

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.neoforged.fml.ModList

object OculusCompatDetector {
    private val isOculusPresent by lazy {
        ModList.get().mods.any { it.modId == "oculus" }
    }
    private var checkShaderMethod: java.lang.reflect.Method? = null

    init {
        try {
            val cls = Class.forName("net.irisshaders.oculus.api.OculusApi")
            checkShaderMethod = cls.getMethod("isShaderPackInUse")
        } catch (e: Exception) {  }
    }

    fun isShadersActive(): Boolean {
        if (ModConfig.shaderCompatModeOverride) return true
        if (!isOculusPresent) return false
        return try {
            checkShaderMethod?.invoke(null) as? Boolean ?: false
        } catch (e: Exception) { false }
    }
}
