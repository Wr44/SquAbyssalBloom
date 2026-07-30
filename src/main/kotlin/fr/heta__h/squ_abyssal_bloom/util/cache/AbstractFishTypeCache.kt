package fr.heta__h.squ_abyssal_bloom.util.cache

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.heta__h.squ_abyssal_bloom.SquAbyssalBloom
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.DefaultAttributes
import net.minecraft.world.entity.animal.fish.AbstractFish
import net.minecraft.world.level.Level
import net.neoforged.fml.loading.FMLPaths
import java.io.File

object AbstractFishTypeCache {

    private val gson = Gson()
    private val cacheFile: File = FMLPaths.CONFIGDIR.get().resolve("squ_abyssal_bloom_abstract_fish_types.json").toFile()
    private val cache = mutableMapOf<String, Boolean>()
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        if (!cacheFile.exists()) return
        try {
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            gson.fromJson<Map<String, Boolean>>(cacheFile.readText(), type)?.let(cache::putAll)
        } catch (e: Exception) {
            SquAbyssalBloom.LOGGER.warn("Failed to load abstract fish type cache", e)
        }
    }

    fun isAbstractFish(entityType: EntityType<*>): Boolean {
        val id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString()
        cache[id]?.let { return it }

        val level = Minecraft.getInstance().level ?: return false

        val result = classify(entityType, level)
        save()
        return result
    }

    private fun classify(entityType: EntityType<*>, level: Level): Boolean {
        val id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString()

        @Suppress("UNCHECKED_CAST")
        val livingType = entityType as EntityType<LivingEntity>
        val result = livingType.create(level, EntitySpawnReason.LOAD) is AbstractFish
        cache[id] = result
        return result
    }

    fun classifyAll() {
        val level = Minecraft.getInstance().level ?: return
        var newlyClassified = 0
        BuiltInRegistries.ENTITY_TYPE.forEach { entityType ->
            if (!DefaultAttributes.hasSupplier(entityType)) return@forEach
            val id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString()
            if (cache.containsKey(id)) return@forEach
            try {
                classify(entityType, level)
                newlyClassified++
            } catch (e: Exception) {
                SquAbyssalBloom.LOGGER.warn("Failed to classify entity type {} for abstract fish cache", id, e)
            }
        }
        if (newlyClassified > 0) {
            save()
        }
    }

    private fun save() {
        try {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(gson.toJson(cache))
        } catch (e: Exception) {
            SquAbyssalBloom.LOGGER.warn("Failed to save abstract fish type cache", e)
        }
    }
}
