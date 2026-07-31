package fr.heta__h.squ_abyssal_bloom.worldgen

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.levelgen.synth.NormalNoise

object ModNoises {

    val ABYSSAL_WALL_PARAMETERS = NormalNoise.NoiseParameters(-3, 1.0, 1.0, 1.0)
    val ABYSSAL_DETAIL_PARAMETERS = NormalNoise.NoiseParameters(-2, 1.0, 1.0)
    val ABYSSAL_TOPO_PARAMETERS = NormalNoise.NoiseParameters(-4, 1.0, 1.0, 1.0, 1.0)

    val ABYSSAL_WALL: ResourceKey<NormalNoise.NoiseParameters> = ResourceKey.create(
        Registries.NOISE,
        Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "abyssal_wall")
    )
    val ABYSSAL_DETAIL: ResourceKey<NormalNoise.NoiseParameters> = ResourceKey.create(
        Registries.NOISE,
        Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "abyssal_detail")
    )
    val ABYSSAL_TOPO: ResourceKey<NormalNoise.NoiseParameters> = ResourceKey.create(
        Registries.NOISE,
        Identifier.fromNamespaceAndPath(SquAbyssalBloom.ID, "abyssal_topo")
    )
}
