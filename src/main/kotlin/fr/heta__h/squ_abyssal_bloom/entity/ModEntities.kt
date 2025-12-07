package fr.heta__h.squ_abyssal_bloom.entity

import fr.heta__h.squ_abyssal_bloom.Squ_abyssal_bloom
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleModel
import fr.heta__h.squ_abyssal_bloom.entity.client.barnacle.BarnacleRenderer
import fr.heta__h.squ_abyssal_bloom.entity.custom.barnacle.BarnacleEntity
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.registries.DeferredRegister

object ModEntities {
    val ENTITY_TYPES: DeferredRegister<EntityType<*>> =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Squ_abyssal_bloom.ID)


    val BARNACLE_KEY: ResourceKey<EntityType<*>> =
        ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("barnacle"))


    val BARNACLE = ENTITY_TYPES.register("barnacle") { _: ResourceLocation ->
        EntityType.Builder.of({ type, level -> BarnacleEntity(type, level) }, MobCategory.MONSTER)
            .sized(2f, 1.25f)
            .clientTrackingRange(8)
            .updateInterval(3)
            .build(BARNACLE_KEY)
    }

    fun registerEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(BARNACLE.get() as EntityType<out BarnacleEntity>, ::BarnacleRenderer)
    }

    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(BarnacleModel.LAYER_LOCATION) {
            BarnacleModel.createBodyLayer()
        }
    }

    fun onRegisterAttributes(event: EntityAttributeCreationEvent) {
        event.put(BARNACLE.get(), BarnacleEntity.createAttributes().build())
    }
}