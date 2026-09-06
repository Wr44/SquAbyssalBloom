package fr.heta__h.squ_abyssal_bloom.event.crystal_jelly

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID)
object PropulsionEffectHandler {

    private const val CHARGE_DRAG = 0.92
    private const val PARTICLE_COUNT = 6
    private const val PARTICLE_SPREAD = 0.2
    private const val PARTICLE_SPEED = 0.01
    private const val WAKE_OFFSET = 0.6
    private const val FULL_STRENGTH_BURST_SPEED = 0.5

    @SubscribeEvent
    fun onEntityTick(event: EntityTickEvent.Post) {
        val entity = event.entity as? LivingEntity ?: return
        if (entity is Player) return
        val level = entity.level() as? ServerLevel ?: return

        val effect = entity.getEffect(ModEffects.PROPULSION) ?: return
        if (!entity.isInWater || entity.isPassenger) return

        val cycleTicks = ModServerConfig.PROPULSION_CYCLE_TICKS.get()
        val chargeTicks = ModServerConfig.PROPULSION_CHARGE_TICKS.get().coerceAtMost(cycleTicks - 1)
        val cycleTick = entity.tickCount % cycleTicks

        if (cycleTick < chargeTicks) {
            entity.deltaMovement = entity.deltaMovement.multiply(CHARGE_DRAG, 1.0, CHARGE_DRAG)
            return
        }

        val releaseTicks = cycleTicks - chargeTicks

        val releaseTick = cycleTick - chargeTicks

        val peakSpeed = ModServerConfig.PROPULSION_BURST_SPEED.get() *
            (1.0 + effect.amplifier * ModServerConfig.PROPULSION_AMPLIFIER_SCALE.get())

        val speed = ModUtilities.decayingImpulseSpeed(
            releaseTick.toFloat(),
            releaseTicks.toFloat(),
            peakSpeed.toFloat(),
            ModServerConfig.PROPULSION_SPEED_DECAY.get().toFloat()
        ).toDouble()

        entity.deltaMovement = entity.deltaMovement.add(entity.lookAngle.scale(speed))
        entity.hurtMarked = true

        if (releaseTick != 0) return

        val wake = entity.position().subtract(entity.lookAngle.scale(WAKE_OFFSET))

        level.sendParticles(
            ModParticles.CRYSTAL_JELLY_GLOW.get(),
            wake.x, wake.y + (entity.bbHeight / 2.0), wake.z,
            PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPEED
        )

        ModUtilities.playWaterPropulsion(
            entity,
            ModSounds.PROPULSION_BURST.get(),
            peakSpeed / FULL_STRENGTH_BURST_SPEED,
            SoundSource.NEUTRAL
        )
    }
}
