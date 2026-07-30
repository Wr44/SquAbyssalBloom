package fr.heta__h.squ_abyssal_bloom.config.server.types

import net.minecraft.network.RegistryFriendlyByteBuf
import net.neoforged.neoforge.common.ModConfigSpec

class StringListOption(key: String, default: List<String>, comment: String) :
    ConfigOption<List<String>>(key, default, comment) {

    @Suppress("DEPRECATION")
    override fun build(builder: ModConfigSpec.Builder): ModConfigSpec.ConfigValue<List<String>> =
        builder.comment(comment).defineListAllowEmpty(key, default) { it is String }

    override fun write(buf: RegistryFriendlyByteBuf, value: List<String>) {
        buf.writeVarInt(value.size)
        for (entry in value) buf.writeUtf(entry)
    }

    override fun read(buf: RegistryFriendlyByteBuf): List<String> {
        val size = buf.readVarInt()
        return (0 until size).map { buf.readUtf() }
    }
}
