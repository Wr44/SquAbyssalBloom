package fr.heta__h.squ_abyssal_bloom.config.server.types

import net.minecraft.network.RegistryFriendlyByteBuf
import net.neoforged.neoforge.common.ModConfigSpec

class IntOption(key: String, default: Int, val min: Int, val max: Int, comment: String) :
    ConfigOption<Int>(key, default, comment) {

    override fun build(builder: ModConfigSpec.Builder): ModConfigSpec.ConfigValue<Int> =
        builder.comment(comment).defineInRange(key, default, min, max)

    override fun write(buf: RegistryFriendlyByteBuf, value: Int) {
        buf.writeVarInt(value)
    }

    override fun read(buf: RegistryFriendlyByteBuf): Int = buf.readVarInt()
}