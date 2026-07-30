package fr.heta__h.squ_abyssal_bloom.config.server.types

import net.minecraft.network.RegistryFriendlyByteBuf
import net.neoforged.neoforge.common.ModConfigSpec

class DoubleOption(key: String, default: Double, val min: Double, val max: Double, comment: String) :
    ConfigOption<Double>(key, default, comment) {

    override fun build(builder: ModConfigSpec.Builder): ModConfigSpec.ConfigValue<Double> =
        builder.comment(comment).defineInRange(key, default, min, max)

    override fun write(buf: RegistryFriendlyByteBuf, value: Double) {
        buf.writeDouble(value)
    }

    override fun read(buf: RegistryFriendlyByteBuf): Double = buf.readDouble()
}