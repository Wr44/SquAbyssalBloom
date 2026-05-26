package fr.heta__h.squ_abyssal_bloom.mixin.entity.nautilus

import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusEquipmentSlot
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.entity.render_layer.nautilus.NautilusLayer
import fr.heta__h.squ_abyssal_bloom.event.conduit.ConduitDomainHandler
import fr.heta__h.squ_abyssal_bloom.mixin.enable.AbstractContainerMenuAccessor
import fr.heta__h.squ_abyssal_bloom.mixin.enable.AbstractNautilusAccessor
import fr.heta__h.squ_abyssal_bloom.network.nautilus_chest.SyncNautilusExtraSlotPayload
import fr.heta__h.squ_abyssal_bloom.util.ModUtilities
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusReopenQueue
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
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

        val extraSlotContainer = SimpleContainer(1)
        extraSlotContainer.setItem(0, savedItem)

        var hadChestState = savedItem.`is`(Items.CHEST)

        val extraSlot = NautilusEquipmentSlot(extraSlotContainer, 0, 8, 54, mount)

        extraSlotContainer.addListener { container ->
            if (mount.isAlive) {
                val newItem: ItemStack = container.getItem(0) ?: ItemStack.EMPTY
                val oldItem = mount.getData(ModAttachments.NAUTILUS_EXTRA_SLOT) ?: ItemStack.EMPTY

                val hasChestNow = newItem.`is`(Items.CHEST)
                val hadChest = hadChestState
                hadChestState = hasChestNow

                mount.setData(ModAttachments.NAUTILUS_EXTRA_SLOT, newItem)

                if (!mount.level().isClientSide) {
                    val hasConduitNow = newItem.item == NautilusLayer.CONDUIT
                    val hadConduit = oldItem.item == NautilusLayer.CONDUIT

                    if (hasConduitNow != hadConduit
                        && player.isInWater
                        && mount.distanceTo(player) <= ConduitDomainHandler.PORTABLE_RADIUS
                    ) {
                        val sound = if (hasConduitNow) SoundEvents.CONDUIT_ACTIVATE else SoundEvents.CONDUIT_DEACTIVATE
                        mount.playSound(sound, 1.0f, 1.0f)
                        ConduitDomainHandler.markConduitEquipmentChange(player.uuid)
                        if (!hasConduitNow) extraSlot.listenerPlayedDeactivate = true
                    }

                    if (!newItem.isEmpty && !ItemStack.matches(oldItem, newItem)) {
                        mount.playSound(SoundEvents.ARMOR_EQUIP_NAUTILUS.value(), 1.0f, 1.5f)
                    }

                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(mount, SyncNautilusExtraSlotPayload(mount.id, newItem))

                    if (hadChest != hasChestNow) {
                        if (player is ServerPlayer) {
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
            }
        }

        val menuAccessor = this as AbstractContainerMenuAccessor
        menuAccessor.invokeAddSlot(extraSlot)

        if (inventoryColumns > 0) {
            val chest = SimpleContainer(15)
            this.abyssalChestContainer = chest

            val savedItems = mount.getData(ModAttachments.NAUTILUS_CHEST_ITEMS) ?: emptyList()
            savedItems.forEachIndexed { i, stack -> if (i < 15) chest.setItem(i, stack ?: ItemStack.EMPTY) }

            chest.addListener { container ->
                if (mount.isAlive && !mount.level().isClientSide) {
                    val items = (0 until 15).map { (container.getItem(it) ?: ItemStack.EMPTY).copy() }
                    mount.setData(ModAttachments.NAUTILUS_CHEST_ITEMS, items)
                }
            }

            for (row in 0 until 3) {
                for (col in 0 until inventoryColumns) {
                    val localIndex = col + row * inventoryColumns
                    menuAccessor.invokeAddSlot(Slot(chest, localIndex, 80 + col * 18, 18 + row * 18))
                }
            }
        }
    }
}