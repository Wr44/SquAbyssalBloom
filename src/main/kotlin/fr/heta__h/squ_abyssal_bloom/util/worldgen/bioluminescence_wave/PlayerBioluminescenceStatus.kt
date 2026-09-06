package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult

enum class PlayerBioluminescenceStatus {
    WAITING,
    ARMED;

    companion object {
        @JvmField
        val CODEC: Codec<PlayerBioluminescenceStatus> = Codec.STRING.comapFlatMap(
            { serializedName ->
                entries.firstOrNull { status -> status.name.equals(serializedName, ignoreCase = true) }
                    ?.let { status -> DataResult.success(status) }
                    ?: DataResult.error { "Unknown player bioluminescence status: $serializedName" }
            },
            { status -> status.name.lowercase() }
        )
    }
}
