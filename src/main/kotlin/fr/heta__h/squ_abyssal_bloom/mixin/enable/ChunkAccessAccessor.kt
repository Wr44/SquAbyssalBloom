package fr.heta__h.squ_abyssal_bloom.mixin.enable

import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.NoiseChunk
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ChunkAccess::class)
interface ChunkAccessAccessor {
    @Accessor("noiseChunk")
    fun getNoiseChunk(): NoiseChunk?
}
