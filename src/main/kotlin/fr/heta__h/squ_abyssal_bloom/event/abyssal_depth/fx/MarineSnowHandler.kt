package fr.heta__h.squ_abyssal_bloom.event.abyssal_depth.fx

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import fr.heta__h.squ_abyssal_bloom.util.cache.AbyssDepthCache
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.material.FogType
import net.minecraft.world.level.material.Fluids
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object MarineSnowHandler {

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        if (!ModConfig.enableAbyssFog) return

        val mc = Minecraft.getInstance()
        if (mc.isPaused) return

        val camera = mc.gameRenderer.mainCamera
        if (camera.fluidInCamera != FogType.WATER) return

        val level = mc.level ?: return
        val camPos = BlockPos.containing(camera.position())

        AbyssDepthCache.refreshIfNeeded(level, camPos)

        val depthFactor = AbyssDepthCache.displayedDepthFactor
        if (depthFactor < 0.05) return

        val count = kotlin.math.ceil(
            ModConfig.maxMarinSnowParticles * depthFactor * ModConfig.marineSnowDensity / 20.0
        ).toInt()

        val range = ModConfig.marineSnowVisibilityRange.toDouble()
        val camVec = camera.position()

        repeat(count) {
            val r = sqrt(level.random.nextDouble()) * range
            val theta = level.random.nextDouble() * 2.0 * PI
            val x = camVec.x + r * cos(theta)
            val z = camVec.z + r * sin(theta)
            val y = camVec.y + (level.random.nextDouble() * 2 - 1) * range * 0.5
            if (!level.getFluidState(BlockPos.containing(x, y, z)).`is`(Fluids.WATER)) return@repeat
            level.addParticle(ModParticles.MARINE_SNOW.get(), x, y, z, 0.0, -0.002, 0.0)
        }
    }
}
