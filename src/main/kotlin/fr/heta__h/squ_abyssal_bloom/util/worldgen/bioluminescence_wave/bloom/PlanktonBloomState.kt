package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import java.util.Optional
import java.util.UUID

data class PlanktonBloomState(
    val id: UUID,
    val waveEventId: UUID,
    val position: BlockPos,
    val visualSeed: Long,
    val maxHarvests: Int,
    var remainingHarvests: Int,
    var lifecycle: PlanktonBloomLifecycle = PlanktonBloomLifecycle.DORMANT,
    var activatedAtGameTime: Long? = null
) {
    companion object {
        private val LIFECYCLE_CODEC: Codec<PlanktonBloomLifecycle> = Codec.STRING.xmap(
            { name -> PlanktonBloomLifecycle.valueOf(name) },
            { lifecycle -> lifecycle.name }
        )

        @JvmField
        val CODEC: Codec<PlanktonBloomState> = RecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.CODEC.fieldOf("id")
                    .forGetter { state: PlanktonBloomState -> state.id },
                UUIDUtil.CODEC.fieldOf("wave_event_id")
                    .forGetter { state: PlanktonBloomState -> state.waveEventId },
                BlockPos.CODEC.fieldOf("position")
                    .forGetter { state: PlanktonBloomState -> state.position },
                Codec.LONG.fieldOf("visual_seed")
                    .forGetter { state: PlanktonBloomState -> state.visualSeed },
                Codec.INT.fieldOf("max_harvests")
                    .forGetter { state: PlanktonBloomState -> state.maxHarvests },
                Codec.INT.fieldOf("remaining_harvests")
                    .forGetter { state: PlanktonBloomState -> state.remainingHarvests },
                LIFECYCLE_CODEC.optionalFieldOf("lifecycle", PlanktonBloomLifecycle.DORMANT)
                    .forGetter { state: PlanktonBloomState -> state.lifecycle },
                Codec.LONG.optionalFieldOf("activated_at_game_time")
                    .forGetter { state: PlanktonBloomState -> Optional.ofNullable(state.activatedAtGameTime) }
            ).apply(instance) { id, waveEventId, position, visualSeed, maxHarvests, remainingHarvests,
                                 lifecycle, activatedAtGameTime ->
                PlanktonBloomState(
                    id,
                    waveEventId,
                    position,
                    visualSeed,
                    maxHarvests,
                    remainingHarvests,
                    lifecycle,
                    activatedAtGameTime.orElse(null)
                )
            }
        }
    }
}
