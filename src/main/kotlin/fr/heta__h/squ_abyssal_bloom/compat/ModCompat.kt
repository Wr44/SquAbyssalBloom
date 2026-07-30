package fr.heta__h.squ_abyssal_bloom.compat

import net.neoforged.fml.ModList

object ModCompat {

    val hasDynLights: Boolean by lazy {
        ModList.get().mods.any { it.modId.contains("lambdynlights", ignoreCase = true) }
    }
}