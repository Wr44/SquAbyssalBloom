package fr.heta__h.squ_abyssal_bloom.fog

import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.fog.FogData
import net.minecraft.client.renderer.fog.environment.FogEnvironment
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.level.material.FogType
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.AbyssDepthCache
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.core.BlockPos
import kotlin.math.pow

class AbyssalAirFogEnvironment : FogEnvironment() {

    override fun isApplicable(fogType: FogType?, entity: Entity): Boolean {
        if (!ModConfig.enableAbyssFog) return false
        if (fogType != FogType.ATMOSPHERIC) return false
        val living = entity as? LivingEntity ?: return false
        if (living.hasEffect(MobEffects.NIGHT_VISION)) return false

        val camPos = BlockPos.containing(entity.eyePosition)
        if (!ModUtilities.isDeepUnderwaterAirPocket(entity.level(), camPos)) return false

        return true
    }

    override fun setupFog(
        fogData: FogData,
        camera: Camera,
        level: ClientLevel,
        renderDistance: Float,
        deltaTracker: DeltaTracker
    ) {
        val rawFactor = AbyssDepthCache.displayedDepthFactor
        val depthEased = rawFactor.pow(0.3).toFloat()

        val intensityScale = ModConfig.fogDarknessIntensity.toFloat()
        val targetStart = -8.0f * intensityScale
        val targetEnd = 12.0f * intensityScale

        val fogStart = net.minecraft.util.Mth.lerp(depthEased, 5.0f, targetStart)
        val fogEndRaw = net.minecraft.util.Mth.lerp(depthEased, 40.0f, targetEnd)
        val fogEnd = fogEndRaw.coerceAtLeast(fogStart + 2.0f)

        fogData.environmentalStart = fogStart
        fogData.environmentalEnd = fogEnd
        fogData.skyEnd = fogEnd
        fogData.cloudEnd = fogEnd

        AbyssalFogOverride.active = rawFactor > 0.0
        AbyssalFogOverride.envStart = fogStart
        AbyssalFogOverride.envEnd = fogEnd
        AbyssalFogOverride.depthEased = depthEased
    }

    override fun providesColor(): Boolean = false
}