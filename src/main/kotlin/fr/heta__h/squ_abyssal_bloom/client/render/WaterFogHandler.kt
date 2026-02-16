package fr.heta__h.squ_abyssal_bloom.client.render

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.isLargeBodyWater
import net.minecraft.core.BlockPos
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.material.FogType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ViewportEvent
import kotlin.math.pow

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
object WaterFogHandler {

    private const val FOG_START_SHALLOW = 5.0
    private const val FOG_END_SHALLOW = 40.0
    private const val FOG_START_DEEP = 0.1
    private const val FOG_END_DEEP = 8.0


    @SubscribeEvent
    fun onComputeFogColor(event: ViewportEvent.ComputeFogColor) {
        if (!ModConfig.enableAbyssFog) return

        val camera = event.camera
        if (camera.fluidInCamera != FogType.WATER) return

        val entity = camera.entity() as? LivingEntity ?: return
        if (entity.hasEffect(MobEffects.NIGHT_VISION)) return

        val level = entity.level()
        val camPos = BlockPos.containing(camera.position())

        if (!isLargeBodyWater(level, camPos, 5)) return

        var depthFactor = ModUtilities.smoothDepthGaussian(level, camPos, sigma = 1.5)
        depthFactor = depthFactor.coerceAtLeast(0.0)

        val eased = depthFactor.pow(0.5)

        event.red = lerp(event.red, 0.0f, eased.toFloat())
        event.green = lerp(event.green, 0.0f, eased.toFloat())
        event.blue = lerp(event.blue, 0.0f, eased.toFloat())
    }

    @SubscribeEvent
    fun onRenderFog(event: ViewportEvent.RenderFog) {
        if (!ModConfig.enableAbyssFog) return

        val camera = event.camera
        if (camera.fluidInCamera != FogType.WATER) return

        val entity = camera.entity() as? LivingEntity ?: return
        if (entity.hasEffect(MobEffects.NIGHT_VISION)) return

        val level = entity.level()
        val camPos = BlockPos.containing(camera.position())

        if (!isLargeBodyWater(level, camPos, 5)) return

        val depthFactor = ModUtilities.smoothDepthGaussian(level, camPos, sigma = 1.5).coerceAtLeast(0.0)
        if (depthFactor < 0.05) return

        val eased = depthFactor.pow(0.5)

        val fogStartDistance = FOG_START_SHALLOW - (FOG_START_SHALLOW - FOG_START_DEEP) * eased
        val fogEndDistance = FOG_END_SHALLOW - (FOG_END_SHALLOW - FOG_END_DEEP) * eased

        event.nearPlaneDistance = fogStartDistance.toFloat()
        event.farPlaneDistance = fogEndDistance.toFloat()
    }

    private fun lerp(start: Float, end: Float, delta: Float): Float {
        return start + delta * (end - start)
    }

}