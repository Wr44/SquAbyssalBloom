package fr.heta__h.squ_abyssal_bloom.network.nautilus_chest

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.util.nautilus.NautilusMouseHelper
import fr.heta__h.squ_abyssal_bloom.attachment.ModAttachments
import fr.heta__h.squ_abyssal_bloom.mixin.enable.AbstractContainerScreenAccessor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.NautilusInventoryScreen
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.network.handling.IPayloadContext

data class SyncNautilusExtraSlotPayload(val entityId: Int, val stack: ItemStack) : CustomPacketPayload {

    companion object {
        val ID = CustomPacketPayload.Type<SyncNautilusExtraSlotPayload>(
            Identifier.fromNamespaceAndPath(Squ_abyssal_bloom.ID, "sync_nautilus_extra_slot")
        )

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncNautilusExtraSlotPayload> = StreamCodec.composite(
            ByteBufCodecs.INT, SyncNautilusExtraSlotPayload::entityId,
            ItemStack.OPTIONAL_STREAM_CODEC, SyncNautilusExtraSlotPayload::stack,
            ::SyncNautilusExtraSlotPayload
        )
    }

    override fun type(): CustomPacketPayload.Type<SyncNautilusExtraSlotPayload> = ID

    fun handle(context: IPayloadContext) {
        context.enqueueWork {
            val level = context.player().level()
            val entity = level.getEntity(entityId) ?: return@enqueueWork

            val mc = Minecraft.getInstance()
            val screen = mc.screen as? NautilusInventoryScreen
            val hadChest = screen?.menu?.slots?.any { it.x == 80 && it.y == 18 } ?: false
            val hasChestNow = stack.`is`(Items.CHEST)

            if (hadChest != hasChestNow) {
                val oldTopPos = (screen as? AbstractContainerScreenAccessor)?.getTopPos() ?: 0
                NautilusMouseHelper.save(mc.mouseHandler, oldTopPos)
            }

            entity.setData(ModAttachments.NAUTILUS_EXTRA_SLOT, stack)
        }
    }
}