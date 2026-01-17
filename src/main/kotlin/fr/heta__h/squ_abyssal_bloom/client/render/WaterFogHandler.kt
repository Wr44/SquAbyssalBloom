package fr.heta__h.squ_abyssal_bloom.client.render

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.config.ModConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids.WATER
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ViewportEvent
import kotlin.math.exp
import kotlin.math.pow


@EventBusSubscriber(
    modid = Squ_abyssal_bloom.ID,
    value = [Dist.CLIENT]
)
object WaterFogHandler {

     @SubscribeEvent
     fun onRenderFog(event: ViewportEvent.ComputeFogColor) {
         if (!ModConfig.enableAbyssFog) return

         val camera = event.camera
         val entity = camera.entity

         val living = entity as? LivingEntity ?: return
         if (!living.isInWater || living.hasEffect(MobEffects.NIGHT_VISION)) return
         val level = living.level()
         val camPos = BlockPos.containing(camera.position)

         if (!isLargeWaterBody(level, camPos)) return
         val surfaceY = findWaterSurface(level, camPos)

         val waterDepth = surfaceY + 1 - camera.position.y

         val depthNorm = (waterDepth / 70.0).coerceIn(0.0, 1.0)
         val depthFactor = depthNorm.pow(2.0)

         val r = (0.04 * (1 - depthFactor)).toFloat().coerceIn(0f, 1f)
         val g = (0.08 * (1 - depthFactor)).toFloat().coerceIn(0f, 1f)
         val b = (0.2  * (1 - depthFactor)).toFloat().coerceIn(0f, 1f)

         event.red = r
         event.green = g
         event.blue = b

     }

    private fun findWaterSurface(level: Level, start: BlockPos): Int {
        val pos = start.mutable()
        while (pos.y < level.maxY) {
            if (!level.getBlockState(pos).fluidState.`is`(WATER)) {
                return pos.y - 1
            }

            pos.move(Direction.UP)
        }
        return start.y
    }

    private fun isLargeWaterBody(level: Level, center: BlockPos): Boolean {
        val radius = 4
        val y = center.y
        var count = 0
        val size = (radius * 2 + 1) * (radius * 2 + 1)

        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val pos = BlockPos(center.x + dx, y, center.z + dz)
                if (level.getBlockState(pos).fluidState.`is`(WATER)) {
                    count++
                }
            }
        }

        return count >= size * 0.75
    }

}