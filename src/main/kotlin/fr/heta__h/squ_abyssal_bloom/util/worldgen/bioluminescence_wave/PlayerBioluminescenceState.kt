package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import java.util.UUID

data class PlayerBioluminescenceState(
    val playerId: UUID,
    var status: PlayerBioluminescenceStatus = PlayerBioluminescenceStatus.WAITING,
    var remainingNightTicksUntilOpportunity: Long = 0L,
    var lastActivityGameTime: Long = 0L
) {
    companion object {
        @JvmField
        val CODEC: Codec<PlayerBioluminescenceState> = RecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.CODEC.fieldOf("player_id")
                    .forGetter { state: PlayerBioluminescenceState -> state.playerId },
                PlayerBioluminescenceStatus.CODEC.optionalFieldOf(
                    "status",
                    PlayerBioluminescenceStatus.WAITING
                ).forGetter { state: PlayerBioluminescenceState -> state.status },
                Codec.LONG.optionalFieldOf("remaining_night_ticks", 0L)
                    .forGetter { state: PlayerBioluminescenceState -> state.remainingNightTicksUntilOpportunity },
                Codec.LONG.optionalFieldOf("last_activity_game_time", 0L)
                    .forGetter { state: PlayerBioluminescenceState -> state.lastActivityGameTime }
            ).apply(instance, ::PlayerBioluminescenceState)
        }
    }
}
