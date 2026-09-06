package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import java.util.Optional
import java.util.UUID

data class ActiveBioluminescenceWave(
    val eventId: UUID,
    val seed: Long,
    val dimension: Identifier,
    val beachId: UUID,
    val anchor: BlockPos,
    val bounds: BioluminescenceBounds,
    val startGameTime: Long,
    val endGameTime: Long,
    val size: BioluminescenceWaveSize,
    val mode: BioluminescenceWaveMode,
    val activity: BioluminescenceWaveActivity,
    val originPlayerId: UUID? = null,
    val createdByCommand: Boolean = false,
    val startSoundPlayerIds: MutableSet<UUID> = linkedSetOf()
) {
    companion object {
        @JvmField
        val CODEC: Codec<ActiveBioluminescenceWave> = RecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.CODEC.fieldOf("event_id")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.eventId },
                Codec.LONG.fieldOf("seed")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.seed },
                Identifier.CODEC.fieldOf("dimension")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.dimension },
                UUIDUtil.CODEC.fieldOf("beach_id")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.beachId },
                BlockPos.CODEC.fieldOf("anchor")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.anchor },
                BioluminescenceBounds.CODEC.fieldOf("bounds")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.bounds },
                Codec.LONG.fieldOf("start_game_time")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.startGameTime },
                Codec.LONG.fieldOf("end_game_time")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.endGameTime },
                BioluminescenceWaveSize.CODEC.fieldOf("size")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.size },
                BioluminescenceWaveMode.CODEC.fieldOf("mode")
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.mode },
                BioluminescenceWaveActivity.CODEC.optionalFieldOf("activity", BioluminescenceWaveActivity.ACTIVE)
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.activity },
                UUIDUtil.CODEC.optionalFieldOf("origin_player_id")
                    .forGetter { wave: ActiveBioluminescenceWave -> Optional.ofNullable(wave.originPlayerId) },
                Codec.BOOL.optionalFieldOf("created_by_command", false)
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.createdByCommand },
                UUIDUtil.CODEC.listOf().optionalFieldOf("start_sound_player_ids", emptyList())
                    .forGetter { wave: ActiveBioluminescenceWave -> wave.startSoundPlayerIds.toList() }
            ).apply(instance) { eventId, seed, dimension, beachId, anchor, bounds,
                                startGameTime, endGameTime, size, mode, activity, originPlayerId, createdByCommand,
                                startSoundPlayerIds ->
                ActiveBioluminescenceWave(
                    eventId,
                    seed,
                    dimension,
                    beachId,
                    anchor,
                    bounds,
                    startGameTime,
                    endGameTime,
                    size,
                    mode,
                    activity,
                    originPlayerId.orElse(null),
                    createdByCommand,
                    startSoundPlayerIds.toCollection(linkedSetOf())
                )
            }
        }
    }

    fun isExpired(gameTime: Long): Boolean {
        return mode == BioluminescenceWaveMode.NORMAL && gameTime >= endGameTime
    }
}
