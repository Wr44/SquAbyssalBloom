package fr.heta__h.squ_abyssal_bloom.compat

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.neoforged.fml.ModList

object ShaderCompatDetector {
    private val hasOculus by lazy { ModList.get().isLoaded("oculus") }
    private val hasIris by lazy { ModList.get().isLoaded("iris") }

    private val checkShaderProvider: (() -> Boolean)? by lazy {
        when {
            hasIris -> {
                try {
                    val apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi")
                    val getInstanceMethod = apiClass.getMethod("getInstance")
                    val isShaderPackInUseMethod = apiClass.getMethod("isShaderPackInUse")

                    val instance = getInstanceMethod.invoke(null)
                    return@lazy { isShaderPackInUseMethod.invoke(instance) as? Boolean ?: false }
                } catch (e: Exception) { null }
            }
            hasOculus -> {
                try {
                    val apiClass = Class.forName("net.irisshaders.oculus.api.OculusApi")
                    val isShaderPackInUseMethod = apiClass.getMethod("isShaderPackInUse")

                    return@lazy { isShaderPackInUseMethod.invoke(null) as? Boolean ?: false }
                } catch (e: Exception) { null }
            }
            else -> null
        }
    }

    fun isShadersActive(): Boolean {
        if (ModConfig.shaderCompatModeOverride) return true

        return checkShaderProvider?.invoke() ?: false
    }
}
