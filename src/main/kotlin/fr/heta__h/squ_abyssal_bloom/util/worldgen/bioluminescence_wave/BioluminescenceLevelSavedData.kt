package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.resources.Identifier
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.Optional
import java.util.UUID

class BioluminescenceLevelSavedData(
    val waves: MutableMap<UUID, ActiveBioluminescenceWave> = linkedMapOf(),
    val nightState: BioluminescentNightState = BioluminescentNightState()
) : SavedData() {
    companion object {
        @JvmField
        val CODEC: Codec<BioluminescenceLevelSavedData> = RecordCodecBuilder.create { instance ->
            instance.group(
                ActiveBioluminescenceWave.CODEC.listOf().optionalFieldOf("waves", emptyList())
                    .forGetter { data: BioluminescenceLevelSavedData -> data.waves.values.toList() },
                BioluminescentNightState.CODEC.optionalFieldOf("night_state")
                    .forGetter { data: BioluminescenceLevelSavedData -> Optional.of(data.nightState) }
            ).apply(instance) { waves, nightState ->
                BioluminescenceLevelSavedData(
                    waves.associateByTo(linkedMapOf()) { wave -> wave.eventId },
                    nightState.orElseGet(::BioluminescentNightState)
                )
            }
        }

        @JvmField
        val TYPE = SavedDataType(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "bioluminescence_waves"),
            ::BioluminescenceLevelSavedData,
            CODEC
        )
    }
}
