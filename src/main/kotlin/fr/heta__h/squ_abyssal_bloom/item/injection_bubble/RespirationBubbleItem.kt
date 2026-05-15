package fr.heta__h.squ_abyssal_bloom.item.injection_bubble

import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import kotlin.math.cos
import kotlin.math.sin

class RespirationBubbleItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        if (!player.isUnderWater) return InteractionResult.FAIL
        if (player.airSupply >= player.maxAirSupply) return InteractionResult.FAIL

        if (level.isClientSide) return InteractionResult.SUCCESS

        val serverLevel = level as ServerLevel

        player.airSupply = player.maxAirSupply

        level.playSound(
            null, player.x, player.y, player.z,
            ModSounds.RESPIRATION_BUBBLE, SoundSource.PLAYERS, 0.2f, 1f
        )

        val headY = player.y + player.eyeHeight.toDouble()

        repeat(24) {
            val theta = level.random.nextDouble() * 2 * Math.PI
            val phi = level.random.nextDouble() * Math.PI
            val r = 0.35 + level.random.nextDouble() * 0.25
            val sx = sin(phi) * cos(theta) * r
            val sy = cos(phi) * r
            val sz = sin(phi) * sin(theta) * r
            serverLevel.sendParticles(
                ParticleTypes.BUBBLE_POP,
                player.x + sx, headY + sy, player.z + sz,
                1, 0.0, 0.0, 0.0, 0.0
            )
        }

        repeat(20) {
            val ox = (level.random.nextDouble() - 0.5) * 0.35
            val oz = (level.random.nextDouble() - 0.5) * 0.35
            val oy = level.random.nextDouble() * player.bbHeight.toDouble()
            serverLevel.sendParticles(
                ParticleTypes.BUBBLE_COLUMN_UP,
                player.x + ox, player.y + oy, player.z + oz,
                1, 0.0, 0.05, 0.0, 0.08
            )
        }

        repeat(12) {
            val ox = (level.random.nextDouble() - 0.5) * 1.8
            val oy = level.random.nextDouble() * 2.2
            val oz = (level.random.nextDouble() - 0.5) * 1.8
            serverLevel.sendParticles(
                ParticleTypes.UNDERWATER,
                player.x + ox, player.y + oy, player.z + oz,
                1, 0.0, 0.0, 0.0, 0.0
            )
        }

        if (!player.abilities.instabuild) {
            player.getItemInHand(hand).shrink(1)
        }

        return InteractionResult.CONSUME
    }
}