package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights

import dev.lambdaurora.lambdynlights.api.behavior.LineLightBehavior
import net.neoforged.fml.ModList
import org.joml.Vector3d

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

    protected fun updateLineLight(
        map: MutableMap<Int, LineLightBehavior>,
        id: Int,
        start: Vector3d,
        end: Vector3d,
        luminance: Int,
        removedCheck: () -> Boolean
    ) {
        val existing = map[id]

        if (existing != null) {
            existing.startPoint = start
            existing.endPoint = end
            existing.luminance = luminance
        } else {
            val newLight = object : LineLightBehavior(start, end, luminance) {
                override fun isRemoved(): Boolean = removedCheck()
            }
            map[id] = newLight
            addDynamicLight(newLight)
        }
    }

    protected fun removeLineLight(map: MutableMap<Int, LineLightBehavior>, id: Int) {
        val light = map.remove(id)
        if (light != null) {
            removeDynamicLight(light)
        }
    }
}