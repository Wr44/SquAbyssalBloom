package fr.heta__h.squ_abyssal_bloom.worldgen.ore

import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import fr.heta__h.squ_abyssal_bloom.config.server.ModServerConfig
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.feature.OreFeature
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import java.util.Collections
import java.util.IdentityHashMap

object AbyssalOreCatalog {
    @Volatile private var oreFeatures: Set<PlacedFeature> = emptySet()
    @Volatile private var configurations: Map<PlacedFeature, OreConfiguration> = emptyMap()
    @Volatile private var idsByFeature: Map<PlacedFeature, Identifier> = emptyMap()

    fun rebuild(server: MinecraftServer) {
        val registry = server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
        val allow = parseIds(ModServerConfig.ABYSSAL_ORE_FEATURE_ALLOWLIST.get())
        val deny = parseIds(ModServerConfig.ABYSSAL_ORE_FEATURE_DENYLIST.get())
        val detected = Collections.newSetFromMap(IdentityHashMap<PlacedFeature, Boolean>())
        val configs = IdentityHashMap<PlacedFeature, OreConfiguration>()
        val ids = IdentityHashMap<PlacedFeature, Identifier>()

        registry.listElements().forEach { holder ->
            val id = holder.key().identifier()
            val placed = holder.value()
            ids[placed] = id
            val configuration = oreConfiguration(placed)
            if (id !in deny && (id in allow || configuration != null)) {
                detected.add(placed)
                if (configuration != null) configs[placed] = configuration
            }
        }

        oreFeatures = Collections.unmodifiableSet(detected)
        configurations = Collections.unmodifiableMap(configs)
        idsByFeature = Collections.unmodifiableMap(ids)
        if (ModServerConfig.ABYSSAL_ORE_CATALOG_DEBUG.get()) {
            SquAbyssalBloom.LOGGER.info(
                "Abyssal ore catalog contains {} placed features: {}",
                detected.size,
                detected.mapNotNull(ids::get).sortedBy(Identifier::toString).joinToString()
            )
        }
    }

    fun clear() {
        oreFeatures = emptySet()
        configurations = emptyMap()
        idsByFeature = emptyMap()
    }

    fun contains(feature: PlacedFeature): Boolean = oreFeatures.contains(feature)

    fun configuration(feature: PlacedFeature): OreConfiguration? = configurations[feature]

    private fun oreConfiguration(placed: PlacedFeature): OreConfiguration? {
        val configured = placed.feature().value()
        return if (configured.feature() is OreFeature) configured.config() as? OreConfiguration else null
    }

    private fun parseIds(values: List<String>): Set<Identifier> = values.mapNotNull { value ->
        Identifier.tryParse(value)?.also { parsed ->
            if (parsed.toString() != value) SquAbyssalBloom.LOGGER.warn("Normalized abyssal ore feature identifier '{}' to '{}'", value, parsed)
        } ?: run {
            SquAbyssalBloom.LOGGER.warn("Ignoring invalid abyssal ore feature identifier '{}'", value)
            null
        }
    }.toSet()
}
