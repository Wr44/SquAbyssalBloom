package fr.heta__h.squ_abyssal_bloom.worldgen

import fr.heta__h.squ_abyssal_bloom.config.ServerConfigCache
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.biome.Climate

object AbyssalOceanBiomes {

    const val OCEAN_MAX_CONT: Float = -0.19f
    const val OCEAN_MIN_CONT: Float = -1.05f

    val ABYSSAL_OCEAN: ResourceKey<Biome> = ModBiomes.ABYSSAL_OCEAN

    val FULL_RANGE: Climate.Parameter = Climate.Parameter.span(-1.0f, 1.0f)
    val SURFACE_DEPTH: Climate.Parameter = Climate.Parameter.point(0.0f)
    val FLOOR_DEPTH: Climate.Parameter = Climate.Parameter.point(1.0f)

    val VANILLA_DEEP_CONT: Climate.Parameter = Climate.Parameter.span(-1.05f, -0.455f)
    val VANILLA_OCEAN_CONT: Climate.Parameter = Climate.Parameter.span(-0.455f, -0.19f)

    val TEMPERATURES: Array<Climate.Parameter> = arrayOf(
        Climate.Parameter.span(-1.0f, -0.45f),
        Climate.Parameter.span(-0.45f, -0.15f),
        Climate.Parameter.span(-0.15f, 0.2f),
        Climate.Parameter.span(0.2f, 0.55f),
        Climate.Parameter.span(0.55f, 1.0f)
    )

    val SHALLOW_OCEANS: Array<ResourceKey<Biome>> = arrayOf(
        Biomes.FROZEN_OCEAN, Biomes.COLD_OCEAN, Biomes.OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.WARM_OCEAN
    )

    val DEEP_OCEANS: Array<ResourceKey<Biome>> = arrayOf(
        Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN
    )

    @Volatile
    private var abyssalOceanHolder: Holder<Biome>? = null

    fun registerAbyssalOceanHolder(holder: Holder<Biome>) {
        abyssalOceanHolder = holder
    }

    fun abyssalOceanHolder(): Holder<Biome>? = abyssalOceanHolder

    fun shallowDeep(): Float = ServerConfigCache.effectiveShallowDeep

    fun deepAbyssal(): Float = ServerConfigCache.effectiveDeepAbyssal

    fun abyssalContinentalness(): Climate.Parameter =
        Climate.Parameter.span(OCEAN_MIN_CONT, deepAbyssal())

    fun deepContinentalness(): Climate.Parameter =
        Climate.Parameter.span(deepAbyssal(), shallowDeep())

    fun shallowContinentalness(): Climate.Parameter =
        Climate.Parameter.span(shallowDeep(), OCEAN_MAX_CONT)

    fun temperatureIndex(temp: Float): Int = when {
        temp < -0.45f -> 0
        temp < -0.15f -> 1
        temp < 0.2f -> 2
        temp < 0.55f -> 3
        else -> 4
    }

    fun resolveOceanBiomeKey(cont: Float, depth: Float, temperature: Float): ResourceKey<Biome>? {
        if (cont > OCEAN_MAX_CONT || cont < OCEAN_MIN_CONT) return null

        val deepAbyssal = deepAbyssal()
        val shallowDeep = shallowDeep()

        return when {
            cont <= deepAbyssal -> {
                if (depth >= 1.0f) null
                else ABYSSAL_OCEAN
            }
            cont <= shallowDeep -> {
                if (depth >= 0.8f) null
                else DEEP_OCEANS[temperatureIndex(temperature)]
            }
            else -> {
                if (depth >= 0.8f) null
                else SHALLOW_OCEANS[temperatureIndex(temperature)]
            }
        }
    }
}
