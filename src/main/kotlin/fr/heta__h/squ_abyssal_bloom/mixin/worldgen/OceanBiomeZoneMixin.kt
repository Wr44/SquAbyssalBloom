package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.config.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.biome.Climate
import net.minecraft.world.level.biome.MultiNoiseBiomeSource
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.stream.Stream

@Mixin(value = [MultiNoiseBiomeSource::class], priority = 500)
abstract class OceanBiomeZoneMixin {

    @Shadow
    protected abstract fun collectPossibleBiomes(): Stream<Holder<Biome>>

    @Unique @Volatile
    private var sab_cache: Map<ResourceKey<Biome>, Holder<Biome>>? = null

    @Unique
    private fun sab_pickDeepOcean(temp: Float): ResourceKey<Biome> = when {
        temp < -0.45f -> Biomes.DEEP_FROZEN_OCEAN
        temp < -0.15f -> Biomes.DEEP_COLD_OCEAN
        temp < 0.2f   -> Biomes.DEEP_OCEAN
        temp < 0.55f  -> Biomes.DEEP_LUKEWARM_OCEAN
        else          -> Biomes.WARM_OCEAN
    }

    @Unique
    private fun sab_pickShallowOcean(temp: Float): ResourceKey<Biome> = when {
        temp < -0.45f -> Biomes.FROZEN_OCEAN
        temp < -0.15f -> Biomes.COLD_OCEAN
        temp < 0.2f   -> Biomes.OCEAN
        temp < 0.55f  -> Biomes.LUKEWARM_OCEAN
        else          -> Biomes.WARM_OCEAN
    }

    @Unique
    private fun sab_resolve(key: ResourceKey<Biome>): Holder<Biome>? {
        val map = sab_cache ?: buildMap<ResourceKey<Biome>, Holder<Biome>> {
            collectPossibleBiomes().forEach { holder ->
                holder.unwrapKey().ifPresent { put(it, holder) }
            }
        }.also { sab_cache = it }
        return map[key]
    }

    @Inject(
        method = ["getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate\$Sampler;)Lnet/minecraft/core/Holder;"],
        at = [At("HEAD")],
        cancellable = true
    )
    private fun sab_enforceOceanZones(
        quartX: Int,
        quartY: Int,
        quartZ: Int,
        sampler: Climate.Sampler,
        cir: CallbackInfoReturnable<Holder<Biome>>
    ) {
        val target = sampler.sample(quartX, quartY, quartZ)
        val cont = Climate.unquantizeCoord(target.continentalness())

        if (cont > -0.19f || cont < -1.05f) return

        val depth = Climate.unquantizeCoord(target.depth())
        if (depth in 0.2f..0.9f) return

        val shallowDeep = ModServerConfig.SHALLOW_DEEP_BOUNDARY.get().toFloat()
        val deepAbyssal = ModServerConfig.DEEP_ABYSSAL_BOUNDARY.get().toFloat()
        val temp = Climate.unquantizeCoord(target.temperature())

        val key: ResourceKey<Biome> = when {
            cont <= deepAbyssal -> ModBiomes.ABYSSAL_OCEAN
            cont <= shallowDeep -> sab_pickDeepOcean(temp)
            else                -> sab_pickShallowOcean(temp)
        }

        cir.returnValue = sab_resolve(key) ?: return
    }
}