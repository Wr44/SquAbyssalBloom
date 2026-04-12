package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.compat.ShaderCompatDetector
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
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

    private const val FOG_START_SHALLOW = 5.0f
    private const val FOG_END_SHALLOW = 40.0f

    private const val TARGET_FOG_START_DEEP = -8.0f
    private const val TARGET_FOG_END_DEEP = 12.0f

    private var lastLogTime = 0L

    @SubscribeEvent
    fun onComputeFogColor(event: ViewportEvent.ComputeFogColor) {
        if (!ModConfig.enableAbyssFog) return

        val camera = event.camera
        val entity = camera.entity() as? LivingEntity ?: return
        val level = entity.level()
        val camPos = BlockPos.containing(camera.position())

        val isUnderwater = camera.fluidInCamera == FogType.WATER
        val isAirPocket = !isUnderwater && ModUtilities.isDeepUnderwaterAirPocket(level, camPos)

        if (!isUnderwater && !isAirPocket) return
        if (entity.hasEffect(MobEffects.NIGHT_VISION)) return

        AbyssDepthCache.refreshIfNeeded(level, camPos)
        var rawFactor = AbyssDepthCache.displayedDepthFactor

        
        
        if (rawFactor <= 0.0) {
            val realDepth = ModUtilities.getDepth(level, camPos)
            rawFactor = ModUtilities.getDepthFactor(realDepth)
        }

        val currentTime = System.currentTimeMillis()
        val shouldLog = currentTime - lastLogTime > 1000L

        if (shouldLog) {
            println("=== [WaterFogColor DEBUG] ===")
            println("isAirPocket: $isAirPocket | rawFactor (Corrigé): $rawFactor")
        }

        if (rawFactor <= 0.0 && !isAirPocket) {
            if (shouldLog) lastLogTime = currentTime
            return
        }

        val safeFactor = maxOf(0.0, rawFactor)
        val depthEased = safeFactor.pow(0.3).toFloat()
        val intensityScale = ModConfig.fogDarknessIntensity.toFloat()
        val finalLerpFactor = (depthEased * intensityScale).coerceIn(0.0f, 1.0f)

        if (isAirPocket) {
            event.red = Mth.lerp(finalLerpFactor, event.red, 0.0f)
            event.green = Mth.lerp(finalLerpFactor, event.green, 0.0f)
            event.blue = Mth.lerp(finalLerpFactor, event.blue, 0.0f)
            if (shouldLog) lastLogTime = currentTime
            return
        }

        val retainedColorPercentage = ModConfig.abyssColorRetention.toFloat()
        val targetRed = event.red * retainedColorPercentage
        val targetGreen = event.green * retainedColorPercentage
        val targetBlue = event.blue * retainedColorPercentage

        event.red = Mth.lerp(finalLerpFactor, event.red, targetRed)
        event.green = Mth.lerp(finalLerpFactor, event.green, targetGreen)
        event.blue = Mth.lerp(finalLerpFactor, event.blue, targetBlue)

        if (shouldLog) lastLogTime = currentTime
    }

    @SubscribeEvent
    fun onRenderFog(event: ViewportEvent.RenderFog) {
        if (!ModConfig.enableAbyssFog) return

        val camera = event.camera
        val entity = camera.entity() as? LivingEntity ?: return
        val level = entity.level()
        val camPos = BlockPos.containing(camera.position())

        val isUnderwater = camera.fluidInCamera == FogType.WATER
        val isAirPocket = !isUnderwater && ModUtilities.isDeepUnderwaterAirPocket(level, camPos)

        if (!isUnderwater && !isAirPocket) return
        if (isAirPocket) return
        if (entity.hasEffect(MobEffects.NIGHT_VISION)) return

        AbyssDepthCache.refreshIfNeeded(level, camPos)
        var rawFactor = AbyssDepthCache.displayedDepthFactor

        
        if (rawFactor <= 0.0) {
            val realDepth = ModUtilities.getDepth(level, camPos)
            rawFactor = ModUtilities.getDepthFactor(realDepth)
        }

        val safeFactor = maxOf(0.0, rawFactor)
        val depthEased = safeFactor.pow(0.3).toFloat()

        val shaderMode = ModConfig.shaderCompatModeOverride || ShaderCompatDetector.isShadersActive()
        val intensityScale = (ModConfig.fogDarknessIntensity * if (shaderMode) 0.6 else 1.0).toFloat()

        val finalTargetStart = TARGET_FOG_START_DEEP * intensityScale
        val finalTargetEnd = TARGET_FOG_END_DEEP * intensityScale

        val baseFogStart = Mth.lerp(depthEased, FOG_START_SHALLOW, finalTargetStart)
        val baseFogEndRaw = Mth.lerp(depthEased, FOG_END_SHALLOW, finalTargetEnd)
        val baseFogEnd = baseFogEndRaw.coerceAtLeast(baseFogStart + 2.0f)

        val lampInfluence = maxOf(
            ModUtilities.getRiderLampInfluence(entity),
            ModUtilities.getNautilusLampInfluence(level, camPos, 32.0, 1.5)
        )
        val lampPower = (lampInfluence * ModConfig.nautilusLampInfluence).toFloat() * depthEased
        val nearPlane = baseFogStart + lampPower * ModConfig.lampNearPlaneMultiplier.toFloat()
        val farPlane = baseFogEnd + lampPower * ModConfig.lampFarPlaneMultiplier.toFloat()

        val fogData = event.fogData
        fogData.environmentalStart = nearPlane
        fogData.environmentalEnd = farPlane
        fogData.renderDistanceStart = nearPlane
        fogData.renderDistanceEnd = farPlane
        fogData.skyEnd = farPlane
        fogData.cloudEnd = farPlane
    }
}