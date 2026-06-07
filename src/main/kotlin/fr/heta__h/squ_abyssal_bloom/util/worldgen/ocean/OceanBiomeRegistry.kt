package fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean

import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.BiomeTags
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Climate

object OceanBiomeRegistry {

    private val abyssalEntries = mutableListOf<OceanBiomeEntry>()
    private val deepPools = Array(5) { mutableListOf<OceanBiomeEntry>() }
    private val shallowPools = Array(5) { mutableListOf<OceanBiomeEntry>() }
    private val holderByKey = mutableMapOf<ResourceKey<Biome>, Holder<Biome>>()
    private val registeredKeys = mutableSetOf<ResourceKey<Biome>>()

    fun getHolder(key: ResourceKey<Biome>): Holder<Biome>? = holderByKey[key]

    fun bootstrap(registry: Registry<Biome>) {
        reset()

        bootstrapFromTag(registry, ModTags.Biomes.IS_ABYSSAL, OceanZone.ABYSSAL)
        bootstrapFromTag(registry, BiomeTags.IS_DEEP_OCEAN, OceanZone.DEEP)
        bootstrapFromTag(registry, ModTags.Biomes.IS_DEEP_OCEAN, OceanZone.DEEP)

        for (holder in registry.getTagOrEmpty(BiomeTags.IS_OCEAN)) {
            if (!OceanBiomeClassifier.isExcludedFromShallow(holder)) {
                addEntry(createFromHolder(holder, OceanZone.SHALLOW))
            }
        }

        for (id in registry.keySet()) {
            val key = ResourceKey.create(Registries.BIOME, id)
            if (registeredKeys.contains(key)) continue
            if (OceanBiomeClassifier.isInTag(registry, key, ModTags.Biomes.IS_ABYSSAL)) continue

            val zone = OceanBiomeClassifier.classifyByContinentalness(key) ?: continue
            registerEntry(registry, OceanBiomeClassifier.createEntry(zone, key))
        }

        seedVanillaDefaults(registry)
    }

    fun pickBiome(zone: OceanZone, target: Climate.TargetPoint, regionIndex: Int = 0): ResourceKey<Biome>? {
        return when (zone) {
            OceanZone.ABYSSAL -> pickFromPool(abyssalEntries, target)
                ?: AbyssalOceanBiomes.ABYSSAL_OCEAN
            OceanZone.DEEP -> pickFromDeepPools(target, regionIndex)
                ?: fallbackDeep(target)
            OceanZone.SHALLOW -> pickFromShallowPools(target, regionIndex)
                ?: fallbackShallow(target)
        }
    }

    fun abyssalEntries(): List<OceanBiomeEntry> = abyssalEntries.toList()

    fun deepEntries(): List<OceanBiomeEntry> = deepPools.flatMap { it.toList() }

    fun shallowEntries(): List<OceanBiomeEntry> = shallowPools.flatMap { it.toList() }

    private fun reset() {
        abyssalEntries.clear()
        deepPools.forEach { it.clear() }
        shallowPools.forEach { it.clear() }
        holderByKey.clear()
        registeredKeys.clear()
    }

    private fun bootstrapFromTag(registry: Registry<Biome>, tag: TagKey<Biome>, zone: OceanZone) {
        for (holder in registry.getTagOrEmpty(tag)) {
            addEntry(createFromHolder(holder, zone))
        }
    }

    private fun createFromHolder(holder: Holder<Biome>, zone: OceanZone): OceanBiomeEntry? {
        val key = holder.unwrapKey().orElse(null) ?: return null
        holderByKey[key] = holder
        return OceanBiomeClassifier.createEntry(zone, key)
    }

    private val oceanTags = listOf(
        ModTags.Biomes.IS_ABYSSAL,
        BiomeTags.IS_DEEP_OCEAN,
        ModTags.Biomes.IS_DEEP_OCEAN,
        BiomeTags.IS_OCEAN
    )

    private fun registerEntry(registry: Registry<Biome>, entry: OceanBiomeEntry?) {
        if (entry == null) return
        addEntry(entry)
        captureHolder(registry, entry.key)
    }

    private fun captureHolder(registry: Registry<Biome>, key: ResourceKey<Biome>) {
        if (holderByKey.containsKey(key)) return

        for (tag in oceanTags) {
            for (holder in registry.getTagOrEmpty(tag)) {
                if (holder.unwrapKey().orElse(null) == key) {
                    holderByKey[key] = holder
                    return
                }
            }
        }

        if (registry.containsKey(key)) {
            holderByKey[key] = registry.getOrThrow(key)
        }
    }

    private fun addEntry(entry: OceanBiomeEntry?) {
        if (entry == null) return

        val pool = when (entry.zone) {
            OceanZone.ABYSSAL -> abyssalEntries
            OceanZone.DEEP -> deepPools[entry.tempBand]
            OceanZone.SHALLOW -> shallowPools[entry.tempBand]
        }

        if (pool.any { it.key == entry.key }) return

        pool.add(entry)
        registeredKeys.add(entry.key)
    }

    private fun pickFromPool(
        pool: List<OceanBiomeEntry>,
        target: Climate.TargetPoint
    ): ResourceKey<Biome>? {
        if (pool.isEmpty()) return null
        return pool.minByOrNull { it.fitness(target) }?.key
    }

    private fun pickFromDeepPools(target: Climate.TargetPoint, regionIndex: Int): ResourceKey<Biome>? {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return pickFromPool(deepPools[band], target)
            ?: deepPools.flatMap { it }.minByOrNull { it.fitness(target) }?.key
    }

    private fun pickFromShallowPools(target: Climate.TargetPoint, regionIndex: Int): ResourceKey<Biome>? {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return pickFromPool(shallowPools[band], target)
            ?: shallowPools.flatMap { it }.minByOrNull { it.fitness(target) }?.key
    }

    private fun fallbackDeep(target: Climate.TargetPoint): ResourceKey<Biome> {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return AbyssalOceanBiomes.fallbackDeepOcean(band)
    }

    private fun fallbackShallow(target: Climate.TargetPoint): ResourceKey<Biome> {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return AbyssalOceanBiomes.fallbackShallowOcean(band)
    }

    private fun seedVanillaDefaults(registry: Registry<Biome>) {
        AbyssalOceanBiomes.SHALLOW_OCEANS.forEachIndexed { band, key ->
            if (shallowPools[band].isEmpty() && registry.containsKey(key)) {
                registerEntry(registry, OceanBiomeClassifier.createEntry(OceanZone.SHALLOW, key))
            }
        }

        AbyssalOceanBiomes.DEEP_OCEANS.forEachIndexed { band, key ->
            if (deepPools[band].isEmpty() && registry.containsKey(key)) {
                registerEntry(registry, OceanBiomeClassifier.createEntry(OceanZone.DEEP, key))
            }
        }

        if (abyssalEntries.isEmpty() && registry.containsKey(AbyssalOceanBiomes.ABYSSAL_OCEAN)) {
            registerEntry(registry, OceanBiomeClassifier.createEntry(OceanZone.ABYSSAL, AbyssalOceanBiomes.ABYSSAL_OCEAN))
        }
    }
}