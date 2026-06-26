package fr.heta__h.squ_abyssal_bloom.worldgen.ocean

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean.OceanBiomeEntry
import fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean.OceanZone
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.BiomeTags
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Climate
import terrablender.worldgen.RegionUtils
import kotlin.math.abs

object OceanBiomeClassifier {

    private const val VANILLA_DEEP_CENTER = (-1.05f + -0.455f) / 2f
    private const val VANILLA_OCEAN_CENTER = (-0.455f + -0.19f) / 2f

    fun isExcludedFromShallow(holder: Holder<Biome>): Boolean {
        return holder.`is`(BiomeTags.IS_DEEP_OCEAN)
                || holder.`is`(ModTags.Biomes.IS_ABYSSAL)
                || holder.`is`(ModTags.Biomes.IS_DEEP_OCEAN)
    }

    fun isInTag(registry: Registry<Biome>, key: ResourceKey<Biome>, tag: TagKey<Biome>): Boolean {
        for (holder in registry.getTagOrEmpty(tag)) {
            if (holder.unwrapKey().orElse(null) == key) return true
        }
        return false
    }

    fun classifyByContinentalness(key: ResourceKey<Biome>): OceanZone? {
        val points = RegionUtils.getVanillaParameterPoints(key)
        if (points.isEmpty()) return null

        val zoneVotes = mutableMapOf<OceanZone, Int>()
        for (point in points) {
            classifyContinentalness(point.continentalness())?.let { zone ->
                zoneVotes[zone] = zoneVotes.getOrDefault(zone, 0) + 1
            }
        }

        return zoneVotes.maxByOrNull { it.value }?.key
    }

    fun createEntry(zone: OceanZone, key: ResourceKey<Biome>): OceanBiomeEntry {
        val vanillaPoints = RegionUtils.getVanillaParameterPoints(key)
        val tempBand = resolveTempBand(key, vanillaPoints.firstOrNull())
        val points = if (vanillaPoints.isNotEmpty()) {
            vanillaPoints
        } else {
            if (zone != OceanZone.ABYSSAL && AbyssalOceanBiomes.overrideFor(key) == null) {
                SquAbyssalBloom.LOGGER.warn("Biome custom {} sans points vanilla ni BiomeOverride, retombe sur la bande 2", key.identifier())
            }
            listOf(syntheticReferencePoint(zone, tempBand, key))
        }
        return OceanBiomeEntry(zone, tempBand, key, points)
    }

    private fun classifyContinentalness(parameter: Climate.Parameter): OceanZone? {
        val min = Climate.unquantizeCoord(parameter.min())
        val max = Climate.unquantizeCoord(parameter.max())

        val shallowDeep = AbyssalOceanBiomes.shallowDeep()
        val deepAbyssal = AbyssalOceanBiomes.deepAbyssal()

        if (fullyWithin(min, max, AbyssalOceanBiomes.OCEAN_MIN_CONT, deepAbyssal)) return null
        if (fullyWithin(min, max, deepAbyssal, shallowDeep)) return OceanZone.DEEP
        if (fullyWithin(min, max, shallowDeep, AbyssalOceanBiomes.OCEAN_MAX_CONT)) return OceanZone.SHALLOW

        val center = (min + max) / 2f
        return if (distance(center, VANILLA_DEEP_CENTER) <= distance(center, VANILLA_OCEAN_CENTER)) {
            OceanZone.DEEP
        } else {
            OceanZone.SHALLOW
        }
    }

    private fun fullyWithin(valueMin: Float, valueMax: Float, zoneMin: Float, zoneMax: Float): Boolean {
        return valueMin >= zoneMin && valueMax <= zoneMax
    }

    private fun distance(a: Float, b: Float): Float = abs(a - b)

    private fun resolveTempBand(key: ResourceKey<Biome>, referencePoint: Climate.ParameterPoint?): Int {
        AbyssalOceanBiomes.overrideFor(key)?.temperature?.let {
            return tempBandFromParameter(it)
        }

        referencePoint?.let {
            return tempBandFromParameter(it.temperature())
        }

        AbyssalOceanBiomes.SHALLOW_OCEANS.indexOf(key).takeIf { it >= 0 }?.let { return it }
        AbyssalOceanBiomes.DEEP_OCEANS.indexOf(key).takeIf { it >= 0 }?.let { return it }

        val points = RegionUtils.getVanillaParameterPoints(key)
        if (points.isNotEmpty()) {
            return tempBandFromParameter(points.first().temperature())
        }

        return 2
    }

    private fun tempBandFromParameter(temperature: Climate.Parameter): Int {
        val center = (Climate.unquantizeCoord(temperature.min()) + Climate.unquantizeCoord(temperature.max())) / 2f
        return AbyssalOceanBiomes.temperatureIndex(center)
    }

    private fun syntheticReferencePoint(zone: OceanZone, tempBand: Int, key: ResourceKey<Biome>): Climate.ParameterPoint {
        val continentalness = when (zone) {
            OceanZone.ABYSSAL -> AbyssalOceanBiomes.abyssalContinentalness()
            OceanZone.DEEP -> AbyssalOceanBiomes.deepContinentalness()
            OceanZone.SHALLOW -> AbyssalOceanBiomes.shallowContinentalness()
        }
        val override = AbyssalOceanBiomes.overrideFor(key)
        val temperature = override?.temperature ?: if (zone == OceanZone.ABYSSAL) {
            AbyssalOceanBiomes.FULL_RANGE
        } else {
            AbyssalOceanBiomes.TEMPERATURES[tempBand]
        }

        return Climate.parameters(
            temperature,
            override?.humidity ?: AbyssalOceanBiomes.FULL_RANGE,
            continentalness,
            override?.erosion ?: AbyssalOceanBiomes.FULL_RANGE,
            override?.depth ?: AbyssalOceanBiomes.SURFACE_DEPTH,
            override?.weirdness ?: AbyssalOceanBiomes.FULL_RANGE,
            0f
        )
    }
}