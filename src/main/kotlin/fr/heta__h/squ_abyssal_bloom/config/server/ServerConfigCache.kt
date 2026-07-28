package fr.heta__h.squ_abyssal_bloom.config.server

import fr.heta__h.squ_abyssal_bloom.config.server.types.ConfigOption
import net.neoforged.fml.loading.FMLEnvironment
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist

object ServerConfigCache {
    @Volatile private var values: Map<String, Any> = emptyMap()

    fun update(data: ServerConfigData) {
        values = HashMap(data.values)
    }

    fun clear() {
        values = emptyMap()
    }

    fun syncFromSpec() {
        update(ServerConfigData.fromSpec())
    }

    fun toData(): ServerConfigData = ServerConfigData(HashMap(values))

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> current(option: ConfigOption<T>): T =
        (values[option.key] as? T) ?: option.default

    fun <T : Any> set(option: ConfigOption<T>, value: T) {
        values = HashMap(values).also { it[option.key] = value }
    }

    fun isSingleplayer(): Boolean {
        return if (FMLEnvironment.getDist() == Dist.CLIENT) {
            val mc = Minecraft.getInstance()
            mc.isLocalServer || mc.connection == null
        } else {
            false
        }
    }
}
