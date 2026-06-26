package fr.heta__h.squ_abyssal_bloom.worldgen.ocean

import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean.OceanZone
import fr.heta__h.squ_abyssal_bloom.worldgen.ModBiomes
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.biome.Climate

object AbyssalOceanBiomes {

    data class BiomeOverride(
        val temperature: Climate.Parameter? = null,
        val humidity: Climate.Parameter? = null,
        val continentalness: Climate.Parameter? = null,
        val erosion: Climate.Parameter? = null,
        val weirdness: Climate.Parameter? = null,
        val depth: Climate.Parameter? = null,
        val offset: Float = 0.0f
    )

    val BIOME_OVERRIDES: Map<ResourceKey<Biome>, BiomeOverride> = mapOf(
        ModBiomes.BLOOD_VALLEY to BiomeOverride(
            temperature = Climate.Parameter.span(0.2f, 0.9f),
            humidity = Climate.Parameter.span(0.3f, 1.0f)
        ),
    )

    fun overrideFor(key: ResourceKey<Biome>): BiomeOverride? = BIOME_OVERRIDES[key]

    const val OCEAN_MAX_CONT: Float = -0.19f
    const val OCEAN_MIN_CONT: Float = -1.05f

    val ABYSSAL_OCEAN: ResourceKey<Biome> = ModBiomes.ABYSSAL_OCEAN

    val FULL_RANGE: Climate.Parameter = Climate.Parameter.span(-1.0f, 1.0f)
    val SURFACE_DEPTH: Climate.Parameter = Climate.Parameter.point(0.0f)
    val FLOOR_DEPTH: Climate.Parameter = Climate.Parameter.point(1.0f)

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
        Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN
    )

    fun shallowDeep(): Float = ModServerConfig.SHALLOW_DEEP_BOUNDARY.get().toFloat()

    fun deepAbyssal(): Float = ModServerConfig.DEEP_ABYSSAL_BOUNDARY.get().toFloat()

    fun vanillaDeepCont(): Climate.Parameter =
        Climate.Parameter.span(OCEAN_MIN_CONT, shallowDeep())

    fun vanillaOceanCont(): Climate.Parameter =
        Climate.Parameter.span(shallowDeep(), OCEAN_MAX_CONT)

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

    fun fallbackDeepOcean(tempIndex: Int): ResourceKey<Biome> =
        DEEP_OCEANS.getOrElse(tempIndex) { Biomes.DEEP_OCEAN }

    fun fallbackShallowOcean(tempIndex: Int): ResourceKey<Biome> =
        SHALLOW_OCEANS.getOrElse(tempIndex) { Biomes.OCEAN }

    fun resolveOceanZone(cont: Float, depth: Float): OceanZone? {
        if (cont !in OCEAN_MIN_CONT..OCEAN_MAX_CONT) return null

        val deepAbyssal = deepAbyssal()
        val shallowDeep = shallowDeep()

        return when {
            cont <= deepAbyssal -> OceanZone.ABYSSAL
            cont <= shallowDeep -> if (depth >= 0.8f) null else OceanZone.DEEP
            else -> if (depth >= 0.8f) null else OceanZone.SHALLOW
        }
    }

    fun resolveOceanBiomeKey(cont: Float, depth: Float, target: Climate.TargetPoint, regionIndex: Int = 0): ResourceKey<Biome>? {
        val zone = resolveOceanZone(cont, depth) ?: return null
        return OceanBiomeRegistry.pickBiome(zone, target, regionIndex)
    }
}