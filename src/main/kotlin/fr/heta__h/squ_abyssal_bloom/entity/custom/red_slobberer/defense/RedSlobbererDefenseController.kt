package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.defense

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import kotlin.math.ceil
import kotlin.math.roundToLong

class RedSlobbererDefenseController(
    private val redSlobberer: RedSlobbererEntity
) {

    companion object {
        private const val TAG_STATE = "RedSlobbererDefenseState"
        private const val TAG_PHASE_REMAINING_TICKS = "RedSlobbererDefensePhaseRemainingTicks"
        private const val TICKS_PER_SECOND = 20.0

        private val HIDE_ANIMATION_TICKS =
            ceil(RedSlobbererEntity.ANIM_HIDE_S * TICKS_PER_SECOND).toInt()
        private val SHOW_ANIMATION_TICKS =
            ceil(RedSlobbererEntity.ANIM_SHOW_S * TICKS_PER_SECOND).toInt()
    }

    var state: RedSlobbererDefenseState = RedSlobbererDefenseState.NORMAL
        private set

    private var phaseStartGameTime = 0L
    private var phaseDurationTicks = 0

    fun onDamage(level: ServerLevel) {
        val gameTime = level.gameTime

        when (state) {
            RedSlobbererDefenseState.NORMAL -> startHiding(gameTime)
            RedSlobbererDefenseState.HIDING -> Unit
            RedSlobbererDefenseState.HIDDEN -> Unit
            RedSlobbererDefenseState.SHOWING -> reverseShowingIntoHiding(gameTime)
        }
    }

    fun tick(level: ServerLevel) {
        val gameTime = level.gameTime
        if (redSlobberer.isBaby) {
            if (state != RedSlobbererDefenseState.NORMAL) {
                transitionTo(RedSlobbererDefenseState.NORMAL, gameTime, 0)
            }
            return
        }

        repeat(RedSlobbererDefenseState.entries.size) {
            when (state) {
                RedSlobbererDefenseState.NORMAL -> return
                RedSlobbererDefenseState.HIDING -> {
                    if (gameTime < phaseEndGameTime()) return
                    transitionTo(
                        RedSlobbererDefenseState.HIDDEN,
                        phaseEndGameTime(),
                        0
                    )
                }
                RedSlobbererDefenseState.HIDDEN -> {
                    if (redSlobberer.hasActiveRefugeUnsafeCooldown()) return
                    transitionTo(
                        RedSlobbererDefenseState.SHOWING,
                        gameTime,
                        SHOW_ANIMATION_TICKS
                    )
                }
                RedSlobbererDefenseState.SHOWING -> {
                    if (gameTime < phaseEndGameTime()) return
                    transitionTo(RedSlobbererDefenseState.NORMAL, phaseEndGameTime(), 0)
                }
            }
        }
    }

    fun save(output: ValueOutput, gameTime: Long) {
        output.putInt(TAG_STATE, state.networkId)
        output.putInt(
            TAG_PHASE_REMAINING_TICKS,
            (phaseEndGameTime() - gameTime).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        )
    }

    fun load(input: ValueInput, gameTime: Long) {
        val loadedState = RedSlobbererDefenseState.fromNetworkId(
            input.getIntOr(TAG_STATE, RedSlobbererDefenseState.NORMAL.networkId)
        )
        if (loadedState == RedSlobbererDefenseState.NORMAL) {
            transitionTo(RedSlobbererDefenseState.NORMAL, gameTime, 0)
            return
        }

        val remainingTicks = input.getIntOr(TAG_PHASE_REMAINING_TICKS, 0).coerceAtLeast(0)
        val fullDurationTicks = when (loadedState) {
            RedSlobbererDefenseState.NORMAL -> 0
            RedSlobbererDefenseState.HIDING -> HIDE_ANIMATION_TICKS
            RedSlobbererDefenseState.HIDDEN -> 0
            RedSlobbererDefenseState.SHOWING -> SHOW_ANIMATION_TICKS
        }
        val clampedRemainingTicks = remainingTicks.coerceAtMost(fullDurationTicks)
        val elapsedTicks = fullDurationTicks - clampedRemainingTicks
        transitionTo(
            loadedState,
            gameTime - elapsedTicks,
            fullDurationTicks
        )
    }

    private fun startHiding(gameTime: Long) {
        transitionTo(
            RedSlobbererDefenseState.HIDING,
            gameTime,
            HIDE_ANIMATION_TICKS
        )
    }

    private fun reverseShowingIntoHiding(gameTime: Long) {
        val showDurationTicks = phaseDurationTicks.coerceAtLeast(1)
        val showElapsedTicks = (gameTime - phaseStartGameTime)
            .coerceIn(0L, showDurationTicks.toLong())
        val showProgress = showElapsedTicks.toDouble() / showDurationTicks
        val hideDurationTicks = HIDE_ANIMATION_TICKS
        val hideElapsedTicks = (hideDurationTicks * (1.0 - showProgress))
            .roundToLong()
            .coerceIn(0L, hideDurationTicks.toLong())

        transitionTo(
            RedSlobbererDefenseState.HIDING,
            gameTime - hideElapsedTicks,
            hideDurationTicks
        )
    }

    private fun transitionTo(
        newState: RedSlobbererDefenseState,
        startGameTime: Long,
        durationTicks: Int
    ) {
        state = newState
        phaseStartGameTime = startGameTime
        phaseDurationTicks = durationTicks.coerceAtLeast(0)
        redSlobberer.syncDefenseState(newState, startGameTime)
    }

    private fun phaseEndGameTime(): Long = phaseStartGameTime + phaseDurationTicks
}
