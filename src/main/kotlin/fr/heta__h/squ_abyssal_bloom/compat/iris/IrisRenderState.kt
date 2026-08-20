package fr.heta__h.squ_abyssal_bloom.compat.iris

import fr.heta__h.squ_abyssal_bloom.compat.ModCompat
import net.irisshaders.iris.api.v0.IrisApi

object IrisRenderState {

    var shaderPackInUse: Boolean = false
        private set

    var renderingShadowPass: Boolean = false
        private set

    fun refresh() {
        if (!ModCompat.hasIris) {
            shaderPackInUse = false
            renderingShadowPass = false
            return
        }
        try {
            val api = IrisApi.getInstance()
            shaderPackInUse = api.isShaderPackInUse
            renderingShadowPass = shaderPackInUse && api.isRenderingShadowPass
        } catch (t: Throwable) {
            shaderPackInUse = false
            renderingShadowPass = false
        }
    }
}
