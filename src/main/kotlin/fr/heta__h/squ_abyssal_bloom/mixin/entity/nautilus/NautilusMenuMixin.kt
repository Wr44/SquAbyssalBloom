package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusEquipmentSlot
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.event.conduit.ConduitDomainHandler
import fr.heta__h.squ_abyssal_bloom.mixin.enable.AbstractContainerMenuAccessor
import fr.heta__h.squ_abyssal_bloom.mixin.enable.AbstractNautilusAccessor
import fr.heta__h.squ_abyssal_bloom.network.nautilus_chest.SyncNautilusExtraSlotPayload
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusLayerItems
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusReopenQueue
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.NautilusInventoryMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.network.PacketDistributor
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(NautilusInventoryMenu::class)
abstract class NautilusMenuMixin {

    @Unique
    private var abyssalChestContainer: SimpleContainer? = null

    @Inject(method = ["<init>"], at = [At("RETURN")])
    private fun initNautilusChestMenu(
        containerId: Int,
        playerInventory: Inventory,
        mountContainer: Container,
        mount: AbstractNautilus,
        inventoryColumns: Int,
        ci: CallbackInfo
    ) {
        val player = playerInventory.player
        val savedItem: ItemStack = mount.getData(ModAttachments.NAUTILUS_EXTRA_SLOT) ?: ItemStack.EMPTY

        var hadChestState = savedItem.`is`(Items.CHEST)

        val wrappedContainer = object : SimpleContainer(1) {
            override fun setChanged() {
                super.setChanged()
                if (!mount.isAlive) return

                val newItem: ItemStack = getItem(0)
                val oldItem = mount.getData(ModAttachments.NAUTILUS_EXTRA_SLOT) ?: ItemStack.EMPTY

                val hasChestNow = newItem.`is`(Items.CHEST)
                val hadChest = hadChestState
                hadChestState = hasChestNow

                mount.setData(ModAttachments.NAUTILUS_EXTRA_SLOT, newItem)

                if (mount.level().isClientSide) return

                val hasConduitNow = newItem.item == NautilusLayerItems.CONDUIT
                val hadConduit = oldItem.item == NautilusLayerItems.CONDUIT

                if (hasConduitNow != hadConduit
                    && player.isInWater
                    && mount.distanceTo(player) <= ConduitDomainHandler.PORTABLE_RADIUS
                ) {
                    val sound = if (hasConduitNow) SoundEvents.CONDUIT_ACTIVATE else SoundEvents.CONDUIT_DEACTIVATE
                    mount.playSound(sound, 1.0f, 1.0f)
                    ConduitDomainHandler.markConduitEquipmentChange(player.uuid)
                }

                if (!newItem.isEmpty && !ItemStack.matches(oldItem, newItem)) {
                    mount.playSound(SoundEvents.ARMOR_EQUIP_NAUTILUS.value(), 1.0f, 1.5f)
                }

                PacketDistributor.sendToPlayersTrackingEntityAndSelf(mount, SyncNautilusExtraSlotPayload(mount.id, newItem))

                if (hadChest != hasChestNow && player is ServerPlayer) {
                    NautilusReopenQueue.schedule {
                        if (!mount.isAlive || !player.isAlive || player.hasDisconnected()) return@schedule
                        val carried: ItemStack = player.containerMenu.carried ?: ItemStack.EMPTY
                        try {
                            player.containerMenu.carried = ItemStack.EMPTY
                            (mount as AbstractNautilusAccessor).invokeCreateInventory()
                            mount.openCustomInventoryScreen(player)
                            player.containerMenu.carried = carried
                            player.containerMenu.broadcastFullState()
                        } catch (e: Exception) {
                            if (!carried.isEmpty) player.inventory.placeItemBackInInventory(carried)
                        }
                    }
                }
            }
        }
        wrappedContainer.setItem(0, savedItem)

        val extraSlot = NautilusEquipmentSlot(wrappedContainer, 0, 8, 54, mount)

        val menuAccessor = this as AbstractContainerMenuAccessor
        menuAccessor.invokeAddSlot(extraSlot)

        if (inventoryColumns > 0) {
            val chest = object : SimpleContainer(15) {
                override fun setChanged() {
                    super.setChanged()
                    if (!mount.isAlive || mount.level().isClientSide) return
                    val items = (0 until 15).map { getItem(it).copy() }
                    mount.setData(ModAttachments.NAUTILUS_CHEST_ITEMS, items)
                }
            }

            this.abyssalChestContainer = chest

            val savedItems = mount.getData(ModAttachments.NAUTILUS_CHEST_ITEMS) ?: emptyList()
            savedItems.forEachIndexed { i, stack -> if (i < 15) chest.setItem(i, stack ?: ItemStack.EMPTY) }

            for (row in 0 until 3) {
                for (col in 0 until inventoryColumns) {
                    menuAccessor.invokeAddSlot(Slot(chest, col + row * inventoryColumns, 80 + col * 18, 18 + row * 18))
                }
            }
        }
    }
}