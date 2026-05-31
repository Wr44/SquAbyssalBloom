package fr.heta__h.squ_abyssal_bloom.data_component.bubble

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

data class SplatterData(
    val entries: List<SplatterEntry>,
    val color: Int
) {
    companion object {
        val CODEC: Codec<SplatterData> = RecordCodecBuilder.create { instance ->
            instance.group(
                SplatterEntry.CODEC.listOf().fieldOf("entries").forGetter(SplatterData::entries),
                Codec.INT.fieldOf("color").forGetter(SplatterData::color)
            ).apply(instance, ::SplatterData)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SplatterData> = StreamCodec.composite(
            SplatterEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), SplatterData::entries,
            ByteBufCodecs.INT, SplatterData::color,
            ::SplatterData
        )

        fun averageColor(colors: List<Int>): Int {
            if (colors.isEmpty()) return 0
            var r = 0; var g = 0; var b = 0
            for (c in colors) {
                r += (c shr 16) and 0xFF
                g += (c shr 8) and 0xFF
                b += c and 0xFF
            }
            val n = colors.size
            return (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
        }
    }
}