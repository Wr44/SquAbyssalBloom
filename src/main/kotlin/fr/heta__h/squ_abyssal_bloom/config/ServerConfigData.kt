package fr.heta__h.squ_abyssal_bloom.config

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

data class ServerConfigData(
    val strictBarnacleSpawning: Boolean,
) {
    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ServerConfigData> = StreamCodec.composite(
            ByteBufCodecs.BOOL, ServerConfigData::strictBarnacleSpawning,
            ::ServerConfigData
        )

        fun fromSpec(): ServerConfigData = ServerConfigData(
            strictBarnacleSpawning = ModServerConfig.STRICT_BARNACLE_SPAWNING.get(),
        )
    }

    fun applyToSpec() {
        ModServerConfig.STRICT_BARNACLE_SPAWNING.set(strictBarnacleSpawning)
        ModServerConfig.SPEC.save()
    }
}