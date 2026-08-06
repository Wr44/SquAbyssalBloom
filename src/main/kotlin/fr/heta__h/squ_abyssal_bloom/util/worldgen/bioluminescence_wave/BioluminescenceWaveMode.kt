package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult

enum class BioluminescenceWaveMode {
    NORMAL,
    TOTAL_NIGHT;

    companion object {
        @JvmField
        val CODEC: Codec<BioluminescenceWaveMode> = Codec.STRING.comapFlatMap(
            { serializedName ->
                entries.firstOrNull { mode -> mode.name.equals(serializedName, ignoreCase = true) }
                    ?.let { mode -> DataResult.success(mode) }
                    ?: DataResult.error { "Unknown bioluminescence wave mode: $serializedName" }
            },
            { mode -> mode.name.lowercase() }
        )
    }
}
