package fr.heta__h.squ_abyssal_bloom.entity.ai.stealth

import net.minecraft.world.entity.Mob
import java.util.concurrent.CopyOnWriteArrayList

object StealthRetargetRegistry {
    private val providers = CopyOnWriteArrayList<StealthRetargetProvider>()

    fun register(provider: StealthRetargetProvider): Boolean = providers.addIfAbsent(provider)

    fun find(mob: Mob): StealthRetargetProvider? = providers.firstOrNull { it.appliesTo(mob) }
}
