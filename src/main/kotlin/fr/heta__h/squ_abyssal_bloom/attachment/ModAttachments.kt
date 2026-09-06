package fr.heta__h.squ_abyssal_bloom.attachment

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.Optional
import java.util.UUID

object ModAttachments {
    val ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SquAbyssalBloom.ID)

    // RENDER LAYER
    val HAS_GUARDIAN_SPIKES = ATTACHMENTS.register("has_spikes") { ->
        AttachmentType.builder { -> false }
            .sync(ByteBufCodecs.BOOL)
            .build()
    }

    // MOB INVENTORY
    val NAUTILUS_EXTRA_SLOT = ATTACHMENTS.register("nautilus_extra_slot") { ->
        AttachmentType.builder { -> ItemStack.EMPTY }
            .serialize(ItemStack.OPTIONAL_CODEC.fieldOf("item"))
            .sync(ItemStack.OPTIONAL_STREAM_CODEC)
            .build()

    }

    // NAUTILUS CHEST INVENTORY
    val NAUTILUS_CHEST_ITEMS = ATTACHMENTS.register("nautilus_chest_items") { ->
        AttachmentType.builder { -> emptyList<ItemStack>() }
            .serialize(ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items"))
            .sync(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()))
            .build()
    }

    // BIOLUMINESCENT WAVE OWNERSHIP
    val CRYSTAL_JELLY_WAVE_ID = ATTACHMENTS.register("crystal_jelly_wave_id") { ->
        AttachmentType.builder { -> Optional.empty<UUID>() }
            .serialize(UUIDUtil.CODEC.optionalFieldOf("wave_id"))
            .build()
    }

    // NAUTILUS CHARGE INVULNERABILITY
    val NAUTILUS_CHARGE_GRACE = ATTACHMENTS.register("nautilus_charge_grace") { ->
        AttachmentType.builder { -> 0L }.build()
    }

    // NAUTILUS SPIKE RECOIL
    val NAUTILUS_RECOIL = ATTACHMENTS.register("nautilus_recoil") { ->
        AttachmentType.builder { -> Vec3.ZERO }.build()
    }

    fun register(bus: IEventBus) {
        ATTACHMENTS.register(bus)
    }

}