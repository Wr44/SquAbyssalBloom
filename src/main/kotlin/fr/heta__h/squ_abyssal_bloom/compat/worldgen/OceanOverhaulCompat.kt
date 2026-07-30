package fr.heta__h.squ_abyssal_bloom.compat.worldgen

import net.neoforged.fml.ModList

object OceanOverhaulCompat {
    val hasTectonic: Boolean by lazy { ModList.get().isLoaded("tectonic") }
    val hasDeeperOceans: Boolean by lazy { ModList.get().isLoaded("deeper_oceans") }
    val hasTerralith: Boolean by lazy { ModList.get().isLoaded("terralith") }

    val hasTerrainMod: Boolean get() = hasTectonic || hasDeeperOceans || hasTerralith
}