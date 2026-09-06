package fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.entity

import dev.lambdaurora.lambdynlights.api.behavior.LineLightBehavior
import fr.heta__h.squ_abyssal_bloom.compat.lambdynlights.AbstractDynamicLightCompat
import net.minecraft.world.entity.Entity
import org.joml.Vector3d
import java.util.concurrent.ConcurrentHashMap

object EntityDynamicLightCompat : AbstractDynamicLightCompat() {

    private val activeLights = ConcurrentHashMap<Int, LineLightBehavior>()


    fun updateLight(entity: Entity, luminance: Int, yOffsetRatio: Float = 0.5f) {
        if (!isInitialized) return

        val pos = Vector3d(entity.x, entity.y + (entity.bbHeight * yOffsetRatio), entity.z)
        updateLineLight(activeLights, entity.id, pos, pos, luminance) { entity.isRemoved }
    }

    fun removeLight(entityId: Int) {
        if (!isInitialized) return

        removeLineLight(activeLights, entityId)
    }

    fun clearLights() {
        if (!isInitialized) {
            activeLights.clear()
            return
        }

        clearLineLights(activeLights)
    }
}
