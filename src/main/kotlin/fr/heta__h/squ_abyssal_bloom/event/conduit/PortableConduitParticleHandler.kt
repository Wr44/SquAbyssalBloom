package fr.heta__h.squ_abyssal_bloom.event.conduit

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import net.minecraft.client.Minecraft
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import kotlin.math.cos
import kotlin.math.sin

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object PortableConduitParticleHandler {

    private const val AXIS_X_MULT = 1.0
    private const val AXIS_Z_MULT = -1.0

    private const val TRAVEL_TIME_TICKS = 35f

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val level = minecraft.level ?: return

        if (level.gameTime % 3 != 0L) return

        level.getEntitiesOfClass(AbstractNautilus::class.java, player.boundingBox.inflate(32.0))
            .forEach { nautilus ->
                if (nautilus.getData(ModAttachments.NAUTILUS_EXTRA_SLOT).item != NautilusLayer.CONDUIT) return@forEach

                val animTime = nautilus.tickCount.toFloat()

                val originX = nautilus.x
                val originY = nautilus.y + nautilus.bbHeight / 2.0
                val originZ = nautilus.z

                val futureMobX = nautilus.x + (nautilus.deltaMovement.x * TRAVEL_TIME_TICKS)
                val futureMobY = nautilus.y + (nautilus.deltaMovement.y * TRAVEL_TIME_TICKS)
                val futureMobZ = nautilus.z + (nautilus.deltaMovement.z * TRAVEL_TIME_TICKS)

                val futureAnimTime = animTime + TRAVEL_TIME_TICKS
                val futureOrbitAngle = futureAnimTime * NautilusLayer.ORBIT_SPEED
                val futureVerticalOffset = sin((futureOrbitAngle * NautilusLayer.VERTICAL_PERIODS).toDouble()) * NautilusLayer.VERTICAL_AMPLITUDE

                val worldOffsetX = cos(futureOrbitAngle.toDouble()) * NautilusLayer.ORBIT_RADIUS * AXIS_X_MULT
                val worldOffsetZ = sin(futureOrbitAngle.toDouble()) * NautilusLayer.ORBIT_RADIUS * AXIS_Z_MULT

                val vanillaDipCompensation = 1.0f

                val targetX = futureMobX + worldOffsetX
                val targetY = futureMobY + NautilusLayer.BASE_HEIGHT + futureVerticalOffset + vanillaDipCompensation
                val targetZ = futureMobZ + worldOffsetZ

                val random = level.random
                val offsetX = (originX - targetX) + (random.nextFloat() - 0.5f) * 0.5f
                val offsetY = (originY - targetY) + (random.nextFloat() - 0.5f) * 0.5f
                val offsetZ = (originZ - targetZ) + (random.nextFloat() - 0.5f) * 0.5f

                level.addParticle(
                    ParticleTypes.NAUTILUS,
                    targetX, targetY, targetZ,
                    offsetX, offsetY, offsetZ
                )
            }
    }
}