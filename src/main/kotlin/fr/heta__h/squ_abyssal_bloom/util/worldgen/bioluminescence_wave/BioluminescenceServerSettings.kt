package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveActivity
import fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common.BioluminescenceWaveSize
import net.minecraft.util.RandomSource
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.roundToLong

data class BioluminescenceServerSettings(
    val enabled: Boolean,
    val opportunityMeanNightTicks: Long,
    val opportunityMinimumNightTicks: Long,
    val opportunityMaximumNightTicks: Long,
    val activeChance: Double,
    val largeChance: Double,
    val durationMeanTicks: Long,
    val durationStandardDeviationTicks: Long,
    val durationMinimumTicks: Long,
    val durationMaximumTicks: Long,
    val visibilityRadius: Double,
    val overlapMargin: Double,
    val preparationDelayTicks: Long,
    val totalNightChance: Double,
    val nightStartTick: Int,
    val nightEndTick: Int,
    val waterSearchRadius: Int,
    val minimumNearbyWaterCells: Int
) {
    companion object {
        const val DAY_TICKS = 24000L
        const val BEACH_CHECK_INTERVAL_TICKS = 10L

        fun current(): BioluminescenceServerSettings {
            val opportunityMinimum = ModServerConfig.BIOLUMINESCENCE_OPPORTUNITY_MIN_NIGHT_TICKS.get()
                .coerceAtLeast(1)
                .toLong()
            val opportunityMaximum = ModServerConfig.BIOLUMINESCENCE_OPPORTUNITY_MAX_NIGHT_TICKS.get()
                .coerceAtLeast(opportunityMinimum.toInt())
                .toLong()
            val durationMinimum = ModServerConfig.BIOLUMINESCENCE_DURATION_MIN_TICKS.get()
                .coerceAtLeast(1)
                .toLong()
            val durationMaximum = ModServerConfig.BIOLUMINESCENCE_DURATION_MAX_TICKS.get()
                .coerceAtLeast(durationMinimum.toInt())
                .toLong()

            return BioluminescenceServerSettings(
                enabled = ModServerConfig.BIOLUMINESCENCE_ENABLED.get(),
                opportunityMeanNightTicks = ModServerConfig.BIOLUMINESCENCE_OPPORTUNITY_MEAN_NIGHT_TICKS.get()
                    .coerceIn(opportunityMinimum.toInt(), opportunityMaximum.toInt())
                    .toLong(),
                opportunityMinimumNightTicks = opportunityMinimum,
                opportunityMaximumNightTicks = opportunityMaximum,
                activeChance = ModServerConfig.BIOLUMINESCENCE_ACTIVE_CHANCE.get().coerceIn(0.0, 1.0),
                largeChance = ModServerConfig.BIOLUMINESCENCE_LARGE_CHANCE.get().coerceIn(0.0, 1.0),
                durationMeanTicks = ModServerConfig.BIOLUMINESCENCE_DURATION_MEAN_TICKS.get()
                    .coerceIn(durationMinimum.toInt(), durationMaximum.toInt())
                    .toLong(),
                durationStandardDeviationTicks = ModServerConfig.BIOLUMINESCENCE_DURATION_STANDARD_DEVIATION_TICKS.get()
                    .coerceAtLeast(0)
                    .toLong(),
                durationMinimumTicks = durationMinimum,
                durationMaximumTicks = durationMaximum,
                visibilityRadius = ModServerConfig.BIOLUMINESCENCE_VISIBILITY_RADIUS.get()
                    .coerceAtLeast(16.0),
                overlapMargin = ModServerConfig.BIOLUMINESCENCE_OVERLAP_MARGIN.get()
                    .coerceAtLeast(0.0),
                preparationDelayTicks = ModServerConfig.BIOLUMINESCENCE_PREPARATION_DELAY_TICKS.get()
                    .coerceAtLeast(0)
                    .toLong(),
                totalNightChance = ModServerConfig.BIOLUMINESCENCE_TOTAL_NIGHT_CHANCE.get()
                    .coerceIn(0.0, 1.0),
                nightStartTick = Math.floorMod(ModServerConfig.BIOLUMINESCENCE_NIGHT_START_TICK.get(), DAY_TICKS.toInt()),
                nightEndTick = Math.floorMod(ModServerConfig.BIOLUMINESCENCE_NIGHT_END_TICK.get(), DAY_TICKS.toInt()),
                waterSearchRadius = ModServerConfig.BIOLUMINESCENCE_WATER_SEARCH_RADIUS.get().coerceAtLeast(1),
                minimumNearbyWaterCells = ModServerConfig.BIOLUMINESCENCE_MINIMUM_NEARBY_WATER_CELLS.get()
                    .coerceAtLeast(1)
            )
        }
    }

    val smallChance: Double
        get() = 1.0 - largeChance

    fun isNight(dayTime: Long): Boolean {
        if (nightStartTick == nightEndTick) return false
        val tick = Math.floorMod(dayTime, DAY_TICKS).toInt()
        return if (nightStartTick < nightEndTick) {
            tick in nightStartTick until nightEndTick
        } else {
            tick >= nightStartTick || tick < nightEndTick
        }
    }

    fun nightIndex(dayTime: Long): Long {
        val dayIndex = Math.floorDiv(dayTime, DAY_TICKS)
        val tick = Math.floorMod(dayTime, DAY_TICKS).toInt()
        return if (nightStartTick > nightEndTick && tick < nightEndTick) dayIndex - 1L else dayIndex
    }

    fun sampleOpportunityDelay(random: RandomSource): Long {
        val unit = random.nextDouble().coerceIn(0.0, Math.nextDown(1.0))
        val sampled = ceil(-opportunityMeanNightTicks.toDouble() * ln(1.0 - unit)).toLong()
        return sampled.coerceIn(opportunityMinimumNightTicks, opportunityMaximumNightTicks)
    }

    fun sampleDuration(random: RandomSource): Long {
        val sampled = durationMeanTicks +
            (random.nextGaussian() * durationStandardDeviationTicks.toDouble()).roundToLong()
        return sampled.coerceIn(durationMinimumTicks, durationMaximumTicks)
    }

    fun sampleSize(random: RandomSource): BioluminescenceWaveSize {
        return if (random.nextDouble() < largeChance) {
            BioluminescenceWaveSize.LARGE
        } else {
            BioluminescenceWaveSize.SMALL
        }
    }

    fun sampleActivity(random: RandomSource): BioluminescenceWaveActivity {
        return if (random.nextDouble() < activeChance) {
            BioluminescenceWaveActivity.ACTIVE
        } else {
            BioluminescenceWaveActivity.INACTIVE
        }
    }

}
