package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.ecology

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.world.phys.Vec3
import java.util.UUID


data class ReefState(
    val id: UUID,
    var anchor: BlockPos,
    var maturityTicks: Long = 0L,
    var lastActiveGameTime: Long = 0L,
    var nextEcologyGameTime: Long = 0L,
    var nextDepositGameTime: Long = 0L,
    var nextDecorationGameTime: Long = 0L,
    var placedDecorations: Int = 0,
    val depositPositions: MutableSet<Long> = linkedSetOf()
) {

    companion object {
        const val DECLINE_GRACE_TICKS = 1200L

        @JvmField
        val CODEC: Codec<ReefState> = RecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter { state: ReefState -> state.id },
                BlockPos.CODEC.fieldOf("anchor").forGetter { state: ReefState -> state.anchor },
                Codec.LONG.optionalFieldOf("maturity_ticks", 0L)
                    .forGetter { state: ReefState -> state.maturityTicks },
                Codec.LONG.optionalFieldOf("last_active_game_time", 0L)
                    .forGetter { state: ReefState -> state.lastActiveGameTime },
                Codec.LONG.optionalFieldOf("next_ecology_game_time", 0L)
                    .forGetter { state: ReefState -> state.nextEcologyGameTime },
                Codec.LONG.optionalFieldOf("next_deposit_game_time", 0L)
                    .forGetter { state: ReefState -> state.nextDepositGameTime },
                Codec.LONG.optionalFieldOf("next_decoration_game_time", 0L)
                    .forGetter { state: ReefState -> state.nextDecorationGameTime },
                Codec.INT.optionalFieldOf("placed_decorations", 0)
                    .forGetter { state: ReefState -> state.placedDecorations },
                Codec.LONG.listOf().optionalFieldOf("deposit_positions", emptyList())
                    .forGetter { state: ReefState -> state.depositPositions.toList() }
            ).apply(instance) { id, anchor, maturityTicks, lastActiveGameTime,
                                nextEcologyGameTime, nextDepositGameTime, nextDecorationGameTime,
                                placedDecorations, depositPositions ->
                ReefState(
                    id = id,
                    anchor = anchor,
                    maturityTicks = maturityTicks,
                    lastActiveGameTime = lastActiveGameTime,
                    nextEcologyGameTime = nextEcologyGameTime,
                    nextDepositGameTime = nextDepositGameTime,
                    nextDecorationGameTime = nextDecorationGameTime,
                    placedDecorations = placedDecorations,
                    depositPositions = depositPositions.toMutableSet()
                )
            }
        }
    }

    @Transient
    var members: List<RedSlobbererEntity> = emptyList()

    @Transient
    var center: Vec3 = Vec3.atCenterOf(anchor)

    @Transient
    var collectiveActivity: Double = 0.0

    @Transient
    var stability: Double = 0.0

    fun stage(maturityRequirement: Int, gameTime: Long): ReefStage {
        if (members.size < 2 && gameTime - lastActiveGameTime > DECLINE_GRACE_TICKS) {
            return ReefStage.DECLINING
        }
        val maturity = maturityRequirement.coerceAtLeast(1).toLong()
        return when {
            maturityTicks >= maturity -> ReefStage.MATURE
            maturityTicks >= maturity * 2L / 5L -> ReefStage.ESTABLISHED
            else -> ReefStage.COLONIZING
        }
    }

}
