package fr.heta__h.squ_abyssal_bloom.util.worldgen

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Climate
import net.minecraft.world.level.levelgen.RandomState
import java.util.Collections
import java.util.IdentityHashMap
import java.util.WeakHashMap

/**
 * Dimension information is not carried by NoiseChunk or by a climate sampler.
 * Keep the exact worldgen objects owned by the Overworld so mixins can reject
 * identical generator types used by other dimensions.
 */
object AbyssalWorldgenScope {
    private val randomStates: MutableSet<RandomState> =
        Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap<RandomState, Boolean>()))
    private val samplers: MutableMap<Climate.Sampler, Boolean> =
        Collections.synchronizedMap(WeakHashMap())

    fun register(level: ServerLevel) {
        if (level.dimension() != Level.OVERWORLD) return
        val randomState = level.chunkSource.randomState()
        randomStates.add(randomState)
        registerSampler(randomState.sampler())
    }

    fun unregister(level: ServerLevel) {
        val randomState = level.chunkSource.randomState()
        randomStates.remove(randomState)
        samplers.remove(randomState.sampler())
    }

    fun isOverworld(randomState: RandomState): Boolean = randomStates.contains(randomState)

    fun registerSampler(sampler: Climate.Sampler) {
        samplers[sampler] = true
    }

    fun isOverworld(sampler: Climate.Sampler): Boolean = samplers.containsKey(sampler)

    fun clear() {
        randomStates.clear()
        samplers.clear()
    }
}
