package fr.heta__h.squ_abyssal_bloom.mixin.render

import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.AbyssDepthCache
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.OptionInstance
import net.minecraft.client.renderer.LightTexture
import net.minecraft.core.BlockPos
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.material.FogType
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect
import kotlin.math.exp

@Mixin(LightTexture::class)
open class LightTextureMixin {

    @Redirect(
        method = ["updateLightTexture"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
            ordinal = 2
        )
    )
    private fun modifyGamma(instance: OptionInstance<Double>): Any {
        val originalValue = instance.get() as Double

        if (!ModConfig.enableAbyssFog) return originalValue

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        if (camera.fluidInCamera != FogType.WATER) return originalValue

        val entity = camera.entity() as? LivingEntity ?: return originalValue
        if (entity.hasEffect(MobEffects.NIGHT_VISION)) return originalValue

        val level = entity.level()
        val camPos = BlockPos.containing(camera.position())

        AbyssDepthCache.refreshIfNeeded(level, camPos)
        if (!AbyssDepthCache.isLargeBody) return originalValue

        val rawFactor = AbyssDepthCache.displayedDepthFactor
        if (rawFactor <= 0.0) return originalValue

        val lampInfluence = maxOf(
            ModUtilities.getRiderLampInfluence(entity),
            ModUtilities.getNautilusLampInfluence(level, camPos, 16.0, 1.0)
        )
        val effectiveFactor = rawFactor * (1.0 - lampInfluence * ModConfig.nautilusLampInfluence)

        
        
        
        val factor = exp(-effectiveFactor * (0.9 * ModConfig.lightDimmingStrength))

        return originalValue * factor
    }

}
