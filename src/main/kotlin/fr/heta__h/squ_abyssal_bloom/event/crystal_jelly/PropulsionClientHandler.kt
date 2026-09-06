package fr.heta__h.squ_abyssal_bloom.event.crystal_jelly

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.effect.ModEffects
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.sounds.SoundSource
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.PlayerTickEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object PropulsionClientHandler {

    private const val CHARGE_DRAG = 0.92
    private const val BURST_PARTICLE_COUNT = 10
    private const val CHARGE_PARTICLE_COUNT = 2
    private const val PARTICLE_SPREAD = 0.18
    private const val WAKE_OFFSET = 0.6
    private const val FULL_STRENGTH_BURST_SPEED = 0.5

    @SubscribeEvent
    fun onClientPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity as? LocalPlayer ?: return
        if (player !== Minecraft.getInstance().player) return

        val effect = player.getEffect(ModEffects.PROPULSION) ?: return
        if (!player.isInWater || player.isPassenger || player.isSpectator) return
        if (!player.input.hasForwardImpulse()) return

        val cycleTicks = ModServerConfig.PROPULSION_CYCLE_TICKS.get()
        val chargeTicks = ModServerConfig.PROPULSION_CHARGE_TICKS.get().coerceAtMost(cycleTicks - 1)
        val cycleTick = (player.tickCount % cycleTicks)

        if (cycleTick < chargeTicks) {
            player.deltaMovement = player.deltaMovement.multiply(CHARGE_DRAG, 1.0, CHARGE_DRAG)
            emitWake(player, CHARGE_PARTICLE_COUNT)
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

        player.deltaMovement = player.deltaMovement.add(player.lookAngle.scale(speed))

        if (releaseTick != 0) return

        emitWake(player, BURST_PARTICLE_COUNT)
        ModUtilities.playWaterPropulsion(
            player,
            ModSounds.PROPULSION_BURST.get(),
            peakSpeed / FULL_STRENGTH_BURST_SPEED,
            SoundSource.PLAYERS
        )
    }

    private fun emitWake(player: LocalPlayer, count: Int) {
        val wake = player.position().subtract(player.lookAngle.scale(WAKE_OFFSET))
        val level = player.level()
        repeat(count) {
            level.addParticle(
                ModParticles.CRYSTAL_JELLY_GLOW.get(),
                wake.x + (level.random.nextDouble() - 0.5) * PARTICLE_SPREAD,
                wake.y + (player.bbHeight / 2.0) + (level.random.nextDouble() - 0.5) * PARTICLE_SPREAD,
                wake.z + (level.random.nextDouble() - 0.5) * PARTICLE_SPREAD,
                0.0, 0.0, 0.0
            )
        }
    }
}
