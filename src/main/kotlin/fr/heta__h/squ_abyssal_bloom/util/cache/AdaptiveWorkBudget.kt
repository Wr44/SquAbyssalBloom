package fr.heta__h.squ_abyssal_bloom.util.cache

class AdaptiveWorkBudget(
    initialNanosPerUnit: Double,
    private val minUnits: Int,
    private val maxUnits: Int
) {
    companion object {
        const val SMOOTHING = 0.35
        const val MIN_NANOS_PER_UNIT = 20.0
        const val MIN_RELIABLE_SAMPLE_NANOS = 1_000L
    }

    private var estimatedNanosPerUnit = initialNanosPerUnit.coerceAtLeast(MIN_NANOS_PER_UNIT)

    fun suggestedUnits(targetNanos: Long): Int {
        val estimate = (targetNanos / estimatedNanosPerUnit).toInt()
        return estimate.coerceIn(minUnits, maxUnits)
    }

    fun recordSample(units: Int, elapsedNanos: Long) {
        if (units <= 0 || elapsedNanos < MIN_RELIABLE_SAMPLE_NANOS) return
        val observedNanosPerUnit = elapsedNanos.toDouble() / units
        estimatedNanosPerUnit = (estimatedNanosPerUnit + (observedNanosPerUnit - estimatedNanosPerUnit) * SMOOTHING)
            .coerceAtLeast(MIN_NANOS_PER_UNIT)
    }

}
