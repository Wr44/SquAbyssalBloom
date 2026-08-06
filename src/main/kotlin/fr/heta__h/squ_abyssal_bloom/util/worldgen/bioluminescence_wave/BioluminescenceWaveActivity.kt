package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult

enum class BioluminescenceWaveActivity {
    ACTIVE,
    INACTIVE;

    companion object {
        @JvmField
        val CODEC: Codec<BioluminescenceWaveActivity> = Codec.STRING.comapFlatMap(
            { serializedName ->
                entries.firstOrNull { activity -> activity.name.equals(serializedName, ignoreCase = true) }
                    ?.let { activity -> DataResult.success(activity) }
                    ?: DataResult.error { "Unknown bioluminescence wave activity: $serializedName" }
            },
            { activity -> activity.name.lowercase() }
        )
    }
}
