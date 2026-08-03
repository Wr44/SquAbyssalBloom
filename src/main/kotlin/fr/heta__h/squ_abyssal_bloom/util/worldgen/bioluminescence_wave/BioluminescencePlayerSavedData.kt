package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.resources.Identifier
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.UUID

class BioluminescencePlayerSavedData(
    val states: MutableMap<UUID, PlayerBioluminescenceState> = linkedMapOf()
) : SavedData() {
    companion object {
        @JvmField
        val CODEC: Codec<BioluminescencePlayerSavedData> = PlayerBioluminescenceState.CODEC.listOf().xmap(
            { states ->
                BioluminescencePlayerSavedData(
                    states.associateByTo(linkedMapOf()) { state -> state.playerId }
                )
            },
            { data -> data.states.values.toList() }
        )

        @JvmField
        val TYPE = SavedDataType(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "bioluminescence_players"),
            ::BioluminescencePlayerSavedData,
            CODEC
        )
    }
}
