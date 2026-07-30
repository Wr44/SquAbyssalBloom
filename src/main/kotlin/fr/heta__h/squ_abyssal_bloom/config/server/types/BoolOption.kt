package fr.heta__h.squ_abyssal_bloom.config.server.types

import net.minecraft.network.RegistryFriendlyByteBuf
import net.neoforged.neoforge.common.ModConfigSpec

class BoolOption(key: String, default: Boolean, comment: String) :
    ConfigOption<Boolean>(key, default, comment) {

    override fun build(builder: ModConfigSpec.Builder): ModConfigSpec.ConfigValue<Boolean> =
        builder.comment(comment).define(key, default)

    override fun write(buf: RegistryFriendlyByteBuf, value: Boolean) {
        buf.writeBoolean(value)
    }

    override fun read(buf: RegistryFriendlyByteBuf): Boolean = buf.readBoolean()
}
