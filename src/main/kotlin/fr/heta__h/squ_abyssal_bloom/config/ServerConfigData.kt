package fr.heta__h.squ_abyssal_bloom.config

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

data class ServerConfigData(
    val strictBarnacleSpawning: Boolean,
    val shallowDeepBoundary: Float,
    val deepAbyssalBoundary: Float,
) {
    companion object {
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ServerConfigData> = StreamCodec.composite(
            ByteBufCodecs.BOOL, ServerConfigData::strictBarnacleSpawning,
            ByteBufCodecs.FLOAT, ServerConfigData::shallowDeepBoundary,
            ByteBufCodecs.FLOAT, ServerConfigData::deepAbyssalBoundary,
            ::ServerConfigData
        )

        fun fromSpec(): ServerConfigData = ServerConfigData(
            strictBarnacleSpawning = ModServerConfig.STRICT_BARNACLE_SPAWNING.get(),
            shallowDeepBoundary = ModServerConfig.SHALLOW_DEEP_BOUNDARY.get().toFloat(),
            deepAbyssalBoundary = ModServerConfig.DEEP_ABYSSAL_BOUNDARY.get().toFloat(),
        )
    }

    fun applyToSpec() {
        ModServerConfig.STRICT_BARNACLE_SPAWNING.set(strictBarnacleSpawning)
        ModServerConfig.SHALLOW_DEEP_BOUNDARY.set(shallowDeepBoundary.toDouble())
        ModServerConfig.DEEP_ABYSSAL_BOUNDARY.set(deepAbyssalBoundary.toDouble())
        ModServerConfig.SPEC.save()
    }
}