package fr.heta__h.squ_abyssal_bloom.config.server.types

import fr.heta__h.squ_abyssal_bloom.config.server.ServerConfigCache
import net.minecraft.network.RegistryFriendlyByteBuf
import net.neoforged.neoforge.common.ModConfigSpec

sealed class ConfigOption<T : Any>(
    val key: String,
    val default: T,
    val comment: String
) {
    lateinit var spec: ModConfigSpec.ConfigValue<T>
        internal set

    abstract fun build(builder: ModConfigSpec.Builder): ModConfigSpec.ConfigValue<T>
    abstract fun write(buf: RegistryFriendlyByteBuf, value: T)
    abstract fun read(buf: RegistryFriendlyByteBuf): T

    fun get(): T = if (ServerConfigCache.isSingleplayer()) spec.get() else ServerConfigCache.current(this)
}
