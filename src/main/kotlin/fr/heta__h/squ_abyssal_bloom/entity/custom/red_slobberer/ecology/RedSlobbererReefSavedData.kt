package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology

import com.mojang.serialization.Codec
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.resources.Identifier
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.UUID

class RedSlobbererReefSavedData(
    val reefs: MutableMap<UUID, ReefState> = linkedMapOf()
) : SavedData() {

    companion object {
        @JvmField
        val CODEC: Codec<RedSlobbererReefSavedData> = ReefState.CODEC.listOf().xmap(
            { states ->
                RedSlobbererReefSavedData(
                    states.associateByTo(linkedMapOf()) { state -> state.id }
                )
            },
            { data -> data.reefs.values.toList() }
        )

        @JvmField
        val TYPE = SavedDataType(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "red_slobberer_reefs"),
            ::RedSlobbererReefSavedData,
            CODEC
        )
    }
}
