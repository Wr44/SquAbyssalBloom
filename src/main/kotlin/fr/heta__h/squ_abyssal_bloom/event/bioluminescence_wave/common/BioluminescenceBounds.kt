package fr.heta__h.squ_abyssal_bloom.event.bioluminescence_wave.common

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlin.math.max

data class BioluminescenceBounds(
    val minimumX: Int,
    val minimumZ: Int,
    val maximumX: Int,
    val maximumZ: Int
) {
    companion object {
        @JvmField
        val CODEC: Codec<BioluminescenceBounds> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("minimum_x").forGetter { bounds: BioluminescenceBounds -> bounds.minimumX },
                Codec.INT.fieldOf("minimum_z").forGetter { bounds: BioluminescenceBounds -> bounds.minimumZ },
                Codec.INT.fieldOf("maximum_x").forGetter { bounds: BioluminescenceBounds -> bounds.maximumX },
                Codec.INT.fieldOf("maximum_z").forGetter { bounds: BioluminescenceBounds -> bounds.maximumZ }
            ).apply(instance, ::BioluminescenceBounds)
        }

        fun around(x: Int, z: Int, radius: Int): BioluminescenceBounds = BioluminescenceBounds(
            x - radius,
            z - radius,
            x + radius,
            z + radius
        )
    }

    val isValid: Boolean
        get() = minimumX <= maximumX && minimumZ <= maximumZ

    fun expanded(margin: Double): BioluminescenceBounds {
        val blockMargin = margin.coerceAtLeast(0.0).toInt()
        return BioluminescenceBounds(
            minimumX - blockMargin,
            minimumZ - blockMargin,
            maximumX + blockMargin,
            maximumZ + blockMargin
        )
    }

    fun intersects(other: BioluminescenceBounds): Boolean {
        return maximumX >= other.minimumX && minimumX <= other.maximumX &&
            maximumZ >= other.minimumZ && minimumZ <= other.maximumZ
    }

    fun horizontalDistanceSqr(x: Double, z: Double): Double {
        val deltaX = max(maximumX.toDouble().let { x - it }, minimumX.toDouble() - x).coerceAtLeast(0.0)
        val deltaZ = max(maximumZ.toDouble().let { z - it }, minimumZ.toDouble() - z).coerceAtLeast(0.0)
        return deltaX * deltaX + deltaZ * deltaZ
    }
}
