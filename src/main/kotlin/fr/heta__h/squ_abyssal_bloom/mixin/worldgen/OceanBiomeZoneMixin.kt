package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.worldgen.AbyssalOceanBiomes
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

@Mixin(value = [MultiNoiseBiomeSource::class], priority = 500)
abstract class OceanBiomeZoneMixin {

    @Shadow
    protected abstract fun collectPossibleBiomes(): Stream<Holder<Biome>>

    @Unique @Volatile
    private var biomeCache: Map<ResourceKey<Biome>, Holder<Biome>>? = null

    @Unique
    private val warnedMissingKeys = mutableSetOf<ResourceKey<Biome>>()

    @Unique
    private fun resolveBiome(key: ResourceKey<Biome>): Holder<Biome>? {
        if (key == AbyssalOceanBiomes.ABYSSAL_OCEAN) {
            AbyssalOceanBiomes.abyssalOceanHolder()?.let { return it }
        }

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
        cancellable = true
    )
    private fun enforceOceanZones(
        quartX: Int,
        quartY: Int,
        quartZ: Int,
        sampler: Climate.Sampler,
        cir: CallbackInfoReturnable<Holder<Biome>>
    ) {
        val terrainTarget = sampler.sample(quartX, 0, quartZ)
        val cont = Climate.unquantizeCoord(terrainTarget.continentalness())

        val actualTarget = sampler.sample(quartX, quartY, quartZ)
        val depth = Climate.unquantizeCoord(actualTarget.depth())
        val temperature = Climate.unquantizeCoord(actualTarget.temperature())

        val biomeKey = AbyssalOceanBiomes.resolveOceanBiomeKey(cont, depth, temperature) ?: return

        val holder = resolveBiome(biomeKey) ?: return
        cir.returnValue = holder
    }
}
