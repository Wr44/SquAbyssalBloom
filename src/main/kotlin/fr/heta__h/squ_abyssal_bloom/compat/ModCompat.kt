package fr.heta__h.squ_abyssal_bloom.compat

import net.neoforged.fml.ModList

object ModCompat {

    val dynLightsModId: String? by lazy {
        ModList.get().mods.firstOrNull { it.modId.contains("lambdynlights", ignoreCase = true) }?.modId
    }

    val hasDynLights: Boolean
        get() = dynLightsModId != null

    val irisModId: String? by lazy {
        ModList.get().mods.firstOrNull { it.modId.contains("iris", ignoreCase = true) }?.modId
    }

    val hasIris: Boolean
        get() = irisModId != null

    val tectonicModId: String? by lazy {
        ModList.get().mods.firstOrNull { it.modId.contains("tectonic", ignoreCase = true) }?.modId
    }

    val hasTectonic: Boolean
        get() = tectonicModId != null

    val deeperOceansModId: String? by lazy {
        ModList.get().mods.firstOrNull { it.modId.contains("deeper_oceans", ignoreCase = true) }?.modId
    }

    val hasDeeperOceans: Boolean
        get() = deeperOceansModId != null

    val terralithModId: String? by lazy {
        ModList.get().mods.firstOrNull { it.modId.contains("terralith", ignoreCase = true) }?.modId
    }

    val hasTerralith: Boolean
        get() = terralithModId != null

    val hasTerrainMod: Boolean get() = hasTectonic || hasDeeperOceans || hasTerralith
}