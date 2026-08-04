package fr.heta__h.squ_abyssal_bloom.particle.bioluminescent_water

import com.mojang.serialization.MapCodec
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

class BioluminescentWaterParticleType(
    overrideLimiter: Boolean
) : ParticleType<BioluminescentWaterParticleOptions>(
    overrideLimiter
) {

    override fun codec(): MapCodec<BioluminescentWaterParticleOptions> = BioluminescentWaterParticleOptions.CODEC

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, BioluminescentWaterParticleOptions> = BioluminescentWaterParticleOptions.STREAM_CODEC

}
