package fr.heta__h.squ_abyssal_bloom.attachment

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries

object ModAttachments {
    val ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SquAbyssalBloom.ID)

    
    val HAS_GUARDIAN_SPIKES = ATTACHMENTS.register("has_spikes") { ->
        AttachmentType.builder { -> false }
            .sync(ByteBufCodecs.BOOL)
            .build()
    }

    
    val NAUTILUS_EXTRA_SLOT = ATTACHMENTS.register("nautilus_extra_slot") { ->
        AttachmentType.builder { -> ItemStack.EMPTY }
            .serialize(ItemStack.OPTIONAL_CODEC.fieldOf("item"))
            .sync(ItemStack.OPTIONAL_STREAM_CODEC)
            .build()

    }

    
    val NAUTILUS_CHEST_ITEMS = ATTACHMENTS.register("nautilus_chest_items") { ->
        AttachmentType.builder { -> emptyList<ItemStack>() }
            .serialize(ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items"))
            .sync(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()))
            .build()
    }

    fun register(bus: IEventBus) {
        ATTACHMENTS.register(bus)
    }

}