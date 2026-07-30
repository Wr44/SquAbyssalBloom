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

    private class RegistryState {
        val abyssalEntries = mutableListOf<OceanBiomeEntry>()
        val deepPools = Array(5) { mutableListOf<OceanBiomeEntry>() }
        val shallowPools = Array(5) { mutableListOf<OceanBiomeEntry>() }
        val abyssalByNamespace = mutableMapOf<String, MutableList<OceanBiomeEntry>>()
        val deepPoolsByNamespace = Array(5) { mutableMapOf<String, MutableList<OceanBiomeEntry>>() }
        val shallowPoolsByNamespace = Array(5) { mutableMapOf<String, MutableList<OceanBiomeEntry>>() }
        val holderByKey = mutableMapOf<ResourceKey<Biome>, Holder<Biome>>()
        val registeredKeys = mutableSetOf<ResourceKey<Biome>>()
    }

    @Volatile
    private var state = RegistryState()

    fun getHolder(key: ResourceKey<Biome>): Holder<Biome>? = state.holderByKey[key]

    fun bootstrap(registry: Registry<Biome>) {
        val newState = RegistryState()

        bootstrapFromTag(registry, ModTags.Biomes.IS_ABYSSAL, OceanZone.ABYSSAL, newState)
        bootstrapFromTag(registry, BiomeTags.IS_DEEP_OCEAN, OceanZone.DEEP, newState)
        bootstrapFromTag(registry, ModTags.Biomes.IS_DEEP_OCEAN, OceanZone.DEEP, newState)

        for (holder in registry.getTagOrEmpty(BiomeTags.IS_OCEAN)) {
            if (!OceanBiomeClassifier.isExcludedFromShallow(holder)) {
                addEntry(createFromHolder(holder, OceanZone.SHALLOW, newState), newState)
            }
        }

        for (id in registry.keySet()) {
            val key = ResourceKey.create(Registries.BIOME, id)
            if (newState.registeredKeys.contains(key)) continue
            if (OceanBiomeClassifier.isInTag(registry, key, ModTags.Biomes.IS_ABYSSAL)) continue

            val zone = OceanBiomeClassifier.classifyByContinentalness(key) ?: continue
            registerEntry(registry, OceanBiomeClassifier.createEntry(zone, key), newState)
        }

        seedVanillaDefaults(registry, newState)

        this.state = newState

        val namespaces = newState.registeredKeys.map { it.identifier().namespace }.toSet()
        val territoryNamespaces = if (ModServerConfig.OCEAN_TERRITORY_INCLUDE_VANILLA.get()) namespaces else namespaces - "minecraft"

        OceanTerritory.setCandidates(
            territoryNamespaces,
            defaultWeight = ModServerConfig.OCEAN_TERRITORY_DEFAULT_WEIGHT.get(),
            namespaceWeights = mapOf(SquAbyssalBloom.ID to ModServerConfig.OCEAN_TERRITORY_OWN_WEIGHT.get())
        )
    }

    fun pickBiome(zone: OceanZone, target: Climate.TargetPoint, x: Int, z: Int): ResourceKey<Biome>? {
        val currentState = this.state
        return when (zone) {
            OceanZone.ABYSSAL -> pickFromPoolByTerritory(currentState.abyssalEntries, currentState.abyssalByNamespace, target, x, z)
                ?: AbyssalOceanBiomes.ABYSSAL_OCEAN
            OceanZone.DEEP -> pickFromDeepPools(currentState, target, x, z)
                ?: fallbackDeep(target)
            OceanZone.SHALLOW -> pickFromShallowPools(currentState, target, x, z)
                ?: fallbackShallow(target)
        }
    }

    fun abyssalEntries(): List<OceanBiomeEntry> = state.abyssalEntries.toList()

    fun deepEntries(): List<OceanBiomeEntry> = state.deepPools.flatMap { it }.distinctBy { it.key }

    fun shallowEntries(): List<OceanBiomeEntry> = state.shallowPools.flatMap { it }.distinctBy { it.key }

    private fun bootstrapFromTag(registry: Registry<Biome>, tag: TagKey<Biome>, zone: OceanZone, newState: RegistryState) {
        for (holder in registry.getTagOrEmpty(tag)) {
            addEntry(createFromHolder(holder, zone, newState), newState)
        }
    }

    private fun createFromHolder(holder: Holder<Biome>, zone: OceanZone, newState: RegistryState): OceanBiomeEntry? {
        val key = holder.unwrapKey().orElse(null) ?: return null
        newState.holderByKey[key] = holder
        return OceanBiomeClassifier.createEntry(zone, key)
    }

    private val oceanTags = listOf(
        ModTags.Biomes.IS_ABYSSAL,
        BiomeTags.IS_DEEP_OCEAN,
        ModTags.Biomes.IS_DEEP_OCEAN,
        BiomeTags.IS_OCEAN
    )

    private fun registerEntry(registry: Registry<Biome>, entry: OceanBiomeEntry?, newState: RegistryState) {
        if (entry == null) return
        addEntry(entry, newState)
        captureHolder(registry, entry.key, newState)
    }

    private fun captureHolder(registry: Registry<Biome>, key: ResourceKey<Biome>, newState: RegistryState) {
        if (newState.holderByKey.containsKey(key)) return

        for (tag in oceanTags) {
            for (holder in registry.getTagOrEmpty(tag)) {
                if (holder.unwrapKey().orElse(null) == key) {
                    newState.holderByKey[key] = holder
                    return
                }
            }
        }

        if (registry.containsKey(key)) {
            newState.holderByKey[key] = registry.getOrThrow(key)
        }
    }

    private fun addEntry(entry: OceanBiomeEntry?, newState: RegistryState) {
        if (entry == null) return
        val namespace = entry.key.identifier().namespace
        when (entry.zone) {
            OceanZone.ABYSSAL -> addToPool(newState.abyssalEntries, newState.abyssalByNamespace, namespace, entry, newState)
            OceanZone.DEEP -> bandsFor(entry).forEach { addToPool(newState.deepPools[it], newState.deepPoolsByNamespace[it], namespace, entry, newState) }
            OceanZone.SHALLOW -> bandsFor(entry).forEach { addToPool(newState.shallowPools[it], newState.shallowPoolsByNamespace[it], namespace, entry, newState) }
        }
    }

    private fun addToPool(pool: MutableList<OceanBiomeEntry>, byNamespace: MutableMap<String, MutableList<OceanBiomeEntry>>, namespace: String, entry: OceanBiomeEntry, newState: RegistryState) {
        if (pool.any { it.key == entry.key }) return
        pool.add(entry)
        byNamespace.getOrPut(namespace) { mutableListOf() }.add(entry)
        newState.registeredKeys.add(entry.key)
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

    private fun pickFromDeepPools(currentState: RegistryState, target: Climate.TargetPoint, x: Int, z: Int): ResourceKey<Biome>? {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return pickFromPoolByTerritory(currentState.deepPools[band], currentState.deepPoolsByNamespace[band], target, x, z)
            ?: pickFromPool(currentState.deepPools.flatMap { it }, target)
    }

    private fun pickFromShallowPools(currentState: RegistryState, target: Climate.TargetPoint, x: Int, z: Int): ResourceKey<Biome>? {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return pickFromPoolByTerritory(currentState.shallowPools[band], currentState.shallowPoolsByNamespace[band], target, x, z)
            ?: pickFromPool(currentState.shallowPools.flatMap { it }, target)
    }

    private fun fallbackDeep(target: Climate.TargetPoint): ResourceKey<Biome> {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return AbyssalOceanBiomes.fallbackDeepOcean(band)
    }

    private fun fallbackShallow(target: Climate.TargetPoint): ResourceKey<Biome> {
        val band = AbyssalOceanBiomes.temperatureIndex(Climate.unquantizeCoord(target.temperature()))
        return AbyssalOceanBiomes.fallbackShallowOcean(band)
    }

    private fun seedVanillaDefaults(registry: Registry<Biome>, newState: RegistryState) {
        AbyssalOceanBiomes.SHALLOW_OCEANS.forEachIndexed { band, key ->
            if (newState.shallowPools[band].isEmpty() && registry.containsKey(key)) {
                addToPool(newState.shallowPools[band], newState.shallowPoolsByNamespace[band], key.identifier().namespace, OceanBiomeClassifier.createEntry(OceanZone.SHALLOW, key), newState)
                captureHolder(registry, key, newState)
            }
        }

        AbyssalOceanBiomes.DEEP_OCEANS.forEachIndexed { band, key ->
            if (newState.deepPools[band].isEmpty() && registry.containsKey(key)) {
                addToPool(newState.deepPools[band], newState.deepPoolsByNamespace[band], key.identifier().namespace, OceanBiomeClassifier.createEntry(OceanZone.DEEP, key), newState)
                captureHolder(registry, key, newState)
            }
        }

        if (newState.abyssalEntries.isEmpty() && registry.containsKey(AbyssalOceanBiomes.ABYSSAL_OCEAN)) {
            registerEntry(registry, OceanBiomeClassifier.createEntry(OceanZone.ABYSSAL, AbyssalOceanBiomes.ABYSSAL_OCEAN), newState)
        }
    }
}