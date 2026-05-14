package fr.heta__h.squ_abyssal_bloom.util.nautilus

import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.Container
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class NautilusEquipmentSlot(
    container: Container,
    index: Int,
    x: Int,
    y: Int,
    private val mount: AbstractNautilus
) : Slot(container, index, x, y) {

    override fun mayPlace(stack: ItemStack): Boolean = ModUtilities.isNautilusExtraEquipment(stack)

    override fun getMaxStackSize(): Int = 1

    override fun mayPickup(player: Player): Boolean {
        if (this.item.`is`(Items.CHEST)) {
            val items = mount.getData(ModAttachments.NAUTILUS_CHEST_ITEMS)
            if (items.any { !it.isEmpty }) return false
        }
        return super.mayPickup(player)
    }

    override fun set(stack: ItemStack) {
        val wasEmpty = !this.hasItem()
        super.set(stack)
        if (wasEmpty && !stack.isEmpty) {
            mount.level().playSound(
                null,
                mount.blockPosition(),
                SoundEvents.ARMOR_EQUIP_NAUTILUS.value(),
                mount.soundSource,
                1.0f,
                1.5f
            )
        }
    }
}