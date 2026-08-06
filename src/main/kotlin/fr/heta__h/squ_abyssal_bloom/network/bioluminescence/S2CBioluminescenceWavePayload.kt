package fr.heta__h.squ_abyssal_bloom.network.bioluminescence

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceBounds
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveActivity
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveMode
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.BioluminescenceWaveSize
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.ActiveBioluminescenceWave
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomLifecycle
import fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave.bloom.PlanktonBloomState
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.Optional
import java.util.UUID

data class S2CBioluminescenceWavePayload(
    val eventId: UUID,
    val seed: Long,
    val dimension: Identifier,
    val beachId: UUID,
    val anchor: BlockPos,
    val bounds: BioluminescenceBounds,
    val startGameTime: Long,
    val endGameTime: Long,
    val serverGameTimeAtSend: Long,
    val size: BioluminescenceWaveSize,
    val mode: BioluminescenceWaveMode,
    val activity: BioluminescenceWaveActivity,
    val blooms: List<BloomSnapshot> = emptyList()
) : CustomPacketPayload {

    data class BloomSnapshot(
        val id: UUID,
        val position: BlockPos,
        val visualSeed: Long,
        val maxHarvests: Int,
        val remainingHarvests: Int,
        val lifecycle: PlanktonBloomLifecycle,
        val activatedAtGameTime: Long?
    ) {
        companion object {
            fun from(bloom: PlanktonBloomState): BloomSnapshot {
                return BloomSnapshot(
                    bloom.id,
                    bloom.position,
                    bloom.visualSeed,
                    bloom.maxHarvests,
                    bloom.remainingHarvests,
                    bloom.lifecycle,
                    bloom.activatedAtGameTime
                )
            }
        }
    }

    companion object {
        private const val MAXIMUM_NORMAL_DURATION_TICKS = 72000L

        val ID = CustomPacketPayload.Type<S2CBioluminescenceWavePayload>(
            Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "bioluminescence_wave")
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, S2CBioluminescenceWavePayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeUUID(payload.eventId)
                buffer.writeLong(payload.seed)
                buffer.writeIdentifier(payload.dimension)
                buffer.writeUUID(payload.beachId)
                buffer.writeBlockPos(payload.anchor)
                buffer.writeInt(payload.bounds.minimumX)
                buffer.writeInt(payload.bounds.minimumZ)
                buffer.writeInt(payload.bounds.maximumX)
                buffer.writeInt(payload.bounds.maximumZ)
                buffer.writeLong(payload.startGameTime)
                buffer.writeLong(payload.endGameTime)
                buffer.writeLong(payload.serverGameTimeAtSend)
                buffer.writeEnum(payload.size)
                buffer.writeEnum(payload.mode)
                buffer.writeEnum(payload.activity)
                buffer.writeCollection(payload.blooms) { buf, bloom ->
                    buf.writeUUID(bloom.id)
                    buf.writeBlockPos(bloom.position)
                    buf.writeLong(bloom.visualSeed)
                    buf.writeVarInt(bloom.maxHarvests)
                    buf.writeVarInt(bloom.remainingHarvests)
                    buf.writeEnum(bloom.lifecycle)
                    buf.writeOptional(Optional.ofNullable(bloom.activatedAtGameTime)) { b, value -> b.writeLong(value) }
                }
            },
            { buffer ->
                S2CBioluminescenceWavePayload(
                    eventId = buffer.readUUID(),
                    seed = buffer.readLong(),
                    dimension = buffer.readIdentifier(),
                    beachId = buffer.readUUID(),
                    anchor = buffer.readBlockPos(),
                    bounds = BioluminescenceBounds(
                        buffer.readInt(),
                        buffer.readInt(),
                        buffer.readInt(),
                        buffer.readInt()
                    ),
                    startGameTime = buffer.readLong(),
                    endGameTime = buffer.readLong(),
                    serverGameTimeAtSend = buffer.readLong(),
                    size = buffer.readEnum(BioluminescenceWaveSize::class.java),
                    mode = buffer.readEnum(BioluminescenceWaveMode::class.java),
                    activity = buffer.readEnum(BioluminescenceWaveActivity::class.java),
                    blooms = buffer.readList { buf ->
                        BloomSnapshot(
                            id = buf.readUUID(),
                            position = buf.readBlockPos(),
                            visualSeed = buf.readLong(),
                            maxHarvests = buf.readVarInt(),
                            remainingHarvests = buf.readVarInt(),
                            lifecycle = buf.readEnum(PlanktonBloomLifecycle::class.java),
                            activatedAtGameTime = buf.readOptional { b -> b.readLong() }.orElse(null)
                        )
                    }
                )
            }
        )

        fun from(
            wave: ActiveBioluminescenceWave,
            serverGameTime: Long,
            blooms: List<PlanktonBloomState> = emptyList()
        ): S2CBioluminescenceWavePayload {
            return S2CBioluminescenceWavePayload(
                wave.eventId,
                wave.seed,
                wave.dimension,
                wave.beachId,
                wave.anchor,
                wave.bounds,
                wave.startGameTime,
                wave.endGameTime,
                serverGameTime,
                wave.size,
                wave.mode,
                wave.activity,
                blooms.map(BloomSnapshot::from)
            )
        }

        fun handle(payload: S2CBioluminescenceWavePayload, context: IPayloadContext) {
            context.enqueueWork {
                BioluminescenceClientPayloadHandler.handleWave(payload)
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<S2CBioluminescenceWavePayload> = ID

    fun isStructurallyValid(): Boolean {
        if (!bounds.isValid) return false
        val maximumDiameter = size.maximumRadius.toLong() * 2L
        if (bounds.maximumX.toLong() - bounds.minimumX.toLong() > maximumDiameter) return false
        if (bounds.maximumZ.toLong() - bounds.minimumZ.toLong() > maximumDiameter) return false
        if (anchor.x !in bounds.minimumX..bounds.maximumX) return false
        if (anchor.z !in bounds.minimumZ..bounds.maximumZ) return false
        if (endGameTime <= startGameTime) return false
        return when (mode) {
            BioluminescenceWaveMode.NORMAL ->
                endGameTime - startGameTime in 1L..MAXIMUM_NORMAL_DURATION_TICKS
            BioluminescenceWaveMode.TOTAL_NIGHT -> endGameTime == Long.MAX_VALUE
        }
    }
}
