package fr.heta__h.squ_abyssal_bloom.util.worldgen.bioluminescence_wave

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.world.level.levelgen.RandomSupport

enum class BioluminescenceWaveSize(
    val geodesicRadiusRange: IntRange,
    val analysisMargin: Int,
    val minimumWaterCells: Int,
    val shorelineFootprintRadius: Int
) {
    SMALL(32..46, 12, 160, 22),
    LARGE(48..80, 18, 384, 40);

    val maximumRadius: Int = geodesicRadiusRange.last + analysisMargin
    val maximumWaterCells: Int = 1 + 2 * maximumRadius * (maximumRadius + 1)
    val commandName: String = name.lowercase()

    fun selectGeodesicRadius(seed: Long): Int {
        val span = (geodesicRadiusRange.last - geodesicRadiusRange.first + 1).toLong()
        val mixed = RandomSupport.mixStafford13(seed xor GEODESIC_RADIUS_SALT)
        return geodesicRadiusRange.first + Math.floorMod(mixed, span).toInt()
    }

    companion object {
        private const val GEODESIC_RADIUS_SALT = 0x7137449123EF65CDL

        @JvmField
        val CODEC: Codec<BioluminescenceWaveSize> = Codec.STRING.comapFlatMap(
            { serializedName ->
                entries.firstOrNull { size -> size.name.equals(serializedName, ignoreCase = true) }
                    ?.let { size -> DataResult.success(size) }
                    ?: DataResult.error { "Unknown bioluminescence wave size: $serializedName" }
            },
            { size -> size.name.lowercase() }
        )
    }
}
