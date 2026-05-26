package fr.heta__h.squ_abyssal_bloom.util.nautilus

import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.event.conduit.ConduitDomainHandler
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
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
    val mount: AbstractNautilus
) : Slot(container, index, x, y) {

    internal var listenerPlayedDeactivate = false

    override fun mayPlace(stack: ItemStack): Boolean = ModUtilities.isNautilusExtraEquipment(stack)

    override fun getMaxStackSize(): Int = 1

    override fun mayPickup(player: Player): Boolean {
        if (this.item.`is`(Items.CHEST)) {
            val items = mount.getData(ModAttachments.NAUTILUS_CHEST_ITEMS)
            if (items.any { !it.isEmpty }) return false
        }
        return super.mayPickup(player)
    }


    override fun onTake(player: Player, stack: ItemStack) {
        super.onTake(player, stack)

        if (stack.item == NautilusLayer.CONDUIT) {
            if (listenerPlayedDeactivate) {
                listenerPlayedDeactivate = false
                return
            }
            if (!mount.level().isClientSide
                && player.isInWater
                && mount.distanceTo(player) <= ConduitDomainHandler.PORTABLE_RADIUS
            ) {
                mount.playSound(SoundEvents.CONDUIT_DEACTIVATE, 1.0f, 1.0f)
                ConduitDomainHandler.markConduitEquipmentChange(player.uuid)
            }
        }

        listenerPlayedDeactivate = false
    }
}