package fr.heta__h.squ_abyssal_bloom.config.server

import fr.heta__h.squ_abyssal_bloom.config.server.types.ConfigOption
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

data class ServerConfigData(val values: Map<String, Any>) {

    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ServerConfigData> = StreamCodec.of(
            { buf, data ->
                for (opt in ModServerConfig.options) {
                    @Suppress("UNCHECKED_CAST")
                    (opt as ConfigOption<Any>).write(buf, data.values.getValue(opt.key))
                }
            },
            { buf ->
                val map = HashMap<String, Any>()
                for (opt in ModServerConfig.options) {
                    map[opt.key] = opt.read(buf)
                }
                ServerConfigData(map)
            }
        )

        fun fromSpec(): ServerConfigData {
            val map = HashMap<String, Any>()
            for (opt in ModServerConfig.options) {
                map[opt.key] = opt.spec.get()
            }
            return ServerConfigData(map)
        }
    }

    fun applyToSpec() {
        for (opt in ModServerConfig.options) {
            @Suppress("UNCHECKED_CAST")
            (opt as ConfigOption<Any>).spec.set(values.getValue(opt.key))
        }
        ModServerConfig.SPEC.save()
    }
}