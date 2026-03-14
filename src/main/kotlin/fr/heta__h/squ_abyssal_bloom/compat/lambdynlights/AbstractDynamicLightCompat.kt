package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights

import dev.lambdaurora.lambdynlights.api.behavior.LineLightBehavior

abstract class AbstractDynamicLightCompat {


    companion object {
        private var managerInstance: Any? = null
        private var addMethod: java.lang.reflect.Method? = null
        private var removeMethod: java.lang.reflect.Method? = null

        @JvmStatic
        var isInitialized = false
            private set


        init {
            try {
                val mainClass = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights")
                val getMethod = mainClass.getMethod("get")
                val mainInstance = getMethod.invoke(null)

                val managerMethod = mainClass.getMethod("dynamicLightBehaviorManager")
                managerInstance = managerMethod.invoke(mainInstance)

                val methods = managerInstance?.javaClass?.methods
                addMethod = methods?.firstOrNull { it.name == "add" && it.parameterCount == 1 }
                removeMethod = methods?.firstOrNull { it.name == "remove" && it.parameterCount == 1 }

                if (addMethod != null && removeMethod != null) {
                    isInitialized = true
                }
            } catch (e: Exception) {
            }
        }
    }

    protected fun addDynamicLight(light: LineLightBehavior) {
        if (isInitialized) {
            try { addMethod?.invoke(managerInstance, light) } catch (e: Exception) {}
        }
    }

    protected fun removeDynamicLight(light: LineLightBehavior) {
        if (isInitialized) {
            try { removeMethod?.invoke(managerInstance, light) } catch (e: Exception) {}
        }
    }
}