package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean.AbyssalOceanBiomes
import fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean.OceanBiomeRegistry
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Climate
import net.minecraft.world.level.biome.MultiNoiseBiomeSource
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.stream.Stream

@Mixin(value = [MultiNoiseBiomeSource::class], priority = 900)
abstract class OceanBiomeZoneMixin {

    @Shadow
    protected abstract fun collectPossibleBiomes(): Stream<Holder<Biome>>

    @Unique @Volatile
    private var biomeCache: Map<ResourceKey<Biome>, Holder<Biome>>? = null

    @Unique
    private val warnedMissingKeys = mutableSetOf<ResourceKey<Biome>>()

    @Unique
    private fun resolveBiome(key: ResourceKey<Biome>): Holder<Biome>? {
        OceanBiomeRegistry.getHolder(key)?.let { return it }

        val cache = biomeCache ?: buildMap {
            collectPossibleBiomes().forEach { holder ->
                holder.unwrapKey().ifPresent { put(it, holder) }
            }
        }.also { biomeCache = it }

        cache[key]?.let { return it }

        if (warnedMissingKeys.add(key)) {
            SquAbyssalBloom.LOGGER.warn("Failed to resolve biome holder for {}", key.identifier())
        }
        return null
    }

    @Inject(
        method = ["getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate\$Sampler;)Lnet/minecraft/core/Holder;"],
        at = [At("HEAD")],
        cancellable = true,
        remap = false,
    )
    private fun enforceOceanZones(
        quartX: Int,
        quartY: Int,
        quartZ: Int,
        sampler: Climate.Sampler,
        cir: CallbackInfoReturnable<Holder<Biome>>
    ) {
        val target = sampler.sample(quartX, quartY, quartZ)
        val cont = Climate.unquantizeCoord(target.continentalness())
        val depth = Climate.unquantizeCoord(target.depth())
        val biomeKey = AbyssalOceanBiomes.resolveOceanBiomeKey(cont, depth, target) ?: return
        val holder = resolveBiome(biomeKey) ?: return
        cir.returnValue = holder
    }
}
