package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.compat.OculusCompatDetector
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
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

    private const val FOG_START_DEEP = 0.0
    private const val FOG_END_DEEP = 12.0

    @SubscribeEvent
    fun onComputeFogColor(event: ViewportEvent.ComputeFogColor) {
        if (!ModConfig.enableAbyssFog) return

        val camera = event.camera
        if (camera.fluidInCamera != FogType.WATER) return

        val entity = camera.entity() as? LivingEntity ?: return
        if (entity.hasEffect(MobEffects.NIGHT_VISION)) return

        val level = entity.level()
        val camPos = BlockPos.containing(camera.position())

        AbyssDepthCache.refreshIfNeeded(level, camPos)
        if (!AbyssDepthCache.isLargeBody) return

        val rawFactor = AbyssDepthCache.displayedDepthFactor
        val lampInfluence = maxOf(
            ModUtilities.getRiderLampInfluence(entity),
            ModUtilities.getNautilusLampInfluence(level, camPos, 16.0, 1.0)
        )
        val effectiveFactor = rawFactor * (1.0 - lampInfluence * ModConfig.nautilusLampInfluence)

        val eased = effectiveFactor.pow(0.3)

        
        
        event.red = lerp(event.red, event.red * 0.20f, eased.toFloat())
        event.green = lerp(event.green, event.green * 0.20f, eased.toFloat())
        event.blue = lerp(event.blue, event.blue * 0.20f, eased.toFloat())
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

        AbyssDepthCache.refreshIfNeeded(level, camPos)
        if (!AbyssDepthCache.isLargeBody) return

        val rawFactor = AbyssDepthCache.displayedDepthFactor
        if (rawFactor < 0.05) return

        val lampInfluence = maxOf(
            ModUtilities.getRiderLampInfluence(entity),
            ModUtilities.getNautilusLampInfluence(level, camPos, 16.0, 1.0)
        )
        val effectiveFactor = rawFactor * (1.0 - lampInfluence * ModConfig.nautilusLampInfluence)

        val eased = effectiveFactor.pow(0.3)

        val shaderMode = OculusCompatDetector.isShadersActive()
        val intensityScale = ModConfig.fogDarknessIntensity * if (shaderMode) 0.6 else 1.0

        val fogStartDistance = FOG_START_SHALLOW - (FOG_START_SHALLOW - FOG_START_DEEP) * eased * intensityScale
        val fogEndDistanceRaw = FOG_END_SHALLOW - (FOG_END_SHALLOW - FOG_END_DEEP) * eased * intensityScale
        val fogEndDistance = fogEndDistanceRaw.coerceAtLeast(fogStartDistance + 1.0)

        event.nearPlaneDistance = fogStartDistance.toFloat()
        event.farPlaneDistance = fogEndDistance.toFloat()
    }

    private fun lerp(start: Float, end: Float, delta: Float): Float {
        return start + delta * (end - start)
    }
}