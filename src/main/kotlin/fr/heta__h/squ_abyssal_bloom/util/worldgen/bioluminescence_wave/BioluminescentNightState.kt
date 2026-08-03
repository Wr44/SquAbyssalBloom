package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class BioluminescentNightState(
    var lastRolledNightIndex: Long = Long.MIN_VALUE,
    var totalNightActive: Boolean = false
) {
    companion object {
        @JvmField
        val CODEC: Codec<BioluminescentNightState> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.LONG.optionalFieldOf("last_rolled_night_index", Long.MIN_VALUE)
                    .forGetter { state: BioluminescentNightState -> state.lastRolledNightIndex },
                Codec.BOOL.optionalFieldOf("total_night_active", false)
                    .forGetter { state: BioluminescentNightState -> state.totalNightActive }
            ).apply(instance, ::BioluminescentNightState)
        }
    }
}
