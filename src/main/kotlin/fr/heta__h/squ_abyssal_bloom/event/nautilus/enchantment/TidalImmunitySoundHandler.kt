package fr.heta__h.squ_abyssal_bloom.event.nautilus.enchantment

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.getEnchantLevel
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.phys.AABB
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent

@EventBusSubscriber(modid = Squ_abyssal_bloom.ID, value = [Dist.CLIENT])
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
            getEnchantLevel(nautilus.getItemBySlot(EquipmentSlot.BODY), level, "tidal_immunity") > 0
        }

        if (hasProtected) event.sound = null
    }
}