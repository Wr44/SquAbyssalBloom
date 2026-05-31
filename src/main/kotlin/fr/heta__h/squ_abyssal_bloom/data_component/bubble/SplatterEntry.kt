package fr.heta__h.squ_abyssal_bloom.data_component.bubble

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.effect.MobEffect

data class SplatterEntry(
    val effect: Holder<MobEffect>,
    val duration: Int,
    val amplifier: Int
) {
    companion object {
        val CODEC: Codec<SplatterEntry> = RecordCodecBuilder.create { instance ->
            instance.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(SplatterEntry::effect),
                Codec.INT.fieldOf("duration").forGetter(SplatterEntry::duration),
                Codec.INT.fieldOf("amplifier").forGetter(SplatterEntry::amplifier)
            ).apply(instance, ::SplatterEntry)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SplatterEntry> = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT), SplatterEntry::effect,
            ByteBufCodecs.INT, SplatterEntry::duration,
            ByteBufCodecs.INT, SplatterEntry::amplifier,
            ::SplatterEntry
        )
    }
}

