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

        repeat(25) {
            val offsetX = (level.random.nextDouble() - 0.5) * 0.6
            val offsetY = level.random.nextDouble() * 1.5
            val offsetZ = (level.random.nextDouble() - 0.5) * 0.6

            serverLevel.sendParticles(
                ParticleTypes.BUBBLE,
                player.x + offsetX,
                player.y + offsetY,
                player.z + offsetZ,
                1,
                0.0, 0.15, 0.0,
                0.0
            )
        }

        if (!player.abilities.instabuild) {
            player.getItemInHand(hand).shrink(1)
        }

        return InteractionResult.CONSUME
    }
}