package fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import fr.heta__h.squ_abyssal_bloom.particle.ModParticles
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

class BioluminescentWaterParticleOptions(
    color: Int,
    alpha: Float
) : ParticleOptions {

    val color: Int = color and 0xFFFFFF
    val alpha: Float = alpha.coerceIn(0f, 1f)

    override fun getType(): ParticleType<*> {
        return ModParticles.BIOLUMINESCENT_WATER_PARTICLE.get()
    }

    companion object {

        @JvmField
        val CODEC: MapCodec<BioluminescentWaterParticleOptions> =
            RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.intRange(0, 0xFFFFFF).fieldOf("color")
                        .forGetter(BioluminescentWaterParticleOptions::color),
                    Codec.floatRange(0f, 1f).fieldOf("alpha")
                        .forGetter(BioluminescentWaterParticleOptions::alpha)
                ).apply(instance, ::BioluminescentWaterParticleOptions)
            }

        @JvmField
        val STREAM_CODEC:
                StreamCodec<RegistryFriendlyByteBuf, BioluminescentWaterParticleOptions> =
            StreamCodec.composite(
                ByteBufCodecs.INT, BioluminescentWaterParticleOptions::color,
                ByteBufCodecs.FLOAT, BioluminescentWaterParticleOptions::alpha,
                ::BioluminescentWaterParticleOptions
            )

        @JvmStatic
        fun rgb(
            red: Int,
            green: Int,
            blue: Int,
            alpha: Float = 1f
        ): BioluminescentWaterParticleOptions {
            val clampedRed = red.coerceIn(0, 255)
            val clampedGreen = green.coerceIn(0, 255)
            val clampedBlue = blue.coerceIn(0, 255)

            return BioluminescentWaterParticleOptions(
                clampedRed shl 16 or (clampedGreen shl 8) or clampedBlue,
                alpha
            )
        }
    }
}
