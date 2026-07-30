package fr.heta__h.squ_abyssal_bloom.event.nautilus.enchantment

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.block.ModBlocks
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getEnchantLevel
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent
import net.neoforged.neoforge.event.entity.EntityMountEvent

@EventBusSubscriber(modid = SquAbyssalBloom.ID, value = [Dist.CLIENT])
object TidalImmunitySoundHandler {

    private val BUBBLE_SOUND_IDENTIFIERS = setOf(
        Identifier.withDefaultNamespace("block.bubble_column.whirlpool_ambient"),
        Identifier.withDefaultNamespace("block.bubble_column.whirlpool_inside"),
        Identifier.withDefaultNamespace("block.bubble_column.upwards_ambient"),
        Identifier.withDefaultNamespace("block.bubble_column.upwards_inside")
    )

    @SubscribeEvent
    fun onPlaySound(event: PlaySoundEvent) {
        val sound = event.sound ?: return
        if (sound.identifier !in BUBBLE_SOUND_IDENTIFIERS) return

        val level = Minecraft.getInstance().level ?: return
        val x = sound.x
        val y = sound.y
        val z = sound.z
        val searchBox = AABB(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0)

        val hasProtected = level.getEntitiesOfClass(AbstractNautilus::class.java, searchBox).any { nautilus ->
            nautilus.controllingPassenger != null &&
                    getEnchantLevel(nautilus.getItemBySlot(EquipmentSlot.BODY), level, "tidal_immunity") > 0
        }

        if (hasProtected) event.sound = null
    }

    @SubscribeEvent
    fun onDismount(event: EntityMountEvent) {
        if (event.isMounting) return
        val entity = event.entityMounting
        val vehicle = event.entityBeingMounted

        if (vehicle !is AbstractNautilus) return
        if (entity !is Player) return

        val level = entity.level()
        if (!level.isClientSide) return

        val hasTidal = getEnchantLevel(vehicle.getItemBySlot(EquipmentSlot.BODY), level, "tidal_immunity") > 0
        if (!hasTidal) return

        val pos = vehicle.position()
        val searchBox = AABB(pos.x - 1.0, pos.y - 1.0, pos.z - 1.0, pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)
        val inBubbleColumn = level.getBlockStates(searchBox).anyMatch { state ->
            state.`is`(Blocks.BUBBLE_COLUMN) || state.`is`(ModBlocks.BRINE_BUBBLE_COLUMN.get())
        }

        if (inBubbleColumn) {
            Minecraft.getInstance().soundManager.play(
                SimpleSoundInstance.forUI(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1f, 2f)
            )
        }
    }

}
