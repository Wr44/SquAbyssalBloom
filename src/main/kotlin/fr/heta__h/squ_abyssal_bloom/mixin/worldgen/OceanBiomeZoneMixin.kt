package fr.heta__h.squ_abyssal_bloom.mixin.worldgen

import fr.heta__h.squ_abyssal_bloom.config.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache
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
    private fun resolveBiome(key: ResourceKey<Biome>): Holder<Biome>? {
        val map = biomeCache ?: buildMap {
            collectPossibleBiomes().forEach { holder ->
                holder.unwrapKey().ifPresent { put(it, holder) }
            }
        }.also { biomeCache = it }
        return map[key]
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

        if (cont > -0.19f || cont < -1.05f) return

        val actualTarget = sampler.sample(quartX, quartY, quartZ)
        val depth = Climate.unquantizeCoord(actualTarget.depth())

        val deepAbyssal = ServerConfigCache.effectiveDeepAbyssal
        val shallowDeep = ServerConfigCache.effectiveShallowDeep

        val finalKey: ResourceKey<Biome>

        if (cont <= deepAbyssal) {
            if (depth >= 1.0f) return
            finalKey = AbyssalOceanBiomes.ABYSSAL_OCEAN

        } else if (cont <= shallowDeep) {
            if (depth >= 0.8f) return
            val tempIdx = getTemperatureIndex(Climate.unquantizeCoord(actualTarget.temperature()))
            finalKey = AbyssalOceanBiomes.DEEP_OCEANS[tempIdx]

        } else {
            if (depth >= 0.8f) return
            val tempIdx = getTemperatureIndex(Climate.unquantizeCoord(actualTarget.temperature()))
            finalKey = AbyssalOceanBiomes.SHALLOW_OCEANS[tempIdx]
        }

        cir.returnValue = resolveBiome(finalKey) ?: return
    }

    @Unique
    private fun getTemperatureIndex(temp: Float): Int {
        return when {
            temp < -0.45f -> 0
            temp < -0.15f -> 1
            temp < 0.2f -> 2
            temp < 0.55f -> 3
            else -> 4
        }
    }
}