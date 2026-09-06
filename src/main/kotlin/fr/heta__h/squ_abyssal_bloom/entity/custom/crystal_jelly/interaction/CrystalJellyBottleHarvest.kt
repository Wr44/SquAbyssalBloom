package fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.interaction

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.data_component.ModDataComponents
import fr.heta__h.squ_abyssal_bloom.entity.custom.crystal_jelly.CrystalJellyEntity
import fr.heta__h.squ_abyssal_bloom.item.ModItems
import fr.heta__h.squ_abyssal_bloom.item.plankton_bottle.PartialPlanktonBottleItem
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import fr.heta__h.squ_abyssal_bloom.sound.ModSounds
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object CrystalJellyBottleHarvest {

    private const val PARTICLE_COUNT = 12
    private const val PARTICLE_SPREAD_HORIZONTAL = 0.25
    private const val PARTICLE_SPREAD_VERTICAL = 0.15
    private const val PARTICLE_SPEED = 0.02
    private const val BASE_PITCH = 1.4f
    private const val PITCH_PER_FILL = 0.1f
    private const val MAX_PITCH = 2.0f

    fun tryHarvest(jelly: CrystalJellyEntity, player: Player, hand: InteractionHand): InteractionResult {
        val held = player.getItemInHand(hand)
        val currentFill = when {
            held.`is`(Items.GLASS_BOTTLE) -> 0
            held.`is`(ModItems.PARTIAL_PLANKTON_BOTTLE.get()) -> held.getOrDefault(ModDataComponents.PLANKTON_FILL.get(), 1)
            else -> return InteractionResult.PASS
        }

        if (!ModServerConfig.CRYSTAL_JELLY_BOTTLE_HARVEST_ENABLED.get()) return InteractionResult.PASS
        if (jelly.isFadingOut) return InteractionResult.PASS

        val level = jelly.level()
        if (level.isClientSide) return InteractionResult.SUCCESS
        val serverLevel = level as? ServerLevel ?: return InteractionResult.PASS

        if (serverLevel.gameTime < jelly.bottleReadyGameTime) return InteractionResult.FAIL

        jelly.bottleReadyGameTime = serverLevel.gameTime + ModServerConfig.CRYSTAL_JELLY_BOTTLE_HARVEST_COOLDOWN.get()

        val required = ModServerConfig.CRYSTAL_JELLY_BOTTLE_FILLS_REQUIRED.get()
        val newFill = currentFill + 1
        val complete = newFill >= required

        if (currentFill > 0) {
            if (complete) {
                player.setItemInHand(hand, ItemStack(ModItems.PLANKTON_BOTTLE.get()))
            } else {
                PartialPlanktonBottleItem.applyFill(held, newFill, required)
            }
        } else {
            val filled = if (complete) {
                ItemStack(ModItems.PLANKTON_BOTTLE.get())
            } else {
                PartialPlanktonBottleItem.create(newFill, required)
            }
            held.consume(1, player)
            if (!player.inventory.add(filled)) player.drop(filled, false)
        }

        serverLevel.playSound(
            null, jelly.x, jelly.y, jelly.z,
            ModSounds.BOTTLE_FILL_BIOLUMINESCENT.get(), SoundSource.PLAYERS,
            0.8f, (BASE_PITCH + PITCH_PER_FILL * newFill).coerceAtMost(MAX_PITCH)
        )
        serverLevel.sendParticles(
            ModParticles.CRYSTAL_JELLY_GLOW.get(),
            jelly.x, jelly.y + 0.2, jelly.z,
            PARTICLE_COUNT, PARTICLE_SPREAD_HORIZONTAL, PARTICLE_SPREAD_VERTICAL, PARTICLE_SPREAD_HORIZONTAL, PARTICLE_SPEED
        )

        jelly.reactToHarvest(player)
        return InteractionResult.SUCCESS_SERVER
    }
}
