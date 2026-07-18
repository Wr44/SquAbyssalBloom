package fr.heta__h.squ_abyssal_bloom.worldgen.ocean

import fr.heta__h.squ_abyssal_bloom.tags.ModTags
import fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean.OceanBiomeEntry
import fr.heta__h.squ_abyssal_bloom.util.worldgen.ocean.OceanZone
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
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
    private val abyssalByNamespace = mutableMapOf<String, MutableList<OceanBiomeEntry>>()
    private val deepPoolsByNamespace = Array(5) { mutableMapOf<String, MutableList<OceanBiomeEntry>>() }
    private val shallowPoolsByNamespace = Array(5) { mutableMapOf<String, MutableList<OceanBiomeEntry>>() }
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

        val namespaces = registeredKeys.map { it.identifier().namespace }.toSet()
        val territoryNamespaces = if (ModServerConfig.OCEAN_TERRITORY_INCLUDE_VANILLA.get()) namespaces else namespaces - "minecraft"

        OceanTerritory.setCandidates(
            territoryNamespaces,
            defaultWeight = ModServerConfig.OCEAN_TERRITORY_DEFAULT_WEIGHT.get(),
            namespaceWeights = mapOf(SquAbyssalBloom.ID to ModServerConfig.OCEAN_TERRITORY_OWN_WEIGHT.get())
        )
    }

    fun pickBiome(zone: OceanZone, target: Climate.TargetPoint, x: Int, z: Int): ResourceKey<Biome>? {
        return when (zone) {
            OceanZone.ABYSSAL -> pickFromPoolByTerritory(abyssalEntries, abyssalByNamespace, target, x, z)
                ?: AbyssalOceanBiomes.ABYSSAL_OCEAN
            OceanZone.DEEP -> pickFromDeepPools(target, x, z)
                ?: fallbackDeep(target)
            OceanZone.SHALLOW -> pickFromShallowPools(target, x, z)
                ?: fallbackShallow(target)
        }
    }

    fun abyssalEntries(): List<OceanBiomeEntry> = abyssalEntries.toList()

    fun deepEntries(): List<OceanBiomeEntry> = deepPools.flatMap { it }.distinctBy { it.key }

    fun shallowEntries(): List<OceanBiomeEntry> = shallowPools.flatMap { it }.distinctBy { it.key }

    private fun reset() {
        abyssalEntries.clear()
        deepPools.forEach { it.clear() }
        shallowPools.forEach { it.clear() }
        abyssalByNamespace.clear()
        deepPoolsByNamespace.forEach { it.clear() }
        shallowPoolsByNamespace.forEach { it.clear() }
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
        val namespace = entry.key.identifier().namespace
        when (entry.zone) {
            OceanZone.ABYSSAL -> addToPool(abyssalEntries, abyssalByNamespace, namespace, entry)
            OceanZone.DEEP -> bandsFor(entry).forEach { addToPool(deepPools[it], deepPoolsByNamespace[it], namespace, entry) }
            OceanZone.SHALLOW -> bandsFor(entry).forEach { addToPool(shallowPools[it], shallowPoolsByNamespace[it], namespace, entry) }
        }
    }

    private fun addToPool(pool: MutableList<OceanBiomeEntry>, byNamespace: MutableMap<String, MutableList<OceanBiomeEntry>>, namespace: String, entry: OceanBiomeEntry) {
        if (pool.any { it.key == entry.key }) return
        pool.add(entry)
        byNamespace.getOrPut(namespace) { mutableListOf() }.add(entry)
        registeredKeys.add(entry.key)
    }

    private fun bandsFor(entry: OceanBiomeEntry): Set<Int> {
        val bands = mutableSetOf<Int>()
        for (point in entry.referencePoints) {
            val lo = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(point.temperature().min()))
            val hi = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(point.temperature().max()))
            for (band in lo..hi) bands.add(band)
        }
        if (bands.isEmpty()) bands.add(entry.tempBand)
        return bands
    }

    private fun pickFromPool(pool: List<OceanBiomeEntry>, target: Climate.TargetPoint): ResourceKey<Biome>? {
        if (pool.isEmpty()) return null
        return pool.minWithOrNull(
            compareBy<OceanBiomeEntry> { it.fitness(target) }
                .thenBy { if (AbyssalOceanBiomes.overrideFor(it.key) != null) 0 else 1 }
        )?.key
    }

    private fun pickFromPoolByTerritory(
        pool: List<OceanBiomeEntry>,
        byNamespace: Map<String, List<OceanBiomeEntry>>,
        target: Climate.TargetPoint,
        x: Int,
        z: Int
    ): ResourceKey<Biome>? {
        if (pool.isEmpty()) return null
        val owner = OceanTerritory.ownerAt(x, z)
        val territoryPool = byNamespace[owner]
        if (territoryPool.isNullOrEmpty()) return pickFromPool(pool, target)
        return pickFromPool(territoryPool, target)
    }

    private fun pickFromDeepPools(target: Climate.TargetPoint, x: Int, z: Int): ResourceKey<Biome>? {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return pickFromPoolByTerritory(deepPools[band], deepPoolsByNamespace[band], target, x, z)
            ?: pickFromPool(deepPools.flatMap { it }, target)
    }

    private fun pickFromShallowPools(target: Climate.TargetPoint, x: Int, z: Int): ResourceKey<Biome>? {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return pickFromPoolByTerritory(shallowPools[band], shallowPoolsByNamespace[band], target, x, z)
            ?: pickFromPool(shallowPools.flatMap { it }, target)
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
                addToPool(shallowPools[band], shallowPoolsByNamespace[band], key.identifier().namespace, OceanBiomeClassifier.createEntry(OceanZone.SHALLOW, key))
                captureHolder(registry, key)
            }
        }

        AbyssalOceanBiomes.DEEP_OCEANS.forEachIndexed { band, key ->
            if (deepPools[band].isEmpty() && registry.containsKey(key)) {
                addToPool(deepPools[band], deepPoolsByNamespace[band], key.identifier().namespace, OceanBiomeClassifier.createEntry(OceanZone.DEEP, key))
                captureHolder(registry, key)
            }
        }

        if (abyssalEntries.isEmpty() && registry.containsKey(AbyssalOceanBiomes.ABYSSAL_OCEAN)) {
            registerEntry(registry, OceanBiomeClassifier.createEntry(OceanZone.ABYSSAL, AbyssalOceanBiomes.ABYSSAL_OCEAN))
        }
    }
}