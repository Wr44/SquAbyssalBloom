package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult

enum class BioluminescenceWaveSize(val maximumRadius: Int) {
    SMALL(44),
    LARGE(98);

    companion object {
        @JvmField
        val CODEC: Codec<BioluminescenceWaveSize> = Codec.STRING.comapFlatMap(
            { serializedName ->
                entries.firstOrNull { size -> size.name.equals(serializedName, ignoreCase = true) }
                    ?.let { size -> DataResult.success(size) }
                    ?: DataResult.error { "Unknown bioluminescence wave size: $serializedName" }
            },
            { size -> size.name.lowercase() }
        )
    }
}
