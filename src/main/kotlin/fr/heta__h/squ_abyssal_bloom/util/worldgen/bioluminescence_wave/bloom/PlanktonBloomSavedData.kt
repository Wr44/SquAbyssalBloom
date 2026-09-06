package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom

import com.mojang.serialization.Codec
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.resources.Identifier
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.UUID

class PlanktonBloomSavedData(
    val blooms: MutableMap<UUID, PlanktonBloomState> = linkedMapOf()
) : SavedData() {
    companion object {
        @JvmField
        val CODEC: Codec<PlanktonBloomSavedData> = PlanktonBloomState.CODEC.listOf().xmap(
            { states -> PlanktonBloomSavedData(states.associateByTo(linkedMapOf()) { state -> state.id }) },
            { data -> data.blooms.values.toList() }
        )

        @JvmField
        val TYPE = SavedDataType(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "plankton_blooms"),
            ::PlanktonBloomSavedData,
            CODEC
        )
    }
}
