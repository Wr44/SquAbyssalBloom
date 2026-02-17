package fr.heta__h.squ_abyssal_bloom.item

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.ModEntities
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.world.item.Item
import net.minecraft.world.item.SpawnEggItem
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister


object ModItems {
    val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(Squ_abyssal_bloom.ID)

    val BARNACLE_SPAWN_EGG: DeferredItem<Item> = ITEMS.registerItem(
        "barnacle_spawn_egg"
    ) { properties -> SpawnEggItem(properties.spawnEgg(ModEntities.BARNACLE.get())) }

    val BARNACLE_TOOTH: DeferredItem<Item> = ITEMS.registerItem(
        "barnacle_tooth"
    ) { properties -> Item(properties) }

    val GUARDIAN_EYE: DeferredItem<Item> = ITEMS.registerItem(
        "guardian_eye"
    ) { properties -> Item(properties) }

    fun register(eventBus: IEventBus) {
        ITEMS.register(eventBus)
    }
}