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

        val id = entity.id
        val pos = Vector3d(entity.x, entity.y + (entity.bbHeight * yOffsetRatio), entity.z)

        val existingLight = activeLights[id]

        if (existingLight != null) {
            existingLight.startPoint = pos
            existingLight.endPoint = pos
            existingLight.luminance = luminance
        } else {
            val newLight = object : LineLightBehavior(pos, pos, luminance) {
                override fun isRemoved(): Boolean = entity.isRemoved
            }
            activeLights[id] = newLight
            addDynamicLight(newLight)
        }
    }

    fun removeLight(entityId: Int) {
        if (!isInitialized) return

        val light = activeLights.remove(entityId)
        if (light != null) {
            removeDynamicLight(light)
        }
    }
}