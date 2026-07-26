package fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.control

import fr.heta__h.squ_abyssal_bloom.entity.custom.red_slobberer.RedSlobbererEntity

class RedSlobbererStepSmoothing(
    private val redSlobberer: RedSlobbererEntity
) {

    private companion object {
        const val NATURAL_RISE_PER_TICK = 0.2
        const val MAX_CATCH_UP_PER_TICK = 0.12
        const val MAX_BANKED_OFFSET = 2.0
        const val TELEPORT_RISE_THRESHOLD = 3.0
    }

    private var lastY: Double? = null
    private var previousOffset: Double = 0.0
    private var currentOffset: Double = 0.0

    fun tick() {
        previousOffset = currentOffset

        val y = redSlobberer.y
        val previousY = lastY
        lastY = y
        if (previousY == null) return

        val rise = y - previousY
        if (rise > NATURAL_RISE_PER_TICK) {
            val bankedRise = rise - NATURAL_RISE_PER_TICK
            currentOffset = if (bankedRise > TELEPORT_RISE_THRESHOLD) {
                0.0
            } else {
                (currentOffset + bankedRise).coerceAtMost(MAX_BANKED_OFFSET)
            }
        }

        if (currentOffset > 0.0) {
            currentOffset = (currentOffset - MAX_CATCH_UP_PER_TICK).coerceAtLeast(0.0)
        }
    }

    fun getInterpolatedRenderOffset(partialTick: Float): Double {
        val alpha = partialTick.toDouble().coerceIn(0.0, 1.0)
        return previousOffset * (1.0 - alpha) + currentOffset * alpha
    }
}
