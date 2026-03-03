package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import fr.heta__h.squ_abyssal_bloom.util.ModAttachments
import fr.heta__h.squ_abyssal_bloom.mixin.`interface`.AbstractContainerMenuAccessor
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities.playSoundLocal
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.NautilusInventoryMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(NautilusInventoryMenu::class)
abstract class NautilusMenuMixin {

    @Inject(method = ["<init>"], at = [At("RETURN")])
    private fun addExtraNautilusSlot(
        containerId: Int,
        playerInventory: Inventory,
        mountContainer: Container,
        mount: AbstractNautilus,
        inventoryColumns: Int,
        ci: CallbackInfo
    ) {
        val savedItem: ItemStack = mount.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
        val player = playerInventory.player

        val extraSlotContainer: SimpleContainer = object : SimpleContainer(1) {
            override fun setChanged() {
                super.setChanged()

                val currentSaved = mount.getData(ModAttachments.NAUTILUS_EXTRA_SLOT)
                val newItem = this.getItem(0)

                if (currentSaved.isEmpty && !newItem.isEmpty) {
                    playSoundLocal(
                        player,
                        SoundEvents.ARMOR_EQUIP_NAUTILUS.value(),
                        mount.soundSource,
                        1.0f,
                        1.5f
                    )
                }

                mount.setData(ModAttachments.NAUTILUS_EXTRA_SLOT, newItem)
            }
        }
        extraSlotContainer.setItem(0, savedItem)

        (this as AbstractContainerMenuAccessor).invokeAddSlot(object : Slot(extraSlotContainer, 0, 8, 54) {
            override fun mayPlace(stack: ItemStack): Boolean = ModUtilities.isNautilusExtraEquipment(stack)
            override fun getMaxStackSize(): Int = 1
        })
    }
}